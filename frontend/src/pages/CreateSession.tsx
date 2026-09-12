import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Layout from '../components/Layout'
import { Alert, Badge, Button, Card, Field, Input, PageHeader, Select, Textarea } from '../components/ui'
import { SchedulePicker } from '../components/SchedulePicker'
import { useAuth } from '../context/AuthContext'
import { useAsync } from '../hooks/useAsync'
import { getColleagues } from '../api/mentor'
import { createSession } from '../api/sessions'
import { getSubjects } from '../api/subjects'
import { cn } from '../lib/cn'
import { getErrorMessage } from '../lib/errors'
import { formatFullDate } from '../lib/format'
import { sessionModeLabel } from '../lib/labels'
import { combine, durationLabel, toMinutes } from '../lib/schedule'
import type { SessionMode } from '../types'

const STEPS = ['Основно', 'Датум и време', 'Ко-ментор и тагови', 'Преглед'] as const

interface FormState {
  title: string
  description: string
  subjectId: string
  mode: SessionMode
  location: string
  /** `2026-09-11` */
  date: string
  /** `14:30` — часот е одделен од датумот, види коментарот кај чекор 2. */
  start: string
  end: string
  coMentorId: string
  tagInput: string
  tags: string[]
}

const INITIAL_FORM: FormState = {
  title: '',
  description: '',
  subjectId: '',
  mode: 'ONLINE',
  location: '',
  date: '',
  start: '10:00',
  end: '11:30',
  coMentorId: '',
  tagInput: '',
  tags: [],
}

