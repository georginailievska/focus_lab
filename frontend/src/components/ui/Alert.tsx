import type { ReactNode } from 'react'
import { cn } from '../../lib/cn'

type AlertTone = 'error' | 'warning' | 'info' | 'success'

const TONES: Record<AlertTone, string> = {
  error: 'border-bad/25 bg-bad/8 text-bad',
  warning: 'border-warm/25 bg-warm/10 text-warm',
  info: 'border-brand/25 bg-brand/8 text-brand',
  success: 'border-good/25 bg-good/8 text-good',
}

interface AlertProps {
  children: ReactNode
  tone?: AlertTone
  className?: string
}

export function Alert({ children, tone = 'error', className }: AlertProps) {
  return (
    <div
      role={tone === 'error' ? 'alert' : 'status'}
      className={cn('animate-pop-in rounded-xl border px-4 py-3 text-sm', TONES[tone], className)}
    >
      {children}
    </div>
  )
}
