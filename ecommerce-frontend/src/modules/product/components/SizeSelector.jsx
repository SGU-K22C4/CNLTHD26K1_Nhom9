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

export default function SizeSelector({ sizes = [], selected, onSelect }) {
  return (
    <div className="flex flex-wrap gap-2">
      {sizes.map((size) => {
        const active = selected === size
        return (
          <button
            key={size}
            onClick={() => onSelect(size)}
            className="w-12 h-10 text-[12px] font-medium border transition-all hover:border-[#202020]"
            style={{
              backgroundColor: active ? PRIMARY : '#FFFFFF',
              color: active ? '#FFFFFF' : '#202020',
              borderColor: active ? PRIMARY : NEUTRAL_GRAY,
            }}
          >
            {SIZE_LABELS[size] || size}
          </button>
        )
      })}
    </div>
  )
}