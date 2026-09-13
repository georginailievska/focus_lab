import { api } from './client'
import type { InterestsRequest, Subject } from '../types'

export function getMyInterests() {
  return api.get<Subject[]>('/interests').then((res) => res.data)
}

/** Се праќа целата листа, не поединечни промени — види InterestsRequest на backend. */
export function updateMyInterests(payload: InterestsRequest) {
  return api.put<Subject[]>('/interests', payload).then((res) => res.data)
}
