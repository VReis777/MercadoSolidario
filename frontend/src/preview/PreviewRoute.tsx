import { Navigate } from 'react-router-dom'
import { isVisualPreviewEnabled } from './previewEnabled'

function PreviewRoute({ children }: { children: React.ReactNode }) {
  if (!isVisualPreviewEnabled()) {
    return <Navigate to="/" replace />
  }
  return <>{children}</>
}

export default PreviewRoute
