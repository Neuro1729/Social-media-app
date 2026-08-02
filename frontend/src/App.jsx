import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth/AuthProvider'
import LoginPage from './auth/LoginPage'
import RegisterPage from './auth/RegisterPage'
import AccountPage from './account/AccountPage'
import DevicesPage from './account/DevicesPage'
import ForgotPasswordPage from './recovery/ForgotPasswordPage'
import ResetPasswordPage from './recovery/ResetPasswordPage'
import ProfilePage from './social/ProfilePage'
import EditProfilePage from './social/EditProfilePage'
import FollowersPage from './social/FollowersPage'
import FollowingPage from './social/FollowingPage'
import FollowRequestsPage from './social/FollowRequestsPage'
import BlockedUsersPage from './social/BlockedUsersPage'

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
      <Route path="/profile/edit" element={<Protected><EditProfilePage /></Protected>} />
      <Route path="/follow-requests" element={<Protected><FollowRequestsPage /></Protected>} />
      <Route path="/blocked-users" element={<Protected><BlockedUsersPage /></Protected>} />
      <Route path="/profile/:username/followers" element={<Protected><FollowersPage /></Protected>} />
      <Route path="/profile/:username/following" element={<Protected><FollowingPage /></Protected>} />
      <Route path="/profile/:username" element={<Protected><ProfilePage /></Protected>} />
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  )
}
