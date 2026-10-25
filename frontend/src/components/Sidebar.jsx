import { NavLink, useNavigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import {
  LayoutDashboard, Clock, BookOpen, BarChart2,
  BookMarked, Edit3, Users, GraduationCap,
  UserPlus, LogOut, ChevronRight, PieChart, Target,
} from 'lucide-react';

// ── Nav link definitions per role ─────────────────────────────
const NAV_LINKS = {
  student: [
    { to: '/student/courses', label: 'My Courses', Icon: BookMarked },
    { to: '/student/registration', label: 'Course Registration', Icon: BookOpen },
    { to: '/student/report', label: 'Academic Report', Icon: BarChart2 },
    { to: '/student/progress', label: 'My Progress', Icon: Target },
  ],
  faculty: [
    { to: '/faculty/courses', label: 'Course Management', Icon: BookMarked },
    { to: '/faculty/marks', label: 'Marks Management', Icon: Edit3 },
    { to: '/faculty/analytics', label: 'Analytics', Icon: PieChart },
  ],
  admin: [
    { to: '/admin/courses', label: 'Course Management', Icon: BookMarked },
    { to: '/admin/students', label: 'Student Management', Icon: Users },
    { to: '/admin/register', label: 'Student Registration', Icon: UserPlus },
    { to: '/admin/results', label: 'Upload Results', Icon: BarChart2 },
    { to: '/admin/term-control', label: 'Term Control', Icon: Clock },
    { to: '/admin/analytics', label: 'Analytics', Icon: PieChart },
  ],
};

// Section label per role
const ROLE_SECTION = {
  student: 'Student Portal',
  faculty: 'Faculty Portal',
  admin: 'Admin Portal',
};

/**
 * Sidebar navigation component.
 * Shows role-appropriate links and a logout button.
 */
export default function Sidebar() {
  const { currentUser, logout } = useAuth();
  const navigate = useNavigate();
  const [isActiveStudent, setIsActiveStudent] = useState(true);

  if (!currentUser) return null;

  // Change 3: Check if student is active
  useEffect(() => {
    if (currentUser.role === 'student') {
      api.get(`/students/${currentUser.id}`)
        .then(res => {
          setIsActiveStudent(res.data.active !== false);
        })
        .catch(err => console.error('Failed to fetch student status', err));
    }
  }, [currentUser]);

  let links = NAV_LINKS[currentUser.role] || [];
  if (currentUser.role === 'student' && !isActiveStudent) {
    links = links.filter(link => link.to !== '/student/registration');
  }

  function handleLogout() {
    logout();
    navigate('/');
  }

  return (
    <aside className="flex flex-col w-64 shrink-0 bg-navy text-white h-full shadow-modal overflow-y-auto">

      {/* ── Logo ─────────────────────────────────────────────── */}
      <div className="flex items-center gap-3 px-5 py-5 border-b border-white/10">
        {/* Graduation cap icon */}
        <div className="w-10 h-10 rounded-xl bg-gold flex items-center justify-center shrink-0">
          <GraduationCap className="w-6 h-6 text-navy" />
        </div>
        <div className="leading-tight">
          <p className="font-bold text-sm text-white leading-none">MRCA</p>
          <p className="text-[10px] text-gold/80 font-medium tracking-wide uppercase">University</p>
        </div>
      </div>

      {/* ── Section label ──────────────────────────────────── */}
      <div className="px-5 pt-5 pb-2">
        <p className="text-[10px] uppercase tracking-widest text-white/40 font-semibold">
          {ROLE_SECTION[currentUser.role]}
        </p>
      </div>

      {/* ── Nav links ────────────────────────────────────────── */}
      <nav className="flex-1 px-3 space-y-1">
        {links.map(({ to, label, Icon }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-smooth group ${isActive
                ? 'bg-gold text-navy shadow-sm'
                : 'text-white/70 hover:bg-white/10 hover:text-white'
              }`
            }
          >
            {({ isActive }) => (
              <>
                <Icon className={`w-4 h-4 shrink-0 ${isActive ? 'text-navy' : 'text-white/60 group-hover:text-white'}`} />
                <span className="flex-1">{label}</span>
                {isActive && <ChevronRight className="w-3 h-3 text-navy/60" />}
              </>
            )}
          </NavLink>
        ))}
      </nav>

      {/* ── Logout ───────────────────────────────────────────── */}
      <div className="p-3 border-t border-white/10">
        <button
          onClick={handleLogout}
          className="flex items-center gap-3 w-full px-3 py-2.5 rounded-xl text-sm font-medium text-white/60 hover:bg-red-500/20 hover:text-red-300 transition-smooth"
        >
          <LogOut className="w-4 h-4" />
          <span>Sign Out</span>
        </button>
      </div>
    </aside>
  );
}
