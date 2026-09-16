
export type Role = 'STUDENT' | 'MENTOR' | 'ADMIN'
export type MentorStatus = 'PENDING' | 'APPROVED' | 'REJECTED'
export type ApplicationStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED'
export type SessionMode = 'ONLINE' | 'IN_PERSON'

// ---- Auth ------------------------------------------------------------

export interface RegisterRequest {
  fullName: string
  email: string
  password: string
  role: Extract<Role, 'STUDENT' | 'MENTOR'>
}

export interface LoginRequest {
  email: string
  password: string
}

export interface AuthResponse {
  token: string
  id: number
  fullName: string
  email: string
  role: Role
  mentorStatus: MentorStatus | null
  avatarUrl: string | null
}

export interface ForgotPasswordRequest {
  email: string
}

export interface ResetPasswordRequest {
  token: string
  newPassword: string
}

// ---- User (mk.focuslab.dto.UserResponse) ------------------------------

export interface User {
  id: number
  fullName: string
  email: string
  role: Role
  mentorStatus: MentorStatus | null
  createdAt: string
  /** Патека до профилната слика (`/api/avatars/...`), или null ако нема качена. */
  avatarUrl: string | null
}

export interface Mentor {
  id: number
  fullName: string
  avatarUrl: string | null
}

// ---- Профил (mk.focuslab.dto.ProfileResponse) --------------------------

export interface ProfileStats {
  sessions: number
  upcomingSessions: number
  applications: number
  acceptedApplications: number
}

export interface Profile {
  id: number
  fullName: string
  email: string | null
  role: Role
  mentorStatus: MentorStatus | null
  createdAt: string
  avatarUrl: string | null
  interests: Subject[] | null
  stats: ProfileStats
}

export interface UpdateProfileRequest {
  fullName: string
}

export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
}

// ---- Subject -----------------------------------------------------------

export interface Subject {
  id: number
  name: string
}

export interface SubjectRequest {
  name: string
}

/** Целата листа интереси на студентот (mk.focuslab.dto.InterestsRequest). */
export interface InterestsRequest {
  subjectIds: number[]
}

// ---- Session -------------------------------------------------------------

export interface SessionRequest {
  title: string
  description?: string
  subjectId: number
  mentorIds: number[]
  mode: SessionMode
  location?: string
  startTime: string // ISO без временска зона, пр. "2026-12-01T14:00:00"
  endTime: string
  tags?: string[]
}

export interface Session {
  id: number
  title: string
  description: string | null
  subject: Subject
  mentors: Mentor[]
  mode: SessionMode
  location: string | null
  startTime: string
  endTime: string
  applicantsCount: number
  maxApplicants: number
  approvedCount: number
  maxApproved: number
  tags: string[]
}

// ---- SessionApplication ---------------------------------------------------

export interface SessionApplication {
  id: number
  student: User
  sessionId: number
  sessionTitle: string
  sessionStartTime: string
  status: ApplicationStatus
  appliedAt: string
  decidedAt: string | null
}

export interface DecisionRequest {
  status: Extract<ApplicationStatus, 'ACCEPTED' | 'REJECTED'>
}

// ---- Admin -----------------------------------------------------------------

export interface AdminStats {
  totalUsers: number
  students: number
  mentors: number
  activeSessions: number
}

// ---- Лента (mk.focuslab.dto.Post*) ----------------------------------------

export interface Author {
  id: number
  fullName: string
  role: Role
  avatarUrl: string | null
}

export interface Attachment {
  id: number
  /** Патека со случаен клуч (`/api/attachments/...`). */
  url: string
  filename: string
  contentType: string
  sizeBytes: number
  /** Сликите се прикажуваат во лентата; останатите се симнуваат. */
  image: boolean
}

export interface PostComment {
  id: number
  author: Author
  text: string
  createdAt: string
}

export interface StudentComment {
  id: number
  author: Author
  text: string
  /** false значи што само авторот го гледа коментарот. */
  sharedWithMentors: boolean
  createdAt: string
}

export interface Overlap {
  sessionId: number
  title: string
  startTime: string
  endTime: string
  subject: Subject
  mentors: Mentor[]
  /** Менторот што прашува е меѓу менторите на таа сесија. */
  mine: boolean
}

export interface MentorNote extends SessionNote {
  sessionId: number
  sessionTitle: string
  sessionStartTime: string
  subject: Subject
}

export interface SessionNote {
  id: number
  author: Author
  text: string
  createdAt: string
}

// ---- Известувања (mk.focuslab.dto.NotificationResponse) --------------------

export type NotificationType =
  | 'NEW_SESSION'
  | 'APPLICATION_RECEIVED'
  | 'NEW_APPLICATION'
  | 'APPLICATION_ACCEPTED'
  | 'APPLICATION_REJECTED'
  | 'SESSION_UPDATED'
  | 'SESSION_CANCELLED'

export interface Notification {
  id: number
  type: NotificationType
  title: string
  body: string | null
  /** null кога сесијата повеќе не постои. */
  sessionId: number | null
  read: boolean
  createdAt: string
}

export interface Post {
  id: number
  author: Author
  subject: Subject | null
  text: string
  attachments: Attachment[]
  comments: PostComment[]
  createdAt: string
}

// ---- Грешки (mk.focuslab.exception.ErrorResponse) --------------------------

export interface ApiError {
  timestamp: string
  status: number
  error: string
  message: string
  fieldErrors?: Record<string, string> | null
}
