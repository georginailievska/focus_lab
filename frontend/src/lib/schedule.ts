
const EARLIEST_HOUR = 8
const LATEST_HOUR = 20
export const MINUTE_STEP = 5

/** 08:00 и 20:00 во минути. */
export const DAY_START = EARLIEST_HOUR * 60
export const DAY_END = LATEST_HOUR * 60

const pad = (value: number) => String(value).padStart(2, '0')

export function toMinutes(clock: string): number {
  const [hours, minutes] = clock.split(':').map(Number)
  return hours * 60 + minutes
}

export function toClock(minutes: number): string {
  return `${pad(Math.floor(minutes / 60))}:${pad(minutes % 60)}`
}

/** Часовите што имаат барем еден дозволен избор по дадената граница. */
export function hourOptions(minMinutes = DAY_START): number[] {
  const hours: number[] = []

  for (let hour = EARLIEST_HOUR; hour <= LATEST_HOUR; hour += 1) {
    const lastInHour = Math.min(hour * 60 + 60 - MINUTE_STEP, DAY_END)
    if (lastInHour >= minMinutes && hour * 60 <= DAY_END) {
      hours.push(hour)
    }
  }

  return hours
}

export function minuteOptions(hour: number, minMinutes = DAY_START): number[] {
  const minutes: number[] = []

  for (let minute = 0; minute < 60; minute += MINUTE_STEP) {
    const total = hour * 60 + minute
    if (total >= minMinutes && total <= DAY_END) {
      minutes.push(minute)
    }
  }

  return minutes
}

/** Најблиското дозволено време што не е пред `minMinutes`. */
export function snapClock(clock: string, minMinutes = DAY_START): string {
  const total = Math.max(minMinutes, Math.min(DAY_END, toMinutes(clock)))
  const rounded = Math.ceil(total / MINUTE_STEP) * MINUTE_STEP
  return toClock(Math.min(rounded, DAY_END))
}

/** `2026-09-11` + `14:30` → `2026-09-11T14:30:00` (форматот што го чека API-то). */
export function combine(date: string, clock: string): string {
  return `${date}T${clock}:00`
}

/** Денешен датум како `2026-09-11` — за `min` на полето за датум. */
function todayDate(): string {
  const now = new Date()
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
}

function nowMinutes(): number {
  const now = new Date()
  return now.getHours() * 60 + now.getMinutes()
}

export function firstSelectableDate(): string {
  if (nowMinutes() + MINUTE_STEP <= DAY_END) {
    return todayDate()
  }

  const now = new Date()
  const tomorrow = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1)
  return `${tomorrow.getFullYear()}-${pad(tomorrow.getMonth() + 1)}-${pad(tomorrow.getDate())}`
}

export function earliestStartFor(date: string): number {
  if (date !== todayDate()) {
    return DAY_START
  }

  const current = nowMinutes()
  if (current + MINUTE_STEP > DAY_END) {
    return DAY_START
  }

  return Math.max(DAY_START, current)
}

/** Пр. „1 ч 30 мин" — за копчињата за траење и за прегледот. */
export function durationLabel(minutes: number): string {
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60

  if (hours === 0) {
    return `${rest} мин`
  }
  if (rest === 0) {
    return `${hours} ч`
  }
  return `${hours} ч ${rest} мин`
}
