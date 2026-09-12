import type { ReactNode } from 'react'

interface PageHeaderProps {
  title: string
  subtitle?: ReactNode
  /** Копчиња или филтри десно од насловот. */
  actions?: ReactNode
}

export function PageHeader({ title, subtitle, actions }: PageHeaderProps) {
  return (
    <div className="mb-7 flex flex-wrap items-start justify-between gap-3 border-b border-line pb-6">
      <div className="min-w-0">
        <h1 className="font-display text-2xl font-semibold tracking-tight text-ink sm:text-[28px]">
          {title}
        </h1>
        {/* max-w: без ова долг поднаслов зема цел ред и ги истиснува копчињата надолу */}
        {subtitle && <div className="mt-1.5 max-w-2xl text-sm text-ink-mute">{subtitle}</div>}
      </div>

      {/* ml-auto: и кога ќе се префрлат во нов ред, копчињата остануваат десно */}
      {actions && <div className="ml-auto flex flex-wrap items-center gap-2">{actions}</div>}
    </div>
  )
}

interface SectionProps {
  title: string
  action?: ReactNode
  children: ReactNode
}

export function Section({ title, action, children }: SectionProps) {
  return (
    <section className="min-w-0">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-x-3 gap-y-1">
        <h2 className="min-w-0 font-display text-lg font-semibold text-ink">{title}</h2>
        {action}
      </div>
      {children}
    </section>
  )
}
