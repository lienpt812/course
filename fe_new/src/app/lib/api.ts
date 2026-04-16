const API_BASE_URL = '/api/v1';

export type UserRole = 'GUEST' | 'STUDENT' | 'INSTRUCTOR' | 'ADMIN';

export interface AuthUser {
  id: number;
  name: string;
  email: string;
  phone_number?: string | null;
  role: UserRole;
  status?: string;
  avatar_url?: string | null;
  bio?: string | null;
  interests?: string | null;
  education?: string | null;
  expertise?: string | null;
  learning_goal?: string | null;
  student_major?: string | null;
}

export interface CourseItem {
  id: number;
  title: string;
  slug: string;
  description: string;
  image_url?: string | null;
  instructor_id: number;
  instructor_name?: string | null;
  instructor_email?: string | null;
  category?: string | null;
  max_capacity: number;
  estimated_hours?: number;
  status: string;
  level: string;
  price?: number;
  certificate_enabled?: boolean;
}

export interface CourseDetail extends CourseItem {
  confirmed_slots?: number;
  remaining_slots?: number;
  registration_open_at?: string | null;
  registration_close_at?: string | null;
}

export interface RegistrationItem {
  id: number;
  user_id: number;
  course_id: number;
  status: string;
  waitlist_position?: number | null;
  created_at: string;
  updated_at: string;
}

export interface LearningSectionItem {
  id: number;
  course_id: number;
  title: string;
  position: number;
}

export interface LearningLessonItem {
  id: number;
  section_id: number;
  title: string;
  type: 'VIDEO' | 'QUIZ' | 'DOC' | 'TEXT';
  content_url?: string | null;
  is_preview: boolean;
  position: number;
  duration_minutes: number;
}

export interface LearningOutlineItem {
  section: LearningSectionItem;
  lessons: LearningLessonItem[];
}

interface ApiError {
  code?: string;
  message?: string;
}

interface ApiEnvelope<T> {
  data: T;
  meta: Record<string, unknown>;
  errors: ApiError[];
}

function clearAuthSession(): void {
  localStorage.removeItem('access_token');
  localStorage.removeItem('refresh_token');
  localStorage.removeItem('auth_user');
}

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = localStorage.getItem('refresh_token');
  if (!refreshToken) return null;

  try {
    const refreshed = await request<{ access_token: string; refresh_token: string; token_type: string }>(
      '/auth/refresh',
      {
        method: 'POST',
        body: JSON.stringify({ refresh_token: refreshToken }),
      },
      false
    );

    localStorage.setItem('access_token', refreshed.access_token);
    localStorage.setItem('refresh_token', refreshed.refresh_token);
    return refreshed.access_token;
  } catch {
    clearAuthSession();
    return null;
  }
}

function extractErrorMessage(payload: any): string {
  if (payload?.errors?.[0]?.message) return payload.errors[0].message;
  if (typeof payload?.detail === 'string') return payload.detail;
  return 'Request failed';
}

async function request<T>(path: string, init?: RequestInit, withAuthRetry: boolean = true): Promise<T> {
  const token = localStorage.getItem('access_token');
  const headers = new Headers(init?.headers ?? {});

  if (!headers.has('Content-Type') && init?.body) {
    headers.set('Content-Type', 'application/json');
  }
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers,
  });

  let payload: ApiEnvelope<T> | null = null;
  try {
    payload = (await response.json()) as ApiEnvelope<T>;
  } catch {
    payload = null;
  }

  if (!response.ok) {
    if (response.status === 401 && withAuthRetry && !path.startsWith('/auth/')) {
      const newToken = await refreshAccessToken();
      if (newToken) {
        const retryHeaders = new Headers(init?.headers ?? {});
        if (!retryHeaders.has('Content-Type') && init?.body) {
          retryHeaders.set('Content-Type', 'application/json');
        }
        retryHeaders.set('Authorization', `Bearer ${newToken}`);

        const retryResponse = await fetch(`${API_BASE_URL}${path}`, {
          ...init,
          headers: retryHeaders,
        });

        const retryPayload = (await retryResponse.json().catch(() => null)) as ApiEnvelope<T> | any;
        if (retryResponse.ok) {
          return retryPayload?.data as T;
        }
        throw new Error(extractErrorMessage(retryPayload));
      }
    }

    throw new Error(extractErrorMessage(payload));
  }

  return payload?.data as T;
}

export const authApi = {
  async login(email: string, password: string) {
    return request<{ access_token: string; refresh_token: string; token_type: string }>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });
  },
  async register(payload: {
    name: string;
    email: string;
    password: string;
    role: UserRole;
    phone?: string;
    education?: string;
    expertise?: string;
    learning_goal?: string;
    student_major?: string;
  }) {
    return request<{ access_token: string; refresh_token: string; token_type: string; user: AuthUser }>('/auth/register', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },
  async refresh(refreshToken: string) {
    return request<{ access_token: string; refresh_token: string; token_type: string }>('/auth/refresh', {
      method: 'POST',
      body: JSON.stringify({ refresh_token: refreshToken }),
    });
  },
  async me() {
    return request<AuthUser>('/auth/me');
  },
  async forgotPassword(email: string) {
    return request<{ message: string; reset_token?: string }>('/auth/forgot-password', {
      method: 'POST',
      body: JSON.stringify({ email }),
    });
  },
  async resetPassword(token: string, new_password: string) {
    return request<{ password_reset: boolean }>('/auth/reset-password', {
      method: 'POST',
      body: JSON.stringify({ token, new_password }),
    });
  },
  async updateMe(payload: {
    name?: string;
    phone_number?: string;
    avatar_url?: string;
    bio?: string;
    interests?: string;
    education?: string;
    expertise?: string;
    learning_goal?: string;
    student_major?: string;
  }) {
    return request<AuthUser>('/auth/me', {
      method: 'PATCH',
      body: JSON.stringify(payload),
    });
  },
};

