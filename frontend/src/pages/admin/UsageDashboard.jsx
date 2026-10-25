import { useEffect, useState } from 'react';
import { LineChart, Line, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import api from '../../services/api';
import DemoBadge from '../../components/DemoBadge';

const NAVY = '#0A1F44';
const MUTED = '#9ca3af';
const tooltipStyle = { borderRadius: '12px', border: '1px solid #e2e8f0', fontWeight: 600, color: '#0f172a' };
// "/admin/term-control" → "Term control (admin)"
const label = feature => {
  const [, portal, page = 'home'] = feature.split('/');
  return `${page[0].toUpperCase()}${page.slice(1).replace(/-/g, ' ')} (${portal})`;
};

/**
 * Feature usage analytics (solo addition after fork): page views per portal page, by role, over time.
 */
export default function UsageDashboard() {
  const [days, setDays] = useState(30);
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.get('/usage/summary', { params: { days } })
      .then(res => setData(res.data))
      .catch(() => setError('Could not load usage data.'));
  }, [days]);

  if (error) return <p className="text-red-600 font-semibold">{error}</p>;
  if (!data) return <p className="text-gray-500">Loading usage…</p>;

  const features = data.features.map(f => ({ ...f, name: label(f.feature) }));
  const daily = data.daily.map(d => ({ ...d, day: d.date.slice(5) }));

  return (
    <div className="space-y-6 max-w-6xl">
      <div className="flex flex-wrap items-center gap-3">
        <h1 className="text-2xl font-extrabold text-navy">Feature Usage</h1>
        {data.demoData && <DemoBadge />}
        <select value={days} onChange={e => setDays(Number(e.target.value))} aria-label="Time range"
          className="ml-auto border border-gray-200 rounded-xl px-3 py-2 text-sm font-semibold text-navy bg-white">
          {[7, 30, 90].map(n => <option key={n} value={n}>Last {n} days</option>)}
        </select>
      </div>
      <p className="text-sm text-gray-500 -mt-3">
        One event per portal page view. Stores the page, the viewer's role and a pseudonymous user hash — no names or record content.
      </p>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {[['Page views', data.totalEvents], ['Active students', data.activeUsers.student],
          ['Active faculty', data.activeUsers.faculty], ['Active admins', data.activeUsers.admin]].map(([k, v]) => (
          <div key={k} className="bg-white rounded-2xl shadow-card border border-gray-100 p-5">
            <p className="text-xs uppercase tracking-wider font-bold text-gray-400">{k}</p>
            <p className="text-3xl font-black text-navy">{v}</p>
          </div>
        ))}
      </div>

      {data.totalEvents === 0 ? (
        <p className="text-gray-500">No page views recorded in this period.</p>
      ) : (
        <>
          <section className="bg-white rounded-2xl shadow-card border border-gray-100 p-6">
            <h2 className="font-extrabold text-navy mb-4">Page views per day</h2>
            <ResponsiveContainer width="100%" height={220}>
              <LineChart data={daily} margin={{ right: 16 }}>
                <CartesianGrid vertical={false} stroke="#f1f5f9" />
                <XAxis dataKey="day" tick={{ fontSize: 11, fill: MUTED }} axisLine={false} tickLine={false} minTickGap={24} />
                <YAxis allowDecimals={false} tick={{ fontSize: 11, fill: MUTED }} axisLine={false} tickLine={false} />
                <Tooltip contentStyle={tooltipStyle} labelFormatter={(_, p) => p?.[0]?.payload.date} formatter={v => [v, 'Page views']} />
                <Line type="monotone" dataKey="events" stroke={NAVY} strokeWidth={2} dot={false} activeDot={{ r: 5 }} />
              </LineChart>
            </ResponsiveContainer>
          </section>

          <section className="bg-white rounded-2xl shadow-card border border-gray-100 p-6">
            <h2 className="font-extrabold text-navy mb-4">Most used pages</h2>
            <ResponsiveContainer width="100%" height={Math.max(160, features.length * 30)}>
              <BarChart data={features} layout="vertical" margin={{ left: 8, right: 24 }}>
                <CartesianGrid horizontal={false} stroke="#f1f5f9" />
                <XAxis type="number" allowDecimals={false} tick={{ fontSize: 11, fill: MUTED }} axisLine={false} tickLine={false} />
                <YAxis type="category" dataKey="name" width={210} tick={{ fontSize: 11, fill: '#475569', fontWeight: 600 }} axisLine={false} tickLine={false} />
                <Tooltip cursor={{ fill: '#f1f5f9' }} contentStyle={tooltipStyle}
                  formatter={(v, _n, p) => [`${v} views by ${p.payload.users} users`, 'Usage']} />
                <Bar dataKey="events" fill={NAVY} radius={[0, 4, 4, 0]} maxBarSize={16} />
              </BarChart>
            </ResponsiveContainer>

            <table className="w-full text-sm mt-6">
              <thead>
                <tr className="text-left text-xs uppercase tracking-wider text-gray-400 border-b">
                  <th className="py-2 pr-4">Page</th><th className="py-2 pr-4 text-right">Views</th>
                  <th className="py-2 pr-4 text-right">Users</th><th className="py-2 text-right">Views per user</th>
                </tr>
              </thead>
              <tbody>
                {features.map(f => (
                  <tr key={f.feature} className="border-b border-gray-50">
                    <td className="py-2 pr-4 font-semibold text-navy">{f.name}</td>
                    <td className="py-2 pr-4 text-right">{f.events}</td>
                    <td className="py-2 pr-4 text-right">{f.users}</td>
                    <td className="py-2 text-right">{f.eventsPerUser}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </section>
        </>
      )}
    </div>
  );
}
