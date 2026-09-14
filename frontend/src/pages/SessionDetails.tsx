import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import Layout from '../components/Layout'
import { Avatar } from '../components/Avatar'
import { SessionNotes } from '../components/SessionNotes'
import { ApplicationStatusBadge } from '../components/StatusBadge'
import { SubjectBadge } from '../components/SubjectBadge'
import { Alert, Badge, Button, Card, Skeleton } from '../components/ui'
import { useAuth } from '../context/AuthContext'
import { useAsync } from '../hooks/useAsync'
import { applyToSession, cancelApplication, getMyApplications, getSession } from '../api/sessions'
import { formatFullDateTime, formatTimeRange, isPast } from '../lib/format'
import { getErrorMessage } from '../lib/errors'
import { sessionModeLabel } from '../lib/labels'

export default function SessionDetails() {
  const { id } = useParams<{ id: string }>()
  const sessionId = Number(id)
  const { user } = useAuth()
  const navigate = useNavigate()

  const [actionError, setActionError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const { data, loading, error, reload } = useAsync(async () => {
    const session = await getSession(sessionId)

    // Само студентот има своја пријава — ендпоинтот е STUDENT-only
    const myApplication =
      user?.role === 'STUDENT'
        ? (await getMyApplications()).find((application) => application.sessionId === sessionId) ?? null
        : null

    return { session, myApplication }
  }, [sessionId])

  async function runAction(action: () => Promise<unknown>, failureMessage: string) {
    setActionError(null)
    setSubmitting(true)

    try {
      await action()
      reload()
    } catch (err) {
      setActionError(getErrorMessage(err, failureMessage))
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return (
      <Layout>
        <Skeleton className="h-64 w-full" />
      </Layout>
    )
  }

  if (error || !data) {
    return (
      <Layout>
        <Alert>{error ?? 'Сесијата не е најдена.'}</Alert>
      </Layout>
    )
  }

  const { session, myApplication } = data
  const finished = isPast(session.endTime)
  const full = session.applicantsCount >= session.maxApplicants
  const isStudent = user?.role === 'STUDENT'
  const hasActiveApplication = myApplication !== null && myApplication.status !== 'REJECTED'

  // Уредува само ментор што ја води сесијата
  const canEdit = session.mentors.some((mentor) => mentor.id === user?.id)

  return (
    <Layout>
      <div className="mb-4 flex flex-wrap items-center gap-2">
        <Button variant="ghost" size="sm" onClick={() => navigate(-1)} className="-ml-2">
          ← Назад
        </Button>

        {canEdit && (
          <Link to={`/mentor/sessions/${session.id}/edit`} className="ml-auto">
            <Button variant="secondary" size="sm">
              Измени сесија
            </Button>
          </Link>
        )}
      </div>

      <Card padding="lg" className="flex flex-col gap-5">
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <SubjectBadge name={session.subject.name} />
            {finished && <Badge tone="neutral">Завршена</Badge>}
            {session.tags.map((tag) => (
              <Badge key={tag} tone="neutral">
                {tag}
              </Badge>
            ))}
          </div>

          <h1 className="mt-3 font-display text-2xl font-semibold text-ink">{session.title}</h1>
        </div>

        <dl className="grid gap-x-6 gap-y-4 text-sm sm:grid-cols-2">
          <div>
            <dt className="text-xs uppercase tracking-wide text-ink-mute">Кога</dt>
            <dd className="mt-0.5 text-ink">
              {formatFullDateTime(session.startTime)}
              <span className="text-ink-mute">
                {' '}
                ({formatTimeRange(session.startTime, session.endTime)})
              </span>
            </dd>
          </div>

          <div>
            <dt className="text-xs uppercase tracking-wide text-ink-mute">Каде</dt>
            <dd className="mt-0.5 text-ink">
              {sessionModeLabel(session.mode)}
              {session.location ? ` — ${session.location}` : ''}
            </dd>
          </div>

          <div>
            <dt className="text-xs uppercase tracking-wide text-ink-mute">Ментори</dt>
            <dd className="mt-1.5 flex flex-wrap items-center gap-x-3 gap-y-2">
              {/* Кликот на ментор води на неговиот профил */}
              {session.mentors.map((mentor) => (
                <Link
                  key={mentor.id}
                  to={`/users/${mentor.id}`}
                  className="flex items-center gap-2 rounded-full py-0.5 pl-0.5 pr-2.5 transition hover:bg-surface-2 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/60"
                >
                  <Avatar name={mentor.fullName} src={mentor.avatarUrl} size="sm" />
                  <span className="text-ink hover:text-brand">{mentor.fullName}</span>
                </Link>
              ))}
            </dd>
          </div>

          <div>
            <dt className="text-xs uppercase tracking-wide text-ink-mute">Места</dt>
            <dd className="mt-0.5 text-ink">
              {/* Студентот го гледа само бројот на одобрени студенти */}
              {isStudent
                ? `${session.approvedCount}/${session.maxApproved} одобрени`
                : `${session.applicantsCount}/${session.maxApplicants} пријавени · ${session.approvedCount}/${session.maxApproved} одобрени`}
            </dd>
          </div>
        </dl>

        {session.description && (
          <p className="whitespace-pre-line text-sm leading-relaxed text-ink-mute">
            {session.description}
          </p>
        )}

        {actionError && <Alert>{actionError}</Alert>}

        {isStudent && !finished && (
          <div className="flex flex-wrap items-center gap-3 border-t border-line pt-5">
            {hasActiveApplication && myApplication ? (
              <>
                <ApplicationStatusBadge status={myApplication.status} />
                <Button
                  variant="secondary"
                  disabled={submitting}
                  onClick={() => runAction(() => cancelApplication(sessionId), 'Откажувањето не успеа.')}
                >
                  Откажи пријава
                </Button>
              </>
            ) : (
              <Button
                disabled={submitting || full}
                onClick={() => runAction(() => applyToSession(sessionId), 'Пријавувањето не успеа.')}
              >
                {full ? 'Сесијата е полна' : submitting ? 'Се пријавува...' : 'Пријави се'}
              </Button>
            )}
          </div>
        )}

        {isStudent && finished && (
          <p className="border-t border-line pt-5 text-sm text-ink-mute">
            Сесијата е завршена — пријавувањето е затворено.
          </p>
        )}
      </Card>

      {user?.role === 'MENTOR' && (
        <div className="mt-9">
          <SessionNotes sessionId={sessionId} />
        </div>
      )}
    </Layout>
  )
}
