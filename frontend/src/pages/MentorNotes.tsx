import { useState } from 'react'
import { Link } from 'react-router-dom'
import Layout from '../components/Layout'
import { Avatar } from '../components/Avatar'
import { SubjectBadge } from '../components/SubjectBadge'
import {
  Alert,
  Card,
  EmptyState,
  PageHeader,
  Select,
  SkeletonList,
} from '../components/ui'
import { useAuth } from '../context/AuthContext'
import { useAsync } from '../hooks/useAsync'
import { deleteSessionNote, getAllNotes } from '../api/mentor'
import { getSubjects } from '../api/subjects'
import { formatDateTime, timeAgo } from '../lib/format'
import { getErrorMessage } from '../lib/errors'

export default function MentorNotes() {
  const { user } = useAuth()

  const [subjectId, setSubjectId] = useState('')
  const [removed, setRemoved] = useState<number[]>([])
  const [problem, setProblem] = useState<string | null>(null)

  const subjects = useAsync(getSubjects, [])
  const { data, loading, error } = useAsync(
    () => getAllNotes(subjectId ? Number(subjectId) : undefined),
    [subjectId],
  )

  const notes = (data ?? []).filter((note) => !removed.includes(note.id))

  async function remove(noteId: number) {
    setProblem(null)

    try {
      await deleteSessionNote(noteId)
      setRemoved((current) => [...current, noteId])
    } catch (err) {
      setProblem(getErrorMessage(err, 'Бришењето не успеа.'))
    }
  }

  return (
    <Layout>
      <PageHeader
        title="Забелешки"
        subtitle="Сите забелешки од сесиите, на едно место. Ги гледаат само менторите."
        actions={
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
        }
      />

      <div className="flex flex-col gap-4">
        {error && <Alert>{error}</Alert>}
        {problem && <Alert>{problem}</Alert>}

        {loading ? (
          <SkeletonList rows={4} />
        ) : notes.length === 0 ? (
          <EmptyState
            emoji="📝"
            title={subjectId ? 'Нема забелешки по овој предмет' : 'Сè уште нема забелешки'}
            description="Забелешка се додава од страницата на сесијата — таму каде што е и контекстот."
          />
        ) : (
          <ul className="flex flex-col gap-4">
            {notes.map((note) => (
              <li key={note.id}>
                <Card padding="lg" className="flex flex-col gap-3">
                  <div className="flex flex-wrap items-start justify-between gap-x-4 gap-y-2">
                    <Link
                      to={`/sessions/${note.sessionId}`}
                      className="min-w-0 font-medium text-ink hover:text-brand hover:underline"
                    >
                      {note.sessionTitle}
                    </Link>
                    <SubjectBadge name={note.subject.name} />
                  </div>

                  <p className="text-xs text-ink-mute">{formatDateTime(note.sessionStartTime)}</p>

                  <p className="whitespace-pre-line break-words border-t border-line pt-3 text-sm leading-relaxed text-ink-soft">
                    {note.text}
                  </p>

                  <div className="flex items-center gap-2.5">
                    <Avatar name={note.author.fullName} src={note.author.avatarUrl} size="sm" />
                    <span className="text-sm text-ink">{note.author.fullName}</span>
                    <span className="text-[11px] text-ink-mute">{timeAgo(note.createdAt)}</span>

                    {note.author.id === user?.id && (
                      <button
                        type="button"
                        aria-label="Избриши забелешка"
                        onClick={() => remove(note.id)}
                        className="ml-auto text-xs text-ink-mute transition hover:text-bad"
                      >
                        ×
                      </button>
                    )}
                  </div>
                </Card>
              </li>
            ))}
          </ul>
        )}
      </div>
    </Layout>
  )
}
