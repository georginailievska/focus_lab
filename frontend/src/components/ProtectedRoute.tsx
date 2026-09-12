import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { homeRouteFor } from '../lib/routes'
import type { Role } from '../types'

interface ProtectedRouteProps {
  allow: Role[]
  children: ReactNode
}

export default function ProtectedRoute({ allow, children }: ProtectedRouteProps) {
  const { user, loading } = useAuth()

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-page text-ink-mute">
        Се вчитува...
      </div>
    )
  }

  if (!user) {
    return <Navigate to="/login" replace />
  }

  if (!allow.includes(user.role)) {
    return <Navigate to={homeRouteFor(user.role)} replace />
  }

  return <>{children}</>
}
