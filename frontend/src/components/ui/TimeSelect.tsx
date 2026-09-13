import { Select } from './Field'
import {
  DAY_START,
  hourOptions,
  minuteOptions,
  toClock,
  toMinutes,
} from '../../lib/schedule'

interface TimeSelectProps {
  /** Времето како `HH:mm`. */
  value: string
  onChange: (value: string) => void
  /** Најраното дозволено време, во минути од полноќ. */
  minMinutes?: number
  disabled?: boolean
  label: string
}

const pad = (value: number) => String(value).padStart(2, '0')

export function TimeSelect({
  value,
  onChange,
  minMinutes = DAY_START,
  disabled = false,
  label,
}: TimeSelectProps) {
  const total = toMinutes(value)
  const hour = Math.floor(total / 60)
  const minute = total % 60

  const hours = hourOptions(minMinutes)
  const minutes = minuteOptions(hour, minMinutes)

  function pickHour(nextHour: number) {
    const allowed = minuteOptions(nextHour, minMinutes)
    const nextMinute = allowed.includes(minute) ? minute : allowed[0] ?? 0
    onChange(toClock(nextHour * 60 + nextMinute))
  }

  return (
    <span className="flex items-center gap-1.5" role="group" aria-label={label}>
      <Select
        aria-label={`${label} — час`}
        className="w-[72px] px-2 text-center tabular-nums"
        disabled={disabled}
        value={hour}
        onChange={(event) => pickHour(Number(event.target.value))}
      >
        {hours.map((option) => (
          <option key={option} value={option}>
            {pad(option)}
          </option>
        ))}
      </Select>

      <span aria-hidden="true" className="text-sm font-semibold text-ink-mute">
        :
      </span>

      <Select
        aria-label={`${label} — минути`}
        className="w-[72px] px-2 text-center tabular-nums"
        disabled={disabled}
        value={minute}
        onChange={(event) => onChange(toClock(hour * 60 + Number(event.target.value)))}
      >
        {minutes.map((option) => (
          <option key={option} value={option}>
            {pad(option)}
          </option>
        ))}
      </Select>
    </span>
  )
}
