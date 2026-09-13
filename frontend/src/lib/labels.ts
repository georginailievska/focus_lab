import type { MentorStatus, Role, SessionMode } from '../types'

export function sessionModeLabel(mode: SessionMode): string {
  return mode === 'ONLINE' ? 'Online' : 'Во живо'
}

export function roleLabel(role: Role): string {
  switch (role) {
    case 'ADMIN':
      return 'Администратор'
    case 'MENTOR':
      return 'Ментор'
    default:
      return 'Студент'
  }
}

export function mentorStatusLabel(status: MentorStatus | null): string {
  switch (status) {
    case 'APPROVED':
      return 'Одобрен/а'
    case 'REJECTED':
      return 'Одбиен/а'
    case 'PENDING':
      return 'Чека одобрување'
    default:
      return '—'
  }
}
