import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import Layout from '../components/Layout'
import { ProfileHeader } from '../components/ProfileHeader'
import { SubjectBadge } from '../components/SubjectBadge'
import {
  Alert,
  Button,
  Card,
  Field,
  Input,
  PageHeader,
  PasswordInput,
  Section,
  Skeleton,
  StatCard,
} from '../components/ui'
import { useAuth } from '../context/AuthContext'
import { useAsync } from '../hooks/useAsync'
import {
  changeMyPassword,
  getMyProfile,
  removeMyAvatar,
  updateMyProfile,
  uploadMyAvatar,
} from '../api/profile'
import { getErrorMessage } from '../lib/errors'
import type { Profile as ProfileData } from '../types'

/** Истите граници како на серверот — подобро е грешката да се види без качување. */
const MAX_IMAGE_BYTES = 2 * 1024 * 1024
const ALLOWED_TYPES = ['image/png', 'image/jpeg', 'image/jpg', 'image/webp', 'image/gif']

export default function Profile() {
  const { updateUser } = useAuth()
  const { data, loading, error } = useAsync(getMyProfile, [])

  const [profile, setProfile] = useState<ProfileData | null>(null)
  useEffect(() => setProfile(data), [data])

  if (loading) {
    return (
      <Layout>
        <PageHeader title="Мојот профил" />
        <Skeleton className="h-44 w-full" />
      </Layout>
    )
  }

  if (error || !profile) {
    return (
      <Layout>
        <PageHeader title="Мојот профил" />
        <Alert>{error ?? 'Профилот не можеше да се вчита.'}</Alert>
      </Layout>
    )
  }

  function applyProfile(updated: ProfileData) {
    setProfile(updated)
    updateUser({ fullName: updated.fullName, avatarUrl: updated.avatarUrl })
  }

  return (
    <Layout>
      <PageHeader
        title="Мојот профил"
        subtitle="Твоите податоци, профилна слика и лозинка."
      />

      <div className="flex flex-col gap-7">
        <ProfileHeader
          profile={profile}
          avatarAction={<AvatarButtons profile={profile} onChange={applyProfile} />}
        />

        <ProfileNumbers profile={profile} />

        <NameForm profile={profile} onChange={applyProfile} />

        {profile.role === 'STUDENT' && profile.interests && (
          <Section
            title="Мои интереси"
            action={
              <Link to="/interests" className="text-sm font-medium text-brand hover:underline">
                Промени
              </Link>
            }
          >
            <Card>
              {profile.interests.length === 0 ? (
                <p className="text-sm text-ink-mute">
                  Сè уште не си избрала предмети. Со избран интерес добиваш известување
                  на email кога ќе се закаже нова сесија по тој предмет.
                </p>
              ) : (
                <div className="flex flex-wrap gap-2">
                  {profile.interests.map((subject) => (
                    <SubjectBadge key={subject.id} name={subject.name} />
                  ))}
                </div>
              )}
            </Card>
          </Section>
        )}

        <PasswordForm />
      </div>
    </Layout>
  )
}

// ---- Слика ------------------------------------------------------------------

