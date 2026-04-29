package com.eduplatform.mbt.journey;

import com.eduplatform.mbt.impl.AuthImpl;
import com.eduplatform.mbt.support.GraphWalkerExecutionPolicy;
import org.graphwalker.java.annotation.GraphWalker;

/**
 * Deterministic shortcut: Guest to login to student dashboard (a-star to goal vertex).
 * Requires seed + guards {@code gwGuard_loginCredentialsOk} as in full {@link AuthImpl}.
 */
@GraphWalker(
        value = GraphWalkerExecutionPolicy.JOURNEY_AUTH_LOGIN_STUDENT,
        start = "v_GuestOnCourses")
public class AuthStudentLoginJourneyImpl extends AuthImpl {}
