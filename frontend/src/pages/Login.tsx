import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { AuthCard } from '../components/AuthCard'
import { Alert, Button, Field, Input, PasswordInput } from '../components/ui'
import { useAuth } from '../context/AuthContext'
import { getErrorMessage } from '../lib/errors'
import { homeRouteFor } from '../lib/routes'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)

    try {
      const user = await login({ email, password })
      navigate(homeRouteFor(user.role))
    } catch (err) {
      setError(getErrorMessage(err, 'Најавата не успеа.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthCard
      subtitle="Најави се за да продолжиш."
      footer={
        <>
          Немаш профил?{' '}
          <Link to="/register" className="text-brand hover:underline">
            Регистрирај се
          </Link>
        </>
      }
    >
      <form onSubmit={handleSubmit} className="mt-6 flex flex-col gap-4">
        <Field label="Email">
          <Input
            type="email"
            autoComplete="email"
            required
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />
        </Field>

        <Field label="Лозинка">
          <PasswordInput
            autoComplete="current-password"
            required
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
        </Field>

        <div className="-mt-1 text-right">
          <Link to="/forgot-password" className="text-xs text-ink-mute hover:text-brand hover:underline">
            Заборавена лозинка?
          </Link>
        </div>

        {error && <Alert>{error}</Alert>}

        <Button type="submit" disabled={submitting} className="mt-2 w-full">
          {submitting ? 'Најавување...' : 'Најави се'}
        </Button>
      </form>
    </AuthCard>
  )
}
