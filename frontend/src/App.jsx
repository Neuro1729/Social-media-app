import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth/AuthProvider'
import LoginPage from './auth/LoginPage'
import RegisterPage from './auth/RegisterPage'
import AccountPage from './account/AccountPage'
import DevicesPage from './account/DevicesPage'
import ForgotPasswordPage from './recovery/ForgotPasswordPage'
import ResetPasswordPage from './recovery/ResetPasswordPage'

function Protected({ children }) {
  const { accessToken, bootstrapping } = useAuth()
  if (bootstrapping) return <div className="page center">Loading…</div>
  if (!accessToken) return <Navigate to="/login" replace />
  return children
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/account" replace />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/reset-password" element={<ResetPasswordPage />} />
      <Route path="/account" element={<Protected><AccountPage /></Protected>} />
      <Route path="/account/devices" element={<Protected><DevicesPage /></Protected>} />
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  )
}
