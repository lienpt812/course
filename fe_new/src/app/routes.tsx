import { createBrowserRouter } from "react-router";
import { RootLayout } from "./components/RootLayout";
import { HomePage } from "./pages/HomePage";
import { CoursesPage } from "./pages/CoursesPage";
import { CourseDetailPage } from "./pages/CourseDetailPage";
import { LoginPage } from "./pages/LoginPage";
import { RegisterPage } from "./pages/RegisterPage";
import { StudentDashboard } from "./pages/StudentDashboard";
import { InstructorDashboard } from "./pages/InstructorDashboard";
import { AdminDashboard } from "./pages/AdminDashboard";
import { LearningPage } from "./pages/LearningPage";
import { ProfilePage } from "./pages/ProfilePage";
import { NotFoundPage } from "./pages/NotFoundPage";

export const router = createBrowserRouter([
  {
    path: "/",
    Component: RootLayout,
    children: [
      { index: true, Component: HomePage },
      { path: "login", Component: LoginPage },
      { path: "register", Component: RegisterPage },
      { path: "courses", Component: CoursesPage },
      { path: "courses/:courseId", Component: CourseDetailPage },
      { path: "student/dashboard", Component: StudentDashboard },
      { path: "instructor/dashboard", Component: InstructorDashboard },
      { path: "admin/dashboard", Component: AdminDashboard },
      { path: "learn/:courseId", Component: LearningPage },
      { path: "profile", Component: ProfilePage },
      { path: "*", Component: NotFoundPage },
    ],
  },
]);
