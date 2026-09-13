import type { ReactNode } from 'react'
import { cn } from '../../lib/cn'

export type BadgeTone = 'brand' | 'warm' | 'good' | 'bad' | 'neutral'

const TONES: Record<BadgeTone, string> = {
  brand: 'bg-brand/12 text-brand',
  warm: 'bg-warm/14 text-warm',
  good: 'bg-good/12 text-good',
  bad: 'bg-bad/12 text-bad',
  neutral: 'bg-surface-2 text-ink-soft',
}

interface BadgeProps {
  children: ReactNode
  tone?: BadgeTone
  className?: string
}

export function Badge({ children, tone = 'neutral', className }: BadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 whitespace-nowrap rounded-full px-2.5 py-1 text-xs font-medium',
        TONES[tone],
        className,
      )}
    >
      {children}
    </span>
  )
}
