import type {
  InputHTMLAttributes,
  ReactNode,
  SelectHTMLAttributes,
  TextareaHTMLAttributes,
} from 'react'
import { cn } from '../../lib/cn'

/** Заедничкиот изглед на сите полиња за внес — порано препишуван во секоја форма. */
const CONTROL =
  'w-full rounded-xl border border-line bg-surface px-3.5 py-2.5 text-sm text-ink transition ' +
  'placeholder:text-ink-mute ' +
  'hover:border-ink-mute/50 ' +
  'focus:border-brand focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/25 ' +
  'disabled:cursor-not-allowed disabled:bg-surface-2 disabled:opacity-60'

interface FieldProps {
  label: string
  hint?: ReactNode
  children: ReactNode
}

export function Field({ label, hint, children }: FieldProps) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-sm font-medium text-ink">{label}</span>
      {children}
      {hint && <span className="mt-1.5 block text-xs text-ink-mute">{hint}</span>}
    </label>
  )
}

export function Input({ className, ...rest }: InputHTMLAttributes<HTMLInputElement>) {
  return <input className={cn(CONTROL, className)} {...rest} />
}

export function Select({ className, children, ...rest }: SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <select className={cn(CONTROL, 'cursor-pointer', className)} {...rest}>
      {children}
    </select>
  )
}

export function Textarea({ className, ...rest }: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea className={cn(CONTROL, 'resize-y', className)} {...rest} />
}
