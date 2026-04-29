package com.eduplatform.mbt.runners;

import com.eduplatform.mbt.impl.RegistrationImpl;
import org.graphwalker.core.machine.ExecutionContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Runs Module C - Course registration & approval flow. */
public class RegistrationRunner extends AbstractGraphWalkerRunner {

    @BeforeAll
    static void setUpTestSuite() {
        if (System.getProperty("mbt.seed.skipPendingRequirement") == null) {
            System.setProperty("mbt.seed.skipPendingRequirement", "true");
        }
        initSuiteBeforeModel();
    }

    @Override protected Class<? extends ExecutionContext> contextClass() { return RegistrationImpl.class; }
    @Test void runRegistrationModel() { runModel(); }
}
