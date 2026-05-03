import { ArrowLeft, Bot, Circle, RefreshCcw, SlidersHorizontal, Sparkles } from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import ChatInput from '../components/ChatInput'
import ChatMessage from '../components/ChatMessage'
import { useChatbot } from '../hooks/useChatbot'
import { Link } from 'react-router-dom'

const STYLE_OPTIONS = ['Minimalist', 'Classic', 'Avant-garde', 'Capsule wardrobe']
const TONE_OPTIONS = ['Professional', 'Friendly', 'Concise', 'Luxury']
const FOCUS_OPTIONS = ['Sustainability', 'Silhouette & Fit', 'Trend-forward', 'Daily Comfort']

export default function ChatbotPage() {
  const {
    messages,
    preferences,
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

  return (
    <div className="mx-auto mt-4 w-full max-w-[1400px] rounded-[26px] border border-[#d8d8d8] bg-[#ececec] p-3">
      <div className="grid min-h-[80vh] grid-cols-1 gap-3 lg:grid-cols-[2.2fr_1fr]">
        <section className="flex min-h-[70vh] flex-col rounded-[22px] bg-[#f3f3f3] p-5">
          <header className="mb-4 flex items-center justify-between">
            <div>
              <p className="text-[11px] font-semibold uppercase tracking-[0.12em] text-[#7d7d7d]">The Digital Curator</p>
              <h1 className="text-[34px] font-bold leading-tight text-[#1c1c1c]">Stylist Concierge</h1>
            </div>

            <div className="flex items-center gap-2">
              <Link
                to="/"
                className="inline-flex items-center gap-2 rounded-lg border border-[#c9c9c9] bg-white px-3 py-2 text-xs font-semibold text-[#333] transition hover:bg-[#f9f9f9]"
              >
                <ArrowLeft size={14} />
                Trang chủ
              </Link>
              <button
                type="button"
                onClick={startNewSession}
                className="inline-flex items-center gap-2 rounded-lg border border-[#c9c9c9] bg-white px-3 py-2 text-xs font-semibold text-[#333] transition hover:bg-[#f9f9f9]"
              >
                <RefreshCcw size={14} />
                New session
              </button>
            </div>
          </header>

          <div className="mb-4 grid gap-3 md:grid-cols-2">
            <div className="rounded-xl border border-[#dfdfdf] bg-white p-4">
              <p className="text-xs font-semibold uppercase tracking-[0.1em] text-[#808080]">System Status</p>
              <p className="mt-2 text-2xl font-bold leading-tight text-[#232323]">
                Your wardrobe is being curated by artificial intelligence with a bespoke human touch.
              </p>
            </div>

            <div className="rounded-xl border border-[#dfdfdf] bg-white p-4">
              <p className="text-xs font-semibold uppercase tracking-[0.1em] text-[#808080]">Stylist Insight</p>
              <p className="mt-2 text-sm leading-6 text-[#3b3b3b]">
                Mình đang ưu tiên các gợi ý dựa trên size, phong cách và lịch sử tương tác của bạn để tăng độ cá nhân hóa.
              </p>
            </div>
          </div>

          {lastBotSuggestions.length > 0 && (
            <div className="mb-3 rounded-xl border border-[#dcdcdc] bg-white p-3">
              <p className="mb-2 text-xs font-semibold uppercase tracking-[0.08em] text-[#757575]">Latest Curated Picks</p>
              <div className="grid gap-2 md:grid-cols-2 xl:grid-cols-3">
                {lastBotSuggestions.slice(0, 3).map((item, idx) => (
                  <Link
                    key={`${item.productId || 'pick'}-${idx}`}
                    to={item.link || '#'}
                    className="rounded-lg border border-[#e6e6e6] bg-[#fafafa] p-2 transition hover:border-[#cacaca]"
                  >
                    {item.imageUrl ? (
                      <img src={item.imageUrl} alt={item.name} className="h-36 w-full rounded-md object-cover" loading="lazy" />
                    ) : (
                      <div className="flex h-36 items-center justify-center rounded-md bg-[#f0f0f0] text-xs text-[#8a8a8a]">No image</div>
                    )}
                    <p className="mt-2 text-sm font-semibold text-[#232323]">{item.name}</p>
                    <p className="text-xs text-[#727272]">{item.price}</p>
                  </Link>
                ))}
              </div>
            </div>
          )}

          <div ref={scrollRef} className="flex-1 space-y-3 overflow-y-auto rounded-xl border border-[#dddddd] bg-white p-3">
            {messages.map((message) => (
              <ChatMessage key={message.id} message={message} />
            ))}

            {error && (
              <div className="rounded-lg border border-[#ffdada] bg-[#fff5f5] px-3 py-2 text-xs text-[#994444]">
                {error}
              </div>
            )}

            {(isHydrating || isSending) && (
              <div className="inline-flex items-center gap-2 rounded-full bg-[#f3f3f3] px-3 py-1 text-xs text-[#666]">
                <Circle size={8} className="fill-current" />
                AI đang xử lý...
              </div>
            )}
          </div>

          <div className="mt-3">
            <ChatInput
              onSend={sendMessage}
              disabled={isSending}
              placeholder="Specify your aesthetic preference..."
            />
          </div>
        </section>

        <aside className="rounded-[22px] border border-[#d9d9d9] bg-[#efefef] p-5">
          <h2 className="text-[34px] font-bold leading-tight text-[#1f1f1f]">Personalization</h2>
          <p className="mt-1 text-xs font-semibold uppercase tracking-[0.12em] text-[#888]">Tailor your concierge</p>

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
                    className={`w-full rounded-md border px-3 py-2 text-left text-sm transition ${
                      draftPreferences.style === style
                        ? 'border-[#171717] bg-white text-[#171717]'
                        : 'border-[#d7d7d7] bg-[#f8f8f8] text-[#4a4a4a] hover:bg-white'
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
                    className={`w-full rounded-md border px-3 py-2 text-left text-sm transition ${
                      draftPreferences.tone === tone
                        ? 'border-[#171717] bg-white text-[#171717]'
                        : 'border-[#d7d7d7] bg-[#f8f8f8] text-[#4a4a4a] hover:bg-white'
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
                      className={`w-full rounded-md border px-3 py-2 text-left text-sm transition ${
                        active
                          ? 'border-[#171717] bg-white text-[#171717]'
                          : 'border-[#d7d7d7] bg-[#f8f8f8] text-[#4a4a4a] hover:bg-white'
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
                className="w-full rounded-md border border-[#d7d7d7] bg-white px-3 py-2 text-sm outline-none transition focus:border-[#171717]"
              />
            </div>

            <button
              type="button"
              onClick={applyPreferences}
              className="w-full rounded-md bg-[#121212] py-3 text-sm font-semibold uppercase tracking-[0.08em] text-white transition hover:bg-black"
            >
              Apply settings
            </button>
          </div>
        </aside>
      </div>
    </div>
  )
}
