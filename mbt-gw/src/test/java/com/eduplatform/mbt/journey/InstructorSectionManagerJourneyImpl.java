package com.eduplatform.mbt.journey;

import com.eduplatform.mbt.impl.InstructorContentImpl;
import com.eduplatform.mbt.support.GraphWalkerExecutionPolicy;
import org.graphwalker.java.annotation.GraphWalker;

/**
 * Shortest path to section manager. Requires at least one owned course
 * ({@code gwGuard_ownedCourseExists}).
 */
@GraphWalker(
        value = GraphWalkerExecutionPolicy.JOURNEY_INSTRUCTOR_SECTION_MANAGER,
        start = "v_InstructorDashboard")
public class InstructorSectionManagerJourneyImpl extends InstructorContentImpl {}
