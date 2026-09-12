const MONTHS = [
  'јануари', 'февруари', 'март', 'април', 'мај', 'јуни',
  'јули', 'август', 'септември', 'октомври', 'ноември', 'декември',
]

/** Индекс како `Date.getDay()`: 0 = недела. */
const WEEKDAY_NAMES = [
  'недела', 'понеделник', 'вторник', 'среда', 'четврток', 'петок', 'сабота',
]

export type DayKey = string

export interface CalendarDay {
  key: DayKey
  /** Ден во месецот, 1–31. */
  day: number
  /** Дали припаѓа на прикажаниот месец (мрежата има и денови од соседните). */
  inMonth: boolean
  isToday: boolean
  isPast: boolean
}

const pad = (value: number) => String(value).padStart(2, '0')

function toKey(year: number, month: number, day: number): DayKey {
  return `${year}-${pad(month + 1)}-${pad(day)}`
}

/** Денот на кој припаѓа една сесија (`startTime` како локален ISO текст). */
function dayKeyOf(iso: string): DayKey {
  return iso.slice(0, 10)
}

export function todayKey(): DayKey {
  const now = new Date()
  return toKey(now.getFullYear(), now.getMonth(), now.getDate())
}

/** Пр. „септември 2026". */
export function monthLabel(year: number, month: number): string {
  return `${MONTHS[month]} ${year}`
}

/** Пр. „четврток, 11 септември" — наслов над списокот за избран ден. */
export function dayLabel(key: DayKey): string {
  const [year, month, day] = key.split('-').map(Number)
  const weekday = WEEKDAY_NAMES[new Date(year, month - 1, day).getDay()]
  return `${weekday}, ${day} ${MONTHS[month - 1]}`
}

export function addMonths(year: number, month: number, delta: number) {
  const date = new Date(year, month + delta, 1)
  return { year: date.getFullYear(), month: date.getMonth() }
}

/** Неделата почнува во понеделник — како во календарите тука. */
export const WEEKDAYS = ['Пон', 'Вто', 'Сре', 'Чет', 'Пет', 'Саб', 'Нед']

export function monthGrid(year: number, month: number): CalendarDay[] {
  const today = todayKey()
  const first = new Date(year, month, 1)

  // getDay(): 0 = недела. Нам ни треба 0 = понеделник.
  const leading = (first.getDay() + 6) % 7
  const start = new Date(year, month, 1 - leading)

  const days: CalendarDay[] = []
  const total = 42 // шест недели: секој месец се смести, мрежата не се движи

  for (let i = 0; i < total; i += 1) {
    const date = new Date(start.getFullYear(), start.getMonth(), start.getDate() + i)
    const key = toKey(date.getFullYear(), date.getMonth(), date.getDate())

    days.push({
      key,
      day: date.getDate(),
      inMonth: date.getMonth() === month,
      isToday: key === today,
      isPast: key < today,
    })
  }

  return days
}

/** Групирање сесии по ден — една минување низ листата. */
export function groupByDay<T extends { startTime: string }>(items: T[]): Map<DayKey, T[]> {
  const groups = new Map<DayKey, T[]>()

  for (const item of items) {
    const key = dayKeyOf(item.startTime)
    const bucket = groups.get(key)
    if (bucket) {
      bucket.push(item)
    } else {
      groups.set(key, [item])
    }
  }

  return groups
}
