package com.eduplatform.mbt.journey;

import com.eduplatform.mbt.impl.CourseExplorationImpl;
import com.eduplatform.mbt.support.GraphWalkerExecutionPolicy;
import org.graphwalker.java.annotation.GraphWalker;

/** List view → first course detail. */
@GraphWalker(
        value = GraphWalkerExecutionPolicy.JOURNEY_COURSE_TO_DETAIL,
        start = "v_CourseListVisible")
public class CourseExplorationDetailJourneyImpl extends CourseExplorationImpl {}
