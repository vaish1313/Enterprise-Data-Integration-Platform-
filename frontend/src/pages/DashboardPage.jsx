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
const COLORS = ['#ffffff', '#a3a3a3', '#6b7280', '#4b5563', '#374151', '#1f2937']

/* ── Animated KPI card ──────────────────────────────────────────────────── */
function KpiCard({ icon: Icon, label, value, sub, delay = 0, onClick }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay, duration: 0.4 }}
      onClick={onClick}
      className={`glass-card p-5 cursor-pointer hover:scale-[1.02] transition-transform duration-200`}
    >
      <div className="flex items-start justify-between">
        <div className="p-2.5 rounded-full" style={{ background: 'rgba(255,255,255,0.08)' }}>
          <Icon className="w-5 h-5" style={{ color: 'var(--text-primary)' }} />
        </div>
        {sub != null && (
          <span className="text-xs font-semibold" style={{ color: 'var(--text-muted)' }}>{sub}</span>
        )}
      </div>
      <p className="mt-4 text-2xl font-extrabold" style={{ color: 'var(--text-primary)' }}>{value}</p>
      <p className="text-xs font-medium mt-1" style={{ color: 'var(--text-secondary)' }}>{label}</p>
    </motion.div>
  )
}

/* ── Section wrapper ────────────────────────────────────────────────────── */
function Section({ title, action, children }) {
  return (
    <div className="glass-card p-5">
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
    <div className="rounded-xl px-3 py-2 text-xs" style={{ background: 'var(--bg-base)', border: '1px solid var(--glass-border)' }}>
      <p className="font-semibold mb-1" style={{ color: 'var(--text-primary)' }}>{label}</p>
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
    { name: 'Completed', value: sync.completedJobs,  fill: '#ffffff' },
    { name: 'Failed',    value: sync.failedJobs,     fill: '#6b7280' },
    { name: 'Running',   value: sync.runningJobs,    fill: '#a3a3a3' },
    { name: 'Pending',   value: sync.pendingJobs,    fill: '#4b5563' },
  ] : []

  const auditActionData = audit?.eventsByAction
    ? Object.entries(audit.eventsByAction).slice(0, 6).map(([name, value]) => ({ name, value }))
    : []

  const dsData = overview ? [
    { name: 'Active',   value: overview.activeDataSources,   fill: '#ffffff' },
    { name: 'Inactive', value: overview.inactiveDataSources, fill: '#a3a3a3' },
    { name: 'Error',    value: overview.errorDataSources,    fill: '#6b7280' },
  ].filter(d => d.value > 0) : []

  return (
    <div className="space-y-6 animate-fade-in">
      {/* ── Page header ─────────────────────────────────────────────────── */}
      <div className="page-header">
        <div>
          <h2 className="page-title">Platform Overview</h2>
          <p className="page-subtitle">Real-time metrics across all integration modules</p>
        </div>
        <div className="flex items-center gap-2 text-xs" style={{ color: 'var(--text-muted)' }}>
          <span className="w-2 h-2 rounded-full animate-pulse-slow" style={{ background: 'var(--text-primary)' }} />
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
          <KpiCard icon={HiUsers}        label="Total Users"        value={fmtNumber(overview?.totalUsers)}           delay={0}    onClick={() => navigate('/users')} />
          <KpiCard icon={HiDatabase}     label="Data Sources"       value={fmtNumber(overview?.totalDataSources)}     delay={0.05} onClick={() => navigate('/data-sources')} />
          <KpiCard icon={HiCheckCircle}  label="Active Sources"     value={fmtNumber(overview?.activeDataSources)}    delay={0.1}  onClick={() => navigate('/data-sources')} />
          <KpiCard icon={HiDownload}     label="Ingestion Jobs"     value={fmtNumber(overview?.totalIngestionJobs)}   delay={0.15} onClick={() => navigate('/ingestion')} />
          <KpiCard icon={HiLightningBolt}label="Transform Rules"    value={fmtNumber(overview?.totalTransformationRules)} delay={0.2} onClick={() => navigate('/transformation')} />
          <KpiCard icon={HiRefresh}      label="Sync Jobs"          value={fmtNumber(overview?.totalSyncJobs)}        delay={0.25} onClick={() => navigate('/sync')} />
        </div>
      )}

      {/* ── Second KPI row ──────────────────────────────────────────────── */}
      {!loading && (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          <KpiCard icon={HiTrendingUp}  label="Records Imported"   value={fmtNumber(overview?.totalImportedRecords)} delay={0.3} />
          <KpiCard icon={HiCheckCircle} label="Sync Success Rate"  value={fmtPercent(overview?.syncSuccessPercent)}  delay={0.35} />
          <KpiCard icon={HiXCircle}     label="Failed Ingestions"  value={fmtNumber(overview?.failedIngestionJobs)}  delay={0.4} />
          <KpiCard icon={HiClock}       label="Last Sync"          value={fmtRelative(overview?.lastSynchronizationTime)} delay={0.45} />
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
                  formatter={v => <span style={{ color: 'var(--text-muted)' }}>{v}</span>} />
              </PieChart>
            </ResponsiveContainer>
          ) : (
            <div className="h-[220px] flex items-center justify-center text-sm" style={{ color: 'var(--text-muted)' }}>No data yet</div>
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
                  formatter={v => <span style={{ color: 'var(--text-muted)' }}>{v}</span>} />
              </PieChart>
            </ResponsiveContainer>
          ) : (
            <div className="h-[220px] flex items-center justify-center text-sm" style={{ color: 'var(--text-muted)' }}>No data yet</div>
          )}
        </Section>
      </div>

      {/* ── Charts row 2 ────────────────────────────────────────────────── */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Audit events by action */}
        <Section
          title="Top Audit Actions"
          action={
            <button onClick={() => navigate('/audit')} className="text-xs flex items-center gap-1 hover:opacity-80" style={{ color: 'var(--text-primary)' }}>
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
                <Bar dataKey="value" fill="#ffffff" radius={[0, 6, 6, 0]} />
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <div className="h-[220px] flex items-center justify-center text-sm" style={{ color: 'var(--text-muted)' }}>No audit data yet</div>
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
                {['#ffffff','#a3a3a3','#6b7280','#4b5563'].map((c, i) => <Cell key={i} fill={c} />)}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </Section>
      </div>

      {/* ── Recent audit events ──────────────────────────────────────────── */}
      <Section
        title="Recent Activity"
        action={
          <button onClick={() => navigate('/audit')} className="text-xs flex items-center gap-1 hover:opacity-80" style={{ color: 'var(--text-primary)' }}>
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
              className="flex items-center gap-3 px-3 py-2.5 rounded-xl hover:opacity-80 transition-colors" style={{ background: 'var(--glass-fill)' }}
            >
              <span className="w-2 h-2 rounded-full flex-shrink-0" style={{ background: 'var(--text-secondary)' }} />
              <span className="text-xs font-mono w-44 truncate flex-shrink-0" style={{ color: 'var(--text-primary)' }}>{e.action}</span>
              <span className="text-xs flex-1 truncate" style={{ color: 'var(--text-secondary)' }}>{e.username}</span>
              <span className="text-xs flex-shrink-0" style={{ color: 'var(--text-muted)' }}>{fmtRelative(e.timestamp)}</span>
            </motion.div>
          )) : (
            <p className="text-sm text-center py-6" style={{ color: 'var(--text-muted)' }}>No recent activity</p>
          )}
        </div>
      </Section>
    </div>
  )
}
