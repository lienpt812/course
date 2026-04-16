using Altom.AltWalker;
using Lms.MbtWeb;
using Lms.MbtWeb.Models;

var service = new ExecutorService();

service.RegisterSetup<Setup>();

service.RegisterModel<Auth>();
service.RegisterModel<CourseCatalog>();
service.RegisterModel<CourseRegistration>();
service.RegisterModel<StudentDashboard>();
service.RegisterModel<InstructorDashboard>();
service.RegisterModel<AdminDashboard>();
service.RegisterModel<LearningPage>();
service.RegisterModel<ProfilePage>();

service.Run(args);
