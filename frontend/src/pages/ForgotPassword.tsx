import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { AuthCard } from '../components/AuthCard'
import { Alert, Button, Field, Input } from '../components/ui'
import { forgotPassword } from '../api/auth'
import { getErrorMessage } from '../lib/errors'

export default function ForgotPassword() {
  const [email, setEmail] = useState('')
  const [sent, setSent] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)

    try {
      await forgotPassword({ email })
      setSent(true)
    } catch (err) {
      setError(getErrorMessage(err, 'Барањето не успеа.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthCard
      subtitle={
        sent
          ? 'Провери го својот email.'
          : 'Внеси ја email адресата со која се најавуваш.'
      }
      footer={
        <Link to="/login" className="text-brand hover:underline">
          ← Назад на најава
        </Link>
      }
    >
      {sent ? (
        <div className="mt-6 flex flex-col gap-4">
          <Alert tone="success">
            Ако адресата е регистрирана, на неа пристигна линк за поставување нова лозинка.
            Линкот важи 30 минути.
          </Alert>

          <p className="text-sm text-ink-mute">
            Не пристигна ништо? Провери во „спам" или{' '}
            <button
              type="button"
              onClick={() => setSent(false)}
              className="font-medium text-brand hover:underline"
            >
              пробај со друга адреса
            </button>
            .
          </p>
        </div>
      ) : (
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

          {error && <Alert>{error}</Alert>}

          <Button type="submit" disabled={submitting} className="mt-2 w-full">
            {submitting ? 'Се праќа...' : 'Прати линк'}
          </Button>
        </form>
      )}
    </AuthCard>
  )
}
