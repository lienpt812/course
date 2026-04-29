package com.eduplatform.mbt.runners;

import com.eduplatform.mbt.journey.AuthStudentLoginJourneyImpl;
import com.eduplatform.mbt.journey.CertificateIssuedJourneyImpl;
import com.eduplatform.mbt.journey.CourseExplorationDetailJourneyImpl;
import com.eduplatform.mbt.journey.InstructorSectionManagerJourneyImpl;
import com.eduplatform.mbt.journey.LearningPageJourneyImpl;
import com.eduplatform.mbt.journey.RegistrationPendingJourneyImpl;
import org.graphwalker.core.machine.ExecutionContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Optional deterministic a-star happy path smokes. Run the failsafe class alone via Maven
 * with property {@code it.test} pointing at this class.
 *
 * <p>These complement bounded random runs ({@code edge_coverage} with a {@code length} cap) in module runners.
 * Some journeys need matching seed (e.g. PENDING registration, owned course, CONFIRMED learn).
 */
@Tag("critical-journey")
public class CriticalJourneysRunner extends AbstractGraphWalkerRunner {

    @BeforeAll
    static void setUpTestSuite() {
        if (System.getProperty("mbt.seed.skipPendingRequirement") == null) {
            System.setProperty("mbt.seed.skipPendingRequirement", "true");
        }
        initSuiteBeforeModel();
    }

    @Override
    protected Class<? extends ExecutionContext> contextClass() {
        return AuthStudentLoginJourneyImpl.class;
    }

    @Test
    void journey_Auth_StudentLogin() {
        runModelFor(AuthStudentLoginJourneyImpl.class);
    }

    @Test
    void journey_Course_ToDetail() {
        runModelFor(CourseExplorationDetailJourneyImpl.class);
    }

    @Test
    void journey_Instructor_SectionManager() {
        runModelFor(InstructorSectionManagerJourneyImpl.class);
    }

    @Test
    void journey_Learning_PageLoaded() {
        runModelFor(LearningPageJourneyImpl.class);
    }

    @Test
    void journey_Certificate_Issued() {
        runModelFor(CertificateIssuedJourneyImpl.class);
    }

    @Test
    void journey_Registration_Pending() {
        runModelFor(RegistrationPendingJourneyImpl.class);
    }
}
