import { useEffect, useState } from 'react'
import Layout from '../components/Layout'
import { Alert, Button, PageHeader, Skeleton } from '../components/ui'
import { useAsync } from '../hooks/useAsync'
import { getMyInterests, updateMyInterests } from '../api/interests'
import { getSubjects } from '../api/subjects'
import { cn } from '../lib/cn'
import { getErrorMessage } from '../lib/errors'
import { solidColorFor } from '../lib/palette'

function sameSelection(a: Set<number>, b: Set<number>): boolean {
  return a.size === b.size && [...a].every((id) => b.has(id))
}

export default function MyInterests() {
  const subjects = useAsync(() => getSubjects(), [])
  const interests = useAsync(() => getMyInterests(), [])

  const [selected, setSelected] = useState<Set<number>>(new Set())
  const [saved, setSaved] = useState<Set<number>>(new Set())
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [justSaved, setJustSaved] = useState(false)

  // Штом ќе дојдат зачуваните интереси, тие се и почетна и референтна состојба
  useEffect(() => {
    if (interests.data) {
      const ids = new Set(interests.data.map((subject) => subject.id))
      setSelected(ids)
      setSaved(ids)
    }
  }, [interests.data])

  function toggle(subjectId: number) {
    setJustSaved(false)
    setSelected((current) => {
      const next = new Set(current)
      if (next.has(subjectId)) {
        next.delete(subjectId)
      } else {
        next.add(subjectId)
      }
      return next
    })
  }

  async function handleSave() {
    setError(null)
    setSaving(true)

    try {
      const updated = await updateMyInterests({ subjectIds: [...selected] })
      const ids = new Set(updated.map((subject) => subject.id))
      setSelected(ids)
      setSaved(ids)
      setJustSaved(true)
    } catch (err) {
      setError(getErrorMessage(err, 'Интересите не се зачуваа.'))
    } finally {
      setSaving(false)
    }
  }

  const loading = subjects.loading || interests.loading
  const dirty = !sameSelection(selected, saved)
  const loadError = subjects.error ?? interests.error

  return (
    <Layout>
      <PageHeader
        title="Мои интереси"
        subtitle="Избери предметите што те интересираат. Кога ментор ќе закаже нова сесија по некој од нив, ќе добиеш email со линк до сесијата."
        actions={
          <Button onClick={handleSave} disabled={saving || !dirty}>
            {saving ? 'Се зачувува...' : dirty ? 'Зачувај' : 'Зачувано'}
          </Button>
        }
      />

      {loadError && <Alert className="mb-6">{loadError}</Alert>}
      {error && <Alert className="mb-6">{error}</Alert>}

      {justSaved && !dirty && (
        <Alert tone="success" className="mb-6">
          Интересите се зачувани. Од сега ќе добиваш известување за нови сесии по тие предмети.
        </Alert>
      )}

      {loading ? (
        <div className="grid gap-3 sm:grid-cols-2">
          {Array.from({ length: 4 }).map((_, index) => (
            <Skeleton key={index} className="h-16" />
          ))}
        </div>
      ) : (
        <>
          <ul className="grid gap-3 sm:grid-cols-2">
            {(subjects.data ?? []).map((subject) => {
              const active = selected.has(subject.id)

              return (
                <li key={subject.id}>
                  <button
                    type="button"
                    aria-pressed={active}
                    onClick={() => toggle(subject.id)}
                    className={cn(
                      'flex w-full items-center gap-3 rounded-2xl border px-4 py-3.5 text-left transition',
                      'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/60 focus-visible:ring-offset-2 focus-visible:ring-offset-page',
                      active
                        ? 'border-brand/50 bg-brand/8 shadow-soft'
                        : 'border-line bg-surface hover:border-ink-mute/50',
                    )}
                  >
                    <span
                      aria-hidden="true"
                      style={{ backgroundColor: solidColorFor(subject.name) }}
                      className="h-2.5 w-2.5 shrink-0 rounded-full"
                    />

                    <span className={cn('min-w-0 flex-1 text-sm', active ? 'font-medium text-ink' : 'text-ink-soft')}>
                      {subject.name}
                    </span>

                    {/* Празно/пополнето квадратче — состојбата се чита на прв поглед */}
                    <span
                      aria-hidden="true"
                      className={cn(
                        'grid h-5 w-5 shrink-0 place-items-center rounded-md border text-[11px] font-bold transition',
                        active
                          ? 'border-brand bg-brand text-brand-on'
                          : 'border-line text-transparent',
                      )}
                    >
                      ✓
                    </span>
                  </button>
                </li>
              )
            })}
          </ul>

          <p className="mt-6 text-sm text-ink-mute">
            {selected.size === 0
              ? 'Ниту еден предмет не е избран — нема да добиваш известувања.'
              : `Избрани: ${selected.size}.`}
            {dirty && ' Промените не се зачувани.'}
          </p>
        </>
      )}
    </Layout>
  )
}
