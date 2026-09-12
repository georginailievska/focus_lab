import { cn } from '../../lib/cn'

interface SkeletonProps {
  className?: string
}

export function Skeleton({ className }: SkeletonProps) {
  return <div className={cn('animate-pulse rounded-xl bg-surface-2', className)} />
}

export function SkeletonList({ rows = 3 }: { rows?: number }) {
  return (
    <div className="flex flex-col gap-3">
      {Array.from({ length: rows }).map((_, index) => (
        <Skeleton key={index} className="h-[70px] w-full" />
      ))}
    </div>
  )
}

export function SkeletonStats({ cards = 4 }: { cards?: number }) {
  return (
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
      {Array.from({ length: cards }).map((_, index) => (
        <Skeleton key={index} className="h-[104px]" />
      ))}
    </div>
  )
}
