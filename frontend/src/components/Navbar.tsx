import { NavLink, useNavigate } from 'react-router-dom'
import { Avatar } from './Avatar'
import { BrandMark } from './BrandMark'
import { ThemeToggle } from './ThemeToggle'
import { useAuth } from '../context/AuthContext'
import { cn } from '../lib/cn'
import { roleLabel } from '../lib/labels'
import type { Role } from '../types'

interface NavItem {
  to: string
  label: string
  end?: boolean
}

const LINKS: Record<Role, NavItem[]> = {
  STUDENT: [
    { to: '/', label: 'Дома', end: true },
    { to: '/sessions', label: 'Сесии' },
    { to: '/calendar', label: 'Календар' },
    { to: '/feed', label: 'Постови' },
    { to: '/interests', label: 'Интереси' },
  ],
  MENTOR: [
    { to: '/mentor', label: 'Преглед', end: true },
    { to: '/calendar', label: 'Календар' },
    { to: '/feed', label: 'Постови' },
    { to: '/mentor/notes', label: 'Забелешки' },
    { to: '/mentor/create-session', label: 'Нова сесија' },
  ],
  ADMIN: [
    { to: '/admin', label: 'Преглед', end: true },
    { to: '/calendar', label: 'Календар' },
    { to: '/feed', label: 'Постови' },
  ],
}

function navLinkClass({ isActive }: { isActive: boolean }): string {
  return cn(
    'rounded-xl px-3 py-1.5 text-sm font-medium whitespace-nowrap transition',
    'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/60',
    isActive ? 'bg-brand/12 text-brand' : 'text-ink-soft hover:bg-surface-2 hover:text-ink',
  )
}

export default function Navbar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  if (!user) {
    return null
  }

  const links = LINKS[user.role]

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    // sticky + backdrop: навигацијата останува достапна при скролање
    <header className="sticky top-0 z-10 border-b border-line bg-page/90 shadow-soft backdrop-blur">
      <div className="mx-auto max-w-5xl px-4">
        <div className="flex h-16 items-center justify-between gap-3">
          <BrandMark />

          {/* На широк екран линковите се во средината... */}
          <nav className="hidden items-center gap-1 sm:flex">
            {links.map((link) => (
              <NavLink key={link.to} to={link.to} end={link.end} className={navLinkClass}>
                {link.label}
              </NavLink>
            ))}
          </nav>

          <div className="flex min-w-0 items-center gap-1.5">
            <ThemeToggle />

            <NavLink
              to="/profile"
              title="Мојот профил"
              className={({ isActive }) =>
                cn(
                  'flex min-w-0 items-center gap-2 rounded-full py-1 pl-1 pr-1 transition sm:pr-3',
                  'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/60',
                  isActive ? 'bg-brand/12' : 'hover:bg-surface-2',
                )
              }
            >
              <Avatar name={user.fullName} src={user.avatarUrl} size="sm" />
              <span className="hidden min-w-0 sm:block">
                <span className="block truncate text-sm font-medium text-ink">{user.fullName}</span>
                <span className="block text-xs text-ink-mute">{roleLabel(user.role)}</span>
              </span>
            </NavLink>

            <button
              onClick={handleLogout}
              title="Одјави се"
              aria-label="Одјави се"
              className="grid h-9 w-9 place-items-center rounded-full text-ink-mute transition hover:bg-bad/10 hover:text-bad"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" className="h-5 w-5">
                <path strokeLinecap="round" strokeLinejoin="round" d="M15 17l5-5-5-5M20 12H9M12 4H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h6" />
              </svg>
            </button>
          </div>
        </div>

        {/* ...а на телефон под името, во ред што може да се лизга */}
        <nav className="-mx-1 flex items-center gap-1 overflow-x-auto pb-2.5 sm:hidden">
          {links.map((link) => (
            <NavLink key={link.to} to={link.to} end={link.end} className={navLinkClass}>
              {link.label}
            </NavLink>
          ))}
        </nav>
      </div>
    </header>
  )
}
