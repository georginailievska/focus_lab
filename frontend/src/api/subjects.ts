import { api } from './client'
import type { Subject } from '../types'

export function getSubjects() {
  return api.get<Subject[]>('/subjects').then((res) => res.data)
}
