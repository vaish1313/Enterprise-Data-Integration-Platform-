/**
 * navigationService.js
 *
 * A module-level singleton that holds React Router's `navigate` function.
 * Because axios interceptors run outside React component context, they
 * cannot call `useNavigate()` directly. Instead:
 *   1. App.jsx calls `setNavigate(navigate)` once on mount.
 *   2. axiosInstance.js calls `redirect('/login')` from the interceptor.
 *
 * This avoids a full `window.location.href` reload which would wipe all
 * React state, Zustand/context stores, and pending toasts.
 */

let _navigate = null
let _onLogout = null

/**
 * Called once from App.jsx (or any top-level component that has router context).
 * @param {import('react-router-dom').NavigateFunction} navigateFn
 */
export function setNavigate(navigateFn) {
  _navigate = navigateFn
}

/**
 * Called once from AuthContext.jsx to register the React state-aware logout function.
 * @param {function} logoutFn
 */
export function setOnLogout(logoutFn) {
  _onLogout = logoutFn
}

/**
 * Redirect imperatively from outside React.
 * Falls back to window.location only if navigate hasn't been set yet
 * (e.g. during SSR or early init — shouldn't happen in this SPA).
 * @param {string} path
 */
export function redirect(path) {
  if (_navigate) {
    _navigate(path, { replace: true })
  } else {
    // Fallback — should never reach here in normal operation
    window.location.href = path
  }
}

/**
 * Trigger the React AuthContext logout from outside React.
 * @param {string} [msg] Optional toast message to display.
 */
export function triggerLogout(msg) {
  if (_onLogout) {
    _onLogout(msg)
  }
}

