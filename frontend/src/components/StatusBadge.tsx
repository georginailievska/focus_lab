import { Badge, type BadgeTone } from './ui'
import type { ApplicationStatus, MentorStatus } from '../types'

const APPLICATION: Record<ApplicationStatus, { label: string; tone: BadgeTone; dot: string }> = {
  ACCEPTED: { label: 'Прифатена', tone: 'good', dot: 'bg-good' },
  PENDING: { label: 'Чека одлука', tone: 'warm', dot: 'bg-warm' },
  REJECTED: { label: 'Одбиена', tone: 'bad', dot: 'bg-bad' },
}

const MENTOR: Record<MentorStatus, { label: string; tone: BadgeTone; dot: string }> = {
  APPROVED: { label: 'Одобрен/а', tone: 'good', dot: 'bg-good' },
  PENDING: { label: 'Чека одобрување', tone: 'warm', dot: 'bg-warm' },
  REJECTED: { label: 'Одбиен/а', tone: 'bad', dot: 'bg-bad' },
}

/** Статус на пријава — истата боја и текст на секој екран каде се појавува. */
export function ApplicationStatusBadge({ status }: { status: ApplicationStatus }) {
  const { label, tone, dot } = APPLICATION[status]

  return (
    <Badge tone={tone}>
      <span aria-hidden="true" className={`h-1.5 w-1.5 rounded-full ${dot}`} />
      {label}
    </Badge>
  )
}

export function MentorStatusBadge({ status }: { status: MentorStatus | null }) {
  if (!status) {
    return null
  }

  const { label, tone, dot } = MENTOR[status]

  return (
    <Badge tone={tone}>
      <span aria-hidden="true" className={`h-1.5 w-1.5 rounded-full ${dot}`} />
      {label}
    </Badge>
  )
}
