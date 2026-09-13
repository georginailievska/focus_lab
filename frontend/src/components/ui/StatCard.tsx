import { cn } from '../../lib/cn'
import { Card } from './Card'

type StatTone = 'brand' | 'warm' | 'good' | 'neutral'

interface StatCardProps {
  label: string
  value: number | string
  tone?: StatTone
  /** Мал знак што му дава лице на бројката. */
  emoji?: string
}

const CHIP: Record<StatTone, string> = {
  brand: 'bg-brand/12 text-brand ring-1 ring-inset ring-brand/20',
  warm: 'bg-warm/12 text-warm ring-1 ring-inset ring-warm/20',
  good: 'bg-good/12 text-good ring-1 ring-inset ring-good/20',
  neutral: 'bg-surface-2 text-ink-soft ring-1 ring-inset ring-line',
}

/** Бројката ја носи бојата на тонот — така картичките не се четири сиви квадрати. */
const VALUE: Record<StatTone, string> = {
  brand: 'text-brand',
  warm: 'text-warm',
  good: 'text-good',
  neutral: 'text-ink',
}

/** Иста картичка се користеше во Mentor и во Admin Dashboard — сега е една. */
export function StatCard({ label, value, tone = 'neutral', emoji }: StatCardProps) {
  return (
    <Card padding="none" className="px-4 py-4">
      {emoji && (
        <span
          aria-hidden="true"
          className={cn('mb-3 grid h-9 w-9 place-items-center rounded-xl text-base', CHIP[tone])}
        >
          {emoji}
        </span>
      )}

      <p className={cn('font-display text-[26px] font-bold leading-none', VALUE[tone])}>{value}</p>
      <p className="mt-2 text-xs font-medium text-ink-mute">{label}</p>
    </Card>
  )
}
