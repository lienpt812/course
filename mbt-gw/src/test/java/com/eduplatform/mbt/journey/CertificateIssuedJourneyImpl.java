package com.eduplatform.mbt.journey;

import com.eduplatform.mbt.impl.CertificateImpl;
import com.eduplatform.mbt.support.GraphWalkerExecutionPolicy;
import org.graphwalker.java.annotation.GraphWalker;

@GraphWalker(
        value = GraphWalkerExecutionPolicy.JOURNEY_CERTIFICATE_ISSUED,
        start = "v_AllLessonsCompleted")
public class CertificateIssuedJourneyImpl extends CertificateImpl {}
