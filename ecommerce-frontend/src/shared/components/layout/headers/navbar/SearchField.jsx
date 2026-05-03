import { useRef, useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { productService } from '../../../../../modules/product/services/productService'
import { formatCurrency } from '../../../../utils/format'

/* ═══════════════════════════════════════════════════════════════
   SVG Icons
   ═══════════════════════════════════════════════════════════════ */
function SearchSvg({ color = '#5A6D57', size = 20 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <circle cx="11" cy="11" r="7" stroke={color} strokeWidth="1.8" />
      <path d="M16.5 16.5L21 21" stroke={color} strokeWidth="1.8" strokeLinecap="round" />
    </svg>
  )
}

function ClearSvg() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d="M18 6L6 18M6 6L18 18" stroke="#ADADAD" strokeWidth="2" strokeLinecap="round" />
    </svg>
  )
}

function ArrowSvg() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d="M7 17L17 7M17 7H7M17 7V17" stroke="#ADADAD" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

function TrendSvg() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d="M13 3L21 3L21 11" stroke="#5A6D57" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M21 3L13 11L9 7L3 13" stroke="#5A6D57" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

/* ═══════════════════════════════════════════════════════════════
   Keyframes (injected once)
   ═══════════════════════════════════════════════════════════════ */
const KEYFRAMES = `
@keyframes searchSlideDown {
  from { opacity: 0; transform: translateY(-12px); }
  to   { opacity: 1; transform: translateY(0); }
}
@keyframes searchFadeIn {
  from { opacity: 0; transform: translateY(6px); }
  to   { opacity: 1; transform: translateY(0); }
}
@keyframes shimmer {
  0%   { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50%      { opacity: 0.5; }
}
`

let injected = false
function injectKeyframes() {
  if (injected || typeof document === 'undefined') return
  const style = document.createElement('style')
  style.innerHTML = KEYFRAMES
  document.head.appendChild(style)
  injected = true
}

/* ═══════════════════════════════════════════════════════════════
   Debounce helper
   ═══════════════════════════════════════════════════════════════ */
function useDebounce(value, delay) {
  const [debounced, setDebounced] = useState(value)
  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay)
    return () => clearTimeout(timer)
  }, [value, delay])
  return debounced
}

/* ═══════════════════════════════════════════════════════════════
   Skeleton loaders
   ═══════════════════════════════════════════════════════════════ */
function SkeletonItem() {
  const shimmerBg = {
    background: 'linear-gradient(90deg, #f0f0f0 25%, #e8e8e8 50%, #f0f0f0 75%)',
    backgroundSize: '200% 100%',
    animation: 'shimmer 1.5s ease-in-out infinite',
    borderRadius: '6px',
  }
  return (
    <div style={{ display: 'flex', gap: '14px', alignItems: 'center', padding: '10px 0' }}>
      <div style={{ ...shimmerBg, width: '56px', height: '70px', flexShrink: 0, borderRadius: '8px' }} />
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '8px' }}>
        <div style={{ ...shimmerBg, width: '70%', height: '13px' }} />
        <div style={{ ...shimmerBg, width: '40%', height: '12px' }} />
      </div>
    </div>
  )
}

/* ═══════════════════════════════════════════════════════════════
   Popular search suggestions
   ═══════════════════════════════════════════════════════════════ */
const POPULAR_SEARCHES = ['Áo sơ mi', 'Quần jean', 'Áo khoác', 'Váy', 'Chân váy', 'Áo polo']

/* ═══════════════════════════════════════════════════════════════
   Main Component
   ═══════════════════════════════════════════════════════════════ */
