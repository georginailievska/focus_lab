import { Link } from 'react-router-dom'
import { SubjectBadge } from './SubjectBadge'
import { Badge, Card } from './ui'
import { dayOfMonth, formatTimeRange, isPast, shortMonth } from '../lib/format'
import { sessionModeLabel } from '../lib/labels'
import type { Session } from '../types'

interface SessionCardProps {
  session: Session
  showApplicants?: boolean
}

/** Едно место за изгледот на сесија во листа — Browse, Home и Mentor Dashboard. */
export function SessionCard({ session, showApplicants = false }: SessionCardProps) {
  const full = session.applicantsCount >= session.maxApplicants
  const finished = isPast(session.endTime)

  return (
    <Link
      to={`/sessions/${session.id}`}
      className="block rounded-2xl focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/60 focus-visible:ring-offset-2 focus-visible:ring-offset-page"
    >
      <Card interactive className="flex items-center gap-4">
        {/* Датумот како блок — датата се фаќа со око, без читање */}
        <span
          aria-hidden="true"
          className="grid h-14 w-14 shrink-0 place-items-center rounded-xl bg-brand/10 leading-none"
        >
          <span className="font-display text-lg font-bold text-brand">
            {dayOfMonth(session.startTime)}
          </span>
          <span className="mt-0.5 text-[10px] font-medium uppercase tracking-wide text-brand/80">
            {shortMonth(session.startTime)}
          </span>
        </span>

        <div className="min-w-0 flex-1">
          <p className="line-clamp-2 font-medium text-ink">{session.title}</p>

          <div className="mt-1.5 flex min-w-0 flex-wrap items-center gap-x-2 gap-y-1">
            <SubjectBadge name={session.subject.name} />
            <span className="whitespace-nowrap text-xs text-ink-mute">
              {formatTimeRange(session.startTime, session.endTime)} · {sessionModeLabel(session.mode)}
            </span>
          </div>
        </div>

        <div className="flex shrink-0 flex-col items-end gap-1.5">
          {finished ? (
            <Badge tone="neutral">Завршена</Badge>
          ) : full ? (
            <Badge tone="bad">Пополнето</Badge>
          ) : (
            <Badge tone="good">
              {session.approvedCount}/{session.maxApproved}
            </Badge>
          )}

          {showApplicants && !finished && (
            <span className="text-[11px] text-ink-mute">
              {session.applicantsCount} пријавени
            </span>
          )}
        </div>
      </Card>
    </Link>
  )
}
