package net.staro.event.bus.api;

/**
 * An event that can be canceled by listeners.
 */
public class CancellableEvent
{
    private boolean cancelled;

    public CancellableEvent() {
        cancelled = false;
    }

    public boolean isCancelled()
    {
        return cancelled;
    }

    public void setCancelled(boolean cancelled)
    {
        this.cancelled = cancelled;
    }

}
