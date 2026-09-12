import { Button, Field, Input, TimeSelect } from './ui'
import {
  DAY_END,
  DAY_START,
  durationLabel,
  earliestStartFor,
  firstSelectableDate,
  MINUTE_STEP,
  snapClock,
  toClock,
  toMinutes,
} from '../lib/schedule'

export interface Schedule {
  /** `2026-09-11` */
  date: string
  /** `14:30` */
  start: string
  end: string
}

interface SchedulePickerProps {
  value: Schedule
  onChange: (next: Schedule) => void
  allowPast?: boolean
}

/** Готови траења — еден клик наместо пресметување на времето на крај. */
const DURATIONS = [45, 60, 90, 120]

export function SchedulePicker({ value, onChange, allowPast = false }: SchedulePickerProps) {
  const earliest = allowPast || !value.date ? DAY_START : earliestStartFor(value.date)
  const duration = Math.max(0, toMinutes(value.end) - toMinutes(value.start))

  function pickDate(date: string) {
    const minStart = allowPast || !date ? DAY_START : earliestStartFor(date)
    const start = snapClock(value.start, minStart)

    onChange({
      date,
      start,
      // Траењето се задржува — само се лепи на новиот почеток
      end: toClock(Math.min(toMinutes(start) + duration, DAY_END)),
    })
  }

  function pickStart(start: string) {
    const kept = Math.min(toMinutes(start) + duration, DAY_END)
    const end = Math.max(kept, toMinutes(start) + MINUTE_STEP)
    onChange({ ...value, start, end: toClock(Math.min(end, DAY_END)) })
  }

  return (
    <>
      <Field label="Датум" hint="Сесијата почнува и завршува истиот ден.">
        <Input
          type="date"
          className="w-full sm:w-56"
          value={value.date}
          min={allowPast ? undefined : firstSelectableDate()}
          onChange={(event) => pickDate(event.target.value)}
        />
      </Field>

      <div className="flex flex-wrap items-end gap-x-6 gap-y-4">
        <Field label="Почеток">
          <TimeSelect
            label="Почеток"
            value={value.start}
            minMinutes={earliest}
            onChange={pickStart}
          />
        </Field>

        <Field label="Крај">
          <TimeSelect
            label="Крај"
            value={value.end}
            minMinutes={toMinutes(value.start) + MINUTE_STEP}
            onChange={(end) => onChange({ ...value, end })}
          />
        </Field>

        <span className="pb-2.5 text-sm text-ink-mute">
          Траење: <span className="font-medium text-ink">{durationLabel(duration)}</span>
        </span>
      </div>

      <div className="flex flex-wrap items-center gap-2">
        <span className="text-xs font-medium uppercase tracking-wide text-ink-mute">
          Времетраење
        </span>
        {DURATIONS.map((minutes) => {
          const fits = toMinutes(value.start) + minutes <= DAY_END
          return (
            <Button
              key={minutes}
              variant={duration === minutes ? 'primary' : 'secondary'}
              size="sm"
              disabled={!fits}
              onClick={() => onChange({ ...value, end: toClock(toMinutes(value.start) + minutes) })}
            >
              {durationLabel(minutes)}
            </Button>
          )
        })}
      </div>

      <p className="text-xs text-ink-mute">Работно време: 08:00 – 20:00 часот.</p>
    </>
  )
}
