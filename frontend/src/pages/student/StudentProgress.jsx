import { useEffect, useState } from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, LabelList } from 'recharts';
import { CheckCircle2, AlertTriangle, TrendingUp, TrendingDown, Minus } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';
import DemoBadge from '../../components/DemoBadge';

// Validated categorical slots 1 and 2 (dataviz reference palette)
const CGPA_COLOR = '#2a78d6';
const SGPA_COLOR = '#eb6834';

/**
 * Student progress (solo addition after fork): CGPA trend and credits left to graduate.
 */
export default function StudentProgress() {
  const { currentUser } = useAuth();
  const [p, setP] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.get(`/analytics/progress/${currentUser.id}`)
      .then(res => setP(res.data))
      .catch(() => setError('Could not load your progress.'));
  }, [currentUser.id]);

  if (error) return <p className="text-red-600 font-semibold">{error}</p>;
  if (!p) return <p className="text-gray-500">Loading progress…</p>;

  const pct = v => `${Math.min(100, (v / p.creditsRequired) * 100)}%`;
  const TrendIcon = p.cgpaChange == null || p.cgpaChange === 0 ? Minus : p.cgpaChange > 0 ? TrendingUp : TrendingDown;
  const chartData = p.semesters.map(s => ({ name: `Sem ${s.semester}`, CGPA: s.cgpa, SGPA: s.sgpa }));

  return (
    <div className="space-y-6 max-w-5xl">
      <div className="flex items-center gap-3">
        <h1 className="text-2xl font-extrabold text-navy">My Progress</h1>
        {p.demoData && <DemoBadge />}
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-white rounded-2xl shadow-card border border-gray-100 p-5">
          <p className="text-xs uppercase tracking-wider font-bold text-gray-400">CGPA</p>
          <p className="text-4xl font-black text-navy">{p.cgpa ?? '—'}</p>
          <p className="flex items-center gap-1 text-sm text-gray-600 mt-1">
            <TrendIcon className="w-4 h-4" aria-hidden />
            {p.cgpaChange == null ? 'Trend appears after two semesters'
              : `${p.cgpaChange > 0 ? '+' : ''}${p.cgpaChange} since last semester`}
          </p>
        </div>
        <div className="bg-white rounded-2xl shadow-card border border-gray-100 p-5">
          <p className="text-xs uppercase tracking-wider font-bold text-gray-400">Credits remaining</p>
          <p className="text-4xl font-black text-navy">{p.creditsRemaining}</p>
          <p className="text-sm text-gray-600 mt-1">of {p.creditsRequired} required to graduate</p>
        </div>
        <div className="bg-white rounded-2xl shadow-card border border-gray-100 p-5">
          <p className="text-xs uppercase tracking-wider font-bold text-gray-400">Pace needed</p>
          <p className="text-4xl font-black text-navy">{p.creditsPerSemesterNeeded ?? '—'}</p>
          <p className="text-sm text-gray-600 mt-1">credits/semester over {p.semestersLeft} semesters (cap {p.maxCreditsPerSemester})</p>
        </div>
      </div>

      <section className="bg-white rounded-2xl shadow-card border border-gray-100 p-6">
        <div className="flex items-center justify-between mb-3">
          <h2 className="font-extrabold text-navy">Graduation progress</h2>
          {p.onTrack ? (
            <span className="flex items-center gap-1.5 text-sm font-bold text-green-700">
              <CheckCircle2 className="w-4 h-4" aria-hidden /> On track
            </span>
          ) : (
            <span className="flex items-center gap-1.5 text-sm font-bold text-amber-700">
              <AlertTriangle className="w-4 h-4" aria-hidden /> Behind pace: talk to your advisor
            </span>
          )}
        </div>
        <div className="flex h-4 rounded-full overflow-hidden bg-gray-100 gap-0.5" role="img"
          aria-label={`${p.creditsEarned} credits earned, ${p.creditsInProgress} in progress, ${p.creditsRemaining} remaining`}>
          <div className="bg-navy" style={{ width: pct(p.creditsEarned) }} />
          <div className="bg-navy/40" style={{ width: pct(p.creditsInProgress) }} />
        </div>
        <div className="flex flex-wrap gap-x-6 gap-y-1 mt-3 text-sm text-gray-600">
          <span className="flex items-center gap-2"><i className="w-3 h-3 rounded-sm bg-navy" />Earned {p.creditsEarned}</span>
          <span className="flex items-center gap-2"><i className="w-3 h-3 rounded-sm bg-navy/40" />In progress {p.creditsInProgress}</span>
          <span className="flex items-center gap-2"><i className="w-3 h-3 rounded-sm bg-gray-200" />Not started {Math.max(0, p.creditsRemaining - p.creditsInProgress)}</span>
          {p.creditsFailed > 0 && <span className="text-gray-500">Failed courses ({p.creditsFailed} credits) don't count toward graduation</span>}
        </div>
        <p className="text-xs text-gray-400 mt-2">
          Requirement of {p.creditsRequired} credits is a configurable program setting (sarms.graduation-credits).
        </p>
      </section>

      <section className="bg-white rounded-2xl shadow-card border border-gray-100 p-6">
        <h2 className="font-extrabold text-navy">CGPA trend</h2>
        <p className="text-xs text-gray-500 mb-4">Cumulative GPA after each completed semester, with that semester's SGPA.</p>
        {chartData.length === 0 ? (
          <p className="text-sm text-gray-500">Your trend appears once your first semester's results are published.</p>
        ) : (
          <ResponsiveContainer width="100%" height={280}>
            <LineChart data={chartData} margin={{ top: 16, right: 24, left: 0, bottom: 0 }}>
              <CartesianGrid vertical={false} stroke="#f1f5f9" />
              <XAxis dataKey="name" tick={{ fontSize: 12, fill: '#475569', fontWeight: 700 }} axisLine={false} tickLine={false} />
              <YAxis domain={[0, 10]} ticks={[0, 2, 4, 6, 8, 10]} tick={{ fontSize: 11, fill: '#9ca3af' }} axisLine={false} tickLine={false} />
              <Tooltip contentStyle={{ borderRadius: '12px', border: '1px solid #e2e8f0', fontWeight: 600, color: '#0f172a' }} />
              <Legend iconType="plainline" wrapperStyle={{ fontSize: 12, fontWeight: 600 }} />
              <Line type="monotone" dataKey="CGPA" stroke={CGPA_COLOR} strokeWidth={2} dot={{ r: 4, strokeWidth: 2, fill: '#fff' }} activeDot={{ r: 6 }}>
                <LabelList dataKey="CGPA" position="top" offset={10} style={{ fontSize: 11, fill: '#334155', fontWeight: 700 }} />
              </Line>
              <Line type="monotone" dataKey="SGPA" stroke={SGPA_COLOR} strokeWidth={2} strokeDasharray="5 4" dot={{ r: 4, strokeWidth: 2, fill: '#fff' }} activeDot={{ r: 6 }} />
            </LineChart>
          </ResponsiveContainer>
        )}
      </section>
    </div>
  );
}
