import { api } from './client'
import type {
  AuthResponse,
  ForgotPasswordRequest,
  LoginRequest,
  RegisterRequest,
  ResetPasswordRequest,
} from '../types'

export function register(payload: RegisterRequest) {
  return api.post<AuthResponse>('/auth/register', payload).then((res) => res.data)
}

export function login(payload: LoginRequest) {
  return api.post<AuthResponse>('/auth/login', payload).then((res) => res.data)
}

export function forgotPassword(payload: ForgotPasswordRequest) {
  return api.post<void>('/auth/forgot-password', payload).then((res) => res.data)
}

export function resetPassword(payload: ResetPasswordRequest) {
  return api.post<void>('/auth/reset-password', payload).then((res) => res.data)
}
