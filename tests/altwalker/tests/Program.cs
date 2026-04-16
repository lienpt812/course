using Altom.AltWalker;
using CourseRegistration.Tests;
using CourseRegistration.Tests.Models;

namespace CourseRegistration.Tests;

public class Program
{
    public static void Main(string[] args)
    {
        var service = new ExecutorService();

        // Global setup/teardown
        service.RegisterSetup<Setup>();

        // Register all model implementations
        service.RegisterModel<Test_Auth>();
        service.RegisterModel<Test_Explorecourse>();
        service.RegisterModel<Registration_Course>();
        service.RegisterModel<Admin_Approval_Management>();
        service.RegisterModel<Create_Course>();
        service.RegisterModel<Content_Management>();
        service.RegisterModel<Update_Learning_Progress>();
        service.RegisterModel<Certification>();
        service.RegisterModel<Student_Dashboard>();
        service.RegisterModel<Notification>();
        service.RegisterModel<Instructor_Dashboard>();
        service.RegisterModel<Course_Status_Lifecycle>();
        service.RegisterModel<Learning_Progress_Strict>();
        service.RegisterModel<Section_Lesson_Management>();
        service.RegisterModel<Waitlist_AutoPromote>();

        // Start the executor service
        service.Run(args);
    }
}
