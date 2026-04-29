package com.eduplatform.mbt.runners;

import com.eduplatform.mbt.impl.AdminManagementImpl;
import org.graphwalker.core.machine.ExecutionContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Runs Module G - Admin management (users, seed, jobs). */
public class AdminManagementRunner extends AbstractGraphWalkerRunner {

    @BeforeAll
    static void setUpTestSuite() {
        if (System.getProperty("mbt.seed.skipPendingRequirement") == null) {
            System.setProperty("mbt.seed.skipPendingRequirement", "true");
        }
        initSuiteBeforeModel();
    }

    @Override protected Class<? extends ExecutionContext> contextClass() { return AdminManagementImpl.class; }
    @Test void runAdminManagementModel() { runModel(); }
}
