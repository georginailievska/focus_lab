import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './context/AuthContext'
import ProtectedRoute from './components/ProtectedRoute'
import Login from './pages/Login'
import Register from './pages/Register'
import Home from './pages/Home'
import MentorDashboard from './pages/MentorDashboard'
import MentorNotes from './pages/MentorNotes'
import AdminDashboard from './pages/AdminDashboard'
import BrowseSessions from './pages/BrowseSessions'
import SessionDetails from './pages/SessionDetails'
import MyInterests from './pages/MyInterests'
import CreateSession from './pages/CreateSession'
import CalendarPage from './pages/CalendarPage'
import EditSession from './pages/EditSession'
import FeedPage from './pages/FeedPage'
import Profile from './pages/Profile'
import UserProfile from './pages/UserProfile'
import ForgotPassword from './pages/ForgotPassword'
import ResetPassword from './pages/ResetPassword'

export default function App() {
  const { loading } = useAuth()

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-page text-ink-mute">
        Се вчитува...
      </div>
    )
  }

  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />

      {/* Заборавена лозинка: двата екрана мора да се достапни без најава */}
      <Route path="/forgot-password" element={<ForgotPassword />} />
      <Route path="/reset-password" element={<ResetPassword />} />

      {/* Лентата, календарот и профилот се исти за сите улоги — по една рута */}
      <Route
        path="/feed"
        element={
          <ProtectedRoute allow={['STUDENT', 'MENTOR', 'ADMIN']}>
            <FeedPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/calendar"
        element={
          <ProtectedRoute allow={['STUDENT', 'MENTOR', 'ADMIN']}>
            <CalendarPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/profile"
        element={
          <ProtectedRoute allow={['STUDENT', 'MENTOR', 'ADMIN']}>
            <Profile />
          </ProtectedRoute>
        }
      />

      <Route
        path="/users/:id"
        element={
          <ProtectedRoute allow={['STUDENT', 'MENTOR', 'ADMIN']}>
            <UserProfile />
          </ProtectedRoute>
        }
      />

      <Route
        path="/"
        element={
          <ProtectedRoute allow={['STUDENT']}>
            <Home />
          </ProtectedRoute>
        }
      />

      <Route
        path="/sessions"
        element={
          <ProtectedRoute allow={['STUDENT']}>
            <BrowseSessions />
          </ProtectedRoute>
        }
      />

      <Route
        path="/interests"
        element={
          <ProtectedRoute allow={['STUDENT']}>
            <MyInterests />
          </ProtectedRoute>
        }
      />

      <Route
        path="/sessions/:id"
        element={
          <ProtectedRoute allow={['STUDENT', 'MENTOR', 'ADMIN']}>
            <SessionDetails />
          </ProtectedRoute>
        }
      />

      <Route
        path="/mentor"
        element={
          <ProtectedRoute allow={['MENTOR']}>
            <MentorDashboard />
          </ProtectedRoute>
        }
      />

      <Route
        path="/mentor/sessions/:id/edit"
        element={
          <ProtectedRoute allow={['MENTOR']}>
            <EditSession />
          </ProtectedRoute>
        }
      />

      <Route
        path="/mentor/notes"
        element={
          <ProtectedRoute allow={['MENTOR']}>
            <MentorNotes />
          </ProtectedRoute>
        }
      />

      <Route
        path="/mentor/create-session"
        element={
          <ProtectedRoute allow={['MENTOR']}>
            <CreateSession />
          </ProtectedRoute>
        }
      />

      <Route
        path="/admin"
        element={
          <ProtectedRoute allow={['ADMIN']}>
            <AdminDashboard />
          </ProtectedRoute>
        }
      />

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
