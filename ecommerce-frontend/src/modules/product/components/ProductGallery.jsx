import { useState } from 'react'

export default function ProductGallery({ images = [], productName = '' }) {
  const [selected, setSelected] = useState(0)

  if (!images.length) return null

  return (
    <div className="flex flex-col-reverse md:flex-row gap-3 w-full">
      {/* ── Thumbnail strip ── */}
      <div className="flex md:flex-col gap-2 overflow-x-auto md:overflow-y-auto md:max-h-[600px] md:w-[88px] flex-shrink-0">
        {images.map((src, i) => (
          <button
            key={i}
            onClick={() => setSelected(i)}
            className="flex-shrink-0 w-16 h-20 md:w-[88px] md:h-[112px] overflow-hidden bg-[#F5F5F3] transition-opacity"
            style={{
              outline: i === selected ? '2px solid #5A6D57' : '2px solid transparent',
              outlineOffset: '-2px',
              opacity: i === selected ? 1 : 0.65,
            }}
            aria-label={`View image ${i + 1}`}
          >
            <img
              src={src}
              alt={`${productName} view ${i + 1}`}
              className="w-full h-full object-cover"
              loading="lazy"
            />
          </button>
        ))}
      </div>

      {/* ── Main image ── */}
      <div className="flex-1 overflow-hidden bg-[#F5F5F3]">
        <img
          src={images[selected]}
          alt={productName}
          className="w-full h-full object-cover aspect-[3/4]"
        />
      </div>
    </div>
  )
}