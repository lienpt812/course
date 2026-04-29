package com.eduplatform.mbt.journey;

import com.eduplatform.mbt.impl.RegistrationImpl;
import com.eduplatform.mbt.support.GraphWalkerExecutionPolicy;
import org.graphwalker.java.annotation.GraphWalker;

@GraphWalker(
        value = GraphWalkerExecutionPolicy.JOURNEY_REG_PENDING,
        start = "v_CourseDetailUnregistered")
public class RegistrationPendingJourneyImpl extends RegistrationImpl {}
