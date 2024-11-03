package net.staro.event.bus.api;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;

/**
 * The event bus for registering and distributing events.
 * @see EventListener
 */
public class EventBus
{
    /**
     * A hashmap is used for fast event lookup. Listeners are put into a list based on their event class.
     */
    private final Map<Class<?>, List<EventListener>> listeners = new HashMap<>();
    /**
     * Weak references subscribed objects to the event types they are listening to.
     * This allows an automatic unsubscription when the object is garbage collected.
     */
    private final Map<Object, List<Class<?>>> subscriptions = new HashMap<>();
    /**
     * Stores factory functions for creating listeners based on their annotations.
     */
    private final Map<Class<? extends Annotation>, BiFunction<Object, Method, EventListener>> listenerFactories = new HashMap<>();

    public EventBus()
    {
        registerListenerFactory(Listener.class, (instance, method) -> new EventListener(instance, method, method.getAnnotation(Listener.class).priority().getVal()));
    }

    /**
     * Registers a factory function for creating a specific type of listener based on its annotation.
     *
     * @param annotationType The annotation class associated with the listener type.
     * @param factory        The function that creates the listener instance given the object and method.
     */
    public void registerListenerFactory(Class<? extends Annotation> annotationType, BiFunction<Object, Method, EventListener> factory)
    {
        listenerFactories.put(annotationType, factory);
    }

    /**
     * Posts (dispatches) an event to all registered listeners.
     *
     * @param event The event object.
     */
    public void post(Object event)
    {
        List<EventListener> listeners = this.listeners.get(event.getClass());
        if (listeners == null)
        {
            return;
        }

        for (EventListener l : listeners)
        {
            if (l.getInstance() == null)
            {
                return;
            }

            Class<?> eventParamType = l.getMethod().getParameterTypes()[0];
            if (eventParamType.isAssignableFrom(event.getClass()))
            {
                l.invoke(event);
            }
        }
    }

    /**
     * Registers a listener instance to the event bus.
     *
     * @param instance is the listener to subscribe (register).
     */
    public void subscribe(Object instance)
    {
        addListeners(getListeningMethods(instance.getClass()), instance);
    }

    /**
     * Unregisters a listener instance from the event bus, so the events are not being dispatched to it.
     *
     * @param instance is the listener to unsubscribe (unregister).
     */
    public void unsubscribe(Object instance)
    {
        removeListeners(getListeningMethods(instance.getClass()), instance);
        subscriptions.remove(instance);
    }

    /**
     * Checks if a listener instance is subscribed (registered) to the event bus.
     *
     * @param instance The instance to check.
     * @return True if the instance is subscribed, false otherwise.
     */
    public boolean isSubscribed(Object instance)
    {
        return subscriptions.containsKey(instance);
    }

    /**
     * Turns methods we know are listeners into listener objects.
     *
     * @param methods  the methods we want to turn into listeners.
     * @param instance the method's class' instance (null if methods are static).
     */
    private void addListeners(List<Method> methods, Object instance)
    {
        List<Class<?>> subscribedEvents = subscriptions.computeIfAbsent(instance, k -> new ArrayList<>());
        for (Method method : methods)
        {
            Class<?> eventType = getEventParameterType(method);
            listeners.putIfAbsent(eventType, new CopyOnWriteArrayList<>());
            List<EventListener> list = listeners.get(eventType);
            for (Annotation annotation : method.getAnnotations())
            {
                if (listenerFactories.containsKey(annotation.annotationType()))
                {
                    BiFunction<Object, Method, EventListener> factory = listenerFactories.get(annotation.annotationType());
                    EventListener listener = factory.apply(instance, method);
                    list.add(listener);
                    list.sort(Comparator.comparingInt(EventListener::getPriority).reversed());
                    subscribedEvents.add(eventType);
                    break;
                }
            }
        }
    }

    /**
     * Removes Listeners by looping over their respective lists.
     *
     * @param methods  the methods we want to remove.
     * @param instance method's class' instance (null if methods are static).
     */
    private void removeListeners(List<Method> methods, Object instance)
    {
        for (Method method : methods)
        {
            Class<?> eventType = getEventParameterType(method);
            List<EventListener> list = listeners.get(eventType);
            if (list == null)
            {
                continue;
            }

            list.removeIf(l -> l.getMethod().equals(method) && l.getInstance() == instance);
        }
    }

    private List<Method> getListeningMethods(Class<?> clazz)
    {
        ArrayList<Method> listening = new ArrayList<>();
        for (Method method : clazz.getDeclaredMethods())
        {
            for (Annotation annotation : method.getDeclaredAnnotations())
            {
                if (listenerFactories.containsKey(annotation.annotationType()) && method.getParameterCount() == 1)
                {
                    listening.add(method);
                }
            }
        }

        return listening;
    }

    private static Class<?> getEventParameterType(Method method)
    {
        if (method.getParameterCount() != 1)
        {
            return null;
        }

        return method.getParameters()[0].getType();
    }

}
