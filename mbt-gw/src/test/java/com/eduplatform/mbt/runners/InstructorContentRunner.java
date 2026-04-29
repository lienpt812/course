package com.eduplatform.mbt.runners;

import com.eduplatform.mbt.impl.InstructorContentImpl;
import org.graphwalker.core.machine.ExecutionContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Runs Module F - Instructor content management. */
public class InstructorContentRunner extends AbstractGraphWalkerRunner {

    @BeforeAll
    static void setUpTestSuite() {
        initSuiteBeforeModel();
    }

    @Override protected Class<? extends ExecutionContext> contextClass() { return InstructorContentImpl.class; }
    @Test void runInstructorContentModel() { runModel(); }
}
