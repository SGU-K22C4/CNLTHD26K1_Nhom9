import api from '@/shared/utils/api'
import { buildUserHeaders } from '@/shared/utils/userHeaders'
import { generateUUID } from '@/shared/utils/uuid'

const CHATBOT_SESSION_PREFIX = 'chatbot_session_'
const CHATBOT_TIMEOUT_MS = 25000

function getSessionKey(userId) {
  return userId ? `${CHATBOT_SESSION_PREFIX}${userId}` : `${CHATBOT_SESSION_PREFIX}guest`
}

function getStoredSessionId(userId) {
  return localStorage.getItem(getSessionKey(userId))
}

function saveSessionId(sessionId, userId) {
  if (sessionId) {
    localStorage.setItem(getSessionKey(userId), sessionId)
  }
}

function createSessionId(userId) {
  const sessionId = generateUUID()
  saveSessionId(sessionId, userId)
  return sessionId
}

export const chatbotService = {
  _currentUserId: null,

  getOrCreateSessionId(userId = null) {
    this._currentUserId = userId
    return getStoredSessionId(userId) || createSessionId(userId)
  },

  async send({ message, preferences }) {
    const sessionId = this.getOrCreateSessionId(this._currentUserId)
    const payload = {
      message,
      sessionId,
      preferences,
    }

    const response = await api.post('/api/v1/chatbot/chat', payload, {
      headers: buildUserHeaders(),
      timeout: CHATBOT_TIMEOUT_MS,
    })

    if (response?.sessionId) {
      saveSessionId(response.sessionId, this._currentUserId)
    }

    return response
  },

  async getSession(sessionId = null) {
    const activeSessionId = sessionId || getStoredSessionId(this._currentUserId)
    if (!activeSessionId) return null

    return api.get(`/api/v1/chatbot/sessions/${activeSessionId}`, {
      headers: buildUserHeaders(),
      timeout: CHATBOT_TIMEOUT_MS,
    })
  },

  resetSession() {
    localStorage.removeItem(getSessionKey(this._currentUserId))
  },
}