import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { chatbotService } from '../services/chatbotService'
import { useAuth } from '@/modules/auth/hooks/useAuth'
import { generateUUID } from '@/shared/utils/uuid'

const DEFAULT_GREETING = {
  id: 'welcome-message',
  sender: 'BOT',
  text: 'Xin chào, mình là Stylist Concierge. Mình có thể tư vấn size, gợi ý outfit theo mùa/xu hướng và kiểm tra khuyến mãi đang diễn ra.',
  createdAt: new Date().toISOString(),
}

const DEFAULT_PREFERENCES = {
  tone: 'Friendly',
  style: 'Minimalist',
  focus: ['Silhouette & Fit'],
  budget: '',
}

function normalizeMessage(message) {
  return {
    id: message?.messageId || generateUUID(),
    sender: message?.sender || 'BOT',
    text: message?.content || message?.text || '',
    intent: message?.intent || null,
    suggestions: message?.suggestions || [],
    promotions: message?.promotions || [],
    missingFields: message?.missingFields || [],
    createdAt: message?.createdAt || new Date().toISOString(),
  }
}

export function useChatbot({ autoLoadHistory = true } = {}) {
  const { user } = useAuth()
  const userId = user?.id || user?.email || null
  const prevUserIdRef = useRef(userId)

  const [messages, setMessages] = useState([DEFAULT_GREETING])
  const [preferences, setPreferences] = useState(DEFAULT_PREFERENCES)
  const [sessionId, setSessionId] = useState(() => chatbotService.getOrCreateSessionId(userId))
  const [isSending, setIsSending] = useState(false)
  const [isHydrating, setIsHydrating] = useState(false)
  const [error, setError] = useState('')

  // === Auth state change detection ===
  // When user logs in or out, reset chatbot session to prevent cross-contamination
  useEffect(() => {
    if (prevUserIdRef.current !== userId) {
      prevUserIdRef.current = userId
      chatbotService.resetSession()
      const newSessionId = chatbotService.getOrCreateSessionId(userId)
      setSessionId(newSessionId)
      setMessages([DEFAULT_GREETING])
      setError('')
    }
  }, [userId])

  const hydrateSession = useCallback(async () => {
    if (!autoLoadHistory) return

    setIsHydrating(true)
    setError('')

    try {
      const session = await chatbotService.getSession(sessionId)
      if (!session) return

      const history = Array.isArray(session.messages)
        ? session.messages.map(normalizeMessage)
        : []

      if (history.length > 0) {
        setMessages(history)
      }

      if (session.profile) {
        setPreferences((prev) => ({
          ...prev,
          tone: session.profile.preferredTone || prev.tone,
          style: session.profile.style || prev.style,
          focus: Array.isArray(session.profile.focusTags) && session.profile.focusTags.length
            ? session.profile.focusTags
            : prev.focus,
          budget: session.profile.budget || prev.budget,
        }))
      }
    } catch (err) {
      if (err?.message === 'Không tìm thấy phiên chat' || err?.response?.status === 404) {
        // Đây là session mới, server chưa có data => bỏ qua lỗi chứ không hiện đỏ UI
        setError('')
        return
      }
      setError(err?.message || 'Không thể tải lịch sử chatbot')
    } finally {
      setIsHydrating(false)
    }
  }, [autoLoadHistory, sessionId])

  useEffect(() => {
    hydrateSession()
  }, [hydrateSession])

  const sendMessage = useCallback(async (rawMessage) => {
    const message = String(rawMessage || '').trim()
    if (!message || isSending) return null

    const userMessage = normalizeMessage({
      sender: 'USER',
      content: message,
      createdAt: new Date().toISOString(),
    })

    setMessages((prev) => [...prev, userMessage])
    setIsSending(true)
    setError('')

    try {
      const response = await chatbotService.send({
        message,
        preferences,
      })

      if (response?.sessionId) {
        setSessionId(response.sessionId)
      }

      if (response?.profile) {
        setPreferences((prev) => ({
          ...prev,
          tone: response.profile.preferredTone || prev.tone,
          style: response.profile.style || prev.style,
          focus: Array.isArray(response.profile.focusTags) && response.profile.focusTags.length
            ? response.profile.focusTags
            : prev.focus,
          budget: response.profile.budget || prev.budget,
        }))
      }

      const botMessage = normalizeMessage({
        sender: 'BOT',
        content: response?.reply || 'Mình chưa thể phản hồi lúc này, bạn thử lại giúp mình nhé.',
        intent: response?.intent ? { intentName: response.intent, confidence: response.confidence } : null,
        suggestions: response?.suggestions || [],
        promotions: response?.promotions || [],
        missingFields: response?.missingFields || [],
        createdAt: response?.createdAt || new Date().toISOString(),
      })

      setMessages((prev) => [...prev, botMessage])
      return response
    } catch (err) {
      const fallback = normalizeMessage({
        sender: 'BOT',
        content: 'Xin lỗi, mình chưa xử lý được yêu cầu này. Bạn thử lại sau ít phút nhé.',
      })
      setMessages((prev) => [...prev, fallback])
      setError(err?.message || 'Gửi tin nhắn thất bại')
      return null
    } finally {
      setIsSending(false)
    }
  }, [isSending, preferences])

  const updatePreferences = useCallback((nextPreferences) => {
    setPreferences((prev) => ({
      ...prev,
      ...nextPreferences,
      focus: Array.isArray(nextPreferences?.focus)
        ? nextPreferences.focus
        : prev.focus,
    }))
  }, [])

  const startNewSession = useCallback(() => {
    chatbotService.resetSession()
    const nextSession = chatbotService.getOrCreateSessionId(userId)
    setSessionId(nextSession)
    setMessages([DEFAULT_GREETING])
    setError('')
  }, [userId])

  const canSend = useMemo(() => !isSending, [isSending])

  return {
    sessionId,
    messages,
    preferences,
    isSending,
    isHydrating,
    canSend,
    error,
    sendMessage,
    updatePreferences,
    startNewSession,
  }
}