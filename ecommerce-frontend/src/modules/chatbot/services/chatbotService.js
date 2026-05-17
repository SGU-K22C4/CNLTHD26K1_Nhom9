import api from '@/shared/utils/api'
import { buildUserHeaders } from '@/shared/utils/userHeaders'
import { generateUUID } from '@/shared/utils/uuid'

const CHATBOT_SESSION_PREFIX = 'chatbot_session_'
const CHATBOT_ATTRIBUTION_KEY = 'chatbot_product_attribution'
const CHATBOT_CHECKOUT_ATTRIBUTION_KEY = 'chatbot_checkout_attribution'
const parsedTimeout = Number(import.meta.env.VITE_CHATBOT_TIMEOUT_MS)
const CHATBOT_TIMEOUT_MS = Number.isFinite(parsedTimeout) && parsedTimeout > 0
  ? parsedTimeout
  : 180000

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

function readAttributionMap() {
  try {
    const raw = sessionStorage.getItem(CHATBOT_ATTRIBUTION_KEY)
    return raw ? JSON.parse(raw) : {}
  } catch {
    return {}
  }
}

function writeAttributionMap(map) {
  try {
    sessionStorage.setItem(CHATBOT_ATTRIBUTION_KEY, JSON.stringify(map || {}))
  } catch {
    // ignore storage failures on tracking-only data
  }
}

function readCheckoutAttributionMap() {
  try {
    const raw = sessionStorage.getItem(CHATBOT_CHECKOUT_ATTRIBUTION_KEY)
    return raw ? JSON.parse(raw) : {}
  } catch {
    return {}
  }
}

function writeCheckoutAttributionMap(map) {
  try {
    sessionStorage.setItem(CHATBOT_CHECKOUT_ATTRIBUTION_KEY, JSON.stringify(map || {}))
  } catch {
    // ignore storage failures on tracking-only data
  }
}

export const chatbotService = {
  _currentUserId: null,

  getOrCreateSessionId(userId = null) {
    const resolvedUserId = userId ?? this._currentUserId
    this._currentUserId = resolvedUserId
    return getStoredSessionId(resolvedUserId) || createSessionId(resolvedUserId)
  },

  async send({ message, preferences, selectedProductContext = null }) {
    const sessionId = this.getOrCreateSessionId(this._currentUserId)
    const payload = {
      message,
      sessionId,
      preferences,
      selectedProductContext,
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

  async sendFeedbackEvent(payload) {
    if (!payload?.sessionId || !payload?.eventType) {
      return null
    }

    return api.post('/api/v1/chatbot/analytics/events', payload, {
      headers: buildUserHeaders(),
      timeout: CHATBOT_TIMEOUT_MS,
    })
  },

  saveProductAttribution(productId, attribution) {
    if (!productId || !attribution) return
    const map = readAttributionMap()
    map[String(productId)] = {
      ...attribution,
      savedAt: new Date().toISOString(),
    }
    writeAttributionMap(map)
  },

  getProductAttribution(productId) {
    if (!productId) return null
    const map = readAttributionMap()
    return map[String(productId)] || null
  },

  clearProductAttribution(productId) {
    if (!productId) return
    const map = readAttributionMap()
    delete map[String(productId)]
    writeAttributionMap(map)
  },

  saveCheckoutAttribution(productId, attribution) {
    if (!productId || !attribution) return
    const map = readCheckoutAttributionMap()
    map[String(productId)] = {
      ...attribution,
      savedAt: new Date().toISOString(),
    }
    writeCheckoutAttributionMap(map)
  },

  getCheckoutAttributions() {
    return Object.values(readCheckoutAttributionMap())
  },

  clearCheckoutAttributions() {
    writeCheckoutAttributionMap({})
  },

  resetSession(userId = null) {
    const resolvedUserId = userId ?? this._currentUserId
    localStorage.removeItem(getSessionKey(resolvedUserId))
  },
}
