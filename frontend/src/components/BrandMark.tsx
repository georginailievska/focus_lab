import { cn } from '../lib/cn'

interface BrandMarkProps {
  size?: 'md' | 'lg'
  /** Само знакот, без текстот „FOCUS Lab" (за тесни места). */
  markOnly?: boolean
  className?: string
}

const MARK = {
  md: 'h-10 w-10',
  lg: 'h-20 w-20',
} as const

const WORD = {
  md: 'text-lg',
  lg: 'text-2xl',
} as const

export function BrandMark({ size = 'md', markOnly = false, className }: BrandMarkProps) {
  return (
    <span className={cn('flex items-center gap-2.5', className)}>
      <img
        src="/brand/focuslab-logo-128.png"
        srcSet="/brand/focuslab-logo-128.png 1x, /brand/focuslab-logo-256.png 2x"
        alt=""
        aria-hidden="true"
        width={size === 'lg' ? 80 : 40}
        height={size === 'lg' ? 80 : 40}
        className={cn('shrink-0 select-none', MARK[size])}
      />

      {!markOnly && (
        <span className={cn('font-display font-semibold tracking-tight text-ink', WORD[size])}>
          FOCUS <span className="text-brand">Lab</span>
        </span>
      )}
    </span>
  )
}
