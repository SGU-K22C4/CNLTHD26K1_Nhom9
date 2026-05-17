import { Link } from 'react-router-dom'

function intentLabel(intentName) {
  switch (intentName) {
    case 'CONSULT_SIZE':
      return 'Tư vấn size'
    case 'CONSULT_SEASON':
      return 'Tư vấn outfit'
    case 'ASK_PROMOTION':
      return 'Khuyến mãi'
    case 'ASK_POLICY':
      return 'Chính sách'
    case 'SEARCH_PRODUCT':
      return 'Gợi ý sản phẩm'
    default:
      return 'Stylist Reply'
  }
}

function formatBotText(message) {
  const raw = String(message?.text || '').trim()
  if (!raw) return ''

  const hasSuggestions = Array.isArray(message?.suggestions) && message.suggestions.length > 0
  const isCompareIntent = message?.intent?.intentName === 'COMPARE_PRODUCT'

  if (!hasSuggestions || isCompareIntent) {
    return raw
  }

  const compact = raw
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .find((line) => !line.startsWith('- '))

  if (!compact) {
    return 'Mình đã chọn sẵn vài mẫu phù hợp cho bạn ở bên dưới.'
  }

  return compact
}

function ProductCard({ item, onClick, navigationState }) {
  return (
    <Link
      to={item.link || '#'}
      onClick={() => onClick?.(item)}
      state={navigationState}
      className="group block min-w-[182px] max-w-[182px] rounded-2xl border border-[#e7ddd0] bg-[#fffdf8] p-2.5 transition hover:-translate-y-0.5 hover:border-[#c9b59d]"
    >
      {item.imageUrl ? (
        <img
          src={item.imageUrl}
          alt={item.name}
          className="h-44 w-full rounded-xl object-cover"
          loading="lazy"
        />
      ) : (
        <div className="flex h-44 w-full items-center justify-center rounded-xl bg-[#f4ede4] text-xs text-[#8f8170]">
          No image
        </div>
      )}

      <div className="mt-3 space-y-1">
        <p className="line-clamp-2 text-[12px] font-semibold uppercase leading-5 tracking-[0.05em] text-[#1f1a14]">
          {item.name}
        </p>
        <p className="text-[11px] uppercase tracking-[0.12em] text-[#8a7a67]">
          {item.category || 'Fashion Pick'}
        </p>
        <p className="text-sm font-semibold text-[#221c16]">{item.price}</p>
      </div>
    </Link>
  )
}

function PromotionCard({ item }) {
  return (
    <div className="rounded-2xl border border-[#d8e6d8] bg-[#f4faf4] p-3">
      <p className="text-xs font-semibold uppercase tracking-[0.08em] text-[#233423]">Mã {item.code}</p>
      <p className="mt-1 text-[12px] text-[#355035]">Giảm {item.discountValue} ({item.discountType})</p>
      <p className="mt-1 text-[11px] text-[#355035]">Đơn tối thiểu: {item.minOrderAmount}</p>
      <p className="mt-1 text-[11px] text-[#355035]">Hết hạn: {item.endDate}</p>
    </div>
  )
}

export default function ChatMessage({ message, onProductClick }) {
  const isBot = message.sender === 'BOT'
  const hasSuggestions = isBot && Array.isArray(message.suggestions) && message.suggestions.length > 0
  const formattedText = formatBotText(message)

  return (
    <div className={`flex ${isBot ? 'justify-start' : 'justify-end'}`}>
      <div className={`max-w-[92%] ${isBot ? '' : 'items-end'}`}>
        {isBot && message.intent?.intentName && (
          <p className="mb-2 text-[10px] font-semibold uppercase tracking-[0.16em] text-[#8d7a64]">
            {intentLabel(message.intent.intentName)}
          </p>
        )}

        <div
          className={`rounded-[22px] px-4 py-3 text-sm leading-7 shadow-sm ${
            isBot
              ? 'border border-[#eadfce] bg-[#fffaf4] text-[#2b241d]'
              : 'bg-[#18120f] text-white'
          }`}
        >
          <p className={`${hasSuggestions ? 'line-clamp-4' : ''} whitespace-pre-wrap`}>
            {formattedText}
          </p>
        </div>

        {isBot && Array.isArray(message.missingFields) && message.missingFields.length > 0 && (
          <div className="mt-3 rounded-2xl border border-[#f2d8d8] bg-[#fff6f6] px-3 py-2 text-xs text-[#863a3a]">
            Cần bổ sung: {message.missingFields.join(', ')}
          </div>
        )}

        {isBot && Array.isArray(message.promotions) && message.promotions.length > 0 && (
          <div className="mt-3 grid gap-2">
            {message.promotions.map((promotion, idx) => (
              <PromotionCard key={`${promotion.code || 'promo'}-${idx}`} item={promotion} />
            ))}
          </div>
        )}

        {isBot && hasSuggestions && (
          <div className="mt-3 flex gap-3 overflow-x-auto pb-2">
            {message.suggestions.map((item, idx) => (
              <ProductCard
                key={`${item.productId || 'item'}-${idx}`}
                item={item}
                navigationState={{
                  chatbotAttribution: {
                    source: 'chatbot',
                    sessionId: message.sessionId,
                    sourceMessageId: message.id,
                    productId: item?.productId || '',
                    productName: item?.name || '',
                    surface: 'chat_message',
                    position: idx,
                  },
                }}
                onClick={(product) =>
                  onProductClick?.({
                    item: product,
                    sessionId: message.sessionId,
                    sourceMessageId: message.id,
                    surface: 'chat_message',
                    position: idx,
                  })
                }
              />
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
