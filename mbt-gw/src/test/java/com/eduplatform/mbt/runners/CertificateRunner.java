package com.eduplatform.mbt.runners;

import com.eduplatform.mbt.impl.CertificateImpl;
import org.graphwalker.core.machine.ExecutionContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Runs Module E - Certificate issuance & verification. */
public class CertificateRunner extends AbstractGraphWalkerRunner {

    @BeforeAll
    static void setUpTestSuite() {
        initSuiteBeforeModel();
    }

    @Override protected Class<? extends ExecutionContext> contextClass() { return CertificateImpl.class; }
    @Test void runCertificateModel() { runModel(); }
}
