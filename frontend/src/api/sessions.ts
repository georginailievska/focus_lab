import { api } from './client'
import type { DecisionRequest, Session, SessionApplication, SessionRequest } from '../types'

export function getSessions(subjectId?: number) {
  return api
    .get<Session[]>('/sessions', { params: subjectId ? { subjectId } : undefined })
    .then((res) => res.data)
}

export function getSessionsInRange(from: string, to: string, subjectId?: number) {
  return api
    .get<Session[]>('/sessions', {
      params: { from, to, ...(subjectId ? { subjectId } : {}) },
    })
    .then((res) => res.data)
}

export function getSession(id: number) {
  return api.get<Session>(`/sessions/${id}`).then((res) => res.data)
}

export function getMyApplications() {
  return api.get<SessionApplication[]>('/sessions/my-applications').then((res) => res.data)
}

export function createSession(payload: SessionRequest) {
  return api.post<Session>('/sessions', payload).then((res) => res.data)
}

/** Уредување на сесија — смее само ментор што ја води. */
export function updateSession(sessionId: number, payload: SessionRequest) {
  return api.put<Session>(`/sessions/${sessionId}`, payload).then((res) => res.data)
}

export function deleteSession(sessionId: number) {
  return api.delete<void>(`/sessions/${sessionId}`).then((res) => res.data)
}

export function applyToSession(sessionId: number) {
  return api.post<SessionApplication>(`/sessions/${sessionId}/apply`).then((res) => res.data)
}

export function cancelApplication(sessionId: number) {
  return api.delete<void>(`/sessions/${sessionId}/apply`)
}

export function decideApplication(applicationId: number, payload: DecisionRequest) {
  return api
    .patch<SessionApplication>(`/sessions/applications/${applicationId}`, payload)
    .then((res) => res.data)
}
