import { Link } from 'react-router-dom'
import { Avatar } from './Avatar'
import { SubjectBadge } from './SubjectBadge'
import { Card, EmptyState, Section, SkeletonList } from './ui'
import { useAsync } from '../hooks/useAsync'
import { getFeed } from '../api/feed'
import { timeAgo } from '../lib/format'

/** Колку објави се прикажуваат во панелот пред „отвори ги постовите". */
const DEFAULT_PREVIEW = 3

interface FeedPanelProps {
  /** Пократок панел кога стои во тесна колона (пр. под барањата на менторот). */
  limit?: number
}

export function FeedPanel({ limit = DEFAULT_PREVIEW }: FeedPanelProps) {
  const { data, loading, error } = useAsync(() => getFeed(), [])
  const posts = (data ?? []).slice(0, limit)

  return (
    <Section
      title="Постови"
      action={
        <Link to="/feed" className="text-sm font-medium text-brand hover:underline">
          Отвори ги постовите
        </Link>
      }
    >
      {loading ? (
        <SkeletonList rows={Math.min(2, limit)} />
      ) : error || posts.length === 0 ? (
        <EmptyState
          emoji="💬"
          title="Сè уште нема постови"
          description="Прашај за задача, побарај соработка или подели материјал со другите."
          action={
            <Link to="/feed" className="text-sm font-medium text-brand hover:underline">
              Напиши објава
            </Link>
          }
        />
      ) : (
        <ul className="flex flex-col gap-3">
          {posts.map((post) => (
            <li key={post.id}>
              <Link to="/feed" className="block rounded-2xl">
                <Card interactive className="flex items-start gap-3">
                  <Avatar name={post.author.fullName} src={post.author.avatarUrl} size="sm" />

                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-x-2">
                      <span className="text-sm font-medium text-ink">{post.author.fullName}</span>
                      {post.author.role === 'MENTOR' && (
                        <span className="text-[11px] font-medium text-brand">ментор</span>
                      )}
                      <span className="text-[11px] text-ink-mute">{timeAgo(post.createdAt)}</span>
                    </div>

                    {/* line-clamp-2: во панелот стои поводот, не целата објава */}
                    <p className="mt-0.5 line-clamp-2 break-words text-sm text-ink-soft">
                      {post.text}
                    </p>

                    <div className="mt-1.5 flex flex-wrap items-center gap-2 text-xs text-ink-mute">
                      {post.subject && <SubjectBadge name={post.subject.name} />}
                      {post.attachments.length > 0 && (
                        <span>📎 {post.attachments.length}</span>
                      )}
                      {post.comments.length > 0 && <span>💬 {post.comments.length}</span>}
                    </div>
                  </div>
                </Card>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </Section>
  )
}
