export const required = (msg = 'This field is required') => ({ required: msg })

export const minLen = (n) => ({
  minLength: { value: n, message: `Minimum ${n} characters` },
})

export const emailPattern = {
  pattern: { value: /\S+@\S+\.\S+/, message: 'Invalid email address' },
}

export const passwordRules = {
  required: 'Password is required',
  minLength: { value: 8, message: 'Password must be at least 8 characters' },
}
