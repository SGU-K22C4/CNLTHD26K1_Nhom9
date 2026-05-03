import { ExternalLink, MessageCircle, Minus, Sparkles } from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { useLocation } from 'react-router-dom'
import { useAuth } from '@/modules/auth/hooks/useAuth'
import ChatInput from './ChatInput'
import ChatMessage from './ChatMessage'
import { useChatbot } from '../hooks/useChatbot'

export default function ChatWidget() {
  const { user } = useAuth()
  const location = useLocation()
  const isChatbotPage = location.pathname.startsWith('/chatbot')
  const isLoggedIn = Boolean(user)
  const [open, setOpen] = useState(false)
  const messageListRef = useRef(null)

  const {
    messages,
    isSending,
    sendMessage,
    error,
  } = useChatbot({ autoLoadHistory: true })

  const visibleMessages = useMemo(() => messages.slice(-20), [messages])

  useEffect(() => {
    if (!messageListRef.current || !open) return
    messageListRef.current.scrollTop = messageListRef.current.scrollHeight
  }, [visibleMessages, open])

  if (isChatbotPage) {
    return null
  }

  return (
    <div className="fixed bottom-5 right-5 z-40">
      {open && (
        <div className="mb-3 w-[360px] overflow-hidden rounded-2xl border border-[#dddddd] bg-[#f6f6f6] shadow-2xl">
          <div className="flex items-center justify-between border-b border-[#e2e2e2] bg-white px-4 py-3">
            <div>
              <p className="text-[11px] font-semibold uppercase tracking-[0.12em] text-[#6e6e6e]">The Digital Curator</p>
              <p className="text-sm font-semibold text-[#1f1f1f]">Stylist Concierge</p>
            </div>

            <div className="flex items-center gap-1">
              {isLoggedIn && (
                <button
                  type="button"
                  onClick={() => window.open('/chatbot', '_blank', 'noopener,noreferrer')}
                  className="rounded-md p-1.5 text-[#505050] transition hover:bg-[#f1f1f1]"
                  aria-label="Mở tab chatbot"
                >
                  <ExternalLink size={15} />
                </button>
              )}

              <button
                type="button"
                onClick={() => setOpen(false)}
                className="rounded-md p-1.5 text-[#505050] transition hover:bg-[#f1f1f1]"
                aria-label="Thu gọn chatbot"
              >
                <Minus size={15} />
              </button>
            </div>
          </div>

          <div
            ref={messageListRef}
            className="max-h-[420px] space-y-3 overflow-y-auto px-3 py-3"
          >
            {!isLoggedIn && (
              <div className="rounded-lg border border-[#d9ddff] bg-[#f4f6ff] px-3 py-2 text-xs text-[#38408f]">
                Bạn đang dùng chế độ chat nhanh. Muốn mở trang tùy chỉnh AI theo tone, style và focus, vui lòng
                {' '}
                <button
                  type="button"
                  onClick={() => { window.location.href = '/login' }}
                  className="font-semibold underline"
                >
                  đăng nhập tài khoản
                </button>
                .
              </div>
            )}

            {visibleMessages.map((message) => (
              <ChatMessage key={message.id} message={message} />
            ))}

            {error && (
              <div className="rounded-lg border border-[#ffd2d2] bg-[#fff4f4] px-3 py-2 text-xs text-[#8f3b3b]">
                {error}
              </div>
            )}
          </div>

          <div className="border-t border-[#e2e2e2] bg-white p-3">
            <ChatInput
              onSend={sendMessage}
              disabled={isSending}
              compact
              placeholder={isSending ? 'Đang xử lý...' : 'Nhập câu hỏi mua sắm...'}
            />
          </div>
        </div>
      )}

      <button
        type="button"
        onClick={() => setOpen((prev) => !prev)}
        className="flex items-center gap-2 rounded-full border border-[#171717] bg-[#151515] px-4 py-3 text-white shadow-xl transition hover:bg-black"
      >
        <MessageCircle size={18} />
        <span className="text-xs font-semibold uppercase tracking-[0.12em]">AI Stylist</span>
        <Sparkles size={14} className="text-[#d7e7ff]" />
      </button>
    </div>
  )
}