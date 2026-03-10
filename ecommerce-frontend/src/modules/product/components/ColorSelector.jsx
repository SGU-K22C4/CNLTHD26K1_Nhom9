const PRIMARY = '#5A6D57'
const NEUTRAL_GRAY = '#CBCBCB'

export default function ColorSelector({ colors = [], selected, onSelect }) {
  return (
    <div className="flex flex-wrap gap-2.5">
      {colors.map((hex) => {
        const active = selected === hex
        const needsBorder = ['#A8D5E2', '#D2B48C', '#C0C0C0', '#FFFFFF', '#A8C5A0', '#6B8E23'].includes(hex)
        return (
          <button
            key={hex}
            onClick={() => onSelect(hex)}
            title={hex}
            aria-label={hex}
            aria-pressed={active}
            className="w-7 h-7 rounded-full flex-shrink-0 transition-transform hover:scale-110"
            style={{
              backgroundColor: hex,
              border: needsBorder ? `1px solid ${NEUTRAL_GRAY}` : 'none',
              outline: active ? `2px solid ${PRIMARY}` : '2px solid transparent',
              outlineOffset: '2px',
            }}
          />
        )
      })}
    </div>
  )
}