function AvatarButtons({
  profile,
  onChange,
}: {
  profile: ProfileData
  onChange: (profile: ProfileData) => void
}) {
  const fileInput = useRef<HTMLInputElement>(null)
  const [busy, setBusy] = useState(false)
  const [problem, setProblem] = useState<string | null>(null)

  async function handleFile(file: File) {
    setProblem(null)

    if (!ALLOWED_TYPES.includes(file.type)) {
      setProblem('Дозволени формати: PNG, JPG, WEBP или GIF.')
      return
    }

    if (file.size > MAX_IMAGE_BYTES) {
      setProblem('Сликата е поголема од 2 MB.')
      return
    }

    setBusy(true)
    try {
      onChange(await uploadMyAvatar(file))
    } catch (err) {
      setProblem(getErrorMessage(err, 'Качувањето не успеа.'))
    } finally {
      setBusy(false)
    }
  }

  async function handleRemove() {
    setProblem(null)
    setBusy(true)
    try {
      onChange(await removeMyAvatar())
    } catch (err) {
      setProblem(getErrorMessage(err, 'Отстранувањето не успеа.'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="flex flex-col items-center gap-2">
      <input
        ref={fileInput}
        type="file"
        accept={ALLOWED_TYPES.join(',')}
        className="sr-only"
        onChange={(event) => {
          const file = event.target.files?.[0]
          if (file) {
            void handleFile(file)
          }
          // Ресетирање: инаку избор на истата датотека по грешка не пали onChange
          event.target.value = ''
        }}
      />

      <Button size="sm" disabled={busy} onClick={() => fileInput.current?.click()}>
        {busy ? 'Се качува...' : profile.avatarUrl ? 'Смени слика' : 'Додај слика'}
      </Button>

      {profile.avatarUrl && (
        <Button variant="ghost" size="sm" disabled={busy} onClick={handleRemove}>
          Отстрани
        </Button>
      )}

      {problem && (
        <Alert className="mt-1 text-center text-xs">{problem}</Alert>
      )}
    </div>
  )
}

// ---- Бројки -----------------------------------------------------------------

function ProfileNumbers({ profile }: { profile: ProfileData }) {
  if (profile.role === 'MENTOR') {
    return (
      <div className="grid grid-cols-2 gap-3">
        <StatCard label="Сесии" value={profile.stats.sessions} tone="brand" emoji="📚" />
        <StatCard
          label="Претстојни сесии"
          value={profile.stats.upcomingSessions}
          tone="good"
          emoji="📅"
        />
      </div>
    )
  }

  if (profile.role === 'STUDENT') {
    return (
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
        <StatCard label="Пријави" value={profile.stats.applications} tone="brand" emoji="✋" />
        <StatCard
          label="Прифатени"
          value={profile.stats.acceptedApplications}
          tone="good"
          emoji="✅"
        />
        <StatCard
          label="Интереси"
          value={profile.interests?.length ?? 0}
          tone="warm"
          emoji="⭐"
        />
      </div>
    )
  }

  return null
}

// ---- Име --------------------------------------------------------------------

function NameForm({
  profile,
  onChange,
}: {
  profile: ProfileData
  onChange: (profile: ProfileData) => void
}) {
  const [fullName, setFullName] = useState(profile.fullName)
  const [saving, setSaving] = useState(false)
  const [problem, setProblem] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)

  const dirty = fullName.trim() !== profile.fullName

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setProblem(null)
    setSaved(false)
    setSaving(true)

    try {
      onChange(await updateMyProfile({ fullName: fullName.trim() }))
      setSaved(true)
    } catch (err) {
      setProblem(getErrorMessage(err, 'Промената не успеа.'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Section title="Лични податоци">
      <Card padding="lg">
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <Field label="Име и презиме" hint="Ова име го гледаат менторите и другите студенти.">
            <Input
              required
              minLength={3}
              maxLength={100}
              value={fullName}
              onChange={(event) => {
                setFullName(event.target.value)
                setSaved(false)
              }}
            />
          </Field>

          <Field label="Email" hint="Email адресата не може да се менува — со неа се најавуваш.">
            <Input value={profile.email ?? ''} disabled readOnly />
          </Field>

          {problem && <Alert>{problem}</Alert>}
          {saved && !dirty && <Alert tone="success">Профилот е зачуван.</Alert>}

          <div className="flex items-center gap-3">
            <Button type="submit" disabled={saving || !dirty}>
              {saving ? 'Се зачувува...' : 'Зачувај'}
            </Button>

            {dirty && (
              <Button variant="ghost" onClick={() => setFullName(profile.fullName)}>
                Откажи
              </Button>
            )}
          </div>
        </form>
      </Card>
    </Section>
  )
}

// ---- Лозинка ----------------------------------------------------------------

function PasswordForm() {
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [repeated, setRepeated] = useState('')
  const [saving, setSaving] = useState(false)
  const [problem, setProblem] = useState<string | null>(null)
  const [done, setDone] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setProblem(null)
    setDone(false)

    if (newPassword !== repeated) {
      setProblem('Двете нови лозинки не се исти.')
      return
    }

    setSaving(true)
    try {
      await changeMyPassword({ currentPassword, newPassword })
      setDone(true)
      setCurrentPassword('')
      setNewPassword('')
      setRepeated('')
    } catch (err) {
      setProblem(getErrorMessage(err, 'Промената на лозинка не успеа.'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Section title="Лозинка">
      <Card padding="lg">
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <Field label="Тековна лозинка">
            <PasswordInput
              autoComplete="current-password"
              required
              value={currentPassword}
              onChange={(event) => setCurrentPassword(event.target.value)}
            />
          </Field>

          <div className="grid gap-4 sm:grid-cols-2">
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
          </div>

          {problem && <Alert>{problem}</Alert>}
          {done && <Alert tone="success">Лозинката е променета.</Alert>}

          <div className="flex flex-wrap items-center gap-x-4 gap-y-2">
            <Button type="submit" disabled={saving}>
              {saving ? 'Се менува...' : 'Промени лозинка'}
            </Button>

            <Link to="/forgot-password" className="text-sm text-ink-mute hover:text-brand hover:underline">
              Ја заборави тековната лозинка?
            </Link>
          </div>
        </form>
      </Card>
    </Section>
  )
}
