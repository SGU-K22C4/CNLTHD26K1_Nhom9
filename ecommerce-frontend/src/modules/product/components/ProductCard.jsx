import { useNavigate } from 'react-router-dom'
import { formatCurrency } from '../../../shared/utils/format'
import { useWishlistContext } from '../../wishlist/context/WishlistContext'

/* ── Wishlist heart icons (SVG, no MUI dependency) ───────── */
function HeartOutline() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path
        d="M12 21C12 21 3 14.5 3 8.5C3 5.42 5.42 3 8.5 3C10.24 3 11.91 3.81 13 5.08C14.09 3.81 15.76 3 17.5 3C20.58 3 23 5.42 23 8.5C23 14.5 12 21 12 21Z"
        stroke="white"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
        style={{ filter: 'drop-shadow(0 1px 3px rgba(0,0,0,0.5))' }}
      />
    </svg>
  )
}
function HeartFilled() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path
        d="M12 21C12 21 3 14.5 3 8.5C3 5.42 5.42 3 8.5 3C10.24 3 11.91 3.81 13 5.08C14.09 3.81 15.76 3 17.5 3C20.58 3 23 5.42 23 8.5C23 14.5 12 21 12 21Z"
        fill="#C0392B"
        stroke="#C0392B"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  )
}

export default function ProductCard({ product }) {
  const navigate = useNavigate()
  const { isWishlisted, toggleWishlist } = useWishlistContext()

  if (!product) return null

  const { id, name, category, price, isNew, image, colors } = product
  const wishlisted = isWishlisted(id)

  return (
    <div
      className="group relative flex flex-col cursor-pointer"
      style={{ fontFamily: 'Montserrat, sans-serif', boxSizing: 'border-box' }}
      onClick={() => navigate(`/products/${id}`)}
    >
      {/* ── Image ───────────────────────────────────────── */}
      <div className="relative overflow-hidden bg-[#F5F5F3]">
        <img
          src={image}
          alt={name}
          className="w-full object-cover aspect-[4/5] transition-transform duration-700 group-hover:scale-105"
          loading="lazy"
        />

        {/* New badge */}
        {isNew && (
          <span
            className="absolute top-3 left-3 bg-white px-2 py-0.5 tracking-wide"
            style={{ fontSize: '10px', fontWeight: 500, fontFamily: 'Montserrat, sans-serif' }}
          >
            New
          </span>
        )}

        {/* Wishlist */}
        <button
          className="absolute top-3 right-3 p-1 transition-transform hover:scale-110"
          onClick={(e) => {
            e.stopPropagation()
            toggleWishlist(id)
          }}
          aria-label="Toggle wishlist"
        >
          {wishlisted ? <HeartFilled /> : <HeartOutline />}
        </button>
      </div>

      {/* ── Info ────────────────────────────────────────── */}
      <div className="pt-3 pb-1 flex flex-col gap-1">
        {/* Name + price row */}
        <div className="flex items-start justify-between gap-2">
          <span
            style={{ fontSize: '13px', fontWeight: 600, color: '#202020', lineHeight: '1.3', fontFamily: 'Montserrat, sans-serif' }}
          >
            {name}
          </span>
          <span
            style={{ fontSize: '13px', fontWeight: 500, color: '#202020', whiteSpace: 'nowrap', fontFamily: 'Montserrat, sans-serif' }}
          >
            {formatCurrency(price)}
          </span>
        </div>

        {/* Category */}
        <span
          style={{ fontSize: '12px', fontWeight: 400, color: '#888', fontFamily: 'Montserrat, sans-serif', lineHeight: '1.3' }}
        >
          {category}
        </span>

        {/* Colour swatches */}
        {colors?.length > 0 && (
          <div className="flex gap-1.5 mt-1">
            {colors.map((color) => (
              <span
                key={color}
                className="w-[14px] h-[14px] rounded-full inline-block flex-shrink-0"
                style={{
                  backgroundColor: color,
                  border: '1px solid #E0E0E0',
                }}
                title={color}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  )
}