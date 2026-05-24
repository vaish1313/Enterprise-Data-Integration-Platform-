import { Component } from 'react'
import { HiExclamationCircle } from 'react-icons/hi'

export class ErrorBoundary extends Component {
  state = { hasError: false, error: null }

  static getDerivedStateFromError(error) {
    return { hasError: true, error }
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="flex flex-col items-center justify-center h-64 gap-4">
          <HiExclamationCircle className="w-12 h-12 text-red-400" />
          <div className="text-center">
            <p className="font-semibold text-slate-800 dark:text-white">Something went wrong</p>
            <p className="text-sm text-slate-500 mt-1">{this.state.error?.message}</p>
          </div>
          <button className="btn-secondary" onClick={() => this.setState({ hasError: false })}>
            Try again
          </button>
        </div>
      )
    }
    return this.props.children
  }
}