export default function CreateSession() {
  const navigate = useNavigate()
  const { user } = useAuth()

  const [step, setStep] = useState(0)
  const [form, setForm] = useState<FormState>(INITIAL_FORM)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const subjects = useAsync(() => getSubjects(), [])
  const colleagues = useAsync(() => getColleagues(), [])

  function update<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((current) => ({ ...current, [key]: value }))
  }

  function addTag() {
    const tag = form.tagInput.trim()
    setForm((current) => ({
      ...current,
      tagInput: '',
      tags: tag && !current.tags.includes(tag) ? [...current.tags, tag] : current.tags,
    }))
  }

  function validateStep(): string | null {
    if (step === 0) {
      if (!form.title.trim()) return 'Насловот е задолжителен.'
      if (!form.subjectId) return 'Избери предмет.'
    }

    if (step === 1) {
      if (!form.date) return 'Избери датум.'
      if (toMinutes(form.end) <= toMinutes(form.start)) {
        return 'Крајот мора да биде по почетокот.'
      }
      // Датумот може да е избран пред некое време, па почетокот да е веќе поминат
      if (new Date(combine(form.date, form.start)).getTime() <= Date.now()) {
        return 'Почетокот мора да биде во иднина.'
      }
      if (form.mode === 'IN_PERSON' && !form.location.trim()) {
        return 'Внеси локација за сесија во живо.'
      }
    }

    return null
  }

  function goNext() {
    const validationError = validateStep()
    if (validationError) {
      setError(validationError)
      return
    }

    setError(null)
    setStep((current) => Math.min(current + 1, STEPS.length - 1))
  }

  function goBack() {
    setError(null)
    setStep((current) => Math.max(current - 1, 0))
  }

  async function handleSubmit() {
    if (!user) {
      return
    }

    setSubmitting(true)
    setError(null)

    try {
      const session = await createSession({
        title: form.title.trim(),
        description: form.description.trim() || undefined,
        subjectId: Number(form.subjectId),
        // Менторот кој креира мора да е во листата (backend бара најмалку еден)
        mentorIds: [user.id, ...(form.coMentorId ? [Number(form.coMentorId)] : [])],
        mode: form.mode,
        location: form.location.trim() || undefined,
        startTime: combine(form.date, form.start),
        endTime: combine(form.date, form.end),
        tags: form.tags.length > 0 ? form.tags : undefined,
      })

      navigate(`/sessions/${session.id}`)
    } catch (err) {
      setError(getErrorMessage(err, 'Креирањето на сесијата не успеа.'))
    } finally {
      setSubmitting(false)
    }
  }

  const selectedSubject = (subjects.data ?? []).find((subject) => String(subject.id) === form.subjectId)
  const selectedColleague = (colleagues.data ?? []).find(
    (colleague) => String(colleague.id) === form.coMentorId,
  )

  return (
    <Layout>
      <PageHeader title="Нова сесија" subtitle={`Чекор ${step + 1} од ${STEPS.length}`} />

      {/* Степер */}
      <ol className="mb-8 flex items-center gap-2">
        {STEPS.map((label, index) => (
          <li key={label} className="flex flex-1 items-center gap-2">
            <span
              className={cn(
                'flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-xs font-medium transition',
                index < step && 'bg-brand/20 text-brand',
                index === step && 'bg-brand text-brand-on',
                index > step && 'bg-surface-2 text-ink-mute',
              )}
              aria-current={index === step ? 'step' : undefined}
            >
              {index < step ? '✓' : index + 1}
            </span>

            <span
              className={cn(
                'hidden whitespace-nowrap text-xs sm:block',
                index <= step ? 'text-ink' : 'text-ink-mute',
              )}
            >
              {label}
            </span>

            {index < STEPS.length - 1 && <span className="h-px flex-1 bg-line" />}
          </li>
        ))}
      </ol>

      <Card padding="lg">
        {error && <Alert className="mb-4">{error}</Alert>}

        {step === 0 && (
          <div className="flex flex-col gap-4">
            <Field label="Наслов">
              <Input
                value={form.title}
                onChange={(event) => update('title', event.target.value)}
                placeholder="пр. Подготовка за колоквиум по Алгоритми"
              />
            </Field>

            <Field label="Опис (опционално)">
              <Textarea
                rows={4}
                value={form.description}
                onChange={(event) => update('description', event.target.value)}
                placeholder="Што ќе покриете на сесијата?"
              />
            </Field>

            <Field label="Предмет">
              <Select
                value={form.subjectId}
                onChange={(event) => update('subjectId', event.target.value)}
              >
                <option value="">Избери предмет...</option>
                {(subjects.data ?? []).map((subject) => (
                  <option key={subject.id} value={subject.id}>
                    {subject.name}
                  </option>
                ))}
              </Select>
            </Field>
          </div>
        )}

        {step === 1 && (
          <div className="flex flex-col gap-4">
            <div>
              <span className="mb-1 block text-sm text-ink-soft">Начин на одржување</span>
              <div className="flex gap-3" role="radiogroup" aria-label="Начин на одржување">
                {(['ONLINE', 'IN_PERSON'] as SessionMode[]).map((mode) => (
                  <button
                    key={mode}
                    type="button"
                    role="radio"
                    aria-checked={form.mode === mode}
                    onClick={() => update('mode', mode)}
                    className={cn(
                      'flex-1 rounded-lg border px-3 py-2 text-sm transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/50',
                      form.mode === mode
                        ? 'border-brand bg-brand/10 text-brand'
                        : 'border-line text-ink-soft hover:border-ink-mute/50',
                    )}
                  >
                    {sessionModeLabel(mode)}
                  </button>
                ))}
              </div>
            </div>

            {form.mode === 'IN_PERSON' && (
              <Field label="Локација">
                <Input
                  value={form.location}
                  onChange={(event) => update('location', event.target.value)}
                  placeholder="пр. ФИНКИ, сала 210"
                />
              </Field>
            )}

            {form.mode === 'ONLINE' && (
              <Field label="Линк за средба (опционално)">
                <Input
                  value={form.location}
                  onChange={(event) => update('location', event.target.value)}
                  placeholder="пр. Teams / Meet линк"
                />
              </Field>
            )}

            <SchedulePicker
              value={{ date: form.date, start: form.start, end: form.end }}
              onChange={(next) => setForm((current) => ({ ...current, ...next }))}
            />
          </div>
        )}

        {step === 2 && (
          <div className="flex flex-col gap-4">
            <Field label="Ко-ментор (опционално)" hint="Најмногу двајца ментори по сесија — ти си веќе вклучен/а.">
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

            <div>
              <span className="mb-1 block text-sm text-ink-soft">Тагови (опционално)</span>
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
                <Button variant="secondary" onClick={addTag} className="shrink-0">
                  Додади
                </Button>
              </div>

              {form.tags.length > 0 && (
                <div className="mt-3 flex flex-wrap gap-2">
                  {form.tags.map((tag) => (
                    <span
                      key={tag}
                      className="flex items-center gap-1.5 rounded-full border border-line px-2.5 py-1 text-xs text-ink-soft"
                    >
                      {tag}
                      <button
                        type="button"
                        aria-label={`Отстрани ${tag}`}
                        onClick={() =>
                          update(
                            'tags',
                            form.tags.filter((current) => current !== tag),
                          )
                        }
                        className="text-ink-mute transition hover:text-bad"
                      >
                        ×
                      </button>
                    </span>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}

        {step === 3 && (
          <dl className="grid gap-4 text-sm sm:grid-cols-2">
            <ReviewRow label="Наслов" value={form.title || '—'} />
            <ReviewRow label="Предмет" value={selectedSubject?.name ?? '—'} />
            <ReviewRow label="Начин" value={sessionModeLabel(form.mode)} />
            <ReviewRow label={form.mode === 'ONLINE' ? 'Линк' : 'Локација'} value={form.location || '—'} />
            <ReviewRow
              label="Кога"
              value={form.date ? formatFullDate(combine(form.date, form.start)) : '—'}
            />
            <ReviewRow
              label="Време"
              value={`${form.start} – ${form.end} (${durationLabel(toMinutes(form.end) - toMinutes(form.start))})`}
            />
            <ReviewRow label="Ко-ментор" value={selectedColleague?.fullName ?? 'Нема'} />

            {form.description && (
              <div className="sm:col-span-2">
                <dt className="text-xs uppercase tracking-wide text-ink-mute">Опис</dt>
                <dd className="mt-0.5 whitespace-pre-line text-ink">{form.description}</dd>
              </div>
            )}

            {form.tags.length > 0 && (
              <div className="sm:col-span-2">
                <dt className="mb-1 text-xs uppercase tracking-wide text-ink-mute">Тагови</dt>
                <dd className="flex flex-wrap gap-2">
                  {form.tags.map((tag) => (
                    <Badge key={tag} tone="neutral">
                      {tag}
                    </Badge>
                  ))}
                </dd>
              </div>
            )}
          </dl>
        )}
      </Card>

      <div className="mt-6 flex justify-between">
        <Button variant="secondary" onClick={goBack} disabled={step === 0}>
          Назад
        </Button>

        {step < STEPS.length - 1 ? (
          <Button onClick={goNext}>Продолжи</Button>
        ) : (
          <Button onClick={handleSubmit} disabled={submitting}>
            {submitting ? 'Се креира...' : 'Креирај сесија'}
          </Button>
        )}
      </div>
    </Layout>
  )
}

function ReviewRow({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-xs uppercase tracking-wide text-ink-mute">{label}</dt>
      <dd className="mt-0.5 text-ink">{value}</dd>
    </div>
  )
}
