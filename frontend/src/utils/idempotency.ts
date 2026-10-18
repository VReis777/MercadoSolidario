export function newRequestReference(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return `ref-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 12)}`
}
