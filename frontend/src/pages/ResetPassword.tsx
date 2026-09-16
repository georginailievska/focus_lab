import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { AuthCard } from '../components/AuthCard'
import { Alert, Button, Field, PasswordInput } from '../components/ui'
import { resetPassword } from '../api/auth'
import { getErrorMessage } from '../lib/errors'

export default function ResetPassword() {
  const [params] = useSearchParams()
  const token = params.get('token') ?? ''
  const navigate = useNavigate()

  const [newPassword, setNewPassword] = useState('')
  const [repeated, setRepeated] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [done, setDone] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)

    if (newPassword !== repeated) {
      setError('Двете лозинки не се исти.')
      return
    }

    setSubmitting(true)
    try {
      await resetPassword({ token, newPassword })
      setDone(true)
    } catch (err) {
      setError(getErrorMessage(err, 'Поставувањето нова лозинка не успеа.'))
    } finally {
      setSubmitting(false)
    }
  }

  // Линк без токен: обично значи копирана само половина адреса од email-от
  if (!token) {
    return (
      <AuthCard
        subtitle="Линкот не е целосен."
        footer={
          <Link to="/forgot-password" className="text-brand hover:underline">
            Побарај нов линк
          </Link>
        }
      >
        <Alert className="mt-6">
          Линкот за нова лозинка не е валиден. Побарај нов од страницата „Заборавена лозинка".
        </Alert>
      </AuthCard>
    )
  }

  if (done) {
    return (
      <AuthCard
        subtitle="Лозинката е поставена."
        footer={
          <Link to="/login" className="text-brand hover:underline">
            Оди на најава
          </Link>
        }
      >
        <div className="mt-6 flex flex-col gap-4">
          <Alert tone="success">Новата лозинка е активна. Најави се со неа.</Alert>

          <Button className="w-full" onClick={() => navigate('/login')}>
            Најави се
          </Button>
        </div>
      </AuthCard>
    )
  }

  return (
    <AuthCard
      subtitle="Постави нова лозинка."
      footer={
        <Link to="/login" className="text-brand hover:underline">
          ← Назад на најава
        </Link>
      }
    >
      <form onSubmit={handleSubmit} className="mt-6 flex flex-col gap-4">
        <Field label="Нова лозинка" hint="Најмалку 8 карактери.">
          <PasswordInput
            autoComplete="new-password"
            required
            minLength={8}
            value={newPassword}
            onChange={(event) => setNewPassword(event.target.value)}
          />
        </Field>

        <Field label="Повтори нова лозинка">
          <PasswordInput
            autoComplete="new-password"
            required
            minLength={8}
            value={repeated}
            onChange={(event) => setRepeated(event.target.value)}
          />
        </Field>

        {error && <Alert>{error}</Alert>}

        <Button type="submit" disabled={submitting} className="mt-2 w-full">
          {submitting ? 'Се поставува...' : 'Постави лозинка'}
        </Button>
      </form>
    </AuthCard>
  )
}
