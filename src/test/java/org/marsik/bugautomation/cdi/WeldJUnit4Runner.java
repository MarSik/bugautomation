package org.marsik.bugautomation.cdi;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

public class WeldJUnit4Runner extends BlockJUnit4ClassRunner {

    private final Weld weld;
    private final WeldContainer container;

    public WeldJUnit4Runner(final Class<Object> klass) throws InitializationError {
        super(klass);
        this.weld = new Weld();
        this.container = weld.initialize();
    }

    @Override
    protected Object createTest() throws Exception {
        final Object test = container.instance()
            .select(getTestClass().getJavaClass()).get();

        return test;
    }
}
