/**
 * Comprehensive Vietnamese color name → HEX mapping.
 *
 * Used by ProductCard (swatches), ColorSelector (detail page),
 * and FilterSidebar (color filter chips).
 *
 * Keys are LOWERCASE & TRIMMED for normalisation.
 * Add new entries here when the product catalog introduces new color names.
 */
export const COLOR_NAME_TO_HEX = {
  // ── Blacks ───────────────────────────────────────────────
  'màu đen':            '#1C1C1C',
  'đen':                '#1C1C1C',

  // ── Whites / Creams ──────────────────────────────────────
  'màu trắng':          '#FFFFFF',
  'trắng':              '#FFFFFF',
  'màu trắng ngà':      '#FFFDD0',
  'kem':                '#F5F0E1',
  'màu kem':            '#F5F0E1',

  // ── Grays ────────────────────────────────────────────────
  'màu xám':            '#8C8C8C',
  'xám':                '#8C8C8C',
  'màu xám nhạt':       '#C0C0C0',
  'màu xám đậm':        '#4A4A4A',
  'màu xám đá':         '#7D7D7D',
  'màu xám than':       '#3C3C3C',
  'màu xám ngọc trai':  '#B8B8B0',

  // ── Browns ───────────────────────────────────────────────
  'màu nâu':            '#8B5E3C',
  'nâu':                '#8B5E3C',
  'màu nâu đậm':        '#5C3317',
  'màu nâu nhạt dịu':   '#C4A882',
  'màu nâu đất':        '#7B4B2A',
  'màu nâu sô cô la':   '#6B3A2A',
  'màu sôcôla':         '#6B3A2A',
  'nâu vàng':           '#C19A6B',
  'màu caramel':        '#A0522D',

  // ── Beiges / Tans ────────────────────────────────────────
  'màu be':             '#D2B48C',
  'màu be nhạt':        '#E8D5B7',
  'màu be đậm':         '#B8A078',
  'kaki':               '#BDB76B',
  'màu vàng kaki nhạt': '#C3B091',
  'màu vàng kaki đậm':  '#8B7D3C',
  'màu vàng bò dịu':    '#C8A96E',
  'màu vàng bò đậm':    '#A07830',
  'màu vàng bơ':        '#FFFACD',
  'màu vàng cát':       '#E3C98A',

  // ── Yellows / Oranges ────────────────────────────────────
  'màu vàng':           '#E8B828',
  'vàng':               '#E8B828',
  'màu cam':            '#E67E22',
  'cam':                '#E67E22',

  // ── Reds / Wines ─────────────────────────────────────────
  'màu đỏ':             '#C0392B',
  'đỏ':                 '#C0392B',
  'màu đỏ rượu':        '#722F37',

  // ── Pinks / Purples ──────────────────────────────────────
  'màu hồng':           '#E8909C',
  'hồng':               '#E8909C',
  'màu hồng nhạt pha tím': '#D8A0C8',
  'màu tím cà':         '#8B668B',

  // ── Blues ─────────────────────────────────────────────────
  'xanh dương':         '#4A7FB5',
  'màu xanh dương đậm': '#1A3A6B',
  'màu xanh nước biển': '#2E6B8A',
  'màu xanh da trời':   '#6CB4D8',
  'màu xanh nhạt':      '#94BDD8',
  'xanh nhạt':          '#94BDD8',
  'màu xanh dịu':       '#7AACB8',
  'màu xanh biển':      '#2D6E7E',
  'màu xanh hải quân đậm': '#1B2F4B',
  'màu xanh mực':       '#1C2951',
  'màu xanh cửu long':  '#2E5C6E',
  'màu xanh cổ vịt':    '#008080',

  // ── Greens ───────────────────────────────────────────────
  'xanh lục':           '#5A8C5A',
  'xanh lục nhạt':      '#98D898',
  'màu xanh lá cây đậm': '#2E5A2E',
  'màu xanh lá dịu':   '#7CB07C',
  'màu xanh ô liu':     '#6B8E23',

  // ── Teals / Blue-Greens ──────────────────────────────────
  'màu xanh xám':       '#5A7D7C',

  // ── Indigos / Charcoals ──────────────────────────────────
  'màu chàm':           '#354D73',
  'chàm':               '#354D73',
  'màu chàm đậm':       '#1F2D50',
  'than củi':           '#3D3D3D',

  // ── Combo / Multi / Pattern ──────────────────────────────
  'nhiều màu':          'linear-gradient(135deg, #E8909C 0%, #6CB4D8 33%, #E8B828 66%, #5A8C5A 100%)',
  'sọc':                'repeating-linear-gradient(90deg, #D0D0D0 0px, #D0D0D0 4px, #FFFFFF 4px, #FFFFFF 8px)',

  // ── Combo color names (slash-separated) ──────────────────
  'xanh dương/chàm':       '#3D5A80',
  'xanh dương/trắng':      '#6CABDD',
  'xanh dương/xanh lá':    '#4A8C6E',  // teal-ish blend
  'màu nâu nhạt/đen':      '#6B5A4E',
  'màu nâu nhạt/xanh dương': '#7A6B5E',
  'màu nâu nhạt/xanh hải quân': '#6B5D50',
  'màu nâu/nâu nhạt':      '#A07050',
  'màu nâu/ trắng':        '#B89070',
  'màu đen/trắng':         '#404040',
  'màu đỏ/đen':            '#8B2020',
  'màu trắng/xanh da trời': '#D0E8F0',
  'màu trắng/xám':         '#E0E0E0',
  'màu xanh dương/xanh lá': '#4A8C6E',
  'màu xám/nâu vàng':      '#9A8A70',
  'màu xám/tự nhiên':      '#A8A090',
  'màu trắng/ngọc lam':    '#D0F0F0',
  'màu nâu/trắng':         '#B89070',
  'thuốc lá':              '#8B7355',

  // ── English fallbacks (legacy / mock data) ───────────────
  'black':   '#1C1C1C',
  'white':   '#FFFFFF',
  'red':     '#C0392B',
  'blue':    '#4A7FB5',
  'green':   '#5A8C5A',
  'yellow':  '#E8B828',
  'gray':    '#8C8C8C',
  'grey':    '#8C8C8C',
  'pink':    '#E8909C',
  'purple':  '#9B59B6',
  'brown':   '#8B5E3C',
  'orange':  '#E67E22',
  'beige':   '#D2B48C',
  'navy':    '#1B2F4B',
}

