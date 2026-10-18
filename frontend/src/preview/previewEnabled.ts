export function isVisualPreviewEnabled(): boolean {
  const value = import.meta.env.VITE_ENABLE_VISUAL_PREVIEW
  if (value === 'true') return true
  if (value === 'false') return false
  return import.meta.env.DEV
}
