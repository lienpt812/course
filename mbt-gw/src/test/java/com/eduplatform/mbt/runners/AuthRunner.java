package com.eduplatform.mbt.runners;

import com.eduplatform.mbt.impl.AuthImpl;
import org.graphwalker.core.machine.ExecutionContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Runs Module A - Authentication & authorization.
 *
 * <p>Path cap is {@link com.eduplatform.mbt.support.GraphWalkerExecutionPolicy#BOUNDED_AUTH} (length 200) on
 * {@link com.eduplatform.mbt.impl.AuthImpl}.
 */
public class AuthRunner extends AbstractGraphWalkerRunner {

    @BeforeAll
    static void setUpTestSuite() {
        // Auth model has no PENDING/registration vertices; seeder PENDING is only for other modules.
        if (System.getProperty("mbt.seed.skipPendingRequirement") == null) {
            System.setProperty("mbt.seed.skipPendingRequirement", "true");
        }
        initSuiteBeforeModel();
    }

    @Override protected Class<? extends ExecutionContext> contextClass() { return AuthImpl.class; }
    @Test void runAuthModel() { runModel(); }
}
