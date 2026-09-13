import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import Layout from '../components/Layout'
import { SchedulePicker, type Schedule } from '../components/SchedulePicker'
import {
  Alert,
  Badge,
  Button,
  Card,
  Field,
  Input,
  PageHeader,
  Section,
  Select,
  Skeleton,
  Textarea,
} from '../components/ui'
import { useAuth } from '../context/AuthContext'
import { useAsync } from '../hooks/useAsync'
import { getColleagues } from '../api/mentor'
import { deleteSession, getSession, updateSession } from '../api/sessions'
import { getSubjects } from '../api/subjects'
import { cn } from '../lib/cn'
import { getErrorMessage } from '../lib/errors'
import { combine, toMinutes } from '../lib/schedule'
import { sessionModeLabel } from '../lib/labels'
import type { SessionMode } from '../types'

interface FormState extends Schedule {
  title: string
  description: string
  subjectId: string
  mode: SessionMode
  location: string
  coMentorId: string
  tagInput: string
  tags: string[]
}

export default function EditSession() {
  const { id } = useParams<{ id: string }>()
  const sessionId = Number(id)
  const { user } = useAuth()
  const navigate = useNavigate()

  const { data: session, loading, error } = useAsync(() => getSession(sessionId), [sessionId])
  const subjects = useAsync(getSubjects, [])
  const colleagues = useAsync(getColleagues, [])

  const [form, setForm] = useState<FormState | null>(null)
  const [problem, setProblem] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [confirmingCancel, setConfirmingCancel] = useState(false)

  // Формата се полни од сесијата штом пристигне
  useEffect(() => {
    if (!session || !user) {
      return
    }

    const coMentor = session.mentors.find((mentor) => mentor.id !== user.id)

    setForm({
      title: session.title,
      description: session.description ?? '',
      subjectId: String(session.subject.id),
      mode: session.mode,
      location: session.location ?? '',
      date: session.startTime.slice(0, 10),
      start: session.startTime.slice(11, 16),
      end: session.endTime.slice(11, 16),
      coMentorId: coMentor ? String(coMentor.id) : '',
      tagInput: '',
      tags: session.tags,
    })
  }, [session, user])

  if (loading || !form) {
    return (
      <Layout>
        <PageHeader title="Измени сесија" />
        {error ? <Alert>{error}</Alert> : <Skeleton className="h-72 w-full" />}
      </Layout>
    )
  }

  const owns = session?.mentors.some((mentor) => mentor.id === user?.id) ?? false

  if (!owns) {
    return (
      <Layout>
        <PageHeader title="Измени сесија" />
        <Alert>Само менторите на сесијата можат да ја менуваат.</Alert>
      </Layout>
    )
  }

  function update<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((current) => (current ? { ...current, [key]: value } : current))
  }

  function addTag() {
    setForm((current) => {
      if (!current) {
        return current
      }
      const tag = current.tagInput.trim()
      return {
        ...current,
        tagInput: '',
        tags: tag && !current.tags.includes(tag) ? [...current.tags, tag] : current.tags,
      }
    })
  }

  function validate(state: FormState): string | null {
    if (!state.title.trim()) return 'Насловот е задолжителен.'
    if (!state.subjectId) return 'Избери предмет.'
    if (!state.date) return 'Избери датум.'
    if (toMinutes(state.end) <= toMinutes(state.start)) return 'Крајот мора да биде по почетокот.'
    if (state.mode === 'IN_PERSON' && !state.location.trim()) {
      return 'Внеси локација за сесија во живо.'
    }
    return null
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()

    if (!form || !user) {
      return
    }

    const invalid = validate(form)
    if (invalid) {
      setProblem(invalid)
      return
    }

    setProblem(null)
    setSaving(true)

    try {
      await updateSession(sessionId, {
        title: form.title.trim(),
        description: form.description.trim() || undefined,
        subjectId: Number(form.subjectId),
        mentorIds: [user.id, ...(form.coMentorId ? [Number(form.coMentorId)] : [])],
        mode: form.mode,
        location: form.location.trim() || undefined,
        startTime: combine(form.date, form.start),
        endTime: combine(form.date, form.end),
        tags: form.tags.length > 0 ? form.tags : undefined,
      })

      navigate(`/sessions/${sessionId}`)
    } catch (err) {
      setProblem(getErrorMessage(err, 'Промената не успеа.'))
    } finally {
      setSaving(false)
    }
  }

  async function handleCancelSession() {
    setProblem(null)
    setSaving(true)

    try {
      await deleteSession(sessionId)
      navigate('/mentor')
    } catch (err) {
      setProblem(getErrorMessage(err, 'Откажувањето не успеа.'))
      setSaving(false)
    }
  }

  const applicants = session?.applicantsCount ?? 0

  return (
    <Layout>
      <PageHeader
        title="Измени сесија"
        subtitle={
          applicants > 0
            ? `${applicants} пријавени студенти ќе добијат известување за промената.`
            : 'Сесијата сè уште нема пријавени студенти.'
        }
        actions={
          <Link to={`/sessions/${sessionId}`} className="text-sm font-medium text-brand hover:underline">
            Назад до сесијата
          </Link>
        }
      />

      <form onSubmit={handleSubmit} className="flex flex-col gap-7">
        <Card padding="lg" className="flex flex-col gap-4">
          <Field label="Наслов">
            <Input value={form.title} onChange={(event) => update('title', event.target.value)} />
          </Field>

          <Field label="Предмет">
            <Select
              value={form.subjectId}
              onChange={(event) => update('subjectId', event.target.value)}
            >
              <option value="">Избери предмет</option>
              {(subjects.data ?? []).map((subject) => (
                <option key={subject.id} value={subject.id}>
                  {subject.name}
                </option>
              ))}
            </Select>
          </Field>

          <Field label="Опис (опционално)">
            <Textarea
              rows={3}
              value={form.description}
              onChange={(event) => update('description', event.target.value)}
            />
          </Field>
        </Card>

        <Card padding="lg" className="flex flex-col gap-4">
          <Field label="Начин на одржување">
            <div className="grid grid-cols-2 gap-2">
              {(['ONLINE', 'IN_PERSON'] as const).map((mode) => (
                <button
                  key={mode}
                  type="button"
                  onClick={() => update('mode', mode)}
                  className={cn(
                    'rounded-xl border px-4 py-2.5 text-sm font-medium transition',
                    form.mode === mode
                      ? 'border-brand bg-brand/10 text-brand'
                      : 'border-line bg-surface text-ink-soft hover:bg-surface-2',
                  )}
                >
                  {sessionModeLabel(mode)}
                </button>
              ))}
            </div>
          </Field>

          <Field label={form.mode === 'ONLINE' ? 'Линк за средба (опционално)' : 'Локација'}>
            <Input
              value={form.location}
              onChange={(event) => update('location', event.target.value)}
              placeholder={form.mode === 'ONLINE' ? 'пр. Teams / Meet линк' : 'пр. ФИНКИ, сала 210'}
            />
          </Field>

          <SchedulePicker
            allowPast
            value={{ date: form.date, start: form.start, end: form.end }}
            onChange={(next) => setForm((current) => (current ? { ...current, ...next } : current))}
          />
        </Card>

        <Card padding="lg" className="flex flex-col gap-4">
          <Field label="Ко-ментор (опционално)" hint="Најмногу двајца ментори по сесија.">
            <Select
              value={form.coMentorId}
              onChange={(event) => update('coMentorId', event.target.value)}
            >
              <option value="">Без ко-ментор</option>
              {(colleagues.data ?? []).map((colleague) => (
                <option key={colleague.id} value={colleague.id}>
                  {colleague.fullName}
                </option>
              ))}
            </Select>
          </Field>

          <Field label="Тагови (опционално)">
            <div className="flex gap-2">
              <Input
                value={form.tagInput}
                onChange={(event) => update('tagInput', event.target.value)}
                onKeyDown={(event) => {
                  if (event.key === 'Enter') {
                    event.preventDefault()
                    addTag()
                  }
                }}
                placeholder="пр. колоквиум"
              />
              <Button variant="secondary" onClick={addTag}>
                Додај
              </Button>
            </div>
          </Field>

          {form.tags.length > 0 && (
            <div className="flex flex-wrap gap-2">
              {form.tags.map((tag) => (
                <Badge key={tag} tone="brand">
                  {tag}
                  <button
                    type="button"
                    aria-label={`Тргни ${tag}`}
                    onClick={() => update('tags', form.tags.filter((item) => item !== tag))}
                    className="ml-0.5 text-brand/70 hover:text-brand"
                  >
                    ×
                  </button>
                </Badge>
              ))}
            </div>
          )}
        </Card>

        {problem && <Alert>{problem}</Alert>}

        <div className="flex flex-wrap items-center gap-3">
          <Button type="submit" size="lg" disabled={saving}>
            {saving ? 'Се зачувува...' : 'Зачувај промени'}
          </Button>
          <Link to={`/sessions/${sessionId}`}>
            <Button variant="ghost" size="lg">
              Откажи
            </Button>
          </Link>
        </div>
      </form>

      <div className="mt-10 border-t border-line pt-8">
        <Section title="Откажи сесија">
        <Card padding="lg" className="border-bad/30">
          <p className="text-sm text-ink-soft">
            Сесијата се брише и {applicants > 0 ? 'сите пријавени' : 'пријавените'} студенти добиваат
            известување на email. Ова не може да се врати.
          </p>

          {confirmingCancel ? (
            <div className="mt-4 flex flex-wrap items-center gap-3">
              <span className="text-sm font-medium text-ink">Сигурно?</span>
              <Button variant="bad" disabled={saving} onClick={handleCancelSession}>
                {saving ? 'Се откажува...' : 'Да, откажи сесијата'}
              </Button>
              <Button variant="ghost" disabled={saving} onClick={() => setConfirmingCancel(false)}>
                Не
              </Button>
            </div>
          ) : (
            <Button variant="bad" className="mt-4" onClick={() => setConfirmingCancel(true)}>
              Откажи сесија
            </Button>
          )}
          </Card>
        </Section>
      </div>
    </Layout>
  )
}
