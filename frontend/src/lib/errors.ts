import axios from 'axios'
import type { ApiError } from '../types'

const FALLBACK = 'Нешто не е во ред. Пробај повторно.'

export function getErrorMessage(error: unknown, fallback: string = FALLBACK): string {
  if (axios.isAxiosError<ApiError>(error)) {
    const data = error.response?.data

    const fieldErrors = data?.fieldErrors
    if (fieldErrors) {
      const messages = Object.values(fieldErrors).filter(Boolean)
      if (messages.length > 0) {
        return messages.join(' ')
      }
    }

    if (data?.message) {
      return data.message
    }

    // Backend-от не одговара воопшто (изгасен, рестартира, нема мрежа)
    if (!error.response) {
      return 'Нема врска со серверот. Провери дали backend-от работи.'
    }
  }

  if (error instanceof Error && error.message) {
    return error.message
  }

  return fallback
}
