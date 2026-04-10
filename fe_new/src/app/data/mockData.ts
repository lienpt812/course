export type UserRole = 'GUEST' | 'STUDENT' | 'INSTRUCTOR' | 'ADMIN';
export type CourseStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED' | 'COMING_SOON';
export type CourseLevel = 'Beginner' | 'Intermediate' | 'Advanced';
export type RegistrationStatus = 'PENDING' | 'CONFIRMED' | 'WAITLIST' | 'CANCELLED' | 'REJECTED' | 'EXPIRED';

export interface Course {
  id: string;
  title: string;
  slug: string;
  description: string;
  category: string;
  instructorId: string;
  instructorName: string;
  maxCapacity: number;
  currentEnrolled: number;
  level: CourseLevel;
  status: CourseStatus;
  price: number;
  duration: number; // hours
  rating: number;
  reviewCount: number;
  imageUrl: string;
  startDate: string;
  endDate: string;
  registrationDeadline: string;
  certificateEnabled: boolean;
}

export interface Registration {
  id: string;
  userId: string;
  courseId: string;
  status: RegistrationStatus;
  waitlistPosition?: number;
  createdAt: string;
  updatedAt: string;
  cancelReason?: string;
}

export interface User {
  id: string;
  name: string;
  email: string;
  role: UserRole;
  avatar?: string;
}

// Current logged-in user (can be changed to test different roles)
export let currentUser: User = {
  id: 'user-1',
  name: 'Nguyễn Văn An',
  email: 'nguyenvanan@example.com',
  role: 'STUDENT',
};

export function setCurrentUser(user: User) {
  currentUser = user;
}

// Mock courses
export const courses: Course[] = [
  {
    id: 'course-1',
    title: 'Lập Trình React & TypeScript Từ Cơ Bản Đến Nâng Cao',
    slug: 'lap-trinh-react-typescript',
    description: 'Khóa học toàn diện về React và TypeScript, từ những kiến thức cơ bản đến các kỹ thuật nâng cao. Bạn sẽ học cách xây dựng ứng dụng web hiện đại, quản lý state, làm việc với API, và deploy production.',
    category: 'Lập Trình Web',
    instructorId: 'instructor-1',
    instructorName: 'Trần Minh Tuấn',
    maxCapacity: 30,
    currentEnrolled: 27,
    level: 'Intermediate',
    status: 'PUBLISHED',
    price: 2500000,
    duration: 40,
    rating: 4.8,
    reviewCount: 124,
    imageUrl: 'https://images.unsplash.com/photo-1633356122544-f134324a6cee?w=800&q=80',
    startDate: '2026-05-01',
    endDate: '2026-07-31',
    registrationDeadline: '2026-04-25',
    certificateEnabled: true,
  },
  {
    id: 'course-2',
    title: 'UI/UX Design: Thiết Kế Trải Nghiệm Người Dùng',
    slug: 'ui-ux-design-thiet-ke-trai-nghiem',
    description: 'Học cách thiết kế giao diện và trải nghiệm người dùng chuyên nghiệp với Figma. Khóa học bao gồm design thinking, user research, wireframing, prototyping và design system.',
    category: 'Thiết Kế',
    instructorId: 'instructor-2',
    instructorName: 'Lê Thị Hương',
    maxCapacity: 25,
    currentEnrolled: 25,
    level: 'Beginner',
    status: 'PUBLISHED',
    price: 1800000,
    duration: 32,
    rating: 4.9,
    reviewCount: 89,
    imageUrl: 'https://images.unsplash.com/photo-1561070791-2526d30994b5?w=800&q=80',
    startDate: '2026-04-20',
    endDate: '2026-06-30',
    registrationDeadline: '2026-04-15',
    certificateEnabled: true,
  },
  {
    id: 'course-3',
    title: 'Data Science & Machine Learning với Python',
    slug: 'data-science-machine-learning-python',
    description: 'Khóa học chuyên sâu về khoa học dữ liệu và machine learning. Sử dụng Python, pandas, scikit-learn để phân tích dữ liệu và xây dựng mô hình dự đoán.',
    category: 'Data Science',
    instructorId: 'instructor-3',
    instructorName: 'Phạm Đức Anh',
    maxCapacity: 20,
    currentEnrolled: 18,
    level: 'Advanced',
    status: 'PUBLISHED',
    price: 3200000,
    duration: 50,
    rating: 4.7,
    reviewCount: 67,
    imageUrl: 'https://images.unsplash.com/photo-1551288049-bebda4e38f71?w=800&q=80',
    startDate: '2026-05-10',
    endDate: '2026-08-15',
    registrationDeadline: '2026-05-01',
    certificateEnabled: true,
  },
  {
    id: 'course-4',
    title: 'Digital Marketing: Chiến Lược & Thực Hành',
    slug: 'digital-marketing-chien-luoc',
    description: 'Học các chiến lược marketing số hiệu quả: SEO, SEM, Social Media Marketing, Content Marketing, Email Marketing và Analytics.',
    category: 'Marketing',
    instructorId: 'instructor-1',
    instructorName: 'Trần Minh Tuấn',
    maxCapacity: 35,
    currentEnrolled: 22,
    level: 'Beginner',
    status: 'PUBLISHED',
    price: 1500000,
    duration: 28,
    rating: 4.6,
    reviewCount: 145,
    imageUrl: 'https://images.unsplash.com/photo-1460925895917-afdab827c52f?w=800&q=80',
    startDate: '2026-04-25',
    endDate: '2026-06-20',
    registrationDeadline: '2026-04-20',
    certificateEnabled: true,
  },
  {
    id: 'course-5',
    title: 'AWS Cloud Architecture & DevOps',
    slug: 'aws-cloud-devops',
    description: 'Làm chủ AWS cloud services và DevOps practices. Học cách deploy, scale và maintain applications trên AWS infrastructure.',
    category: 'Cloud & DevOps',
    instructorId: 'instructor-3',
    instructorName: 'Phạm Đức Anh',
    maxCapacity: 15,
    currentEnrolled: 12,
    level: 'Advanced',
    status: 'PUBLISHED',
    price: 3500000,
    duration: 45,
    rating: 4.9,
    reviewCount: 52,
    imageUrl: 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=800&q=80',
    startDate: '2026-05-15',
    endDate: '2026-08-01',
    registrationDeadline: '2026-05-05',
    certificateEnabled: true,
  },
  {
    id: 'course-6',
    title: 'Mobile App Development với Flutter',
    slug: 'mobile-app-flutter',
    description: 'Xây dựng ứng dụng mobile đa nền tảng với Flutter. Học Dart, state management, API integration và publish app lên store.',
    category: 'Mobile Development',
    instructorId: 'instructor-2',
    instructorName: 'Lê Thị Hương',
    maxCapacity: 28,
    currentEnrolled: 15,
    level: 'Intermediate',
    status: 'PUBLISHED',
    price: 2200000,
    duration: 38,
    rating: 4.7,
    reviewCount: 78,
    imageUrl: 'https://images.unsplash.com/photo-1512941937669-90a1b58e7e9c?w=800&q=80',
    startDate: '2026-06-01',
    endDate: '2026-08-20',
    registrationDeadline: '2026-05-25',
    certificateEnabled: true,
  },
];

