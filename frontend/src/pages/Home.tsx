import { Link } from 'react-router-dom'
import Layout from '../components/Layout'
import { FeedPanel } from '../components/FeedPanel'
import { ApplicationStatusBadge } from '../components/StatusBadge'
import { SubjectBadge } from '../components/SubjectBadge'
import { Alert, Card, EmptyState, PageHeader, Section, SkeletonList } from '../components/ui'
import { useAuth } from '../context/AuthContext'
import { useAsync } from '../hooks/useAsync'
import { getMyApplications } from '../api/sessions'
import { getMyInterests } from '../api/interests'
import { dayOfMonth, formatTime, isPast, shortMonth } from '../lib/format'

export default function Home() {
  const { user } = useAuth()

  const { data, loading, error } = useAsync(
    () => Promise.all([getMyApplications(), getMyInterests()]),
    [],
  )

  const [applications, interests] = data ?? [[], []]

  // „Претстојни" значи: не е одбиена пријава и сесијата уште не се одржала
  const upcoming = applications
    .filter((application) => application.status !== 'REJECTED')
    .filter((application) => !isPast(application.sessionStartTime))
    .sort((a, b) => a.sessionStartTime.localeCompare(b.sessionStartTime))

  const firstName = (user?.fullName ?? '').split(' ')[0]

  return (
    <Layout>
      <PageHeader
        title={`Здраво, ${firstName} 👋`}
        subtitle="Најди помош, поврзи се со ментори и успеј заедно."
        actions={
          <Link to="/calendar" className="text-sm font-medium text-brand hover:underline">
            🗓️ Календар
          </Link>
        }
      />

      {error && <Alert className="mb-6">{error}</Alert>}

      <div className="grid grid-cols-1 gap-8 md:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]">
        <Section
          title="Моите претстојни сесии"
          action={
            <Link to="/sessions" className="text-sm font-medium text-brand hover:underline">
              Прегледај ги сите
            </Link>
          }
        >
          {loading ? (
            <SkeletonList rows={3} />
          ) : upcoming.length === 0 ? (
            <EmptyState
              emoji="📚"
              title="Сè уште немаш пријави"
              description="Избери сесија по предмет што те интересира и пријави се — менторот потоа одлучува."
              action={
                <Link to="/sessions" className="text-sm font-medium text-brand hover:underline">
                  Прегледај сесии
                </Link>
              }
            />
          ) : (
            <ul className="flex flex-col gap-3">
              {upcoming.map((application) => (
                <li key={application.id}>
                  <Link
                    to={`/sessions/${application.sessionId}`}
                    className="block rounded-2xl focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/60 focus-visible:ring-offset-2 focus-visible:ring-offset-page"
                  >
                    <Card interactive className="flex items-center gap-4">
                      <span
                        aria-hidden="true"
                        className="grid h-14 w-14 shrink-0 place-items-center rounded-xl bg-brand/10 leading-none"
                      >
                        <span className="font-display text-lg font-bold text-brand">
                          {dayOfMonth(application.sessionStartTime)}
                        </span>
                        <span className="mt-0.5 text-[10px] font-medium uppercase tracking-wide text-brand/80">
                          {shortMonth(application.sessionStartTime)}
                        </span>
                      </span>

                      <div className="min-w-0 flex-1">
                        <p className="line-clamp-2 font-medium text-ink">{application.sessionTitle}</p>
                        <p className="mt-1 text-xs text-ink-mute">
                          во {formatTime(application.sessionStartTime)}
                        </p>
                      </div>

                      <ApplicationStatusBadge status={application.status} />
                    </Card>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </Section>

        <Section
          title="Мои интереси"
          action={
            <Link to="/interests" className="text-sm font-medium text-brand hover:underline">
              Уреди
            </Link>
          }
        >
          {loading ? (
            <SkeletonList rows={3} />
          ) : interests.length === 0 ? (
            <EmptyState
              emoji="🔔"
              title="Немаш избрани интереси"
              description="Избери предмети и ќе добиеш email кога ќе се закаже нова сесија по нив."
              action={
                <Link to="/interests" className="text-sm font-medium text-brand hover:underline">
                  Избери интереси
                </Link>
              }
            />
          ) : (
            <Card className="flex flex-wrap gap-2">
              {interests.map((subject) => (
                <Link
                  key={subject.id}
                  to={`/sessions?subjectId=${subject.id}`}
                  className="rounded-full transition hover:brightness-95 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/60"
                >
                  <SubjectBadge name={subject.name} />
                </Link>
              ))}
            </Card>
          )}
        </Section>
      </div>

      <div className="mt-9">
        <FeedPanel />
      </div>
    </Layout>
  )
}
