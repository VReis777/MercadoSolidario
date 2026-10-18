/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_URL: string
  readonly VITE_ENABLE_VISUAL_PREVIEW?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
