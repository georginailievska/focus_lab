import { api } from './client'
import type { Post } from '../types'

interface FeedQuery {
  /** ID на последната прикажана објава — курсор за „прикажи повеќе". */
  before?: number
  subjectId?: number
}

export function getFeed({ before, subjectId }: FeedQuery = {}) {
  return api
    .get<Post[]>('/posts', {
      params: { ...(before ? { before } : {}), ...(subjectId ? { subjectId } : {}) },
    })
    .then((res) => res.data)
}

export function createPost(text: string, subjectId: number | null, files: File[]) {
  const form = new FormData()
  form.append(
    'post',
    new Blob([JSON.stringify({ text, subjectId })], { type: 'application/json' }),
  )
  files.forEach((file) => form.append('files', file))

  return api.post<Post>('/posts', form).then((res) => res.data)
}

/** Одговорот е целата објава со новиот коментар — еден извор на вистина. */
export function addComment(postId: number, text: string) {
  return api.post<Post>(`/posts/${postId}/comments`, { text }).then((res) => res.data)
}

export function deletePost(postId: number) {
  return api.delete<void>(`/posts/${postId}`).then((res) => res.data)
}

export function deleteComment(commentId: number) {
  return api.delete<void>(`/posts/comments/${commentId}`).then((res) => res.data)
}
