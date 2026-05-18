import { isGradientColor, needsSwatchBorder } from '../../../shared/utils/colorMap'

const PRIMARY = '#5A6D57'

export default function ColorSelector({ colors = [], selected, onSelect }) {
  return (
    <div className="flex flex-wrap gap-2.5">
      {colors.map((hex) => {
        const active = selected === hex
        const gradient = isGradientColor(hex)
        const lightBorder = needsSwatchBorder(hex)
        return (
          <button
            key={hex}
            onClick={() => onSelect(hex)}
            title={hex}
            aria-label={hex}
            aria-pressed={active}
            className="w-7 h-7 rounded-full flex-shrink-0 transition-transform hover:scale-110"
            style={{
              background: gradient ? hex : undefined,
              backgroundColor: gradient ? undefined : hex,
              border: lightBorder ? '1px solid #CBCBCB' : 'none',
              outline: active ? `2px solid ${PRIMARY}` : '2px solid transparent',
              outlineOffset: '2px',
            }}
          />
        )
      })}
    </div>
  )
}