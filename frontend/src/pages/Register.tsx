import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { AuthCard } from '../components/AuthCard'
import { Alert, Button, Field, Input, PasswordInput } from '../components/ui'
import { useAuth } from '../context/AuthContext'
import { cn } from '../lib/cn'
import { getErrorMessage } from '../lib/errors'
import { homeRouteFor } from '../lib/routes'
import type { RegisterRequest } from '../types'

type SelectableRole = RegisterRequest['role']

const ROLE_OPTIONS: Array<{ value: SelectableRole; label: string }> = [
  { value: 'STUDENT', label: 'Студент' },
  { value: 'MENTOR', label: 'Ментор' },
]

export default function Register() {
  const { register } = useAuth()
  const navigate = useNavigate()

  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState<SelectableRole>('STUDENT')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)

    try {
      const user = await register({ fullName, email, password, role })
      navigate(homeRouteFor(user.role))
    } catch (err) {
      setError(getErrorMessage(err, 'Регистрацијата не успеа.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthCard
      subtitle="Креирај нов профил."
      footer={
        <>
          Веќе имаш профил?{' '}
          <Link to="/login" className="text-brand hover:underline">
            Најави се
          </Link>
        </>
      }
    >
      <form onSubmit={handleSubmit} className="mt-6 flex flex-col gap-4">
        <Field label="Име и презиме">
          <Input
            autoComplete="name"
            required
            value={fullName}
            onChange={(event) => setFullName(event.target.value)}
          />
        </Field>

        <Field label="Email" hint="Само факултетски email адреса.">
          <Input
            type="email"
            autoComplete="email"
            required
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />
        </Field>

        <Field label="Лозинка" hint="Најмалку 8 карактери.">
          <PasswordInput
            autoComplete="new-password"
            required
            minLength={8}
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
        </Field>

        <div>
          <span className="mb-1 block text-sm text-ink-soft">Сум...</span>
          <div className="flex gap-2" role="radiogroup" aria-label="Улога">
            {ROLE_OPTIONS.map((option) => (
              <button
                key={option.value}
                type="button"
                role="radio"
                aria-checked={role === option.value}
                onClick={() => setRole(option.value)}
                className={cn(
                  'flex-1 rounded-lg border px-3 py-2 text-sm transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/50',
                  role === option.value
                    ? 'border-brand bg-brand/10 text-brand'
                    : 'border-line text-ink-soft hover:border-ink-mute/50',
                )}
              >
                {option.label}
              </button>
            ))}
          </div>

          {role === 'MENTOR' && (
            <p className="mt-2 text-xs text-ink-mute">
              Менторските профили чекаат одобрување од админ пред да можат да закажуваат сесии.
            </p>
          )}
        </div>

        {error && <Alert>{error}</Alert>}

        <Button type="submit" disabled={submitting} className="mt-2 w-full">
          {submitting ? 'Се регистрира...' : 'Регистрирај се'}
        </Button>
      </form>
    </AuthCard>
  )
}
