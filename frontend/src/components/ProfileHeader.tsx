import type { ReactNode } from 'react'
import { Avatar } from './Avatar'
import { Badge, Card } from './ui'
import { formatDate } from '../lib/format'
import { mentorStatusLabel, roleLabel } from '../lib/labels'
import type { Profile } from '../types'

interface ProfileHeaderProps {
  profile: Profile
  /** Копчиња за сликата — само на сопствениот профил. */
  avatarAction?: ReactNode
}

export function ProfileHeader({ profile, avatarAction }: ProfileHeaderProps) {
  const isMentor = profile.role === 'MENTOR'

  return (
    <Card padding="lg" className="relative overflow-hidden">
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-x-0 top-0 h-32 bg-gradient-to-b from-brand/18 via-accent/8 to-transparent"
      />

      <div className="relative flex flex-col items-center gap-5 sm:flex-row sm:items-start">
        <div className="flex flex-col items-center gap-3.5">
          <Avatar
            name={profile.fullName}
            src={profile.avatarUrl}
            size="xl"
            className="ring-4 ring-surface shadow-lift"
          />
          {avatarAction}
        </div>

        <div className="min-w-0 flex-1 text-center sm:pt-2 sm:text-left">
          <h2 className="font-display text-xl font-semibold tracking-tight text-ink">
            {profile.fullName}
          </h2>

          <div className="mt-2 flex flex-wrap items-center justify-center gap-2 sm:justify-start">
            <Badge tone="brand">{roleLabel(profile.role)}</Badge>
            {isMentor && (
              <Badge tone={profile.mentorStatus === 'APPROVED' ? 'good' : 'warm'}>
                {mentorStatusLabel(profile.mentorStatus)}
              </Badge>
            )}
          </div>

          <dl className="mt-4 grid gap-x-8 gap-y-2 text-sm sm:grid-cols-2">
            {/* email-от го има само сопственикот (и администраторот) */}
            {profile.email && (
              <div className="min-w-0">
                <dt className="text-xs uppercase tracking-wide text-ink-mute">Email</dt>
                <dd className="mt-0.5 truncate text-ink" title={profile.email}>
                  {profile.email}
                </dd>
              </div>
            )}

            <div>
              <dt className="text-xs uppercase tracking-wide text-ink-mute">Член од</dt>
              <dd className="mt-0.5 text-ink">{formatDate(profile.createdAt)}</dd>
            </div>
          </dl>
        </div>
      </div>
    </Card>
  )
}
