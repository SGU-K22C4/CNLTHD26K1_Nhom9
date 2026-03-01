import { X } from 'lucide-react'

/**
 * CartItem — a single product row in the Cart Drawer.
 *
 * Props
 *   item                – { id, name, color, size, price, image, quantity }
 *   onRemove(id)        – remove the item entirely
 *   onUpdateQuantity(id, qty) – change quantity (removes when qty ≤ 0)
 */
export default function CartItem({ item, onRemove, onUpdateQuantity }) {
  const { id, name, color, size, price, image, quantity } = item

  const decrement = () =>
    quantity > 1 ? onUpdateQuantity(id, quantity - 1) : onRemove(id)

  const increment = () => onUpdateQuantity(id, quantity + 1)

  return (
    <div className="relative flex gap-4 py-5 border-b border-neutral-cbcbcb last:border-b-0">
      {/* Thumbnail */}
      <div className="w-24 h-28 flex-shrink-0 overflow-hidden bg-gray-100">
        <img src={image} alt={name} className="h-full w-full object-cover" />
      </div>

      {/* Remove (X) — top-right */}
      <button
        onClick={() => onRemove(id)}
        aria-label={`Remove ${name}`}
        className="absolute top-5 right-0 p-1 text-neutral-404040 hover:text-ink transition-colors"
      >
        <X size={18} />
      </button>

      {/* Details */}
      <div className="flex flex-1 flex-col justify-between min-w-0 pr-6">
        <div>
          <h3 className="font-sans font-semibold text-[15px] leading-snug text-ink mb-2">
            {name}
          </h3>

          {size && (
            <p className="font-sans text-[13px] text-neutral-404040 mb-1">
              Size: {size}
            </p>
          )}

          {color && (
            <p className="font-sans text-[13px] text-neutral-404040">
              Color: {color}
            </p>
          )}
        </div>

        {/* Quantity stepper + Price */}
        <div className="flex items-center justify-between mt-4">
          <div className="flex items-center bg-brand-sage rounded">
            <button
              onClick={decrement}
              aria-label="Decrease quantity"
              className="flex h-8 w-8 items-center justify-center rounded-l font-sans text-base text-neutral-404040 transition-colors hover:bg-brand-sage-hover"
            >
              −
            </button>

            <span className="flex h-8 w-8 items-center justify-center select-none font-sans text-sm font-medium text-ink">
              {quantity}
            </span>

            <button
              onClick={increment}
              aria-label="Increase quantity"
              className="flex h-8 w-8 items-center justify-center rounded-r font-sans text-base text-neutral-404040 transition-colors hover:bg-brand-sage-hover"
            >
              +
            </button>
          </div>

          <p className="font-sans font-bold text-base text-ink">
            ${(price * quantity).toFixed(2)}
          </p>
        </div>
      </div>
    </div>
  )
}
