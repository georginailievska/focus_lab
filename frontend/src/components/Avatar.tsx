import { useEffect, useState } from 'react'
import { cn } from '../lib/cn'
import { initialsFor, solidColorFor } from '../lib/palette'

interface AvatarProps {
  name: string
  /** Патека до качената слика. Без неа се прикажуваат иницијали. */
  src?: string | null
  size?: 'sm' | 'md' | 'lg' | 'xl'
  className?: string
}

const SIZES = {
  sm: 'h-8 w-8 text-[11px]',
  md: 'h-10 w-10 text-xs',
  lg: 'h-12 w-12 text-sm',
  xl: 'h-24 w-24 text-2xl',
} as const

export function Avatar({ name, src, size = 'md', className }: AvatarProps) {
  const [failed, setFailed] = useState(false)

  // Нова слика (клучот се менува при секое качување) значи нов обид
  useEffect(() => setFailed(false), [src])

  const showImage = Boolean(src) && !failed

  return (
    <span
      aria-hidden="true"
      style={showImage ? undefined : { backgroundColor: solidColorFor(name) }}
      className={cn(
        'relative grid shrink-0 place-items-center overflow-hidden rounded-full font-semibold text-white',
        SIZES[size],
        className,
      )}
    >
      {showImage ? (
        <img
          src={src as string}
          alt=""
          loading="lazy"
          onError={() => setFailed(true)}
          className="h-full w-full object-cover"
        />
      ) : (
        initialsFor(name)
      )}
    </span>
  )
}
