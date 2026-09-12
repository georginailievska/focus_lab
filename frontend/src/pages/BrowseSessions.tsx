import { useSearchParams } from 'react-router-dom'
import Layout from '../components/Layout'
import { SessionCard } from '../components/SessionCard'
import { Alert, EmptyState, PageHeader, Select, SkeletonList } from '../components/ui'
import { useAsync } from '../hooks/useAsync'
import { getSessions } from '../api/sessions'
import { getSubjects } from '../api/subjects'
import { isPast } from '../lib/format'

export default function BrowseSessions() {
  const [searchParams, setSearchParams] = useSearchParams()
  const subjectId = searchParams.get('subjectId')

  const subjects = useAsync(() => getSubjects(), [])
  const sessions = useAsync(
    () => getSessions(subjectId ? Number(subjectId) : undefined),
    [subjectId],
  )

  const all = sessions.data ?? []
  const upcoming = all.filter((session) => !isPast(session.endTime))
  const finished = all.filter((session) => isPast(session.endTime))

  return (
    <Layout>
      <PageHeader
        title="Сесии"
        subtitle="Избери предмет и пријави се на сесија што ти одговара."
        actions={
          <Select
            aria-label="Филтер по предмет"
            className="w-auto"
            value={subjectId ?? ''}
            onChange={(event) => {
              const value = event.target.value
              setSearchParams(value ? { subjectId: value } : {})
            }}
          >
            <option value="">Сите предмети</option>
            {(subjects.data ?? []).map((subject) => (
              <option key={subject.id} value={subject.id}>
                {subject.name}
              </option>
            ))}
          </Select>
        }
      />

      {sessions.error && <Alert className="mb-6">{sessions.error}</Alert>}

      {sessions.loading ? (
        <SkeletonList rows={4} />
      ) : all.length === 0 ? (
        <EmptyState
          title="Нема закажани сесии"
          description={
            subjectId
              ? 'За овој предмет сè уште нема сесии. Пробај со друг предмет.'
              : 'Штом ментор закаже сесија, ќе се појави тука.'
          }
        />
      ) : (
        <div className="flex flex-col gap-8">
          <ul className="flex flex-col gap-3">
            {upcoming.map((session) => (
              <li key={session.id}>
                <SessionCard session={session} />
              </li>
            ))}
          </ul>

          {/* Одржаните сесии не се кријат, но одат подолу и придушено */}
          {finished.length > 0 && (
            <div>
              <h2 className="mb-3 text-sm font-medium text-ink-mute">Веќе одржани</h2>
              <ul className="flex flex-col gap-3 opacity-60">
                {finished.map((session) => (
                  <li key={session.id}>
                    <SessionCard session={session} />
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}
    </Layout>
  )
}
