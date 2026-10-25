import { useEffect, useState } from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell } from 'recharts';
import { AlertTriangle, BookOpen, Percent, Users } from 'lucide-react';
import api from '../services/api';
import DemoBadge from './DemoBadge';

const NAVY = '#0A1F44';
const MUTED = '#9ca3af';
const LOWEST_N = 10;
const tooltipStyle = { borderRadius: '12px', border: '1px solid #e2e8f0', fontWeight: 600, color: '#0f172a' };

function Tile({ icon, label, value, sub }) {
  const Icon = icon;
  return (
    <div className="bg-white rounded-2xl shadow-card border border-gray-100 p-5 flex items-start gap-4">
      <Icon className="w-9 h-9 text-navy bg-navy/10 p-2 rounded-xl shrink-0" />
      <div>
        <p className="text-xs uppercase tracking-wider font-bold text-gray-400">{label}</p>
        <p className="text-3xl font-black text-navy">{value}</p>
        {sub && <p className="text-xs text-gray-500 mt-0.5">{sub}</p>}
      </div>
    </div>
  );
}

/**
 * Academic analytics (solo addition after fork).
 * Admin: all courses, with department/semester filters. Faculty: only their own courses (scoped server-side).
 */
export default function AnalyticsDashboard({ showFilters }) {
  const [department, setDepartment] = useState('');
  const [semester, setSemester] = useState('');
  const [departments, setDepartments] = useState([]);
  const [outcomes, setOutcomes] = useState(null);
  const [attention, setAttention] = useState(null);
  const [selected, setSelected] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    if (showFilters) api.get('/departments').then(res => setDepartments(res.data)).catch(() => {});
    api.get('/analytics/attention').then(res => setAttention(res.data)).catch(() => setError('Could not load analytics.'));
  }, [showFilters]);

  useEffect(() => {
    const params = {};
    if (department) params.department = department;
    if (semester) params.semester = semester;
    api.get('/analytics/courses', { params })
      .then(res => { setOutcomes(res.data); setSelected(res.data.rows[0]?.courseCode ?? null); })
      .catch(() => setError('Could not load analytics.'));
  }, [department, semester]);

  if (error) return <p className="text-red-600 font-semibold">{error}</p>;
  if (!outcomes || !attention) return <p className="text-gray-500">Loading analytics…</p>;

  const rows = outcomes.rows;
  const totalGraded = rows.reduce((s, r) => s + r.graded, 0);
  const overallPass = totalGraded ? rows.reduce((s, r) => s + r.passRate * r.graded, 0) / totalGraded : 0;
  const lowest = rows.slice(0, LOWEST_N).map(r => ({ ...r, passPct: Math.round(r.passRate * 100) }));
  const course = rows.find(r => r.courseCode === selected);
  const distData = course ? Object.entries(course.distribution).map(([grade, count]) => ({ grade, count })) : [];
  // One row per student: flags come per (student, course), so CGPA reasons would otherwise repeat per course
  const students = new Map();
  attention.rows.filter(f => !department || f.departmentCode === department).forEach(f => {
    const s = students.get(f.rollNo) ?? { ...f, courses: [], reasons: new Set() };
    s.courses.push(f);
    f.reasons.forEach(r => s.reasons.add(r.startsWith('Current marks') ? `${f.courseCode}: ${r}` : r));
    students.set(f.rollNo, s);
  });
  const flagged = [...students.values()];

  return (
    <div className="space-y-6 max-w-6xl">
      <div className="flex flex-wrap items-center gap-3">
        <h1 className="text-2xl font-extrabold text-navy">Academic Analytics</h1>
        {(outcomes.demoData || attention.demoData) && <DemoBadge />}
        {showFilters && (
          <div className="flex gap-3 ml-auto">
            <select value={department} onChange={e => setDepartment(e.target.value)} aria-label="Department"
              className="border border-gray-200 rounded-xl px-3 py-2 text-sm font-semibold text-navy bg-white">
              <option value="">All departments</option>
              {departments.map(d => <option key={d.code} value={d.code}>{d.code}</option>)}
            </select>
            <select value={semester} onChange={e => setSemester(e.target.value)} aria-label="Semester"
              className="border border-gray-200 rounded-xl px-3 py-2 text-sm font-semibold text-navy bg-white">
              <option value="">All semesters</option>
              {[1, 2, 3, 4, 5, 6, 7, 8].map(n => <option key={n} value={n}>Semester {n}</option>)}
            </select>
          </div>
        )}
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Tile icon={BookOpen} label="Courses with results" value={rows.length} sub={`${totalGraded} final grades`} />
        <Tile icon={Percent} label="Overall pass rate" value={`${Math.round(overallPass * 100)}%`} sub="Grades D and above" />
        <Tile icon={Users} label="Students needing attention" value={flagged.length} sub={`Current semester${department ? `, ${department}` : ''}`} />
      </div>

      {rows.length === 0 ? (
        <p className="text-gray-500">No published results match these filters yet.</p>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <section className="bg-white rounded-2xl shadow-card border border-gray-100 p-6">
            <h2 className="font-extrabold text-navy">Lowest pass rates</h2>
            <p className="text-xs text-gray-500 mb-4">
              {lowest.length} of {rows.length} courses, lowest first. Click a bar to see its grades.
            </p>
            <ResponsiveContainer width="100%" height={Math.max(160, lowest.length * 32)}>
              <BarChart data={lowest} layout="vertical" margin={{ left: 8, right: 24 }}>
                <CartesianGrid horizontal={false} stroke="#f1f5f9" />
                <XAxis type="number" domain={[0, 100]} unit="%" tick={{ fontSize: 11, fill: MUTED }} axisLine={false} tickLine={false} />
                <YAxis type="category" dataKey="courseCode" width={64} tick={{ fontSize: 11, fill: '#475569', fontWeight: 700 }} axisLine={false} tickLine={false} />
                <Tooltip cursor={{ fill: '#f1f5f9' }} contentStyle={tooltipStyle}
                  formatter={(v, _n, p) => [`${v}% (${p.payload.graded} graded)`, p.payload.courseName]} />
                <Bar dataKey="passPct" radius={[0, 4, 4, 0]} maxBarSize={18} cursor="pointer"
                  onClick={d => setSelected(d.payload?.courseCode ?? d.courseCode)}>
                  {lowest.map(r => <Cell key={r.courseCode} fill={NAVY} fillOpacity={r.courseCode === selected ? 1 : 0.55} />)}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </section>

          <section className="bg-white rounded-2xl shadow-card border border-gray-100 p-6">
            <div className="flex items-center gap-2">
              <h2 className="font-extrabold text-navy">Grade distribution</h2>
              <select value={selected ?? ''} onChange={e => setSelected(e.target.value)} aria-label="Course"
                className="ml-auto border border-gray-200 rounded-lg px-2 py-1 text-xs font-semibold text-navy bg-white">
                {rows.map(r => <option key={r.courseCode} value={r.courseCode}>{r.courseCode}</option>)}
              </select>
            </div>
            <p className="text-xs text-gray-500 mb-4">
              {course?.courseName} · {course?.graded} students · average {course?.avgGradePoints} grade points
            </p>
            <ResponsiveContainer width="100%" height={260}>
              <BarChart data={distData} margin={{ right: 8 }}>
                <CartesianGrid vertical={false} stroke="#f1f5f9" />
                <XAxis dataKey="grade" tick={{ fontSize: 12, fill: '#475569', fontWeight: 700 }} axisLine={false} tickLine={false} />
                <YAxis allowDecimals={false} tick={{ fontSize: 11, fill: MUTED }} axisLine={false} tickLine={false} />
                <Tooltip cursor={{ fill: '#f1f5f9' }} contentStyle={tooltipStyle} formatter={v => [v, 'Students']} />
                <Bar dataKey="count" fill={NAVY} radius={[4, 4, 0, 0]} maxBarSize={40} />
              </BarChart>
            </ResponsiveContainer>
          </section>
        </div>
      )}

      <section className="bg-white rounded-2xl shadow-card border border-gray-100 p-6">
        <h2 className="font-extrabold text-navy">Needs attention this semester</h2>
        <p className="text-xs text-gray-500 mb-4">
          Flagged if marks entered so far are below {attention.passPercent}% or CGPA is below {attention.cgpaCutoff}.
          Marks are scored only against components already entered, so a missing end-sem exam doesn't count against anyone.
        </p>
        {flagged.length === 0 ? (
          <p className="text-sm text-gray-500">No students flagged.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-xs uppercase tracking-wider text-gray-400 border-b">
                  <th className="py-2 pr-4">Student</th><th className="py-2 pr-4 text-right">CGPA</th>
                  <th className="py-2 pr-4">Flagged courses (marks so far)</th><th className="py-2">Why</th>
                </tr>
              </thead>
              <tbody>
                {flagged.map(f => (
                  <tr key={f.rollNo} className="border-b border-gray-50 align-top">
                    <td className="py-2 pr-4"><p className="font-bold text-navy">{f.studentName}</p><p className="text-xs text-gray-400">{f.rollNo}</p></td>
                    <td className="py-2 pr-4 text-right font-bold">{f.cgpa ?? '—'}</td>
                    <td className="py-2 pr-4 whitespace-nowrap">
                      {f.courses.map(c => (
                        <p key={c.courseCode}><span className="font-semibold text-gray-600">{c.courseCode}</span> {c.currentPercent ?? '—'}{c.currentPercent != null && '%'}</p>
                      ))}
                    </td>
                    <td className="py-2">
                      {[...f.reasons].map(r => (
                        <p key={r} className="flex items-start gap-1.5 text-gray-700">
                          <AlertTriangle className="w-4 h-4 text-amber-500 shrink-0 mt-0.5" aria-label="Needs attention" />{r}
                        </p>
                      ))}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
