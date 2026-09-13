import { useRef, useState } from 'react'
import { Alert, Button, Card, Select, Textarea } from './ui'
import { Avatar } from './Avatar'
import { useAuth } from '../context/AuthContext'
import { createPost } from '../api/feed'
import { getErrorMessage } from '../lib/errors'
import { fileSize } from '../lib/format'
import type { Post, Subject } from '../types'

/** Истите граници како на серверот — грешката да се види без качување. */
const MAX_FILES = 4
const MAX_FILE_BYTES = 5 * 1024 * 1024
const MAX_TEXT = 2000

const ACCEPT = [
  'image/png',
  'image/jpeg',
  'image/webp',
  'image/gif',
  'application/pdf',
  'text/plain',
  'application/zip',
  '.doc',
  '.docx',
  '.xls',
  '.xlsx',
  '.ppt',
  '.pptx',
].join(',')

interface PostComposerProps {
  subjects: Subject[]
  onPosted: (post: Post) => void
}

export function PostComposer({ subjects, onPosted }: PostComposerProps) {
  const { user } = useAuth()
  const fileInput = useRef<HTMLInputElement>(null)

  const [text, setText] = useState('')
  const [subjectId, setSubjectId] = useState('')
  const [files, setFiles] = useState<File[]>([])
  const [problem, setProblem] = useState<string | null>(null)
  const [sending, setSending] = useState(false)

  function addFiles(selected: FileList | null) {
    if (!selected || selected.length === 0) {
      return
    }

    setProblem(null)
    const incoming = Array.from(selected)

    const tooBig = incoming.find((file) => file.size > MAX_FILE_BYTES)
    if (tooBig) {
      setProblem(`„${tooBig.name}" е поголема од 5 MB.`)
      return
    }

    if (files.length + incoming.length > MAX_FILES) {
      setProblem(`Најмногу ${MAX_FILES} датотеки по објава.`)
      return
    }

    setFiles((current) => [...current, ...incoming])
  }

  async function submit() {
    const body = text.trim()
    if (!body) {
      setProblem('Напиши нешто пред да објавиш.')
      return
    }

    setProblem(null)
    setSending(true)

    try {
      const post = await createPost(body, subjectId ? Number(subjectId) : null, files)

      setText('')
      setSubjectId('')
      setFiles([])
      onPosted(post)
    } catch (err) {
      setProblem(getErrorMessage(err, 'Објавата не се зачува.'))
    } finally {
      setSending(false)
    }
  }

  if (!user) {
    return null
  }

  return (
    <Card padding="lg" className="flex gap-3">
      <Avatar name={user.fullName} src={user.avatarUrl} />

      <div className="flex min-w-0 flex-1 flex-col gap-3">
        <Textarea
          rows={3}
          maxLength={MAX_TEXT}
          value={text}
          onChange={(event) => setText(event.target.value)}
          placeholder="Прашај за задача, побарај соработка или подели материјал..."
        />

        {files.length > 0 && (
          <ul className="flex flex-wrap gap-2">
            {files.map((file, index) => (
              <li
                key={`${file.name}-${index}`}
                className="flex items-center gap-2 rounded-xl bg-surface-2 px-2.5 py-1.5 text-xs text-ink-soft"
              >
                <span aria-hidden="true">{file.type.startsWith('image/') ? '🖼️' : '📎'}</span>
                <span className="max-w-[180px] truncate">{file.name}</span>
                <span className="text-ink-mute">{fileSize(file.size)}</span>
                <button
                  type="button"
                  aria-label={`Тргни ${file.name}`}
                  onClick={() => setFiles((current) => current.filter((_, i) => i !== index))}
                  className="text-ink-mute transition hover:text-bad"
                >
                  ×
                </button>
              </li>
            ))}
          </ul>
        )}

        {problem && <Alert>{problem}</Alert>}

        <div className="flex flex-wrap items-center gap-2">
          <input
            ref={fileInput}
            type="file"
            multiple
            accept={ACCEPT}
            className="sr-only"
            onChange={(event) => {
              addFiles(event.target.files)
              event.target.value = ''
            }}
          />

          <Button
            variant="secondary"
            size="sm"
            disabled={sending || files.length >= MAX_FILES}
            onClick={() => fileInput.current?.click()}
          >
            📎 Слика или датотека
          </Button>

          <span className="block w-40 sm:w-52">
            <Select
              aria-label="Предмет"
              className="py-1.5 text-xs"
              value={subjectId}
              onChange={(event) => setSubjectId(event.target.value)}
            >
              <option value="">Без предмет</option>
              {subjects.map((subject) => (
                <option key={subject.id} value={subject.id}>
                  {subject.name}
                </option>
              ))}
            </Select>
          </span>

          <span className="ml-auto flex items-center gap-3">
            {text.length > MAX_TEXT - 200 && (
              <span className="text-xs text-ink-mute">
                {MAX_TEXT - text.length} знаци
              </span>
            )}
            <Button disabled={sending || !text.trim()} onClick={submit}>
              {sending ? 'Се објавува...' : 'Објави'}
            </Button>
          </span>
        </div>
      </div>
    </Card>
  )
}
