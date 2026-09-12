import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import Layout from '../components/Layout'
import { Avatar } from '../components/Avatar'
import { SubjectBadge } from '../components/SubjectBadge'
import {
  Alert,
  Button,
  Card,
  EmptyState,
  Input,
  PageHeader,
  Section,
  SkeletonList,
  SkeletonStats,
  StatCard,
} from '../components/ui'
import { useAsync } from '../hooks/useAsync'
import {
  approveMentor,
  createSubject,
  getAdminSubjects,
  getPendingMentors,
  getStats,
  rejectMentor,
} from '../api/admin'
import { formatDate } from '../lib/format'
import { getErrorMessage } from '../lib/errors'

export default function AdminDashboard() {
  const [decidingId, setDecidingId] = useState<number | null>(null)
  const [mentorError, setMentorError] = useState<string | null>(null)

  const [newSubject, setNewSubject] = useState('')
  const [subjectError, setSubjectError] = useState<string | null>(null)
  const [addingSubject, setAddingSubject] = useState(false)

  const { data, loading, error, reload } = useAsync(
    () => Promise.all([getStats(), getPendingMentors(), getAdminSubjects()]),
    [],
  )

  const [stats, pendingMentors, subjects] = data ?? [null, [], []]

  async function decideMentor(userId: number, approve: boolean) {
    setMentorError(null)
    setDecidingId(userId)

    try {
      await (approve ? approveMentor(userId) : rejectMentor(userId))
      reload()
    } catch (err) {
      setMentorError(getErrorMessage(err, 'Одлуката не се зачува.'))
    } finally {
      setDecidingId(null)
    }
  }

  async function handleAddSubject(event: FormEvent) {
    event.preventDefault()

    const name = newSubject.trim()
    if (!name) {
      return
    }

    setSubjectError(null)
    setAddingSubject(true)

    try {
      await createSubject({ name })
      setNewSubject('')
      reload()
    } catch (err) {
      // Дупликат име сега враќа читлив 409 од backend, наместо 500
      setSubjectError(getErrorMessage(err, 'Предметот не можеше да се додаде.'))
    } finally {
      setAddingSubject(false)
    }
  }

  return (
    <Layout>
      <PageHeader
        title="Преглед"
        subtitle="Ментори, предмети и состојба на платформата."
        actions={
          <Link to="/calendar" className="text-sm font-medium text-brand hover:underline">
            🗓️ Календар
          </Link>
        }
      />

      {error && <Alert className="mb-6">{error}</Alert>}

      {loading ? (
        <SkeletonStats />
      ) : (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          <StatCard emoji="👥" tone="brand" label="Вкупно корисници" value={stats?.totalUsers ?? '—'} />
          <StatCard emoji="🎓" tone="neutral" label="Студенти" value={stats?.students ?? '—'} />
          <StatCard emoji="🧑‍🏫" tone="good" label="Ментори" value={stats?.mentors ?? '—'} />
          <StatCard emoji="📅" tone="warm" label="Активни сесии" value={stats?.activeSessions ?? '—'} />
        </div>
      )}

      <div className="mt-8 grid grid-cols-1 gap-8 md:grid-cols-[minmax(0,3fr)_minmax(0,2fr)]">
        <Section title="Ментори на чекање">
          {mentorError && <Alert className="mb-3">{mentorError}</Alert>}

          {loading ? (
            <SkeletonList rows={2} />
          ) : pendingMentors.length === 0 ? (
            <EmptyState
              title="Нема барања за одобрување"
              description="Секој нов менторски профил ќе се појави тука пред да може да закажува сесии."
            />
          ) : (
            <ul className="flex flex-col gap-3">
              {pendingMentors.map((mentor) => (
                <li key={mentor.id}>
                  <Card className="flex flex-wrap items-center gap-3">
                    <Link to={`/users/${mentor.id}`} title="Отвори профил">
                      <Avatar name={mentor.fullName} src={mentor.avatarUrl} />
                    </Link>

                    <div className="min-w-0 flex-1">
                      <Link
                        to={`/users/${mentor.id}`}
                        className="block truncate font-medium text-ink hover:text-brand hover:underline"
                      >
                        {mentor.fullName}
                      </Link>
                      <p className="truncate text-xs text-ink-mute">{mentor.email}</p>
                      <p className="text-xs text-ink-mute">
                        Регистриран/а {formatDate(mentor.createdAt)}
                      </p>
                    </div>

                    <div className="flex shrink-0 gap-2">
                      <Button
                        variant="good"
                        size="sm"
                        disabled={decidingId === mentor.id}
                        onClick={() => decideMentor(mentor.id, true)}
                      >
                        Одобри
                      </Button>
                      <Button
                        variant="bad"
                        size="sm"
                        disabled={decidingId === mentor.id}
                        onClick={() => decideMentor(mentor.id, false)}
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

        <Section title="Предмети">
          <form onSubmit={handleAddSubject} className="mb-4 flex gap-2">
            <Input
              value={newSubject}
              onChange={(event) => setNewSubject(event.target.value)}
              placeholder="Нов предмет..."
              aria-label="Име на нов предмет"
            />
            <Button type="submit" disabled={addingSubject} className="shrink-0">
              Додади
            </Button>
          </form>

          {subjectError && <Alert className="mb-3">{subjectError}</Alert>}

          {loading ? (
            <SkeletonList rows={5} />
          ) : (
            <div className="flex flex-wrap gap-2">
              {subjects.map((subject) => (
                <SubjectBadge key={subject.id} name={subject.name} />
              ))}
            </div>
          )}
        </Section>
      </div>
    </Layout>
  )
}
