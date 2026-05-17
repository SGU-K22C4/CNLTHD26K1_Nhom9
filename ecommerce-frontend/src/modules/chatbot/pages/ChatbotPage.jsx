import { ArrowLeft, Bot, Circle, RefreshCcw, SlidersHorizontal, Sparkles } from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import ChatInput from '../components/ChatInput'
import ChatMessage from '../components/ChatMessage'
import { useChatbot } from '../hooks/useChatbot'
import { Link } from 'react-router-dom'
import { chatbotService } from '../services/chatbotService'

const STYLE_OPTIONS = ['Minimalist', 'Classic', 'Avant-garde', 'Capsule wardrobe']
const TONE_OPTIONS = ['Professional', 'Friendly', 'Concise', 'Luxury']
const FOCUS_OPTIONS = ['Sustainability', 'Silhouette & Fit', 'Trend-forward', 'Daily Comfort']

export default function ChatbotPage() {
  const {
    sessionId,
    messages,
    preferences,
    setSelectedProductContext,
    updatePreferences,
    sendMessage,
    startNewSession,
    isSending,
    isHydrating,
    error,
  } = useChatbot({ autoLoadHistory: true })

  const [draftPreferences, setDraftPreferences] = useState(preferences)
  const scrollRef = useRef(null)

  useEffect(() => {
    setDraftPreferences(preferences)
  }, [preferences])

  useEffect(() => {
    if (!scrollRef.current) return
    scrollRef.current.scrollTop = scrollRef.current.scrollHeight
  }, [messages])

  const lastBotSuggestions = useMemo(() => {
    const reverse = [...messages].reverse()
    const latest = reverse.find((item) => item.sender === 'BOT' && Array.isArray(item.suggestions) && item.suggestions.length > 0)
    return latest?.suggestions || []
  }, [messages])

  const latestSuggestionMessage = useMemo(() => {
    const reverse = [...messages].reverse()
    return reverse.find((item) => item.sender === 'BOT' && Array.isArray(item.suggestions) && item.suggestions.length > 0) || null
  }, [messages])

  const applyPreferences = () => {
    updatePreferences(draftPreferences)
  }

  const toggleFocus = (focusItem) => {
    setDraftPreferences((prev) => {
      const focus = Array.isArray(prev.focus) ? prev.focus : []
      const exists = focus.includes(focusItem)
      return {
        ...prev,
        focus: exists ? focus.filter((item) => item !== focusItem) : [...focus, focusItem],
      }
    })
  }

  const trackProductClick = async ({ item, sourceMessageId, surface, position }) => {
    try {
      const attribution = {
        source: 'chatbot',
        sessionId,
        sourceMessageId,
        productId: item?.productId || '',
        productName: item?.name || '',
        surface,
        position,
      }
      setSelectedProductContext({
        productId: item?.productId || '',
        productName: item?.name || '',
        category: item?.category || '',
        categoryGender: item?.categoryGender || '',
        price: item?.price || '',
        link: item?.link || '',
        sourceMessageId,
      })
      chatbotService.saveProductAttribution(item?.productId, attribution)

      await chatbotService.sendFeedbackEvent({
        sessionId,
        eventType: 'product_click',
        sourceMessageId,
        productId: item?.productId || '',
        productName: item?.name || '',
        metadata: {
          surface,
          position,
          link: item?.link || '',
          category: item?.category || '',
          price: item?.price || '',
        },
      })
    } catch (trackingError) {
      console.debug('trackProductClick failed', trackingError)
    }
  }

  return (
    <div className="mx-auto mt-4 w-full max-w-[1440px] px-3 pb-8">
      <div className="grid min-h-[80vh] grid-cols-1 gap-4 lg:grid-cols-[minmax(0,1.7fr)_360px]">
        <section className="flex min-h-[78vh] flex-col rounded-[28px] border border-[#e4d8ca] bg-[linear-gradient(180deg,#f8f3ec_0%,#f4eee6_100%)] p-5 shadow-[0_18px_50px_rgba(28,21,17,0.08)]">
          <header className="mb-4 flex items-center justify-between">
            <div>
              <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-[#8d7c67]">The Digital Curator</p>
              <h1 className="text-[34px] font-bold leading-tight text-[#1c1a17]">Stylist Concierge</h1>
            </div>

            <div className="flex items-center gap-2">
              <Link
                to="/"
                className="inline-flex items-center gap-2 rounded-full border border-[#d5c7b8] bg-[#fffaf4] px-4 py-2 text-xs font-semibold uppercase tracking-[0.08em] text-[#3b3128] transition hover:bg-white"
              >
                <ArrowLeft size={14} />
                Trang chủ
              </Link>
              <button
                type="button"
                onClick={startNewSession}
                className="inline-flex items-center gap-2 rounded-full border border-[#1d1713] bg-[#1b1511] px-4 py-2 text-xs font-semibold uppercase tracking-[0.08em] text-white transition hover:bg-black"
              >
                <RefreshCcw size={14} />
                New session
              </button>
            </div>
          </header>

          <div className="mb-4 grid gap-3 xl:grid-cols-[1.3fr_0.9fr]">
            <div className="rounded-[22px] border border-[#e3d9cc] bg-[#fffaf4] p-5">
              <p className="text-xs font-semibold uppercase tracking-[0.12em] text-[#8b7d6f]">Session Status</p>
              <p className="mt-2 text-2xl font-bold leading-tight text-[#231d17]">
                Tư vấn mua sắm theo ngữ cảnh, ưu tiên các mẫu dễ chốt và phù hợp với nhu cầu hiện tại.
              </p>
            </div>

            <div className="rounded-[22px] border border-[#e3d9cc] bg-[#fffaf4] p-5">
              <p className="text-xs font-semibold uppercase tracking-[0.12em] text-[#8b7d6f]">Stylist Insight</p>
              <p className="mt-2 text-sm leading-7 text-[#4a3f35]">
                Mình đang ưu tiên các gợi ý dựa trên size, phong cách và lịch sử tương tác để tăng độ cá nhân hóa và khả năng chốt nhanh.
              </p>
            </div>
          </div>

          {lastBotSuggestions.length > 0 && (
            <div className="mb-4 rounded-[24px] border border-[#e3d9cc] bg-[#fffaf4] p-4">
              <p className="mb-3 text-xs font-semibold uppercase tracking-[0.12em] text-[#877664]">Latest Curated Picks</p>
              <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
                {lastBotSuggestions.slice(0, 3).map((item, idx) => (
                  <Link
                    key={`${item.productId || 'pick'}-${idx}`}
                    to={item.link || '#'}
                    state={{
                      chatbotAttribution: {
                        source: 'chatbot',
                        sessionId,
                        sourceMessageId: latestSuggestionMessage?.id,
                        productId: item?.productId || '',
                        productName: item?.name || '',
                        surface: 'latest_curated_picks',
                        position: idx,
                      },
                    }}
                    onClick={() =>
                      trackProductClick({
                        item,
                        sourceMessageId: latestSuggestionMessage?.id,
                        surface: 'latest_curated_picks',
                        position: idx,
                      })
                    }
                    className="rounded-2xl border border-[#eadfce] bg-white p-3 transition hover:-translate-y-0.5 hover:border-[#c8b49d]"
                  >
                    {item.imageUrl ? (
                      <img src={item.imageUrl} alt={item.name} className="h-44 w-full rounded-xl object-cover" loading="lazy" />
                    ) : (
                      <div className="flex h-44 items-center justify-center rounded-xl bg-[#f4efe7] text-xs text-[#8a7a67]">No image</div>
                    )}
                    <p className="mt-3 line-clamp-2 text-sm font-semibold uppercase leading-5 tracking-[0.04em] text-[#221c16]">{item.name}</p>
                    <p className="mt-1 text-xs text-[#7b6e61]">{item.price}</p>
                  </Link>
                ))}
              </div>
            </div>
          )}

          <div ref={scrollRef} className="flex-1 space-y-4 overflow-y-auto rounded-[24px] border border-[#e3d9cc] bg-[#fffdf9] p-4">
            {messages.map((message) => (
              <ChatMessage
                key={message.id}
                message={{ ...message, sessionId }}
                onProductClick={trackProductClick}
              />
            ))}

            {error && (
              <div className="rounded-2xl border border-[#ffdada] bg-[#fff5f5] px-3 py-2 text-xs text-[#994444]">
                {error}
              </div>
            )}

            {(isHydrating || isSending) && (
              <div className="inline-flex items-center gap-2 rounded-full bg-[#f3ece3] px-3 py-1 text-xs text-[#6a5c4d]">
                <Circle size={8} className="fill-current" />
                AI đang xử lý...
              </div>
            )}
          </div>

          <div className="mt-4">
            <ChatInput
              onSend={sendMessage}
              disabled={isSending}
              placeholder="Nhắn nhu cầu mua sắm, ví dụ: áo đen đi làm dưới 700k"
            />
          </div>
        </section>

        <aside className="rounded-[28px] border border-[#e4d8ca] bg-[#f7f1e8] p-5 shadow-[0_18px_50px_rgba(28,21,17,0.06)]">
          <h2 className="text-[34px] font-bold leading-tight text-[#1f1a15]">Personalization</h2>
          <p className="mt-1 text-xs font-semibold uppercase tracking-[0.14em] text-[#8d7c67]">Tailor your concierge</p>

          <div className="mt-6 space-y-5">
            <div>
              <p className="mb-2 inline-flex items-center gap-2 text-xs font-semibold uppercase tracking-[0.1em] text-[#727272]">
                <Bot size={14} /> Style
              </p>
              <div className="grid gap-2">
                {STYLE_OPTIONS.map((style) => (
                  <button
                    key={style}
                    type="button"
                    onClick={() => setDraftPreferences((prev) => ({ ...prev, style }))}
                    className={`w-full rounded-2xl border px-3 py-3 text-left text-sm transition ${
                      draftPreferences.style === style
                        ? 'border-[#1c1713] bg-white text-[#171717]'
                        : 'border-[#dccfbe] bg-[#faf6f0] text-[#4a4a4a] hover:bg-white'
                    }`}
                  >
                    {style}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <p className="mb-2 inline-flex items-center gap-2 text-xs font-semibold uppercase tracking-[0.1em] text-[#727272]">
                <SlidersHorizontal size={14} /> Tone
              </p>
              <div className="grid gap-2">
                {TONE_OPTIONS.map((tone) => (
                  <button
                    key={tone}
                    type="button"
                    onClick={() => setDraftPreferences((prev) => ({ ...prev, tone }))}
                    className={`w-full rounded-2xl border px-3 py-3 text-left text-sm transition ${
                      draftPreferences.tone === tone
                        ? 'border-[#1c1713] bg-white text-[#171717]'
                        : 'border-[#dccfbe] bg-[#faf6f0] text-[#4a4a4a] hover:bg-white'
                    }`}
                  >
                    {tone}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <p className="mb-2 inline-flex items-center gap-2 text-xs font-semibold uppercase tracking-[0.1em] text-[#727272]">
                <Sparkles size={14} /> Focus
              </p>
              <div className="grid gap-2">
                {FOCUS_OPTIONS.map((focus) => {
                  const active = Array.isArray(draftPreferences.focus) && draftPreferences.focus.includes(focus)
                  return (
                    <button
                      key={focus}
                      type="button"
                      onClick={() => toggleFocus(focus)}
                      className={`w-full rounded-2xl border px-3 py-3 text-left text-sm transition ${
                        active
                          ? 'border-[#1c1713] bg-white text-[#171717]'
                          : 'border-[#dccfbe] bg-[#faf6f0] text-[#4a4a4a] hover:bg-white'
                      }`}
                    >
                      {focus}
                    </button>
                  )
                })}
              </div>
            </div>

            <div>
              <label htmlFor="budget" className="mb-2 block text-xs font-semibold uppercase tracking-[0.1em] text-[#727272]">Budget</label>
              <input
                id="budget"
                value={draftPreferences.budget || ''}
                onChange={(event) => setDraftPreferences((prev) => ({ ...prev, budget: event.target.value }))}
                placeholder="Ví dụ: dưới 1.500.000đ"
                className="w-full rounded-2xl border border-[#dccfbe] bg-white px-3 py-3 text-sm outline-none transition focus:border-[#171717]"
              />
            </div>

            <button
              type="button"
              onClick={applyPreferences}
              className="w-full rounded-full bg-[#17120f] py-3 text-sm font-semibold uppercase tracking-[0.08em] text-white transition hover:bg-black"
            >
              Apply settings
            </button>
          </div>
        </aside>
      </div>
    </div>
  )
}
