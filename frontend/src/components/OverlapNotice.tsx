import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getOverlaps } from '../api/mentor'
import { combine } from '../lib/schedule'
import { formatFullDate, formatTimeRange } from '../lib/format'
import type { Overlap } from '../types'
import type { Schedule } from './SchedulePicker'

/** Пауза пред прашањето: избирањето час е неколку клика, не еден. */
const DEBOUNCE_MS = 450

interface OverlapNoticeProps {
  schedule: Schedule
  /** Сесијата што се уредува не се брои како преклопување со самата себе. */
  excludeSessionId?: number
}

export function OverlapNotice({ schedule, excludeSessionId }: OverlapNoticeProps) {
  const [overlaps, setOverlaps] = useState<Overlap[]>([])

  const { date, start, end } = schedule
  const ready = Boolean(date && start && end && end > start)

  useEffect(() => {
    if (!ready) {
      setOverlaps([])
      return
    }

    let active = true
    const timer = setTimeout(() => {
      getOverlaps({
        startTime: combine(date, start),
        endTime: combine(date, end),
        sessionId: excludeSessionId,
      })
        .then((found) => {
          if (active) {
            setOverlaps(found)
          }
        })
        // Тивко: ова е помошна информација, не смее да ја блокира формата
        .catch(() => {
          if (active) {
            setOverlaps([])
          }
        })
    }, DEBOUNCE_MS)

    return () => {
      active = false
      clearTimeout(timer)
    }
  }, [ready, date, start, end, excludeSessionId])

  if (overlaps.length === 0) {
    return null
  }

  const mine = overlaps.filter((overlap) => overlap.mine)

  return (
    <div className="rounded-xl border border-warm/50 bg-warm/10 px-4 py-3">
      <p className="flex items-center gap-2 text-sm font-medium text-ink">
        <span aria-hidden="true">⚠️</span>
        {overlaps.length === 1
          ? 'Веќе има сесија во овој период'
          : `Веќе има ${overlaps.length} сесии во овој период`}
      </p>

      {mine.length > 0 && (
        <p className="mt-1 text-xs text-ink-soft">
          {mine.length === 1
            ? 'Една од нив е твоја — не можеш да водиш две во исто време.'
            : 'Некои од нив се твои — не можеш да водиш две во исто време.'}
        </p>
      )}

      <ul className="mt-2.5 flex flex-col gap-2">
        {overlaps.map((overlap) => (
          <li key={overlap.sessionId} className="text-xs text-ink-soft">
            <Link
              to={`/sessions/${overlap.sessionId}`}
              target="_blank"
              className="font-medium text-ink hover:text-brand hover:underline"
            >
              {overlap.title}
            </Link>
            {overlap.mine && <span className="ml-1.5 font-medium text-warm">твоја</span>}

            <span className="block text-ink-mute">
              {overlap.subject.name} · {formatFullDate(overlap.startTime)},{' '}
              {formatTimeRange(overlap.startTime, overlap.endTime)} ·{' '}
              {overlap.mentors.map((mentor) => mentor.fullName).join(', ')}
            </span>
          </li>
        ))}
      </ul>

      <p className="mt-2.5 text-xs text-ink-mute">
        Ова е само информација — сесијата може да се закаже.
      </p>
    </div>
  )
}
