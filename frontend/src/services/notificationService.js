import toast from 'react-hot-toast'

export const notify = {
  success: (msg) => toast.success(msg),
  error:   (msg) => toast.error(msg),
  info:    (msg) => toast(msg, { icon: 'ℹ️' }),
  warn:    (msg) => toast(msg, { icon: '⚠️' }),
  promise: (promise, msgs) => toast.promise(promise, msgs),
}
