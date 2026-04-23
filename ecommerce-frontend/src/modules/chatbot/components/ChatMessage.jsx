import { Link } from 'react-router-dom'

function intentLabel(intentName) {
  switch (intentName) {
    case 'CONSULT_SIZE':
      return 'Tư vấn size'
    case 'CONSULT_SEASON':
      return 'Tư vấn theo mùa/xu hướng'
    case 'ASK_PROMOTION':
      return 'Tư vấn khuyến mãi'
    default:
      return 'General'
  }
}

function ProductCard({ item }) {
  return (
    <Link
      to={item.link || '#'}
      className="block min-w-44 rounded-lg border border-[#e7e7e7] bg-white p-2 transition hover:border-[#cfcfcf]"
    >
      {item.imageUrl ? (
        <img
          src={item.imageUrl}
          alt={item.name}
          className="h-28 w-full rounded-md object-cover"
          loading="lazy"
        />
      ) : (
        <div className="flex h-28 w-full items-center justify-center rounded-md bg-[#f1f1f1] text-xs text-[#878787]">
          No image
        </div>
      )}

      <p className="mt-2 line-clamp-2 text-xs font-semibold text-[#1f1f1f]">{item.name}</p>
      <p className="mt-1 text-[11px] text-[#6a6a6a]">{item.category}</p>
      <p className="mt-1 text-xs font-semibold text-[#1f1f1f]">{item.price}</p>

      {Array.isArray(item.availableSizes) && item.availableSizes.length > 0 && (
        <p className="mt-1 text-[11px] text-[#666]">Size: {item.availableSizes.slice(0, 4).join(', ')}</p>
      )}
      {Array.isArray(item.availableColors) && item.availableColors.length > 0 && (
        <p className="mt-1 text-[11px] text-[#666]">Màu: {item.availableColors.slice(0, 4).join(', ')}</p>
      )}
    </Link>
  )
}

function PromotionCard({ item }) {
  return (
    <div className="rounded-lg border border-[#d8e6d8] bg-[#f4faf4] p-2">
      <p className="text-xs font-semibold text-[#233423]">Mã: {item.code}</p>
      <p className="text-[11px] text-[#355035]">Giảm: {item.discountValue} ({item.discountType})</p>
      <p className="text-[11px] text-[#355035]">Đơn tối thiểu: {item.minOrderAmount}</p>
      <p className="text-[11px] text-[#355035]">Hết hạn: {item.endDate}</p>
    </div>
  )
}

export default function ChatMessage({ message }) {
  const isBot = message.sender === 'BOT'

  return (
    <div className={`flex ${isBot ? 'justify-start' : 'justify-end'}`}>
      <div className={`max-w-[85%] ${isBot ? '' : 'items-end'}`}>
        {isBot && message.intent?.intentName && (
          <p className="mb-1 text-[10px] font-semibold uppercase tracking-[0.14em] text-[#8a8a8a]">
            {intentLabel(message.intent.intentName)}
          </p>
        )}

        <div
          className={`rounded-2xl px-4 py-3 text-sm leading-relaxed ${
            isBot
              ? 'bg-[#f4f4f4] text-[#242424]'
              : 'bg-[#121212] text-white'
          }`}
        >
          {message.text}
        </div>

        {isBot && Array.isArray(message.missingFields) && message.missingFields.length > 0 && (
          <div className="mt-2 rounded-lg border border-[#f2d8d8] bg-[#fff6f6] px-3 py-2 text-xs text-[#863a3a]">
            Cần bổ sung: {message.missingFields.join(', ')}
          </div>
        )}

        {isBot && Array.isArray(message.promotions) && message.promotions.length > 0 && (
          <div className="mt-2 grid gap-2">
            {message.promotions.map((promotion, idx) => (
              <PromotionCard key={`${promotion.code || 'promo'}-${idx}`} item={promotion} />
            ))}
          </div>
        )}

        {isBot && Array.isArray(message.suggestions) && message.suggestions.length > 0 && (
          <div className="mt-2 flex gap-2 overflow-x-auto pb-1">
            {message.suggestions.map((item, idx) => (
              <ProductCard key={`${item.productId || 'item'}-${idx}`} item={item} />
            ))}
          </div>
        )}
      </div>
    </div>
  )
}