import { useEffect, useState } from 'react'
import Layout from '../components/Layout'
import { PostCard } from '../components/PostCard'
import { PostComposer } from '../components/PostComposer'
import {
  Alert,
  Button,
  EmptyState,
  PageHeader,
  Select,
  SkeletonList,
} from '../components/ui'
import { useAuth } from '../context/AuthContext'
import { useAsync } from '../hooks/useAsync'
import { getFeed } from '../api/feed'
import { getSubjects } from '../api/subjects'
import type { Post } from '../types'

/** Колку објави враќа серверот по страница — исто како PAGE_SIZE во FeedService. */
const PAGE_SIZE = 20

export default function FeedPage() {
  const { user } = useAuth()

  const [subjectId, setSubjectId] = useState('')
  const subjects = useAsync(getSubjects, [])

  const { data, loading, error } = useAsync(
    () => getFeed({ subjectId: subjectId ? Number(subjectId) : undefined }),
    [subjectId],
  )

  const [posts, setPosts] = useState<Post[]>([])
  const [loadingMore, setLoadingMore] = useState(false)
  const [exhausted, setExhausted] = useState(false)
  const [problem, setProblem] = useState<string | null>(null)

  useEffect(() => {
    if (data) {
      setPosts(data)
      setExhausted(data.length < PAGE_SIZE)
    }
  }, [data])

  const canWrite = user?.role === 'STUDENT' || user?.role === 'MENTOR'

  async function loadMore() {
    const last = posts[posts.length - 1]
    if (!last) {
      return
    }

    setProblem(null)
    setLoadingMore(true)

    try {
      const older = await getFeed({
        before: last.id,
        subjectId: subjectId ? Number(subjectId) : undefined,
      })

      setPosts((current) => [...current, ...older])
      setExhausted(older.length < PAGE_SIZE)
    } catch {
      setProblem('Не успеа вчитувањето на постарите објави.')
    } finally {
      setLoadingMore(false)
    }
  }

  return (
    <Layout>
      <PageHeader
        title="Постови"
        subtitle="Прашај за задача, побарај соработка или подели материјал."
        actions={
          <span className="block w-40 sm:w-52">
            <Select
              aria-label="Предмет"
              className="py-1.5 text-xs"
              value={subjectId}
              onChange={(event) => setSubjectId(event.target.value)}
            >
              <option value="">Сите предмети</option>
              {(subjects.data ?? []).map((subject) => (
                <option key={subject.id} value={subject.id}>
                  {subject.name}
                </option>
              ))}
            </Select>
          </span>
        }
      />

      <div className="flex flex-col gap-5">
        {canWrite && (
          <PostComposer
            subjects={subjects.data ?? []}
            onPosted={(post) => setPosts((current) => [post, ...current])}
          />
        )}

        {error && <Alert>{error}</Alert>}
        {problem && <Alert>{problem}</Alert>}

        {loading ? (
          <SkeletonList rows={3} />
        ) : posts.length === 0 ? (
          <EmptyState
            emoji="💬"
            title={subjectId ? 'Нема постови по овој предмет' : 'Сè уште нема постови'}
            description={
              canWrite
                ? 'Биди прва: прикачи слика од задачата и напиши каде запна.'
                : 'Штом студентите почнат да објавуваат, ќе се појави тука.'
            }
          />
        ) : (
          <>
            {posts.map((post) => (
              <PostCard
                key={post.id}
                post={post}
                onChange={(updated) =>
                  setPosts((current) =>
                    current.map((item) => (item.id === updated.id ? updated : item)),
                  )
                }
                onRemoved={(postId) =>
                  setPosts((current) => current.filter((item) => item.id !== postId))
                }
              />
            ))}

            {!exhausted && (
              <div className="flex justify-center">
                <Button variant="secondary" disabled={loadingMore} onClick={loadMore}>
                  {loadingMore ? 'Се вчитува...' : 'Прикажи постари'}
                </Button>
              </div>
            )}
          </>
        )}
      </div>
    </Layout>
  )
}
