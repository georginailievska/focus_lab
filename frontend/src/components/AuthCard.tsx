import type { ReactNode } from 'react'
import { BrandMark } from './BrandMark'
import { ThemeToggle } from './ThemeToggle'

interface AuthCardProps {
  subtitle: string
  children: ReactNode
  /** Линкот на дното („Немаш профил? Регистрирај се"). */
  footer: ReactNode
}

export function AuthCard({ subtitle, children, footer }: AuthCardProps) {
  return (
    <div className="relative flex min-h-screen items-center justify-center bg-page px-4 py-10">
      <div aria-hidden="true" className="pointer-events-none absolute inset-0 overflow-hidden">
        <div className="absolute -left-24 -top-24 h-72 w-72 rounded-full bg-brand/25 blur-3xl" />
        <div className="absolute -bottom-32 -right-16 h-80 w-80 rounded-full bg-accent/20 blur-3xl" />
      </div>

      <div className="absolute right-4 top-4">
        <ThemeToggle />
      </div>

      <div className="relative w-full max-w-sm animate-pop-in">
        <div className="rounded-3xl border border-line bg-surface p-8 shadow-lift">
          <BrandMark size="lg" markOnly />

          <h1 className="mt-4 font-display text-2xl font-semibold tracking-tight text-ink">
            FOCUS <span className="text-brand">Lab</span>
          </h1>
          <p className="mt-1 text-sm text-ink-mute">{subtitle}</p>

          {children}

          <p className="mt-6 text-center text-sm text-ink-mute">{footer}</p>
        </div>
      </div>
    </div>
  )
}
