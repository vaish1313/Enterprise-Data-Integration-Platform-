import React, { Component } from 'react'
import { HiExclamation, HiRefresh } from 'react-icons/hi'

export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props)
    this.state = { hasError: false, error: null }
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error }
  }

  componentDidCatch(error, errorInfo) {
    console.error('ErrorBoundary caught an error:', error, errorInfo)
  }

  handleReload = () => {
    window.location.reload()
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-[60vh] flex flex-col items-center justify-center p-6 text-center animate-fade-in">
          <div className="glass-card p-10 flex flex-col items-center max-w-md w-full border border-[var(--border-subtle)] bg-[var(--bg-card)]">
            <div className="w-16 h-16 rounded-full bg-[var(--bg-hover)] flex items-center justify-center mb-6 border border-[var(--border-strong)]">
              <HiExclamation className="w-8 h-8 text-[var(--text-primary)]" />
            </div>
            
            <h2 className="text-xl font-bold mb-3 tracking-tight" style={{ color: 'var(--text-primary)' }}>
              Something went wrong
            </h2>
            
            <p className="text-sm mb-8 leading-relaxed" style={{ color: 'var(--text-secondary)' }}>
              We encountered an unexpected error while trying to load this page. 
              This might be due to a network issue or a stale deployment.
            </p>
            
            <button
              onClick={this.handleReload}
              className="flex items-center justify-center gap-2 w-full py-3 px-4 rounded-xl font-medium transition-all"
              style={{
                backgroundColor: 'var(--text-primary)',
                color: 'var(--bg-base)',
              }}
              onMouseOver={(e) => e.currentTarget.style.opacity = '0.9'}
              onMouseOut={(e) => e.currentTarget.style.opacity = '1'}
            >
              <HiRefresh className="w-4 h-4" />
              Reload Page
            </button>
          </div>
        </div>
      )
    }

    return this.props.children
  }
}
