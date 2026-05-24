import { motion } from 'framer-motion'
import {
  AreaChart, Area, BarChart, Bar, PieChart, Pie, Cell,
  XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend,
} from 'recharts'
import {
  HiUsers, HiDatabase, HiDownload, HiLightningBolt,
  HiRefresh, HiCheckCircle, HiXCircle, HiClock,
  HiTrendingUp, HiArrowSmRight,
} from 'react-icons/hi'
import { useNavigate } from 'react-router-dom'
import { useFetch } from '../hooks/useFetch'
import { dashboardApi } from '../api/dashboardApi'
import { fmtNumber, fmtPercent, fmtRelative, fmtMs } from '../utils/formatters'
import { SkeletonCard } from '../components/Loader'

/* ── Colour palette ─────────────────────────────────────────────────────── */
const COLORS = ['#6366f1', '#8b5cf6', '#10b981', '#f59e0b', '#ef4444', '#3b82f6']

/* ── Animated KPI card ──────────────────────────────────────────────────── */
function KpiCard({ icon: Icon, label, value, sub, color = 'brand', delay = 0, onClick }) {
  const bg = {
    brand:   'from-brand-500/20 to-brand-600/10 border-brand-500/30',
    green:   'from-emerald-500/20 to-emerald-600/10 border-emerald-500/30',
    red:     'from-red-500/20 to-red-600/10 border-red-500/30',
    amber:   'from-amber-500/20 to-amber-600/10 border-amber-500/30',
    blue:    'from-blue-500/20 to-blue-600/10 border-blue-500/30',
    purple:  'from-purple-500/20 to-purple-600/10 border-purple-500/30',
  }[color]

  const ic = {
    brand: 'text-brand-400',   green: 'text-emerald-400',
    red:   'text-red-400',     amber: 'text-amber-400',
    blue:  'text-blue-400',    purple: 'text-purple-400',
  }[color]

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay, duration: 0.4 }}
      onClick={onClick}
      className={`card p-5 bg-gradient-to-br ${bg} border cursor-pointer
                  hover:scale-[1.02] transition-transform duration-200`}
    >
      <div className="flex items-start justify-between">
        <div className={`p-2.5 rounded-xl bg-white/10 ${ic}`}>
          <Icon className="w-5 h-5" />
        </div>
        {sub != null && (
          <span className="text-xs font-semibold text-slate-400">{sub}</span>
        )}
      </div>
      <p className="mt-4 text-2xl font-extrabold text-white">{value}</p>
      <p className="text-xs font-medium text-slate-400 mt-1">{label}</p>
    </motion.div>
  )
}

/* ── Section wrapper ────────────────────────────────────────────────────── */
function Section({ title, action, children }) {
  return (
    <div className="card p-5">
      <div className="flex items-center justify-between mb-4">
        <h3 className="section-title">{title}</h3>
        {action}
      </div>
      {children}
    </div>
  )
}

/* ── Custom tooltip ─────────────────────────────────────────────────────── */
function ChartTooltip({ active, payload, label }) {
  if (!active || !payload?.length) return null
  return (
    <div className="bg-surface-800 border border-white/10 rounded-xl px-3 py-2 text-xs shadow-card-dark">
      <p className="text-slate-300 font-semibold mb-1">{label}</p>
      {payload.map(p => (
        <p key={p.name} style={{ color: p.color }}>{p.name}: <strong>{fmtNumber(p.value)}</strong></p>
      ))}
    </div>
  )
}

