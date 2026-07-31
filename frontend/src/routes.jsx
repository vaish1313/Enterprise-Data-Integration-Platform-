import { Routes, Route, Navigate } from 'react-router-dom'
import { Suspense, lazy } from 'react'
import MainLayout  from './layouts/MainLayout'
import AuthLayout  from './layouts/AuthLayout'
import { PageLoader } from './components/Loader'
import ErrorBoundary from './components/ErrorBoundary'

const LoginPage          = lazy(() => import('./pages/LoginPage'))
const RegisterPage       = lazy(() => import('./pages/RegisterPage'))
const DashboardPage      = lazy(() => import('./pages/DashboardPage'))
const DataSourcePage     = lazy(() => import('./pages/DataSourcePage'))
const IngestionPage      = lazy(() => import('./pages/IngestionPage'))
const TransformationPage = lazy(() => import('./pages/TransformationPage'))
const SyncPage           = lazy(() => import('./pages/SyncPage'))
const AuditPage          = lazy(() => import('./pages/AuditPage'))
const UserManagementPage = lazy(() => import('./pages/UserManagementPage'))
const NotificationsPage  = lazy(() => import('./pages/NotificationsPage'))
const NotFoundPage       = lazy(() => import('./pages/NotFoundPage'))

export default function AppRoutes() {
  return (
    <ErrorBoundary>
      <Suspense fallback={<PageLoader />}>
        <Routes>
          {/* Auth routes */}
          <Route element={<AuthLayout />}>
            <Route path="/login"    element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
          </Route>

          {/* Protected app routes */}
          <Route element={<MainLayout />}>
            <Route index element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard"      element={<DashboardPage />} />
            <Route path="/data-sources"   element={<DataSourcePage />} />
            <Route path="/ingestion"      element={<IngestionPage />} />
            <Route path="/transformation" element={<TransformationPage />} />
            <Route path="/sync"           element={<SyncPage />} />
            <Route path="/audit"          element={<AuditPage />} />
            <Route path="/users"          element={<UserManagementPage />} />
            <Route path="/notifications"  element={<NotificationsPage />} />
          </Route>

          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </Suspense>
    </ErrorBoundary>
  )
}
