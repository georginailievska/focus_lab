import { api } from './client'
import type { Notification } from '../types'

export function getNotifications() {
  return api.get<Notification[]>('/notifications').then((res) => res.data)
}

/** Само бројот — ѕвончето го прашува периодично, списокот само кога се отвора. */
export function getUnreadCount() {
  return api
    .get<{ count: number }>('/notifications/unread-count')
    .then((res) => res.data.count)
}

export function markRead(id: number) {
  return api.post(`/notifications/${id}/read`).then(() => undefined)
}

export function markAllRead() {
  return api.post('/notifications/read-all').then(() => undefined)
}
