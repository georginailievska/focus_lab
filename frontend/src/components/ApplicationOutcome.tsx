import { Link } from 'react-router-dom'
import type { ApplicationStatus } from '../types'

interface Outcome {
  emoji: string
  title: string
  detail: string
  box: string
}

const OUTCOME: Record<ApplicationStatus, Outcome> = {
  ACCEPTED: {
    emoji: '✅',
    title: 'Пријавата е прифатена',
    detail: 'Местото ти е потврдено. Времето и местото на сесијата се погоре.',
    box: 'border-good/45 bg-good/10',
  },
  PENDING: {
    emoji: '⏳',
    title: 'Пријавата чека одлука',
    detail: 'Менторот ќе одлучи пред сесијата. Штом одлучи, добиваш email.',
    box: 'border-warm/50 bg-warm/10',
  },
  REJECTED: {
    emoji: '✖️',
    title: 'Пријавата е одбиена',
    detail: 'Местата на сесијата се ограничени.',
    box: 'border-bad/40 bg-bad/8',
  },
}

/** Исходот од својата пријава — прво што студентот треба да го види на сесијата. */
export function ApplicationOutcome({ status }: { status: ApplicationStatus }) {
  const { emoji, title, detail, box } = OUTCOME[status]

  return (
    <div className={`rounded-xl border px-4 py-3 ${box}`}>
      <p className="flex items-center gap-2 text-sm font-medium text-ink">
        <span aria-hidden="true">{emoji}</span>
        {title}
      </p>

      <p className="mt-1 text-xs text-ink-soft">
        {detail}
        {status === 'REJECTED' && (
          <>
            {' '}
            <Link to="/sessions" className="font-medium text-brand hover:underline">
              Побарај друга сесија по истиот предмет
            </Link>
            .
          </>
        )}
      </p>
    </div>
  )
}
