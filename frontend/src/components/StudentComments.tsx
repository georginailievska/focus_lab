import { useEffect, useState, type FormEvent } from 'react'
import { Avatar } from './Avatar'
import { Alert, Badge, Button, Card, Field, Section, Textarea } from './ui'
import { useAuth } from '../context/AuthContext'
import {
  addStudentComment,
  changeCommentVisibility,
  deleteStudentComment,
  getStudentComments,
} from '../api/mentor'
import { cn } from '../lib/cn'
import { getErrorMessage } from '../lib/errors'
import { timeAgo } from '../lib/format'
import type { StudentComment } from '../types'

const MAX_LENGTH = 1000

/** Приватно е почетната поставка: делењето треба да е избор, не случајност. */
const DEFAULT_SHARED = false

interface StudentCommentsProps {
  studentId: number
  studentName: string
}

export function StudentComments({ studentId, studentName }: StudentCommentsProps) {
  const { user } = useAuth()

  const [comments, setComments] = useState<StudentComment[]>([])
  const [text, setText] = useState('')
  const [shared, setShared] = useState(DEFAULT_SHARED)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [problem, setProblem] = useState<string | null>(null)

  useEffect(() => {
    let active = true

    getStudentComments(studentId)
      .then((loaded) => {
        if (active) {
          setComments(loaded)
        }
      })
      .catch((err) => {
        if (active) {
          setProblem(getErrorMessage(err, 'Коментарите не се вчитаа.'))
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
  }, [studentId])

  async function submit(event: FormEvent) {
    event.preventDefault()

    const value = text.trim()
    if (!value) {
      return
    }

    setProblem(null)
    setSaving(true)

    try {
      const saved = await addStudentComment(studentId, value, shared)
      setComments((current) => [saved, ...current])
      setText('')
      setShared(DEFAULT_SHARED)
    } catch (err) {
      setProblem(getErrorMessage(err, 'Коментарот не се зачува.'))
    } finally {
      setSaving(false)
    }
  }

  async function toggleVisibility(comment: StudentComment) {
    setProblem(null)

    try {
      const updated = await changeCommentVisibility(comment.id, !comment.sharedWithMentors)
      setComments((current) => current.map((item) => (item.id === updated.id ? updated : item)))
    } catch (err) {
      setProblem(getErrorMessage(err, 'Видливоста не се смени.'))
    }
  }

  async function remove(commentId: number) {
    setProblem(null)

    try {
      await deleteStudentComment(commentId)
      setComments((current) => current.filter((item) => item.id !== commentId))
    } catch (err) {
      setProblem(getErrorMessage(err, 'Бришењето не успеа.'))
    }
  }

  return (
    <Section title="Коментари на менторите">
      <Card padding="lg" className="flex flex-col gap-4">
        <p className="flex items-center gap-2 text-xs text-ink-mute">
          <span aria-hidden="true">🔒</span>
          {studentName.split(' ')[0]} не ги гледа овие коментари.
        </p>

        <form onSubmit={submit} className="flex flex-col gap-3">
          <Field label="Нов коментар">
            <Textarea
              rows={3}
              value={text}
              maxLength={MAX_LENGTH}
              onChange={(event) => setText(event.target.value)}
              placeholder="Пр. добро подготвена, вреди да се повика и на следната сесија."
            />
          </Field>

          <div>
            <span className="mb-1.5 block text-sm font-medium text-ink">Кој смее да го гледа</span>
            <div className="flex flex-wrap gap-2" role="radiogroup" aria-label="Кој смее да го гледа">
              {[
                { value: false, label: 'Само јас' },
                { value: true, label: 'Сите ментори' },
              ].map((option) => (
                <button
                  key={String(option.value)}
                  type="button"
                  role="radio"
                  aria-checked={shared === option.value}
                  onClick={() => setShared(option.value)}
                  className={cn(
                    'rounded-lg border px-3 py-2 text-sm transition',
                    'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/50',
                    shared === option.value
                      ? 'border-brand bg-brand/10 text-brand'
                      : 'border-line text-ink-soft hover:border-ink-mute/50',
                  )}
                >
                  {option.label}
                </button>
              ))}
            </div>
          </div>

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
        ) : comments.length === 0 ? (
          <p className="border-t border-line pt-4 text-sm text-ink-mute">
            Сè уште нема коментари за овој студент.
          </p>
        ) : (
          <ul className="flex flex-col gap-3 border-t border-line pt-4">
            {comments.map((comment) => {
              const mine = comment.author.id === user?.id

              return (
                <li key={comment.id} className="flex items-start gap-2.5">
                  <Avatar name={comment.author.fullName} src={comment.author.avatarUrl} size="sm" />

                  <div className="min-w-0 flex-1 rounded-xl bg-surface-2 px-3 py-2">
                    <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
                      <span className="text-sm font-medium text-ink">{comment.author.fullName}</span>
                      <span className="text-[11px] text-ink-mute">{timeAgo(comment.createdAt)}</span>

                      <Badge tone={comment.sharedWithMentors ? 'brand' : 'neutral'}>
                        {comment.sharedWithMentors ? 'сите ментори' : 'само јас'}
                      </Badge>

                      {mine && (
                        <span className="ml-auto flex items-center gap-2">
                          <button
                            type="button"
                            onClick={() => toggleVisibility(comment)}
                            className="text-xs text-ink-mute transition hover:text-brand"
                          >
                            {comment.sharedWithMentors ? 'Направи приватен' : 'Подели'}
                          </button>
                          <button
                            type="button"
                            aria-label="Избриши коментар"
                            onClick={() => remove(comment.id)}
                            className="text-xs text-ink-mute transition hover:text-bad"
                          >
                            ×
                          </button>
                        </span>
                      )}
                    </div>

                    <p className="mt-0.5 whitespace-pre-line break-words text-sm text-ink-soft">
                      {comment.text}
                    </p>
                  </div>
                </li>
              )
            })}
          </ul>
        )}
      </Card>
    </Section>
  )
}
