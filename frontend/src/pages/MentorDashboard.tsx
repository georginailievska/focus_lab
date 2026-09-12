import { useState } from 'react'
import { Link } from 'react-router-dom'
import Layout from '../components/Layout'
import { Avatar } from '../components/Avatar'
import { FeedPanel } from '../components/FeedPanel'
import { SessionCard } from '../components/SessionCard'
import { MentorStatusBadge } from '../components/StatusBadge'
import { SubjectBadge } from '../components/SubjectBadge'
import {
  Alert,
  Button,
  Card,
  EmptyState,
  PageHeader,
  Section,
  SkeletonList,
  SkeletonStats,
  StatCard,
} from '../components/ui'
import { useAuth } from '../context/AuthContext'
import { useAsync } from '../hooks/useAsync'
import { getMySessions, getPendingRequests } from '../api/mentor'
import { decideApplication } from '../api/sessions'
import { dayOfMonth, formatDateTime, formatTimeRange, isPast, shortMonth } from '../lib/format'
import { getErrorMessage } from '../lib/errors'
import type { ApplicationStatus } from '../types'

export default function MentorDashboard() {
  const { user } = useAuth()
  const isApproved = user?.mentorStatus === 'APPROVED'

  const [decidingId, setDecidingId] = useState<number | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  const { data, loading, error, reload } = useAsync(
    () => Promise.all([getMySessions(), getPendingRequests()]),
    [],
  )

  const [sessions, requests] = data ?? [[], []]

  const upcomingSessions = sessions
    .filter((session) => !isPast(session.startTime))
    .sort((a, b) => a.startTime.localeCompare(b.startTime))

  const nextSession = upcomingSessions[0]
  const totalApprovedStudents = sessions.reduce((sum, session) => sum + session.approvedCount, 0)
  const subjectCount = new Set(sessions.map((session) => session.subject.id)).size

  async function decide(applicationId: number, status: Extract<ApplicationStatus, 'ACCEPTED' | 'REJECTED'>) {
    setActionError(null)
    setDecidingId(applicationId)

    try {
      await decideApplication(applicationId, { status })
      reload()
    } catch (err) {
      // Inline порака наместо browser alert() — не блокира и не изгледа како краш
      setActionError(getErrorMessage(err, 'Одлуката не се зачува.'))
    } finally {
      setDecidingId(null)
    }
  }

  const firstName = (user?.fullName ?? '').split(' ')[0]

  return (
    <Layout>
      <PageHeader
        title={`Добредојде, ${firstName}`}
        subtitle={<MentorStatusBadge status={user?.mentorStatus ?? null} />}
        actions={
          isApproved && (
            <>
              <Link to="/calendar">
                <Button variant="secondary" size="lg">
                  🗓️ Календар
                </Button>
              </Link>
              <Link to="/mentor/create-session">
                <Button size="lg">+ Нова сесија</Button>
              </Link>
            </>
          )
        }
      />

      {!isApproved && (
        <Alert tone="warning" className="mb-6">
          Твојата сметка сè уште чека одобрување од админ. Штом бидеш одобрен/а, ќе можеш да закажуваш сесии.
        </Alert>
      )}

      {error && <Alert className="mb-6">{error}</Alert>}

      {loading ? (
        <SkeletonStats />
      ) : (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          <StatCard emoji="📅" tone="brand" label="Претстојни сесии" value={upcomingSessions.length} />
          <StatCard emoji="⏳" tone="warm" label="Барања на чекање" value={requests.length} />
          <StatCard emoji="🎓" tone="good" label="Одобрени студенти" value={totalApprovedStudents} />
          <StatCard emoji="📘" tone="neutral" label="Предмети" value={subjectCount} />
        </div>
      )}

      <div className="mt-8 grid grid-cols-1 gap-8 md:grid-cols-[minmax(0,3fr)_minmax(0,2fr)]">
        <div className="flex min-w-0 flex-col gap-8">
          <Section title="Барања на чекање">
            {actionError && <Alert className="mb-3">{actionError}</Alert>}

            {loading ? (
              <SkeletonList rows={3} />
            ) : requests.length === 0 ? (
              <EmptyState
                emoji="✅"
                title="Нема барања на чекање"
                description="Штом студент се пријави на некоја од твоите сесии, ќе се појави тука."
              />
            ) : (
              <ul className="flex flex-col gap-3">
                {requests.map((request) => (
                  <li key={request.id}>
                    <Card className="flex flex-wrap items-center gap-3">
                      <Link to={`/users/${request.student.id}`} title="Отвори профил">
                        <Avatar name={request.student.fullName} src={request.student.avatarUrl} />
                      </Link>

                      <div className="min-w-0 flex-1">
                        <Link
                          to={`/users/${request.student.id}`}
                          className="block truncate font-medium text-ink hover:text-brand hover:underline"
                        >
                          {request.student.fullName}
                        </Link>
                        <p className="truncate text-xs text-ink-mute">{request.student.email}</p>
                        <p className="mt-1 truncate text-xs text-ink-mute">
                          {request.sessionTitle} · {formatDateTime(request.sessionStartTime)}
                        </p>
                      </div>

                      <div className="flex shrink-0 gap-2">
                        <Button
                          variant="good"
                          size="sm"
                          disabled={decidingId === request.id}
                          onClick={() => decide(request.id, 'ACCEPTED')}
                        >
                          Прифати
                        </Button>
                        <Button
                          variant="bad"
                          size="sm"
                          disabled={decidingId === request.id}
                          onClick={() => decide(request.id, 'REJECTED')}
                        >
                          Одбиј
                        </Button>
                      </div>
                    </Card>
                  </li>
                ))}
              </ul>
            )}
          </Section>

          <FeedPanel limit={2} />
        </div>

        <div className="flex min-w-0 flex-col gap-8">
          <Section title="Следна сесија">
            {loading ? (
              <SkeletonList rows={1} />
            ) : !nextSession ? (
              <EmptyState
                emoji="🗓️"
                title="Нема закажани сесии"
                description={
                  isApproved ? 'Закажи ја првата и студентите ќе можат да се пријавуваат.' : undefined
                }
                action={
                  isApproved && (
                    <Link
                      to="/mentor/create-session"
                      className="text-sm font-medium text-brand hover:underline"
                    >
                      Нова сесија
                    </Link>
                  )
                }
              />
            ) : (
              <Link
                to={`/sessions/${nextSession.id}`}
                className="block rounded-2xl focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/60 focus-visible:ring-offset-2 focus-visible:ring-offset-page"
              >
                <Card interactive padding="lg">
                  <div className="flex items-start gap-4">
                    <span
                      aria-hidden="true"
                      className="grid h-14 w-14 shrink-0 place-items-center rounded-xl bg-brand/10 leading-none"
                    >
                      <span className="font-display text-lg font-bold text-brand">
                        {dayOfMonth(nextSession.startTime)}
                      </span>
                      <span className="mt-0.5 text-[10px] font-medium uppercase tracking-wide text-brand/80">
                        {shortMonth(nextSession.startTime)}
                      </span>
                    </span>

                    <div className="min-w-0">
                      <p className="font-medium text-ink">{nextSession.title}</p>
                      <p className="mt-1 text-xs text-ink-mute">
                        {formatTimeRange(nextSession.startTime, nextSession.endTime)}
                      </p>
                      <div className="mt-2">
                        <SubjectBadge name={nextSession.subject.name} />
                      </div>
                    </div>
                  </div>

                  {/* Полнењето на местата се чита побрзо како лента отколку како бројка */}
                  <div className="mt-4">
                    <div className="flex items-center justify-between text-xs">
                      <span className="text-ink-mute">Одобрени студенти</span>
                      <span className="font-medium text-ink">
                        {nextSession.approvedCount}/{nextSession.maxApproved}
                      </span>
                    </div>
                    <div className="mt-1.5 h-1.5 overflow-hidden rounded-full bg-surface-2">
                      <div
                        className="h-full rounded-full bg-brand transition-all"
                        style={{
                          width: `${Math.min(
                            100,
                            (nextSession.approvedCount / Math.max(1, nextSession.maxApproved)) * 100,
                          )}%`,
                        }}
                      />
                    </div>
                    <p className="mt-1.5 text-xs text-ink-mute">
                      {nextSession.applicantsCount} пријавени вкупно
                    </p>
                  </div>
                </Card>
              </Link>
            )}
          </Section>

          <Section title="Моите сесии">
            {loading ? (
              <SkeletonList rows={2} />
            ) : sessions.length === 0 ? (
              <p className="text-sm text-ink-mute">Сè уште немаш закажано сесии.</p>
            ) : (
              <ul className="flex flex-col gap-3">
                {sessions.map((session) => (
                  <li key={session.id}>
                    <SessionCard session={session} showApplicants />
                  </li>
                ))}
              </ul>
            )}
          </Section>
        </div>
      </div>
    </Layout>
  )
}
