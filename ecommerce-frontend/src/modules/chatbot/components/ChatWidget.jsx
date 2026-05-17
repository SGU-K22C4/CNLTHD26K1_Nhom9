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

  const visibleMessages = useMemo(() => messages.slice(-14), [messages])

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
        <div className="mb-3 w-[380px] max-w-[calc(100vw-24px)] overflow-hidden rounded-[28px] border border-[#e4d8ca] bg-[#fbf7f1] shadow-[0_24px_80px_rgba(24,18,15,0.18)]">
          <div className="flex items-center justify-between border-b border-[#eadfce] bg-[#fffaf4] px-5 py-4">
            <div>
              <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-[#8c7a67]">The Digital Curator</p>
              <p className="text-lg font-semibold text-[#201a15]">Stylist Concierge</p>
            </div>

            <div className="flex items-center gap-1">
              {isLoggedIn && (
                <button
                  type="button"
                  onClick={() => window.open('/chatbot', '_blank', 'noopener,noreferrer')}
                  className="rounded-full p-2 text-[#5a4d42] transition hover:bg-[#f3eadf]"
                  aria-label="Mở tab chatbot"
                >
                  <ExternalLink size={15} />
                </button>
              )}

              <button
                type="button"
                onClick={() => setOpen(false)}
                className="rounded-full p-2 text-[#5a4d42] transition hover:bg-[#f3eadf]"
                aria-label="Thu gọn chatbot"
              >
                <Minus size={15} />
              </button>
            </div>
          </div>

          <div
            ref={messageListRef}
            className="max-h-[520px] space-y-4 overflow-y-auto px-4 py-4"
          >
            {!isLoggedIn && (
              <div className="rounded-2xl border border-[#e4dbce] bg-white px-4 py-3 text-xs leading-6 text-[#5d5045]">
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
              <div className="rounded-2xl border border-[#ffd2d2] bg-[#fff4f4] px-3 py-2 text-xs text-[#8f3b3b]">
                {error}
              </div>
            )}
          </div>

          <div className="border-t border-[#eadfce] bg-[#fffaf4] p-4">
            <ChatInput
              onSend={sendMessage}
              disabled={isSending}
              compact
              placeholder={isSending ? 'Đang xử lý...' : 'Nhập nhu cầu mua sắm...'}
            />
          </div>
        </div>
      )}

      <button
        type="button"
        onClick={() => setOpen((prev) => !prev)}
        className="flex items-center gap-2 rounded-full border border-[#1b1613] bg-[#17120f] px-4 py-3 text-white shadow-xl transition hover:bg-[#090909]"
      >
        <MessageCircle size={18} />
        <span className="text-xs font-semibold uppercase tracking-[0.12em]">AI Stylist</span>
        <Sparkles size={14} className="text-[#d7e7ff]" />
      </button>
    </div>
  )
}
