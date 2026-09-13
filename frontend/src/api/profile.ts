import { api } from './client'
import type { ChangePasswordRequest, Profile, UpdateProfileRequest } from '../types'

export function getMyProfile() {
  return api.get<Profile>('/me').then((res) => res.data)
}

/** Профил на друг корисник — без email и без интереси, така го праќа серверот. */
export function getUserProfile(userId: number) {
  return api.get<Profile>(`/users/${userId}`).then((res) => res.data)
}

export function updateMyProfile(payload: UpdateProfileRequest) {
  return api.patch<Profile>('/me', payload).then((res) => res.data)
}

export function uploadMyAvatar(file: File) {
  const form = new FormData()
  form.append('file', file)

  return api.post<Profile>('/me/avatar', form).then((res) => res.data)
}

export function removeMyAvatar() {
  return api.delete<Profile>('/me/avatar').then((res) => res.data)
}

export function changeMyPassword(payload: ChangePasswordRequest) {
  return api.post<void>('/me/password', payload).then((res) => res.data)
}