export default function SearchField({ onClose }) {
  injectKeyframes()

  const navigate = useNavigate()
  const inputRef = useRef(null)
  const containerRef = useRef(null)
  const [query, setQuery] = useState('')
  const [focused, setFocused] = useState(false)
  const [suggestions, setSuggestions] = useState([])
  const [loadingSuggestions, setLoadingSuggestions] = useState(false)
  const [activeIndex, setActiveIndex] = useState(-1)

  const debouncedQuery = useDebounce(query, 300)

  /* Auto-focus khi mở */
  useEffect(() => {
    const timer = setTimeout(() => inputRef.current?.focus(), 80)
    return () => clearTimeout(timer)
  }, [])

  /* Đóng khi Escape */
  useEffect(() => {
    const handler = (e) => { if (e.key === 'Escape') onClose?.() }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [onClose])

  /* Đóng khi click bên ngoài */
  useEffect(() => {
    const handler = (e) => {
      if (containerRef.current && !containerRef.current.contains(e.target)) {
        onClose?.()
      }
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [onClose])

  /* Fetch suggestions khi debounced query thay đổi */
  useEffect(() => {
    let cancelled = false

    ;(async () => {
      if (!debouncedQuery.trim()) {
        if (!cancelled) {
          setSuggestions([])
          setLoadingSuggestions(false)
        }
        return
      }

      if (!cancelled) {
        setLoadingSuggestions(true)
        setActiveIndex(-1)
      }

      try {
        const res = await productService.getAll({
          search: debouncedQuery.trim(),
          page: 0,
          size: 6,
        })
        if (!cancelled) {
          setSuggestions(res.items || [])
          setLoadingSuggestions(false)
        }
      } catch {
        if (!cancelled) {
          setSuggestions([])
          setLoadingSuggestions(false)
        }
      }
    })()

    return () => { cancelled = true }
  }, [debouncedQuery])

  const navigateToProduct = useCallback((id) => {
    navigate(`/products/${id}`)
    onClose?.()
  }, [navigate, onClose])

  /* Keyboard navigation */
  const handleKeyDown = useCallback((e) => {
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      setActiveIndex((prev) => (prev < suggestions.length - 1 ? prev + 1 : 0))
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setActiveIndex((prev) => (prev > 0 ? prev - 1 : suggestions.length - 1))
    } else if (e.key === 'Enter' && activeIndex >= 0 && suggestions[activeIndex]) {
      e.preventDefault()
      navigateToProduct(suggestions[activeIndex].id)
    }
  }, [suggestions, activeIndex, navigateToProduct])

  const handleSubmit = (e) => {
    e.preventDefault()
    if (query.trim()) {
      navigate(`/products?q=${encodeURIComponent(query.trim())}`)
      onClose?.()
    }
  }

  const handleClear = () => {
    setQuery('')
    setSuggestions([])
    inputRef.current?.focus()
  }



  const handlePopularClick = (term) => {
    setQuery(term)
    navigate(`/products?q=${encodeURIComponent(term)}`)
    onClose?.()
  }

  const showResults = query.trim().length > 0
  const showPopular = !query.trim()

  return (
    <div ref={containerRef} style={{
      position: 'absolute',
      top: '100%',
      left: 0,
      right: 0,
      zIndex: 1200,
      background: '#fff',
      borderBottom: '1px solid #EBEBEB',
      boxShadow: '0 12px 48px rgba(0,0,0,0.1), 0 2px 8px rgba(0,0,0,0.04)',
      animation: 'searchSlideDown 0.25s cubic-bezier(0.4,0,0.2,1)',
    }}>

      {/* ── Backdrop blur overlay ── */}
      <div style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        background: 'rgba(0,0,0,0.15)',
        backdropFilter: 'blur(2px)',
        zIndex: -1,
        animation: 'searchFadeIn 0.3s ease',
      }} onClick={() => onClose?.()} />

      <div style={{
        maxWidth: '720px',
        margin: '0 auto',
        padding: '24px 28px 20px',
      }}>

        {/* ── Search input ── */}
        <form onSubmit={handleSubmit}>
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '12px',
              background: focused ? '#fff' : '#F7F7F5',
              border: focused ? '1.5px solid #5A6D57' : '1.5px solid #EBEBEB',
              borderRadius: '14px',
              padding: '0 18px',
              height: '52px',
              transition: 'all 0.25s cubic-bezier(0.4,0,0.2,1)',
              boxShadow: focused ? '0 0 0 4px rgba(90,109,87,0.08)' : 'none',
              cursor: 'text',
            }}
            onClick={() => inputRef.current?.focus()}
          >
            <button type="submit" style={{
              background: 'none', border: 'none', cursor: 'pointer', padding: 0,
              display: 'flex', alignItems: 'center', flexShrink: 0,
              transition: 'transform 0.15s', transform: focused ? 'scale(1.05)' : 'scale(1)',
            }} aria-label="Tìm kiếm">
              <SearchSvg color={focused ? '#5A6D57' : '#ADADAD'} />
            </button>

            <input
              ref={inputRef}
              type="text"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              onFocus={() => setFocused(true)}
              onBlur={() => setFocused(false)}
              onKeyDown={handleKeyDown}
              placeholder="Tìm kiếm sản phẩm..."
              autoComplete="off"
              style={{
                flex: 1, border: 'none', outline: 'none', background: 'transparent',
                fontSize: '15px', fontFamily: 'Montserrat, sans-serif', color: '#202020',
                letterSpacing: '0.01em',
              }}
            />

            {/* Searching indicator */}
            {loadingSuggestions && query.trim() && (
              <div style={{
                width: '18px', height: '18px', borderRadius: '50%',
                border: '2px solid #E0E0E0', borderTopColor: '#5A6D57',
                animation: 'pulse 1s linear infinite',
                flexShrink: 0,
              }} />
            )}

            {query && (
              <button
                type="button"
                onClick={(e) => { e.stopPropagation(); handleClear() }}
                style={{
                  background: '#F0F0F0', border: 'none', cursor: 'pointer',
                  padding: '4px', display: 'flex', alignItems: 'center',
                  borderRadius: '50%', flexShrink: 0,
                  transition: 'background 0.15s',
                }}
                onMouseEnter={(e) => e.currentTarget.style.background = '#E0E0E0'}
                onMouseLeave={(e) => e.currentTarget.style.background = '#F0F0F0'}
                aria-label="Xóa"
              >
                <ClearSvg />
              </button>
            )}
          </div>
        </form>

        {/* ── Popular searches (khi chưa nhập) ── */}
        {showPopular && (
          <div style={{
            marginTop: '20px',
            animation: 'searchFadeIn 0.3s ease',
          }}>
            <div style={{
              display: 'flex', alignItems: 'center', gap: '6px',
              marginBottom: '12px',
            }}>
              <TrendSvg />
              <span style={{
                fontSize: '12px', fontWeight: 600, color: '#5A6D57',
                fontFamily: 'Montserrat, sans-serif', letterSpacing: '0.08em',
                textTransform: 'uppercase',
              }}>
                Tìm kiếm phổ biến
              </span>
            </div>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
              {POPULAR_SEARCHES.map((term) => (
                <button
                  key={term}
                  onClick={() => handlePopularClick(term)}
                  style={{
                    background: '#F7F7F5', border: '1px solid #EBEBEB',
                    borderRadius: '50px', padding: '8px 16px',
                    fontSize: '13px', fontFamily: 'Montserrat, sans-serif',
                    color: '#404040', cursor: 'pointer',
                    transition: 'all 0.2s ease',
                    fontWeight: 500, letterSpacing: '0.02em',
                  }}
                  onMouseEnter={(e) => {
                    e.currentTarget.style.background = '#5A6D57'
                    e.currentTarget.style.color = '#fff'
                    e.currentTarget.style.borderColor = '#5A6D57'
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.background = '#F7F7F5'
                    e.currentTarget.style.color = '#404040'
                    e.currentTarget.style.borderColor = '#EBEBEB'
                  }}
                >
                  {term}
                </button>
              ))}
            </div>
          </div>
        )}

        {/* ── Search results ── */}
        {showResults && (
          <div style={{ marginTop: '16px', animation: 'searchFadeIn 0.25s ease' }}>

            {/* Section title */}
            <div style={{
              display: 'flex', alignItems: 'center', justifyContent: 'space-between',
              marginBottom: '8px', paddingBottom: '8px',
              borderBottom: '1px solid #F5F5F5',
            }}>
              <span style={{
                fontSize: '11px', fontWeight: 600, color: '#ADADAD',
                fontFamily: 'Montserrat, sans-serif', letterSpacing: '0.1em',
                textTransform: 'uppercase',
              }}>
                {loadingSuggestions ? 'Đang tìm...' : suggestions.length > 0 ? 'Gợi ý sản phẩm' : 'Không tìm thấy'}
              </span>
              {suggestions.length > 0 && (
                <button
                  onClick={handleSubmit}
                  style={{
                    background: 'none', border: 'none', cursor: 'pointer',
                    fontSize: '12px', color: '#5A6D57', fontFamily: 'Montserrat, sans-serif',
                    fontWeight: 600, display: 'flex', alignItems: 'center', gap: '4px',
                    padding: 0, transition: 'opacity 0.15s',
                  }}
                  onMouseEnter={(e) => e.currentTarget.style.opacity = '0.7'}
                  onMouseLeave={(e) => e.currentTarget.style.opacity = '1'}
                >
                  Xem tất cả <ArrowSvg />
                </button>
              )}
            </div>

            {/* Loading skeleton */}
            {loadingSuggestions && (
              <div>
                <SkeletonItem />
                <SkeletonItem />
                <SkeletonItem />
              </div>
            )}

            {/* Results list */}
            {!loadingSuggestions && suggestions.length > 0 && (
              <div style={{ maxHeight: '380px', overflowY: 'auto' }}>
                {suggestions.map((product, index) => (
                  <div
                    key={product.id}
                    onClick={() => navigateToProduct(product.id)}
                    onMouseEnter={() => setActiveIndex(index)}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '14px',
                      padding: '10px 8px',
                      borderRadius: '10px',
                      cursor: 'pointer',
                      transition: 'all 0.15s ease',
                      background: index === activeIndex ? '#F7F7F5' : 'transparent',
                      animation: `searchFadeIn 0.3s ease ${index * 0.05}s both`,
                    }}
                  >
                    {/* Product image */}
                    <div style={{
                      width: '56px', height: '70px', flexShrink: 0,
                      borderRadius: '8px', overflow: 'hidden',
                      background: '#F5F5F3',
                      boxShadow: '0 1px 4px rgba(0,0,0,0.06)',
                    }}>
                      <img
                        src={product.image}
                        alt={product.name}
                        style={{
                          width: '100%', height: '100%', objectFit: 'cover',
                          transition: 'transform 0.3s ease',
                          transform: index === activeIndex ? 'scale(1.08)' : 'scale(1)',
                        }}
                        loading="lazy"
                      />
                    </div>

                    {/* Product info */}
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <p style={{
                        fontSize: '13px', fontWeight: 600, color: '#202020',
                        fontFamily: 'Montserrat, sans-serif',
                        lineHeight: '1.3', margin: '0 0 3px',
                        overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
                      }}>
                        {highlightMatch(product.name, query)}
                      </p>
                      <p style={{
                        fontSize: '11px', fontWeight: 400, color: '#ADADAD',
                        fontFamily: 'Montserrat, sans-serif',
                        margin: '0 0 4px', lineHeight: '1.2',
                      }}>
                        {product.category}
                      </p>
                      <p style={{
                        fontSize: '13px', fontWeight: 600, color: '#5A6D57',
                        fontFamily: 'Montserrat, sans-serif', margin: 0,
                      }}>
                        {formatCurrency(product.price)}
                      </p>
                    </div>

                    {/* Arrow indicator */}
                    <div style={{
                      flexShrink: 0,
                      opacity: index === activeIndex ? 1 : 0,
                      transition: 'opacity 0.15s',
                      transform: 'rotate(45deg)',
                    }}>
                      <ArrowSvg />
                    </div>
                  </div>
                ))}
              </div>
            )}

            {/* Empty state */}
            {!loadingSuggestions && suggestions.length === 0 && query.trim() && (
              <div style={{
                padding: '32px 0',
                textAlign: 'center',
                animation: 'searchFadeIn 0.3s ease',
              }}>
                <div style={{
                  width: '48px', height: '48px', borderRadius: '50%',
                  background: '#F7F7F5', display: 'flex',
                  alignItems: 'center', justifyContent: 'center',
                  margin: '0 auto 12px',
                }}>
                  <SearchSvg color="#D0D0D0" size={22} />
                </div>
                <p style={{
                  fontSize: '14px', fontWeight: 500, color: '#888',
                  fontFamily: 'Montserrat, sans-serif', margin: 0,
                }}>
                  Không tìm thấy sản phẩm cho "{query}"
                </p>
                <p style={{
                  fontSize: '12px', color: '#ADADAD', marginTop: '6px',
                  fontFamily: 'Montserrat, sans-serif',
                }}>
                  Thử từ khóa khác hoặc duyệt bộ sưu tập
                </p>
              </div>
            )}
          </div>
        )}


      </div>
    </div>
  )
}

/* ═══════════════════════════════════════════════════════════════
   Highlight matching text
   ═══════════════════════════════════════════════════════════════ */
function highlightMatch(text, query) {
  if (!query.trim()) return text
  const regex = new RegExp(`(${escapeRegex(query.trim())})`, 'gi')
  const parts = text.split(regex)
  return parts.map((part, i) =>
    regex.test(part) ? (
      <span key={i} style={{ color: '#5A6D57', fontWeight: 700 }}>{part}</span>
    ) : (
      <span key={i}>{part}</span>
    )
  )
}

function escapeRegex(str) {
  return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}
