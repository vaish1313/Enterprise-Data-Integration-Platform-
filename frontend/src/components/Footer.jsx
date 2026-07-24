export function Footer() {
  return (
    <footer className="text-center text-xs py-4" style={{ color: 'var(--text-muted)' }}>
      © {new Date().getFullYear()} EDIP — Enterprise Data Integration Platform
    </footer>
  )
}
