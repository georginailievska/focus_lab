import { api } from './client'
import type { Mentor, Session, SessionApplication, SessionNote } from '../types'

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
