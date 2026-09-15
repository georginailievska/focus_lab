import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Avatar } from './Avatar'
import { ApplicationStatusBadge } from './StatusBadge'
import { Alert, Button, Card, Section } from './ui'
import { getSessionApplications } from '../api/mentor'
import { decideApplication } from '../api/sessions'
import { getErrorMessage } from '../lib/errors'
import { formatDateTime } from '../lib/format'
import type { ApplicationStatus, SessionApplication } from '../types'

/** Колку реда стојат во прозорчето пред „види ги сите". */
const PREVIEW = 4

/** На чекање оди прво — тоа е редот што бара акција. */
const ORDER: Record<ApplicationStatus, number> = { PENDING: 0, ACCEPTED: 1, REJECTED: 2 }

interface SessionApplicantsProps {
  sessionId: number
}

export function SessionApplicants({ sessionId }: SessionApplicantsProps) {
  const [applications, setApplications] = useState<SessionApplication[]>([])
  const [expanded, setExpanded] = useState(false)
  const [loading, setLoading] = useState(true)
  const [decidingId, setDecidingId] = useState<number | null>(null)
  const [problem, setProblem] = useState<string | null>(null)

  useEffect(() => {
    let active = true

    getSessionApplications(sessionId)
      .then((loaded) => {
        if (active) {
          setApplications(loaded)
        }
      })
      .catch((err) => {
        if (active) {
          setProblem(getErrorMessage(err, 'Пријавите не се вчитаа.'))
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false)
        }
      })

    return () => {
      active = false
    }
  }, [sessionId])

  async function decide(applicationId: number, status: 'ACCEPTED' | 'REJECTED') {
    setProblem(null)
    setDecidingId(applicationId)

    try {
      const updated = await decideApplication(applicationId, { status })
      setApplications((current) =>
        current.map((item) => (item.id === updated.id ? updated : item)),
      )
    } catch (err) {
      setProblem(getErrorMessage(err, 'Одлуката не се зачува.'))
    } finally {
      setDecidingId(null)
    }
  }

  const counts = {
    ACCEPTED: applications.filter((item) => item.status === 'ACCEPTED').length,
    PENDING: applications.filter((item) => item.status === 'PENDING').length,
    REJECTED: applications.filter((item) => item.status === 'REJECTED').length,
  }

  // На чекање горе, потоа најновите одлуки — прозорчето покажува свежото
  const sorted = [...applications].sort(
    (a, b) => ORDER[a.status] - ORDER[b.status] || b.appliedAt.localeCompare(a.appliedAt),
  )
  const shown = expanded ? sorted : sorted.slice(0, PREVIEW)
  const hidden = sorted.length - shown.length

  return (
    <Section
      title="Пријавени студенти"
      action={
        applications.length > 0 && (
          <span className="text-xs text-ink-mute">
            {counts.ACCEPTED} прифатени · {counts.PENDING} на чекање · {counts.REJECTED} одбиени
          </span>
        )
      }
    >
      <Card padding="lg" className="flex flex-col gap-3">
        {problem && <Alert>{problem}</Alert>}

        {loading ? (
          <p className="text-sm text-ink-mute">Се вчитува...</p>
        ) : applications.length === 0 ? (
          <p className="text-sm text-ink-mute">Сè уште никој не се пријавил на сесијата.</p>
        ) : (
          <>
            <ul className="flex flex-col gap-2.5">
              {shown.map((application) => (
                <li
                  key={application.id}
                  className="flex flex-wrap items-center gap-x-3 gap-y-2 border-b border-line pb-2.5 last:border-0 last:pb-0"
                >
                  <Link to={`/users/${application.student.id}`} title="Отвори профил">
                    <Avatar
                      name={application.student.fullName}
                      src={application.student.avatarUrl}
                      size="sm"
                    />
                  </Link>

                  <div className="min-w-0 flex-1">
                    <Link
                      to={`/users/${application.student.id}`}
                      className="block truncate text-sm font-medium text-ink hover:text-brand hover:underline"
                    >
                      {application.student.fullName}
                    </Link>
                    <p className="truncate text-xs text-ink-mute">
                      {application.student.email} · пријава {formatDateTime(application.appliedAt)}
                    </p>
                  </div>

                  {application.status === 'PENDING' ? (
                    <span className="flex shrink-0 gap-2">
                      <Button
                        variant="good"
                        size="sm"
                        disabled={decidingId === application.id}
                        onClick={() => decide(application.id, 'ACCEPTED')}
                      >
                        Прифати
                      </Button>
                      <Button
                        variant="bad"
                        size="sm"
                        disabled={decidingId === application.id}
                        onClick={() => decide(application.id, 'REJECTED')}
                      >
                        Одбиј
                      </Button>
                    </span>
                  ) : (
                    <ApplicationStatusBadge status={application.status} />
                  )}
                </li>
              ))}
            </ul>

            {(hidden > 0 || expanded) && (
              <Button
                variant="ghost"
                size="sm"
                className="self-start"
                onClick={() => setExpanded((current) => !current)}
              >
                {expanded ? 'Прикажи помалку' : `Види ги сите (${sorted.length})`}
              </Button>
            )}
          </>
        )}
      </Card>
    </Section>
  )
}
