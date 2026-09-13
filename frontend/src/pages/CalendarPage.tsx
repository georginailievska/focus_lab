import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import Layout from '../components/Layout'
import { SessionCard } from '../components/SessionCard'
import { Alert, Badge, Button, Card, EmptyState, PageHeader, Select, Skeleton } from '../components/ui'
import { useAuth } from '../context/AuthContext'
import { useAsync } from '../hooks/useAsync'
import { getMyApplications, getSessionsInRange } from '../api/sessions'
import { getSubjects } from '../api/subjects'
import { cn } from '../lib/cn'
import { formatTime } from '../lib/format'
import { solidColorFor } from '../lib/palette'
import {
  addMonths,
  dayLabel,
  groupByDay,
  monthGrid,
  monthLabel,
  todayKey,
  WEEKDAYS,
  type DayKey,
} from '../lib/calendar'
import type { ApplicationStatus, Session } from '../types'

/** Колку сесии се прикажуваат во едно поле пред „+N уште". */
const CHIPS_PER_DAY = 2

export default function CalendarPage() {
  const { user } = useAuth()

  const [view, setView] = useState(() => {
    const now = new Date()
    return { year: now.getFullYear(), month: now.getMonth() }
  })
  const [selected, setSelected] = useState<DayKey>(todayKey())
  const [subjectId, setSubjectId] = useState('')
  const [onlyMine, setOnlyMine] = useState(false)

  const days = useMemo(() => monthGrid(view.year, view.month), [view])
  const from = days[0].key
  const to = days[days.length - 1].key

  const subjects = useAsync(getSubjects, [])

  // Се бара точно периодот што се прикажува (шест недели), не целата историја.
  const { data, loading, error } = useAsync(async () => {
    const sessions = await getSessionsInRange(from, to, subjectId ? Number(subjectId) : undefined)

    // Статусот на сопствените пријави — за да се види што е веќе пријавено
    const applications = user?.role === 'STUDENT' ? await getMyApplications() : []

    const statusBySession = new Map<number, ApplicationStatus>()
    applications.forEach((application) => statusBySession.set(application.sessionId, application.status))

    return { sessions, statusBySession }
  }, [from, to, subjectId, user?.role])

  const isMentor = user?.role === 'MENTOR'
  const canSeeCounts = user?.role !== 'STUDENT'

  const mine = (session: Session) =>
    isMentor
      ? session.mentors.some((mentor) => mentor.id === user?.id)
      : data?.statusBySession.has(session.id) ?? false

  const visible = (data?.sessions ?? []).filter((session) => !onlyMine || mine(session))
  const byDay = useMemo(() => groupByDay(visible), [visible])
  const selectedSessions = byDay.get(selected) ?? []

  function shiftMonth(delta: number) {
    setView((current) => addMonths(current.year, current.month, delta))
  }

  function goToday() {
    const now = new Date()
    setView({ year: now.getFullYear(), month: now.getMonth() })
    setSelected(todayKey())
  }

  return (
    <Layout>
      <PageHeader
        title="Календар"
        subtitle="Сите сесии по денови. Кликни на ден за да ги видиш деталите."
        actions={
          <Link to="/sessions" className="text-sm font-medium text-brand hover:underline">
            Види како листа
          </Link>
        }
      />

      {error && <Alert className="mb-6">{error}</Alert>}

      <div className="mb-5 flex flex-wrap items-center gap-x-3 gap-y-3">
        <div className="flex items-center gap-1">
          <Button variant="secondary" size="sm" aria-label="Претходен месец" onClick={() => shiftMonth(-1)}>
            ←
          </Button>
          <Button variant="secondary" size="sm" aria-label="Следен месец" onClick={() => shiftMonth(1)}>
            →
          </Button>
        </div>

        <h2 className="font-display text-lg font-semibold text-ink first-letter:uppercase">
          {monthLabel(view.year, view.month)}
        </h2>

        <Button variant="ghost" size="sm" onClick={goToday}>
          Денес
        </Button>

        <div className="ml-auto flex flex-wrap items-center gap-2">
          {(isMentor || user?.role === 'STUDENT') && (
            <Button
              variant={onlyMine ? 'primary' : 'secondary'}
              size="sm"
              onClick={() => setOnlyMine((value) => !value)}
            >
              {isMentor ? 'Само моите' : 'Само пријавените'}
            </Button>
          )}

          <span className="block w-40 sm:w-52">
            <Select
              aria-label="Предмет"
              className="py-1.5 text-xs"
              value={subjectId}
              onChange={(event) => setSubjectId(event.target.value)}
            >
              <option value="">Сите предмети</option>
              {(subjects.data ?? []).map((subject) => (
                <option key={subject.id} value={subject.id}>
                  {subject.name}
                </option>
              ))}
            </Select>
          </span>
        </div>
      </div>

      {loading ? (
        <Skeleton className="h-80 w-full" />
      ) : (
        <Card padding="none" className="overflow-hidden">
          {/* Заглавје со деновите. Неделата почнува во понеделник. */}
          <div className="grid grid-cols-7 border-b border-line bg-surface-2">
            {WEEKDAYS.map((label) => (
              <div
                key={label}
                className="px-1 py-2 text-center text-[11px] font-semibold uppercase tracking-wide text-ink-mute"
              >
                {/* На телефон само првата буква — инаку колоните се тесни за текст */}
                <span className="sm:hidden">{label.slice(0, 1)}</span>
                <span className="hidden sm:inline">{label}</span>
              </div>
            ))}
          </div>

          <div className="grid grid-cols-7">
            {days.map((day) => {
              const sessions = byDay.get(day.key) ?? []
              const isSelected = day.key === selected

              return (
                <button
                  key={day.key}
                  type="button"
                  onClick={() => setSelected(day.key)}
                  aria-label={`${dayLabel(day.key)}, ${sessions.length} сесии`}
                  aria-current={isSelected ? 'date' : undefined}
                  className={cn(
                    'group relative min-h-[62px] border-b border-r border-line p-1.5 text-left transition sm:min-h-[104px] sm:p-2',
                    'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-brand/60',
                    day.inMonth ? 'bg-surface' : 'bg-surface-2/40',
                    isSelected ? 'bg-brand/[0.07]' : 'hover:bg-surface-2',
                  )}
                >
                  <span
                    className={cn(
                      'grid h-6 w-6 place-items-center rounded-full text-xs font-semibold',
                      day.isToday && 'bg-brand text-brand-on',
                      !day.isToday && day.inMonth && 'text-ink',
                      !day.isToday && !day.inMonth && 'text-ink-mute',
                      !day.isToday && day.isPast && day.inMonth && 'text-ink-mute',
                    )}
                  >
                    {day.day}
                  </span>

                  {/* На широк екран: ленти со час и наслов */}
                  <span className="mt-1 hidden flex-col gap-1 sm:flex">
                    {sessions.slice(0, CHIPS_PER_DAY).map((session) => (
                      <span
                        key={session.id}
                        className={cn(
                          'flex min-w-0 items-center gap-1 rounded-md px-1.5 py-0.5 text-[11px] leading-tight',
                          'bg-surface-2 text-ink-soft',
                          mine(session) && 'ring-1 ring-brand/50',
                          day.isPast && 'opacity-60',
                        )}
                      >
                        <span
                          aria-hidden="true"
                          style={{ backgroundColor: solidColorFor(session.subject.name) }}
                          className="h-1.5 w-1.5 shrink-0 rounded-full"
                        />
                        <span className="shrink-0 font-medium tabular-nums">
                          {formatTime(session.startTime)}
                        </span>
                        <span className="truncate">{session.title}</span>
                      </span>
                    ))}

                    {sessions.length > CHIPS_PER_DAY && (
                      <span className="px-1.5 text-[11px] font-medium text-brand">
                        +{sessions.length - CHIPS_PER_DAY} уште
                      </span>
                    )}
                  </span>

                  {/* На телефон: само точки — ленти со текст не се читаат на 50px */}
                  {sessions.length > 0 && (
                    <span className="mt-1 flex flex-wrap gap-0.5 sm:hidden">
                      {sessions.slice(0, 4).map((session) => (
                        <span
                          key={session.id}
                          aria-hidden="true"
                          style={{ backgroundColor: solidColorFor(session.subject.name) }}
                          className="h-1.5 w-1.5 rounded-full"
                        />
                      ))}
                    </span>
                  )}
                </button>
              )
            })}
          </div>
        </Card>
      )}

      {/* Избраниот ден, со целите картички */}
      <section className="mt-7 min-w-0">
        <div className="mb-4 flex flex-wrap items-center gap-x-3 gap-y-1">
          <h2 className="font-display text-lg font-semibold text-ink first-letter:uppercase">
            {dayLabel(selected)}
          </h2>
          {selectedSessions.length > 0 && (
            <Badge tone="brand">
              {selectedSessions.length === 1 ? '1 сесија' : `${selectedSessions.length} сесии`}
            </Badge>
          )}
        </div>

        {selectedSessions.length === 0 ? (
          <EmptyState
            emoji="🗓️"
            title="Нема сесии на овој ден"
            description="Избери друг ден во календарот или прегледај ги сите закажани сесии."
            action={
              <Link to="/sessions" className="text-sm font-medium text-brand hover:underline">
                Прегледај сесии
              </Link>
            }
          />
        ) : (
          <ul className="flex flex-col gap-3">
            {selectedSessions.map((session) => (
              <li key={session.id}>
                <SessionCard session={session} showApplicants={canSeeCounts} />
              </li>
            ))}
          </ul>
        )}
      </section>
    </Layout>
  )
}