export default function DashboardPage() {
  const navigate = useNavigate()
  const { data: overview, loading: lo } = useFetch(dashboardApi.overview)
  const { data: ingestion, loading: li } = useFetch(dashboardApi.ingestion)
  const { data: sync,      loading: ls } = useFetch(dashboardApi.synchronization)
  const { data: audit,     loading: la } = useFetch(dashboardApi.audit)

  const loading = lo || li || ls || la

  /* ── Build chart data from real API ─────────────────────────────────── */
  const ingestionPieData = ingestion ? [
    { name: 'Completed', value: ingestion.completedJobs },
    { name: 'Failed',    value: ingestion.failedJobs },
    { name: 'Partial',   value: ingestion.partialJobs },
    { name: 'Running',   value: ingestion.runningJobs },
  ].filter(d => d.value > 0) : []

  const syncBarData = sync ? [
    { name: 'Completed', value: sync.completedJobs,  fill: '#10b981' },
    { name: 'Failed',    value: sync.failedJobs,     fill: '#ef4444' },
    { name: 'Running',   value: sync.runningJobs,    fill: '#3b82f6' },
    { name: 'Pending',   value: sync.pendingJobs,    fill: '#f59e0b' },
  ] : []

  const auditActionData = audit?.eventsByAction
    ? Object.entries(audit.eventsByAction).slice(0, 6).map(([name, value]) => ({ name, value }))
    : []

  const dsData = overview ? [
    { name: 'Active',   value: overview.activeDataSources,   fill: '#10b981' },
    { name: 'Inactive', value: overview.inactiveDataSources, fill: '#6366f1' },
    { name: 'Error',    value: overview.errorDataSources,    fill: '#ef4444' },
  ].filter(d => d.value > 0) : []

  return (
    <div className="space-y-6 animate-fade-in">
      {/* ── Page header ─────────────────────────────────────────────────── */}
      <div className="page-header">
        <div>
          <h2 className="page-title">Platform Overview</h2>
          <p className="page-subtitle">Real-time metrics across all integration modules</p>
        </div>
        <div className="flex items-center gap-2 text-xs text-slate-500 dark:text-slate-400">
          <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse-slow" />
          Live data
        </div>
      </div>

      {/* ── KPI grid ────────────────────────────────────────────────────── */}
      {loading ? (
        <div className="grid grid-cols-2 sm:grid-cols-3 xl:grid-cols-6 gap-4">
          {Array.from({ length: 6 }).map((_, i) => <SkeletonCard key={i} />)}
        </div>
      ) : (
        <div className="grid grid-cols-2 sm:grid-cols-3 xl:grid-cols-6 gap-4">
          <KpiCard icon={HiUsers}        label="Total Users"        value={fmtNumber(overview?.totalUsers)}           color="brand"  delay={0}    onClick={() => navigate('/users')} />
          <KpiCard icon={HiDatabase}     label="Data Sources"       value={fmtNumber(overview?.totalDataSources)}     color="blue"   delay={0.05} onClick={() => navigate('/data-sources')} />
          <KpiCard icon={HiCheckCircle}  label="Active Sources"     value={fmtNumber(overview?.activeDataSources)}    color="green"  delay={0.1}  onClick={() => navigate('/data-sources')} />
          <KpiCard icon={HiDownload}     label="Ingestion Jobs"     value={fmtNumber(overview?.totalIngestionJobs)}   color="purple" delay={0.15} onClick={() => navigate('/ingestion')} />
          <KpiCard icon={HiLightningBolt}label="Transform Rules"    value={fmtNumber(overview?.totalTransformationRules)} color="amber" delay={0.2} onClick={() => navigate('/transformation')} />
          <KpiCard icon={HiRefresh}      label="Sync Jobs"          value={fmtNumber(overview?.totalSyncJobs)}        color="brand"  delay={0.25} onClick={() => navigate('/sync')} />
        </div>
      )}

      {/* ── Second KPI row ──────────────────────────────────────────────── */}
      {!loading && (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          <KpiCard icon={HiTrendingUp}  label="Records Imported"   value={fmtNumber(overview?.totalImportedRecords)} color="green"  delay={0.3} />
          <KpiCard icon={HiCheckCircle} label="Sync Success Rate"  value={fmtPercent(overview?.syncSuccessPercent)}  color="green"  delay={0.35} />
          <KpiCard icon={HiXCircle}     label="Failed Ingestions"  value={fmtNumber(overview?.failedIngestionJobs)}  color="red"    delay={0.4} />
          <KpiCard icon={HiClock}       label="Last Sync"          value={fmtRelative(overview?.lastSynchronizationTime)} color="blue" delay={0.45} />
        </div>
      )}

      {/* ── Charts row 1 ────────────────────────────────────────────────── */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Ingestion status pie */}
        <Section title="Ingestion Status">
          {ingestionPieData.length > 0 ? (
            <ResponsiveContainer width="100%" height={220}>
              <PieChart>
                <Pie data={ingestionPieData} cx="50%" cy="50%" innerRadius={55} outerRadius={85}
                  paddingAngle={3} dataKey="value">
                  {ingestionPieData.map((_, i) => (
                    <Cell key={i} fill={COLORS[i % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip content={<ChartTooltip />} />
                <Legend iconType="circle" iconSize={8}
                  formatter={v => <span className="text-xs text-slate-400">{v}</span>} />
              </PieChart>
            </ResponsiveContainer>
          ) : (
            <div className="h-[220px] flex items-center justify-center text-slate-500 text-sm">No data yet</div>
          )}
        </Section>

        {/* Sync jobs bar */}
        <Section title="Sync Job Status">
          <ResponsiveContainer width="100%" height={220}>
            <BarChart data={syncBarData} barSize={28}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
              <XAxis dataKey="name" tick={{ fontSize: 11, fill: '#94a3b8' }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fontSize: 11, fill: '#94a3b8' }} axisLine={false} tickLine={false} />
              <Tooltip content={<ChartTooltip />} />
              <Bar dataKey="value" radius={[6, 6, 0, 0]}>
                {syncBarData.map((d, i) => <Cell key={i} fill={d.fill} />)}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </Section>

        {/* Data source distribution */}
        <Section title="Data Source Status">
          {dsData.length > 0 ? (
            <ResponsiveContainer width="100%" height={220}>
              <PieChart>
                <Pie data={dsData} cx="50%" cy="50%" outerRadius={85} paddingAngle={3} dataKey="value">
                  {dsData.map((d, i) => <Cell key={i} fill={d.fill} />)}
                </Pie>
                <Tooltip content={<ChartTooltip />} />
                <Legend iconType="circle" iconSize={8}
                  formatter={v => <span className="text-xs text-slate-400">{v}</span>} />
              </PieChart>
            </ResponsiveContainer>
          ) : (
            <div className="h-[220px] flex items-center justify-center text-slate-500 text-sm">No data yet</div>
          )}
        </Section>
      </div>

      {/* ── Charts row 2 ────────────────────────────────────────────────── */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Audit events by action */}
        <Section
          title="Top Audit Actions"
          action={
            <button onClick={() => navigate('/audit')} className="text-xs text-brand-400 hover:text-brand-300 flex items-center gap-1">
              View all <HiArrowSmRight className="w-3.5 h-3.5" />
            </button>
          }
        >
          {auditActionData.length > 0 ? (
            <ResponsiveContainer width="100%" height={220}>
              <BarChart data={auditActionData} layout="vertical" barSize={14}>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" horizontal={false} />
                <XAxis type="number" tick={{ fontSize: 11, fill: '#94a3b8' }} axisLine={false} tickLine={false} />
                <YAxis type="category" dataKey="name" width={130} tick={{ fontSize: 10, fill: '#94a3b8' }} axisLine={false} tickLine={false} />
                <Tooltip content={<ChartTooltip />} />
                <Bar dataKey="value" fill="#6366f1" radius={[0, 6, 6, 0]} />
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <div className="h-[220px] flex items-center justify-center text-slate-500 text-sm">No audit data yet</div>
          )}
        </Section>

        {/* Records overview area */}
        <Section title="Records Overview">
          <ResponsiveContainer width="100%" height={220}>
            <BarChart data={[
              { name: 'Imported',   value: overview?.totalImportedRecords ?? 0 },
              { name: 'Synced',     value: sync?.totalRecordsSynchronized ?? 0 },
              { name: 'Pending',    value: sync?.recordsPendingSync ?? 0 },
              { name: 'Failed',     value: ingestion?.failedRecords ?? 0 },
            ]} barSize={32}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
              <XAxis dataKey="name" tick={{ fontSize: 11, fill: '#94a3b8' }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fontSize: 11, fill: '#94a3b8' }} axisLine={false} tickLine={false} />
              <Tooltip content={<ChartTooltip />} />
              <Bar dataKey="value" radius={[6, 6, 0, 0]}>
                {['#6366f1','#10b981','#f59e0b','#ef4444'].map((c, i) => <Cell key={i} fill={c} />)}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </Section>
      </div>

      {/* ── Recent audit events ──────────────────────────────────────────── */}
      <Section
        title="Recent Activity"
        action={
          <button onClick={() => navigate('/audit')} className="text-xs text-brand-400 hover:text-brand-300 flex items-center gap-1">
            View all <HiArrowSmRight className="w-3.5 h-3.5" />
          </button>
        }
      >
        <div className="space-y-2">
          {audit?.recentEvents?.length > 0 ? audit.recentEvents.map((e, i) => (
            <motion.div
              key={e.timestamp ? `${e.username}-${e.timestamp}` : i}
              initial={{ opacity: 0, x: -10 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: i * 0.04 }}
              className="flex items-center gap-3 px-3 py-2.5 rounded-xl hover:bg-slate-50 dark:hover:bg-surface-700/50 transition-colors"
            >
              <span className={`w-2 h-2 rounded-full flex-shrink-0 ${
                e.status === 'SUCCESS' ? 'bg-emerald-400' :
                e.status === 'FAILED'  ? 'bg-red-400' : 'bg-amber-400'
              }`} />
              <span className="text-xs font-mono text-brand-400 w-44 truncate flex-shrink-0">{e.action}</span>
              <span className="text-xs text-slate-600 dark:text-slate-300 flex-1 truncate">{e.username}</span>
              <span className="text-xs text-slate-400 flex-shrink-0">{fmtRelative(e.timestamp)}</span>
            </motion.div>
          )) : (
            <p className="text-sm text-slate-400 text-center py-6">No recent activity</p>
          )}
        </div>
      </Section>
    </div>
  )
}
