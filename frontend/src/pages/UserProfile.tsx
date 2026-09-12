import { Navigate, useNavigate, useParams } from 'react-router-dom'
import Layout from '../components/Layout'
import { ProfileHeader } from '../components/ProfileHeader'
import { SessionCard } from '../components/SessionCard'
import { Alert, Button, EmptyState, Section, Skeleton, StatCard } from '../components/ui'
import { useAuth } from '../context/AuthContext'
import { useAsync } from '../hooks/useAsync'
import { getUserProfile } from '../api/profile'
import { getSessions } from '../api/sessions'
import { isPast } from '../lib/format'

export default function UserProfile() {
  const { id } = useParams<{ id: string }>()
  const userId = Number(id)
  const { user } = useAuth()
  const navigate = useNavigate()

  const { data, loading, error } = useAsync(async () => {
    const profile = await getUserProfile(userId)

    const sessions =
      profile.role === 'MENTOR'
        ? (await getSessions())
            .filter((session) => session.mentors.some((mentor) => mentor.id === userId))
            .filter((session) => !isPast(session.endTime))
        : []

    return { profile, sessions }
  }, [userId])

  if (user && user.id === userId) {
    return <Navigate to="/profile" replace />
  }

  return (
    <Layout>
      <Button variant="ghost" size="sm" onClick={() => navigate(-1)} className="mb-4 -ml-2">
        ← Назад
      </Button>

      {loading && <Skeleton className="h-44 w-full" />}

      {!loading && (error || !data) && <Alert>{error ?? 'Профилот не е најден.'}</Alert>}

      {!loading && data && (
        <div className="flex flex-col gap-7">
          <ProfileHeader profile={data.profile} />

          {data.profile.role === 'MENTOR' && (
            <>
              <div className="grid grid-cols-2 gap-3">
                <StatCard label="Сесии" value={data.profile.stats.sessions} tone="brand" emoji="📚" />
                <StatCard
                  label="Претстојни сесии"
                  value={data.profile.stats.upcomingSessions}
                  tone="good"
                  emoji="📅"
                />
              </div>

              <Section title="Претстојни сесии">
                {data.sessions.length === 0 ? (
                  <EmptyState
                    emoji="📅"
                    title="Нема закажани сесии"
                    description="Штом овој ментор закаже нова сесија, ќе се појави тука."
                  />
                ) : (
                  <ul className="flex flex-col gap-3">
                    {data.sessions.map((session) => (
                      <li key={session.id}>
                        <SessionCard session={session} />
                      </li>
                    ))}
                  </ul>
                )}
              </Section>
            </>
          )}

          {data.profile.role === 'STUDENT' && (
            <div className="grid grid-cols-2 gap-3">
              <StatCard
                label="Пријави"
                value={data.profile.stats.applications}
                tone="brand"
                emoji="✋"
              />
              <StatCard
                label="Прифатени"
                value={data.profile.stats.acceptedApplications}
                tone="good"
                emoji="✅"
              />
            </div>
          )}
        </div>
      )}
    </Layout>
  )
}
