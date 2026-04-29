package com.eduplatform.mbt.runners;

import com.eduplatform.mbt.impl.LearningImpl;
import org.graphwalker.core.machine.ExecutionContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Runs Module D - Learning & progress. */
public class LearningRunner extends AbstractGraphWalkerRunner {

    @BeforeAll
    static void setUpTestSuite() {
        initSuiteBeforeModel();
    }

    @Override protected Class<? extends ExecutionContext> contextClass() { return LearningImpl.class; }
    @Test void runLearningModel() { runModel(); }
}
