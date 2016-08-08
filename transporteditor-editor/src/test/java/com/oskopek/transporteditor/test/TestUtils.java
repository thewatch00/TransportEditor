package com.oskopek.transporteditor.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Various methods for easier testing. Util methods missing in JUnit, Mockito, etc.
 */
public final class TestUtils {

    private static final Logger logger = LoggerFactory.getLogger(TestUtils.class);

    /**
     * Util class, hide the default constructor.
     */
    private TestUtils() {
        // intentionally empty
    }

    /**
     * Checks if an instance (could be a subclass) of a given {@link Throwable} was thrown.
     * If not, returns false and logs the exception at the {@code debug} level. Does not rethrow.
     *
     * @param runnable the code to run (runs in the same thread as this method)
     * @param throwable the throwable to check for
     * @return true iff a {@link Throwable} that is an instance of the given {@code throwable} was thrown
     */
    public static boolean isThrown(Runnable runnable, Class<? extends Throwable> throwable) {
        try {
            runnable.run();
        } catch (Throwable t) {
            logger.debug("isThrown expecting {}, caught {}", throwable, t.getClass());
            if (throwable.isInstance(t)) {
                return true;
            }
            logger.debug("Throwable detail: {}", t.toString());
        }
        return false;
    }

}