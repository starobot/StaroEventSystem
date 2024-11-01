package boe.jiden.test.dummies.listener;

import boe.jiden.test.dummies.DummyListener;
import boe.jiden.test.dummies.event.TestDummyEvent;

/**
 * This should do nothing since DummyGenericListener has an empty {@code invoke(Object event)} method.
 */
public class TestDummyListener
{
    @DummyListener
    public void invoke(TestDummyEvent ignored)
    {

    }
}
