import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  getNotifications,
  getUnreadCount,
  markAllRead,
  markRead,
} from '../api/notifications'
import { cn } from '../lib/cn'
import { timeAgo } from '../lib/format'
import type { Notification, NotificationType } from '../types'

/** Колку често се прашува за бројот на непрочитани. */
const POLL_MS = 60_000

/** Над ова бројот во кругчето останува „9+", за да не се растегне. */
const MAX_BADGE = 9

const DOT: Record<NotificationType, string> = {
  NEW_SESSION: 'bg-brand',
  APPLICATION_RECEIVED: 'bg-warm',
  NEW_APPLICATION: 'bg-brand',
  APPLICATION_ACCEPTED: 'bg-good',
  APPLICATION_REJECTED: 'bg-bad',
  SESSION_UPDATED: 'bg-warm',
  SESSION_CANCELLED: 'bg-bad',
}

export function NotificationBell() {
  const navigate = useNavigate()

  const [open, setOpen] = useState(false)
  const [unread, setUnread] = useState(0)
  const [items, setItems] = useState<Notification[]>([])
  const [loading, setLoading] = useState(false)

  const wrapper = useRef<HTMLDivElement>(null)

  const refreshCount = useCallback(() => {
    getUnreadCount()
      .then(setUnread)
      .catch(() => undefined)
  }, [])

  // Бројот се обновува во заднина; целиот список само кога ќе се отвори
  useEffect(() => {
    refreshCount()
    const timer = window.setInterval(refreshCount, POLL_MS)
    return () => window.clearInterval(timer)
  }, [refreshCount])

  // Клик надвор или Escape го затвора прозорчето
  useEffect(() => {
    if (!open) {
      return
    }

    function onClick(event: MouseEvent) {
      if (!wrapper.current?.contains(event.target as Node)) {
        setOpen(false)
      }
    }

    function onKey(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setOpen(false)
      }
    }

    document.addEventListener('mousedown', onClick)
    document.addEventListener('keydown', onKey)

    return () => {
      document.removeEventListener('mousedown', onClick)
      document.removeEventListener('keydown', onKey)
    }
  }, [open])

  function toggle() {
    if (open) {
      setOpen(false)
      return
    }

    setOpen(true)
    setLoading(true)

    getNotifications()
      .then((loaded) => {
        setItems(loaded)
        setUnread(loaded.filter((item) => !item.read).length)
      })
      .catch(() => undefined)
      .finally(() => setLoading(false))
  }

  function handleOpenItem(item: Notification) {
    setOpen(false)

    if (!item.read) {
      // Бројот се намалува веднаш: ако барањето падне, следното прашање ќе го поправи
      setItems((current) =>
        current.map((one) => (one.id === item.id ? { ...one, read: true } : one)),
      )
      setUnread((current) => Math.max(0, current - 1))
      markRead(item.id).catch(refreshCount)
    }

    if (item.sessionId) {
      navigate(`/sessions/${item.sessionId}`)
    }
  }

  function handleMarkAll() {
    setItems((current) => current.map((item) => ({ ...item, read: true })))
    setUnread(0)
    markAllRead().catch(refreshCount)
  }

  const badge = unread > MAX_BADGE ? `${MAX_BADGE}+` : String(unread)

  return (
    <div ref={wrapper} className="relative">
      <button
        onClick={toggle}
        aria-label={unread > 0 ? `Известувања (${unread} непрочитани)` : 'Известувања'}
        aria-expanded={open}
        title="Известувања"
        className={cn(
          'relative grid h-9 w-9 place-items-center rounded-full transition',
          'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/60',
          open ? 'bg-brand/12 text-brand' : 'text-ink-soft hover:bg-surface-2 hover:text-ink',
        )}
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" className="h-5 w-5">
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            d="M18 8a6 6 0 1 0-12 0c0 3.5-.8 5.4-1.6 6.5-.4.6 0 1.5.8 1.5h13.6c.8 0 1.2-.9.8-1.5C18.8 13.4 18 11.5 18 8Z"
          />
          <path strokeLinecap="round" d="M9.5 19a2.5 2.5 0 0 0 5 0" />
        </svg>

        {unread > 0 && (
          <span className="absolute -right-0.5 -top-0.5 grid h-[18px] min-w-[18px] place-items-center rounded-full bg-bad px-1 text-[10px] font-semibold leading-none text-white">
            {badge}
          </span>
        )}
      </button>

      {open && (
        <div
          role="dialog"
          aria-label="Известувања"
          // На телефон ѕвончето е блиску до десниот раб, па прозорче закачено
          // под него би излегло од екранот налево — затоа таму е по цела ширина
          className={cn(
            'fixed inset-x-4 top-[4.25rem] z-20 overflow-hidden rounded-2xl',
            'border border-line bg-surface shadow-lift',
            'sm:absolute sm:inset-x-auto sm:right-0 sm:top-full sm:mt-2 sm:w-[22rem]',
          )}
        >
          <div className="flex items-center justify-between gap-2 border-b border-line px-4 py-2.5">
            <span className="text-sm font-semibold text-ink">Известувања</span>

            {unread > 0 && (
              <button
                onClick={handleMarkAll}
                className="rounded-lg px-1.5 py-0.5 text-xs font-medium text-brand transition hover:bg-brand/10"
              >
                Означи ги прочитани
              </button>
            )}
          </div>

          <div className="max-h-80 overflow-y-auto">
            {loading && <p className="px-4 py-6 text-center text-sm text-ink-mute">Се вчитува…</p>}

            {!loading && items.length === 0 && (
              <p className="px-4 py-6 text-center text-sm text-ink-mute">Сè е прочитано.</p>
            )}

            {!loading &&
              items.map((item) => (
                <button
                  key={item.id}
                  onClick={() => handleOpenItem(item)}
                  className={cn(
                    'flex w-full items-start gap-2.5 border-b border-line px-4 py-3 text-left transition last:border-0',
                    'hover:bg-surface-2',
                    item.read ? 'bg-surface' : 'bg-brand/[0.06]',
                  )}
                >
                  <span className={cn('mt-1.5 h-2 w-2 shrink-0 rounded-full', DOT[item.type])} />

                  <span className="min-w-0 flex-1">
                    <span
                      className={cn(
                        'block text-sm text-ink',
                        item.read ? 'font-medium' : 'font-semibold',
                      )}
                    >
                      {item.title}
                    </span>

                    {item.body && (
                      <span className="mt-0.5 block text-xs leading-relaxed text-ink-soft">
                        {item.body}
                      </span>
                    )}

                    <span className="mt-1 block text-[11px] text-ink-mute">
                      {timeAgo(item.createdAt)}
                    </span>
                  </span>
                </button>
              ))}
          </div>
        </div>
      )}
    </div>
  )
}
