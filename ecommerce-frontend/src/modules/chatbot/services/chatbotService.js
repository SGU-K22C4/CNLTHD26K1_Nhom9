import api from '@/shared/utils/api'
import { buildUserHeaders } from '@/shared/utils/userHeaders'

const CHATBOT_SESSION_KEY = 'chatbot_session_id'
const CHATBOT_TIMEOUT_MS = 25000

function getStoredSessionId() {
  return localStorage.getItem(CHATBOT_SESSION_KEY)
}

function saveSessionId(sessionId) {
  if (sessionId) {
    localStorage.setItem(CHATBOT_SESSION_KEY, sessionId)
  }
}

function createSessionId() {
  const sessionId = crypto.randomUUID()
  saveSessionId(sessionId)
  return sessionId
}

export const chatbotService = {
  getOrCreateSessionId() {
    return getStoredSessionId() || createSessionId()
  },

  async send({ message, preferences }) {
    const sessionId = this.getOrCreateSessionId()
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
      saveSessionId(response.sessionId)
    }

    return response
  },

  async getSession(sessionId = null) {
    const activeSessionId = sessionId || getStoredSessionId()
    if (!activeSessionId) return null

    return api.get(`/api/v1/chatbot/sessions/${activeSessionId}`, {
      headers: buildUserHeaders(),
      timeout: CHATBOT_TIMEOUT_MS,
    })
  },

  resetSession() {
    localStorage.removeItem(CHATBOT_SESSION_KEY)
  },
}