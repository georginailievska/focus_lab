import type { ReactNode } from 'react'
import Navbar from './Navbar'

export default function Layout({ children }: { children: ReactNode }) {
  return (
    <div className="min-h-screen bg-page text-ink-soft">
      {/* Мек тополувзаемен сјај зад горниот дел — само атмосфера, не пречи на текстот */}
      <div
        aria-hidden="true"
        className="pointer-events-none fixed inset-x-0 top-0 h-64 bg-gradient-to-b from-brand/8 to-transparent"
      />

      <div className="relative">
        <Navbar />
        <main className="mx-auto max-w-5xl animate-fade-in px-4 py-8">{children}</main>
      </div>
    </div>
  )
}
