export default function StarRating({
  value = 0,
  max = 5,
  size = 18,
  interactive = false,
  onChange,
  className = '',
}) {
  return (
    <div className={`inline-flex items-center gap-1 ${className}`}>
      {Array.from({ length: max }).map((_, index) => {
        const starValue = index + 1
        const filled = starValue <= value

        return (
          <button
            key={starValue}
            type="button"
            disabled={!interactive}
            onClick={() => interactive && onChange?.(starValue)}
            aria-label={`Rate ${starValue} star${starValue > 1 ? 's' : ''}`}
            className={`leading-none ${interactive ? 'cursor-pointer' : 'cursor-default'} ${interactive ? 'hover:scale-110 transition-transform' : ''}`}
            style={{ fontSize: size, color: filled ? '#F59E0B' : '#D1D5DB' }}
          >
            ★
          </button>
        )
      })}
    </div>
  )
}
