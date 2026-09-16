import { useState, type InputHTMLAttributes } from 'react'
import { Input } from './Field'

type PasswordInputProps = Omit<InputHTMLAttributes<HTMLInputElement>, 'type'>

/** Поле за лозинка со око за да се провери што е напишано пред праќање. */
export function PasswordInput({ className, ...rest }: PasswordInputProps) {
  const [visible, setVisible] = useState(false)

  return (
    <span className="relative block">
      {/* pr-11: местото десно е за копчето, за да не му влезе текстот под него */}
      <Input type={visible ? 'text' : 'password'} className={`pr-11 ${className ?? ''}`} {...rest} />

      <button
        type="button"
        onClick={() => setVisible((current) => !current)}
        aria-label={visible ? 'Скрий ја лозинката' : 'Покажи ја лозинката'}
        aria-pressed={visible}
        title={visible ? 'Скрий ја лозинката' : 'Покажи ја лозинката'}
        className="absolute inset-y-0 right-0 grid w-11 place-items-center rounded-r-xl text-ink-mute transition hover:text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/40"
      >
        {visible ? <EyeOff /> : <Eye />}
      </button>
    </span>
  )
}

function Eye() {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      aria-hidden="true"
      className="h-[18px] w-[18px]"
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12Z"
      />
      <circle cx="12" cy="12" r="3" />
    </svg>
  )
}

function EyeOff() {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      aria-hidden="true"
      className="h-[18px] w-[18px]"
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M10.6 6.1A8.6 8.6 0 0 1 12 6c6 0 9.5 6 9.5 6a17 17 0 0 1-2.2 2.9M6.3 7.8A16.6 16.6 0 0 0 2.5 12s3.5 6 9.5 6a8.8 8.8 0 0 0 3.6-.76"
      />
      <path strokeLinecap="round" strokeLinejoin="round" d="m9.9 9.9a3 3 0 0 0 4.2 4.2M3 3l18 18" />
    </svg>
  )
}