// Mock registrations
export const registrations: Registration[] = [
  {
    id: 'reg-1',
    userId: 'user-1',
    courseId: 'course-1',
    status: 'CONFIRMED',
    createdAt: '2026-04-01T10:00:00Z',
    updatedAt: '2026-04-02T14:30:00Z',
  },
  {
    id: 'reg-2',
    userId: 'user-1',
    courseId: 'course-2',
    status: 'WAITLIST',
    waitlistPosition: 3,
    createdAt: '2026-04-05T09:00:00Z',
    updatedAt: '2026-04-05T09:00:00Z',
  },
  {
    id: 'reg-3',
    userId: 'user-1',
    courseId: 'course-4',
    status: 'PENDING',
    createdAt: '2026-04-08T11:20:00Z',
    updatedAt: '2026-04-08T11:20:00Z',
  },
];

// Helper functions
export function getCourseById(id: string): Course | undefined {
  return courses.find(c => c.id === id);
}

export function getUserRegistrations(userId: string): Registration[] {
  return registrations.filter(r => r.userId === userId);
}

export function getCourseRegistration(userId: string, courseId: string): Registration | undefined {
  return registrations.find(r => r.userId === userId && r.courseId === courseId);
}

export function hasActiveRegistration(userId: string, courseId: string): boolean {
  const reg = getCourseRegistration(userId, courseId);
  return reg ? ['PENDING', 'CONFIRMED', 'WAITLIST'].includes(reg.status) : false;
}

export function getAvailableSlots(courseId: string): number {
  const course = getCourseById(courseId);
  return course ? course.maxCapacity - course.currentEnrolled : 0;
}

export function formatPrice(price: number): string {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
}

export function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleDateString('vi-VN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
}
