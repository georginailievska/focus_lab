import { api } from './client'
import type { Mentor, Session, SessionApplication } from '../types'

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
