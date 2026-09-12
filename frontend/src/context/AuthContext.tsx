import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import * as authApi from '../api/auth'
import { clearToken, getToken, setToken } from '../api/client'
import type { AuthResponse, LoginRequest, MentorStatus, RegisterRequest, Role } from '../types'

interface AuthUser {
  id: number
  fullName: string
  email: string
  role: Role
  mentorStatus: MentorStatus | null
  avatarUrl: string | null
}

interface AuthContextValue {
  user: AuthUser | null
  loading: boolean
  login: (payload: LoginRequest) => Promise<AuthUser>
  register: (payload: RegisterRequest) => Promise<AuthUser>
  updateUser: (patch: Partial<Omit<AuthUser, 'id' | 'role'>>) => void
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)
const USER_KEY = 'focuslab_user'

function toAuthUser(res: AuthResponse): AuthUser {
  return {
    id: res.id,
    fullName: res.fullName,
    email: res.email,
    role: res.role,
    mentorStatus: res.mentorStatus,
    avatarUrl: res.avatarUrl,
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const token = getToken()
    const storedUser = localStorage.getItem(USER_KEY)
    if (token && storedUser) {
      try {
        setUser(JSON.parse(storedUser) as AuthUser)
      } catch {
        clearToken()
        localStorage.removeItem(USER_KEY)
      }
    }
    setLoading(false)
  }, [])

  function persist(res: AuthResponse): AuthUser {
    const authUser = toAuthUser(res)
    setToken(res.token)
    localStorage.setItem(USER_KEY, JSON.stringify(authUser))
    setUser(authUser)
    return authUser
  }

  async function login(payload: LoginRequest) {
    const res = await authApi.login(payload)
    return persist(res)
  }

  async function register(payload: RegisterRequest) {
    const res = await authApi.register(payload)
    return persist(res)
  }

  function updateUser(patch: Partial<Omit<AuthUser, 'id' | 'role'>>) {
    setUser((current) => {
      if (!current) {
        return current
      }

      const next = { ...current, ...patch }
      localStorage.setItem(USER_KEY, JSON.stringify(next))
      return next
    })
  }

  function logout() {
    clearToken()
    localStorage.removeItem(USER_KEY)
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, register, updateUser, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth мора да се користи внатре во <AuthProvider>')
  }
  return ctx
}
