import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { Avatar } from './Avatar'
import { SubjectBadge } from './SubjectBadge'
import { Alert, Badge, Button, Card, Input } from './ui'
import { useAuth } from '../context/AuthContext'
import { addComment, deleteComment, deletePost } from '../api/feed'
import { cn } from '../lib/cn'
import { getErrorMessage } from '../lib/errors'
import { fileSize, timeAgo } from '../lib/format'
import { roleLabel } from '../lib/labels'
import type { Attachment, Post } from '../types'

interface PostCardProps {
  post: Post
  /** Одговорот од серверот е целата објава — таа ја заменува старата. */
  onChange: (post: Post) => void
  onRemoved: (postId: number) => void
}

export function PostCard({ post, onChange, onRemoved }: PostCardProps) {
  const { user } = useAuth()

  const [comment, setComment] = useState('')
  const [busy, setBusy] = useState(false)
  const [problem, setProblem] = useState<string | null>(null)
  const [confirmingDelete, setConfirmingDelete] = useState(false)

  const isAdmin = user?.role === 'ADMIN'
  const canDeletePost = post.author.id === user?.id || isAdmin

  async function submitComment(event: FormEvent) {
    event.preventDefault()

    const text = comment.trim()
    if (!text) {
      return
    }

    setProblem(null)
    setBusy(true)

    try {
      onChange(await addComment(post.id, text))
      setComment('')
    } catch (err) {
      setProblem(getErrorMessage(err, 'Коментарот не се зачува.'))
    } finally {
      setBusy(false)
    }
  }

  async function removePost() {
    setProblem(null)
    setBusy(true)

    try {
      await deletePost(post.id)
      onRemoved(post.id)
    } catch (err) {
      setProblem(getErrorMessage(err, 'Бришењето не успеа.'))
      setBusy(false)
    }
  }

  async function removeComment(commentId: number) {
    setProblem(null)

    try {
      await deleteComment(commentId)
      onChange({ ...post, comments: post.comments.filter((item) => item.id !== commentId) })
    } catch (err) {
      setProblem(getErrorMessage(err, 'Бришењето не успеа.'))
    }
  }

  const images = post.attachments.filter((file) => file.image)
  const documents = post.attachments.filter((file) => !file.image)

  return (
    <Card padding="lg" className="flex flex-col gap-4">
      {/* Автор */}
      <div className="flex items-start gap-3">
        <Link to={`/users/${post.author.id}`} title="Отвори профил">
          <Avatar name={post.author.fullName} src={post.author.avatarUrl} />
        </Link>

        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
            <Link
              to={`/users/${post.author.id}`}
              className="font-medium text-ink hover:text-brand hover:underline"
            >
              {post.author.fullName}
            </Link>

            {/* Улогата е важна: совет од ментор не е исто како коментар од колега */}
            {post.author.role === 'MENTOR' && <Badge tone="brand">{roleLabel('MENTOR')}</Badge>}

            <span className="text-xs text-ink-mute">{timeAgo(post.createdAt)}</span>
          </div>

          {post.subject && (
            <div className="mt-1.5">
              <SubjectBadge name={post.subject.name} />
            </div>
          )}
        </div>

        {canDeletePost && !confirmingDelete && (
          <Button
            variant="ghost"
            size="sm"
            aria-label="Избриши објава"
            onClick={() => setConfirmingDelete(true)}
          >
            ×
          </Button>
        )}
      </div>

      <p className="whitespace-pre-line break-words text-sm leading-relaxed text-ink-soft">
        {post.text}
      </p>

      {images.length > 0 && (
        <div className={cn('grid gap-2', images.length > 1 ? 'sm:grid-cols-2' : 'grid-cols-1')}>
          {images.map((image) => (
            <a
              key={image.id}
              href={image.url}
              target="_blank"
              rel="noreferrer"
              className="block overflow-hidden rounded-xl border border-line"
            >
              <img
                src={image.url}
                alt={image.filename}
                loading="lazy"
                className="max-h-80 w-full bg-surface-2 object-cover"
              />
            </a>
          ))}
        </div>
      )}

      {documents.length > 0 && (
        <ul className="flex flex-col gap-2">
          {documents.map((file) => (
            <li key={file.id}>
              <DocumentRow file={file} />
            </li>
          ))}
        </ul>
      )}

      {problem && <Alert>{problem}</Alert>}

      {confirmingDelete && (
        <div className="flex flex-wrap items-center gap-3 rounded-xl border border-bad/30 bg-bad/5 px-4 py-3">
          <span className="text-sm text-ink">Да се избрише објавата со коментарите?</span>
          <Button variant="bad" size="sm" disabled={busy} onClick={removePost}>
            {busy ? 'Се брише...' : 'Избриши'}
          </Button>
          <Button variant="ghost" size="sm" disabled={busy} onClick={() => setConfirmingDelete(false)}>
            Откажи
          </Button>
        </div>
      )}

      {/* Коментари */}
      <div className="border-t border-line pt-4">
        {post.comments.length > 0 && (
          <ul className="mb-3 flex flex-col gap-3">
            {post.comments.map((item) => (
              <li key={item.id} className="flex items-start gap-2.5">
                <Link to={`/users/${item.author.id}`} title="Отвори профил">
                  <Avatar name={item.author.fullName} src={item.author.avatarUrl} size="sm" />
                </Link>

                <div className="min-w-0 flex-1 rounded-xl bg-surface-2 px-3 py-2">
                  <div className="flex flex-wrap items-center gap-x-2">
                    <Link
                      to={`/users/${item.author.id}`}
                      className="text-sm font-medium text-ink hover:text-brand hover:underline"
                    >
                      {item.author.fullName}
                    </Link>
                    {item.author.role === 'MENTOR' && (
                      <span className="text-[11px] font-medium text-brand">ментор</span>
                    )}
                    <span className="text-[11px] text-ink-mute">{timeAgo(item.createdAt)}</span>

                    {(item.author.id === user?.id || isAdmin) && (
                      <button
                        type="button"
                        aria-label="Избриши коментар"
                        onClick={() => removeComment(item.id)}
                        className="ml-auto text-xs text-ink-mute transition hover:text-bad"
                      >
                        ×
                      </button>
                    )}
                  </div>

                  <p className="mt-0.5 whitespace-pre-line break-words text-sm text-ink-soft">
                    {item.text}
                  </p>
                </div>
              </li>
            ))}
          </ul>
        )}

        <form onSubmit={submitComment} className="flex items-center gap-2">
          <Avatar name={user?.fullName ?? '?'} src={user?.avatarUrl} size="sm" />
          <Input
            value={comment}
            maxLength={1000}
            onChange={(event) => setComment(event.target.value)}
            placeholder="Напиши коментар..."
          />
          <Button type="submit" size="sm" disabled={busy || !comment.trim()}>
            Прати
          </Button>
        </form>
      </div>
    </Card>
  )
}

function DocumentRow({ file }: { file: Attachment }) {
  return (
    <a
      href={file.url}
      download={file.filename}
      className="flex items-center gap-3 rounded-xl border border-line bg-surface-2/60 px-3 py-2.5 transition hover:border-brand/40 hover:bg-surface-2"
    >
      <span aria-hidden="true" className="grid h-9 w-9 shrink-0 place-items-center rounded-lg bg-brand/10 text-base">
        {file.contentType === 'application/pdf' ? '📄' : '📎'}
      </span>

      <span className="min-w-0 flex-1">
        <span className="block truncate text-sm font-medium text-ink">{file.filename}</span>
        <span className="block text-xs text-ink-mute">{fileSize(file.sizeBytes)}</span>
      </span>

      <span className="shrink-0 text-xs font-medium text-brand">Симни</span>
    </a>
  )
}