/**
 * Default fallback when no color name matches.
 */
export const COLOR_FALLBACK_HEX = '#A8A8A8'

/**
 * Convert a color name (Vietnamese or English) to a HEX code.
 * Handles both solid colors (returns hex string) and
 * gradient/pattern colors (returns CSS gradient string).
 *
 * @param {string} colorName – raw color name from backend
 * @returns {string} HEX code or CSS gradient
 */
export function colorNameToHex(colorName = '') {
  const key = String(colorName).toLowerCase().trim()
  return COLOR_NAME_TO_HEX[key] || COLOR_FALLBACK_HEX
}

/**
 * Determine if a colorNameToHex result is a gradient/pattern
 * (as opposed to a simple hex value).
 */
export function isGradientColor(hexOrGradient = '') {
  return hexOrGradient.startsWith('linear-gradient') || hexOrGradient.startsWith('repeating-linear-gradient')
}

/**
 * Determine if a swatch needs a visible border
 * (light / white-ish colors that blend into the background).
 */
export function needsSwatchBorder(hexOrGradient = '') {
  const LIGHT_COLORS = [
    '#FFFFFF', '#FFFDD0', '#F5F0E1', '#FFFACD',
    '#E8D5B7', '#C0C0C0', '#B8B8B0', '#D0E8F0',
    '#E0E0E0', '#D0F0F0', '#98D898',
  ]
  return LIGHT_COLORS.includes(hexOrGradient.toUpperCase())
}
