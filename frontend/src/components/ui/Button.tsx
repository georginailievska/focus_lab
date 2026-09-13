import type { ButtonHTMLAttributes, ReactNode } from 'react'
import { cn } from '../../lib/cn'

export type ButtonVariant = 'primary' | 'secondary' | 'good' | 'bad' | 'ghost'
export type ButtonSize = 'sm' | 'md' | 'lg'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant
  size?: ButtonSize
  children: ReactNode
}

const BASE =
  'inline-flex items-center justify-center gap-2 rounded-xl font-semibold transition ' +
  'active:scale-[0.98] ' +
  'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/70 ' +
  'focus-visible:ring-offset-2 focus-visible:ring-offset-page ' +
  'disabled:pointer-events-none disabled:opacity-45 disabled:shadow-none'

const VARIANTS: Record<ButtonVariant, string> = {
  primary: 'bg-brand text-brand-on shadow-brand ring-1 ring-inset ring-white/15 hover:opacity-90 hover:shadow-lift',
  secondary: 'border border-line bg-surface text-ink shadow-soft hover:border-ink-mute/40 hover:bg-surface-2',
  good: 'bg-good/10 text-good ring-1 ring-inset ring-good/40 shadow-soft hover:bg-good/[0.18]',
  bad: 'bg-bad/10 text-bad ring-1 ring-inset ring-bad/40 shadow-soft hover:bg-bad/[0.18]',
  ghost: 'text-ink-soft hover:bg-surface-2 hover:text-ink',
}

const SIZES: Record<ButtonSize, string> = {
  sm: 'px-3 py-1.5 text-xs',
  md: 'px-4 py-2 text-sm',
  lg: 'px-5 py-2.5 text-sm',
}

export function Button({
  variant = 'primary',
  size = 'md',
  className,
  type = 'button',
  children,
  ...rest
}: ButtonProps) {
  return (
    <button type={type} className={cn(BASE, VARIANTS[variant], SIZES[size], className)} {...rest}>
      {children}
    </button>
  )
}
