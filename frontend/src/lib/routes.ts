import type { Role } from '../types'

export function homeRouteFor(role: Role): string {
  switch (role) {
    case 'ADMIN':
      return '/admin'
    case 'MENTOR':
      return '/mentor'
    default:
      return '/'
  }
}
