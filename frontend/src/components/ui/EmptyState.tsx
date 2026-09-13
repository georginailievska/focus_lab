import type { ReactNode } from 'react'

interface EmptyStateProps {
  title: string
  description?: string
  /** Пр. копче „Прегледај сесии" — што може корисникот да направи одовде. */
  action?: ReactNode
  /** Емоџи што го држи тонот пријателски наместо системски. */
  emoji?: string
}

export function EmptyState({ title, description, action, emoji = '🌱' }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center gap-3 rounded-2xl border border-dashed border-line bg-surface/50 px-6 py-10 text-center">
      <span
        aria-hidden="true"
        className="grid h-12 w-12 place-items-center rounded-2xl bg-brand/10 text-2xl"
      >
        {emoji}
      </span>

      <div>
        <p className="font-display font-semibold text-ink">{title}</p>
        {description && <p className="mt-1 max-w-sm text-sm text-ink-mute">{description}</p>}
      </div>

      {action}
    </div>
  )
}
