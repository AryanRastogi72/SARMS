import { FlaskConical } from 'lucide-react';

/** Shown on any analytics view whose numbers include seeded DEMO-* records. */
export default function DemoBadge() {
  return (
    <span
      className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-amber-50 border border-amber-300 text-amber-800 text-xs font-bold"
      title="Includes fictional students seeded by DemoDataSeeder (roll numbers DEMO-*). Not real results."
    >
      <FlaskConical className="w-3.5 h-3.5" />
      Demo data
    </span>
  );
}
