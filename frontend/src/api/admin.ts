import { api } from './client'
import type { AdminStats, Subject, SubjectRequest, User } from '../types'

export function getStats() {
  return api.get<AdminStats>('/admin/stats').then((res) => res.data)
}

export function getPendingMentors() {
  return api.get<User[]>('/admin/mentors/pending').then((res) => res.data)
}

export function approveMentor(userId: number) {
  return api.patch<User>(`/admin/mentors/${userId}/approve`).then((res) => res.data)
}

export function rejectMentor(userId: number) {
  return api.patch<User>(`/admin/mentors/${userId}/reject`).then((res) => res.data)
}

export function getAdminSubjects() {
  return api.get<Subject[]>('/admin/subjects').then((res) => res.data)
}

export function createSubject(payload: SubjectRequest) {
  return api.post<Subject>('/admin/subjects', payload).then((res) => res.data)
}
