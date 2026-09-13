import { cn } from '../lib/cn'
import { solidColorFor } from '../lib/palette'

interface SubjectBadgeProps {
  name: string
  className?: string
}

export function SubjectBadge({ name, className }: SubjectBadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex min-w-0 max-w-full items-center gap-1.5 rounded-full bg-surface-2 px-2.5 py-1 text-xs font-medium text-ink-soft',
        className,
      )}
    >
      <span
        aria-hidden="true"
        style={{ backgroundColor: solidColorFor(name) }}
        className="h-2 w-2 shrink-0 rounded-full"
      />
      <span className="truncate">{name}</span>
    </span>
  )
}
