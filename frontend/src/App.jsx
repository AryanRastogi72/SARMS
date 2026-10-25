import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { TermConfigProvider } from './context/TermConfigContext';
import ProtectedRoute from './components/ProtectedRoute';
import Layout from './components/Layout';

// ── Pages ─────────────────────────────────────────────────────
import LoginPage from './pages/LoginPage';

// Student portal
import MyCourses from './pages/student/MyCourses';
import CourseRegistration from './pages/student/CourseRegistration';
import AcademicReport from './pages/student/AcademicReport';

// Faculty portal
import CourseManagement from './pages/faculty/CourseManagement';
import MarksManagement from './pages/faculty/MarksManagement';

// Admin portal
import CourseOversight from './pages/admin/CourseOversight';
import StudentManagement from './pages/admin/StudentManagement';
import StudentRegistration from './pages/admin/StudentRegistration';
import UploadResults from './pages/admin/UploadResults';
import TermControl from './pages/admin/TermControl';

// Solo additions after fork
import AnalyticsDashboard from './components/AnalyticsDashboard';
import StudentProgress from './pages/student/StudentProgress';

/**
 * App, top-level router.
 * All authenticated portals are wrapped in Layout (sidebar + header).
 * ProtectedRoute enforces role-based access.
 */
export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <TermConfigProvider>
        <Routes>

          {/* ── Public: Login ─────────────────────────────── */}
          <Route path="/" element={<LoginPage />} />

          {/* ── Student Portal ────────────────────────────── */}
          <Route
            path="/student"
            element={
              <ProtectedRoute requiredRole="student">
                <Layout />
              </ProtectedRoute>
            }
          >
            <Route index element={<Navigate to="courses" replace />} />
            <Route path="courses" element={<MyCourses />} />
            <Route path="registration" element={<CourseRegistration />} />
            <Route path="report" element={<AcademicReport />} />
            <Route path="progress" element={<StudentProgress />} />
          </Route>

          {/* ── Faculty Portal ────────────────────────────── */}
          <Route
            path="/faculty"
            element={
              <ProtectedRoute requiredRole="faculty">
                <Layout />
              </ProtectedRoute>
            }
          >
            <Route index element={<Navigate to="courses" replace />} />
            <Route path="courses" element={<CourseManagement />} />
            <Route path="marks" element={<MarksManagement />} />
            <Route path="analytics" element={<AnalyticsDashboard />} />
          </Route>

          {/* ── Admin Portal ──────────────────────────────── */}
          <Route
            path="/admin"
            element={
              <ProtectedRoute requiredRole="admin">
                <Layout />
              </ProtectedRoute>
            }
          >
            <Route index element={<Navigate to="courses" replace />} />
            <Route path="courses" element={<CourseOversight />} />
            <Route path="students" element={<StudentManagement />} />
            <Route path="register" element={<StudentRegistration />} />
            <Route path="results" element={<UploadResults />} />
            <Route path="term-control" element={<TermControl />} />
            <Route path="analytics" element={<AnalyticsDashboard showFilters />} />
          </Route>

          {/* ── Catch-all ─────────────────────────────────── */}
          <Route path="*" element={<Navigate to="/" replace />} />

        </Routes>
        </TermConfigProvider>
      </BrowserRouter>
    </AuthProvider>
  );
}