export const courseApi = {
  async list() {
    return request<CourseItem[]>('/courses');
  },
  async detail(courseId: number) {
    return request<CourseDetail>(`/courses/${courseId}`);
  },
  async create(payload: {
    title: string;
    slug: string;
    description: string;
    image_url?: string;
    category?: string;
    max_capacity: number;
    estimated_hours?: number;
    level: string;
    status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED' | 'COMING_SOON';
    price?: number;
    prerequisites?: string;
    registration_open_at?: string;
    registration_close_at?: string;
    certificate_enabled?: boolean;
  }) {
    return request<CourseDetail>('/courses', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },
  async update(courseId: number, payload: {
    title?: string;
    description?: string;
    image_url?: string;
    category?: string;
    max_capacity?: number;
    estimated_hours?: number;
    level?: string;
    status?: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED' | 'COMING_SOON';
    price?: number;
    prerequisites?: string;
    registration_open_at?: string;
    registration_close_at?: string;
    certificate_enabled?: boolean;
  }) {
    return request<CourseDetail>(`/courses/${courseId}`, {
      method: 'PATCH',
      body: JSON.stringify(payload),
    });
  },
};

export const registrationApi = {
  async list() {
    return request<RegistrationItem[]>('/registrations');
  },
  async create(courseId: number) {
    return request<RegistrationItem>('/registrations', {
      method: 'POST',
      body: JSON.stringify({ course_id: courseId }),
    });
  },
  async approve(registrationId: number, reason?: string) {
    return request<RegistrationItem>(`/registrations/${registrationId}/approve`, {
      method: 'POST',
      body: JSON.stringify({ reason: reason ?? null }),
    });
  },
  async reject(registrationId: number, reason?: string) {
    return request<RegistrationItem>(`/registrations/${registrationId}/reject`, {
      method: 'POST',
      body: JSON.stringify({ reason: reason ?? null }),
    });
  },
  async bulkApprove(payload: { course_id?: number; reason?: string }) {
    return request<{
      approved_count: number;
      rejected_count: number;
      message: string;
      rejected_items: Array<{
        registration_id: number;
        user_id: number;
        user_name?: string | null;
        user_email?: string | null;
        course_id: number;
        course_title?: string | null;
        reason: string;
      }>;
    }>(
      '/registrations/bulk-approve',
      {
        method: 'POST',
        body: JSON.stringify(payload),
      }
    );
  },
};

export const dashboardApi = {
  async student() {
    return request<{ current_courses: number; registration_history: number }>('/dashboards/student');
  },
  async instructor() {
    return request<{
      total_courses: number;
      total_students: number;
      total_registrations: number;
      courses: Array<{
        id: number;
        title: string;
        status: string;
        current_participants: number;
        max_participants: number;
        estimated_hours: number;
        instructor_name: string;
      }>;
    }>('/dashboards/instructor');
  },
  async admin() {
    return request<{ total_courses: number; total_users: number; pending_registrations: number }>('/dashboards/admin');
  },
};

export const learningApi = {
  async createSection(payload: { course_id: number; title: string; position: number }) {
    return request<LearningSectionItem>('/learning/sections', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },
  async createLesson(payload: {
    section_id: number;
    title: string;
    type: 'VIDEO' | 'QUIZ' | 'DOC' | 'TEXT';
    content_url?: string;
    is_preview?: boolean;
    position: number;
    duration_minutes: number;
  }) {
    return request<LearningLessonItem>('/learning/lessons', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },
  async outline(courseId: number) {
    return request<LearningOutlineItem[]>(`/learning/courses/${courseId}/outline`);
  },
  async progress(courseId: number) {
    return request<{ total_lessons: number; completed_lessons: number; completion_pct: number }>(
      `/learning/courses/${courseId}/progress`
    );
  },
  async progressDetail(courseId: number) {
    return request<{ completed_lesson_ids: number[]; completion_by_lesson: Record<string, number> }>(
      `/learning/courses/${courseId}/progress-detail`
    );
  },
  async upsertProgress(payload: { lesson_id: number; completion_pct: number }) {
    return request<{ id: number; lesson_id: number; completion_pct: number; completed: boolean; certificate_issued?: boolean; certificate_id?: number }>(
      '/learning/progress',
      {
        method: 'POST',
        body: JSON.stringify(payload),
      }
    );
  },
  async myCertificates() {
    return request<Array<{
      id: number;
      course_id: number;
      verification_code: string;
      issued_at: string;
      pdf_url?: string;
    }>>('/certificates/me');
  },
  async issueCertificate(courseId: number) {
    return request<{
      id: number;
      course_id: number;
      verification_code: string;
      issued_at: string;
      pdf_url?: string;
    }>(`/certificates/issue/${courseId}`, { method: 'POST' });
  },
};

export const adminApi = {
  async seed() {
    return request<{ seeded: boolean }>('/admin/seed', { method: 'POST' });
  },
  async expirePending() {
    return request<{ expired: number }>('/admin/jobs/expire-pending', { method: 'POST' });
  },
};
