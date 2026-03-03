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
    <div className="relative flex items-start gap-4 py-4 border-b border-[#dfdfdf]">

      {/* LEFT — product image */}
      <div className="w-[80px] shrink-0 overflow-hidden bg-gray-100">
        <img src={image} alt={name} className="w-full h-full object-cover" />
      </div>

      {/* RIGHT — all info, price, quantity */}
      <div className="flex-1 flex flex-col gap-2 pr-6">
        {/* Text block */}
        <div>
          <h3 className="font-[Montserrat] font-bold text-sm leading-snug text-[#202020]">
            {name}
          </h3>
          {size && (
            <p className="font-[Montserrat] text-sm text-[#404040] mt-0.5">
              Size: {size}
            </p>
          )}
          {color && (
            <p className="font-[Montserrat] text-sm text-[#404040] mt-0.5">
              Color: {color}
            </p>
          )}
        </div>

        {/* Bottom row — price (left) + quantity stepper (right) */}
        <div className="flex justify-between items-center mt-auto">
          <p className="font-[Montserrat] font-bold text-sm text-[#202020]">
            ${(price * quantity).toFixed(2)}
          </p>

          <div className="inline-flex items-center border border-[#dfdfdf]">
            <button
              onClick={decrement}
              aria-label="Decrease quantity"
              className="flex h-8 w-8 items-center justify-center font-[Montserrat] text-base text-[#404040] hover:bg-gray-100 transition-colors"
            >
              −
            </button>
            <span className="flex h-8 w-8 items-center justify-center select-none font-[Montserrat] text-sm font-medium text-[#202020]">
              {quantity}
            </span>
            <button
              onClick={increment}
              aria-label="Increase quantity"
              className="flex h-8 w-8 items-center justify-center font-[Montserrat] text-base text-[#404040] hover:bg-gray-100 transition-colors"
            >
              +
            </button>
          </div>
        </div>
      </div>

      {/* X — absolute, pinned top-right of the row */}
      <button
        onClick={() => onRemove(id)}
        aria-label={`Remove ${name}`}
        className="absolute top-4 right-0 text-[#202020] hover:text-[#404040] transition-colors"
      >
        <X size={16} strokeWidth={1.5} />
      </button>

    </div>
  )
}
