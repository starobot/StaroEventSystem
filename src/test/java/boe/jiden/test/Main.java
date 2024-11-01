package boe.jiden.test;

import boe.jiden.test.dummies.DummyEventBus;
import boe.jiden.test.dummies.listener.TestDummyListener;
import boe.jiden.test.event.TestEvent;

public class Main
{
    public static void main(String[] args)
    {
        final DummyEventBus dummyEventBus = new DummyEventBus();
        dummyEventBus.subscribe(new TestDummyListener());
        var timer = System.currentTimeMillis();
        for (int i = 0; i <= 1000000; i++)
        {
            dummyEventBus.post(new TestEvent());
        }

        System.out.println(System.currentTimeMillis() - timer);
    }
}
