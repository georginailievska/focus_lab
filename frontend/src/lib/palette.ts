function hashString(value: string): number {
  let hash = 0
  for (let index = 0; index < value.length; index += 1) {
    hash = (hash * 31 + value.charCodeAt(index)) % 360_000
  }
  return hash
}

/** 12 добро разделени нијанси — доволно за да се разликуваат, без да шарат. */
function hueFor(value: string): number {
  return (hashString(value) % 12) * 30
}

export function solidColorFor(value: string): string {
  return `hsl(${hueFor(value)} 52% 45%)`
}

export function initialsFor(fullName: string): string {
  const parts = fullName.trim().split(/\s+/).filter(Boolean)

  if (parts.length === 0) {
    return '?'
  }

  if (parts.length === 1) {
    return parts[0].slice(0, 2).toUpperCase()
  }

  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
}
