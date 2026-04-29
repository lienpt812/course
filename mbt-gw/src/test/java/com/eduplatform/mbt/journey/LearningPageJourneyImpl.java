package com.eduplatform.mbt.journey;

import com.eduplatform.mbt.impl.LearningImpl;
import com.eduplatform.mbt.support.GraphWalkerExecutionPolicy;
import org.graphwalker.java.annotation.GraphWalker;

/** Dashboard → /learn (confirmed registration). */
@GraphWalker(
        value = GraphWalkerExecutionPolicy.JOURNEY_LEARNING_PAGE,
        start = "v_StudentDashboardEntry")
public class LearningPageJourneyImpl extends LearningImpl {}
