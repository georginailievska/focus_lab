import { api } from './client'
import type {
  Mentor,
  MentorNote,
  Overlap,
  Session,
  SessionApplication,
  SessionNote,
  StudentComment,
} from '../types'

export function getMySessions() {
  return api.get<Session[]>('/mentor/sessions').then((res) => res.data)
}

export function getPendingRequests() {
  return api.get<SessionApplication[]>('/mentor/requests').then((res) => res.data)
}

/** Другите одобрени ментори — за избор на ко-ментор. */
export function getColleagues() {
  return api.get<Mentor[]>('/mentor/colleagues').then((res) => res.data)
}

// Коментари на менторите за студент; видливоста ја бира авторот
export function getStudentComments(studentId: number) {
  return api
    .get<StudentComment[]>(`/mentor/students/${studentId}/comments`)
    .then((res) => res.data)
}

export function addStudentComment(studentId: number, text: string, sharedWithMentors: boolean) {
  return api
    .post<StudentComment>(`/mentor/students/${studentId}/comments`, { text, sharedWithMentors })
    .then((res) => res.data)
}

export function changeCommentVisibility(commentId: number, sharedWithMentors: boolean) {
  return api
    .patch<StudentComment>(`/mentor/student-comments/${commentId}`, null, {
      params: { sharedWithMentors },
    })
    .then((res) => res.data)
}

export function deleteStudentComment(commentId: number) {
  return api.delete(`/mentor/student-comments/${commentId}`).then(() => undefined)
}

// Пријавите на една сесија — само за менторите што ја водат
export function getSessionApplications(sessionId: number) {
  return api
    .get<SessionApplication[]>(`/mentor/sessions/${sessionId}/applications`)
    .then((res) => res.data)
}

// Кои сесии паѓаат во периодот што се избира во формата
export function getOverlaps(params: {
  startTime: string
  endTime: string
  sessionId?: number
}) {
  return api.get<Overlap[]>('/mentor/overlaps', { params }).then((res) => res.data)
}

// Сите забелешки од сите сесии — за табот „Забелешки"
export function getAllNotes(subjectId?: number) {
  return api
    .get<MentorNote[]>('/mentor/notes', { params: subjectId ? { subjectId } : undefined })
    .then((res) => res.data)
}

// Забелешките се под /api/mentor, каде SecurityConfig не пушта друга улога
export function getSessionNotes(sessionId: number) {
  return api.get<SessionNote[]>(`/mentor/sessions/${sessionId}/notes`).then((res) => res.data)
}

export function addSessionNote(sessionId: number, text: string) {
  return api
    .post<SessionNote>(`/mentor/sessions/${sessionId}/notes`, { text })
    .then((res) => res.data)
}

export function deleteSessionNote(noteId: number) {
  return api.delete(`/mentor/notes/${noteId}`).then(() => undefined)
}
