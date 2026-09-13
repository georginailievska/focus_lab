const LOCALE = ['mk-MK', 'en-GB']

/** пр. "чет, 18.09, 10:00" — за листи и картички */
export function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString(LOCALE, {
    weekday: 'short',
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

/** пр. "четврток, 18.09.2026, 10:00" — за екранот со детали */
export function formatFullDateTime(iso: string): string {
  return new Date(iso).toLocaleString(LOCALE, {
    weekday: 'long',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

/** пр. „четврток, 18.09.2026" — датум без час, за прегледот пред креирање */
export function formatFullDate(iso: string): string {
  return new Date(iso).toLocaleDateString(LOCALE, {
    weekday: 'long',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  })
}

/** пр. "18.09.2026" */
export function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString(LOCALE, {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  })
}

/** пр. "10:00" */
export function formatTime(iso: string): string {
  return new Date(iso).toLocaleTimeString(LOCALE, {
    hour: '2-digit',
    minute: '2-digit',
  })
}

/** пр. "10:00 – 12:00" (сесијата секогаш почнува и завршува истиот ден) */
export function formatTimeRange(startIso: string, endIso: string): string {
  return `${formatTime(startIso)} – ${formatTime(endIso)}`
}

/** Ден од месецот, за датумскиот блок на картичките: "18" */
export function dayOfMonth(iso: string): string {
  return new Date(iso).toLocaleDateString(LOCALE, { day: '2-digit' })
}

/** Скратен месец, за истиот блок: "сеп" */
export function shortMonth(iso: string): string {
  return new Date(iso).toLocaleDateString(LOCALE, { month: 'short' }).replace('.', '')
}

export function timeAgo(iso: string): string {
  const seconds = Math.floor((Date.now() - new Date(iso).getTime()) / 1000)

  if (seconds < 60) return 'сега'
  if (seconds < 3600) return `пред ${Math.floor(seconds / 60)} мин`
  if (seconds < 86_400) return `пред ${Math.floor(seconds / 3600)} ч`
  if (seconds < 172_800) return 'вчера'
  if (seconds < 604_800) return `пред ${Math.floor(seconds / 86_400)} дена`

  return formatDate(iso)
}

/** Пр. „1.4 MB" — за прилозите. */
export function fileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

export function isPast(iso: string): boolean {
  return new Date(iso).getTime() < Date.now()
}
