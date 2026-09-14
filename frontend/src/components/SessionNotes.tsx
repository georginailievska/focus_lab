import { useEffect, useState, type FormEvent } from 'react'
import { Avatar } from './Avatar'
import { Alert, Button, Card, Field, Section, Textarea } from './ui'
import { useAuth } from '../context/AuthContext'
import { addSessionNote, deleteSessionNote, getSessionNotes } from '../api/mentor'
import { getErrorMessage } from '../lib/errors'
import { timeAgo } from '../lib/format'
import type { SessionNote } from '../types'

const MAX_LENGTH = 1000

interface SessionNotesProps {
  sessionId: number
}

export function SessionNotes({ sessionId }: SessionNotesProps) {
  const { user } = useAuth()

  const [notes, setNotes] = useState<SessionNote[]>([])
  const [text, setText] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [problem, setProblem] = useState<string | null>(null)

  // Сопствен effect наместо useAsync: списокот се менува локално по додавање
  // и бришење, за да не се вчитува целата сесија по секоја забелешка.
  useEffect(() => {
    let active = true

    getSessionNotes(sessionId)
      .then((loaded) => {
        if (active) {
          setNotes(loaded)
        }
      })
      .catch((err) => {
        if (active) {
          setProblem(getErrorMessage(err, 'Забелешките не се вчитаа.'))
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

  async function submit(event: FormEvent) {
    event.preventDefault()

    const value = text.trim()
    if (!value) {
      return
    }

    setProblem(null)
    setSaving(true)

    try {
      const saved = await addSessionNote(sessionId, value)
      setNotes((current) => [saved, ...current])
      setText('')
    } catch (err) {
      setProblem(getErrorMessage(err, 'Забелешката не се зачува.'))
    } finally {
      setSaving(false)
    }
  }

  async function remove(noteId: number) {
    setProblem(null)

    try {
      await deleteSessionNote(noteId)
      setNotes((current) => current.filter((note) => note.id !== noteId))
    } catch (err) {
      setProblem(getErrorMessage(err, 'Бришењето не успеа.'))
    }
  }

  return (
    <Section title="Забелешки за менторите">
      <Card padding="lg" className="flex flex-col gap-4">
        <p className="flex items-center gap-2 text-xs text-ink-mute">
          <span aria-hidden="true">🔒</span>
          Забелешките ги гледаат само менторите.
        </p>

        <form onSubmit={submit} className="flex flex-col gap-3">
          <Field label="Нова забелешка">
            <Textarea
              rows={3}
              value={text}
              maxLength={MAX_LENGTH}
              onChange={(event) => setText(event.target.value)}
              placeholder="Пр. дојдоа 8 од 12; третата задача им беше најтешка."
            />
          </Field>

          <div className="flex items-center justify-between gap-3">
            <span className="text-xs text-ink-mute">
              {text.length}/{MAX_LENGTH}
            </span>
            <Button type="submit" size="sm" disabled={saving || !text.trim()}>
              {saving ? 'Се зачувува...' : 'Зачувај'}
            </Button>
          </div>
        </form>

        {problem && <Alert>{problem}</Alert>}

        {loading ? (
          <p className="border-t border-line pt-4 text-sm text-ink-mute">Се вчитува...</p>
        ) : notes.length === 0 ? (
          <p className="border-t border-line pt-4 text-sm text-ink-mute">
            Сè уште нема забелешки за оваа сесија.
          </p>
        ) : (
          <ul className="flex flex-col gap-3 border-t border-line pt-4">
            {notes.map((note) => (
              <li key={note.id} className="flex items-start gap-2.5">
                <Avatar name={note.author.fullName} src={note.author.avatarUrl} size="sm" />

                <div className="min-w-0 flex-1 rounded-xl bg-surface-2 px-3 py-2">
                  <div className="flex flex-wrap items-center gap-x-2">
                    <span className="text-sm font-medium text-ink">{note.author.fullName}</span>
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

                  <p className="mt-0.5 whitespace-pre-line break-words text-sm text-ink-soft">
                    {note.text}
                  </p>
                </div>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </Section>
  )
}
