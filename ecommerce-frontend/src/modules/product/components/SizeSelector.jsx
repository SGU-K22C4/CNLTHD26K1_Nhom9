/* Size labels (Figma spec) */
const SIZE_LABELS = {
  XS: 'XS',
  S: 'S',
  M: 'M',
  L: 'L',
  XL: 'XL',
  XXL: 'XXL',
}

const PRIMARY = '#5A6D57'
const NEUTRAL_GRAY = '#CBCBCB'

export default function SizeSelector({ sizes = [], selected, onSelect, disabledSizes = [] }) {
  return (
    <div className="flex flex-wrap gap-2">
      {sizes.map((size) => {
        const active = selected === size
        const outOfStock = disabledSizes.includes(size)
        return (
          <button
            key={size}
            onClick={() => !outOfStock && onSelect(size)}
            disabled={outOfStock}
            className="relative w-12 h-10 text-[12px] font-medium border transition-all hover:border-[#202020] disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:border-[#CBCBCB]"
            style={{
              backgroundColor: active ? PRIMARY : '#FFFFFF',
              color: active ? '#FFFFFF' : outOfStock ? '#CBCBCB' : '#202020',
              borderColor: active ? PRIMARY : NEUTRAL_GRAY,
            }}
          >
            {SIZE_LABELS[size] || size}
            {outOfStock && (
              <span className="absolute inset-0 flex items-center justify-center">
                <span className="block w-[80%] h-[1px] bg-[#CBCBCB] rotate-[-20deg]" />
              </span>
            )}
          </button>
        )
      })}
    </div>
  )
}