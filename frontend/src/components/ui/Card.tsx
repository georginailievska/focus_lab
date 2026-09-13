import type { ReactNode } from 'react'
import { cn } from '../../lib/cn'

interface CardProps {
  children: ReactNode
  className?: string
  /** Додава hover состојба — за картички што се кликаат (линк или копче). */
  interactive?: boolean
  padding?: 'none' | 'sm' | 'md' | 'lg'
}

const PADDING = {
  none: '',
  sm: 'px-3.5 py-2.5',
  md: 'px-4 py-3.5',
  lg: 'p-6',
} as const

export function Card({ children, className, interactive = false, padding = 'md' }: CardProps) {
  return (
    <div
      className={cn(
        'rounded-2xl border border-line bg-surface shadow-soft',
        PADDING[padding],
        interactive && 'transition hover:-translate-y-0.5 hover:border-brand/40 hover:shadow-lift',
        className,
      )}
    >
      {children}
    </div>
  )
}
