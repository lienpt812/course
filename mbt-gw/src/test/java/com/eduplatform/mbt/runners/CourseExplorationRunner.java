package com.eduplatform.mbt.runners;

import com.eduplatform.mbt.impl.CourseExplorationImpl;
import org.graphwalker.core.machine.ExecutionContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Runs Module B - Course exploration. */
public class CourseExplorationRunner extends AbstractGraphWalkerRunner {

    @BeforeAll
    static void setUpTestSuite() {
        System.setProperty("mbt.seed.skipPendingRequirement", "true");
        initSuiteBeforeModel();
    }

    @Override protected Class<? extends ExecutionContext> contextClass() { return CourseExplorationImpl.class; }
    @Test void runCourseExplorationModel() { runModel(); }
}
