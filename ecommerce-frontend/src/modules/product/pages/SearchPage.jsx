import { useMemo, useReducer, useRef, useState } from 'react'
import { useSearchParams, useNavigate } from 'react-router-dom'
import { useProducts } from '../hooks/useProducts'
import { useFilters } from '../hooks/useFilters'
import ProductCard from '../components/ProductCard'
import FilterSidebar from '../components/FilterSidebar'
import SkeletonProductCollection from '../components/SkeletonProductCollection'

/* ── Inline styles (no Tailwind dependency for new elements) ── */
const styles = {
  searchWrapper: {
    position: 'relative',
    display: 'flex',
    alignItems: 'center',
    gap: '0',
  },
  searchInputContainer: {
    display: 'flex',
    alignItems: 'center',
    background: '#F7F7F5',
    border: '1.5px solid transparent',
    borderRadius: '50px',
    padding: '0 18px',
    height: '44px',
    gap: '10px',
    transition: 'border-color 0.25s ease, box-shadow 0.25s ease, width 0.35s cubic-bezier(0.4,0,0.2,1)',
    overflow: 'hidden',
    cursor: 'text',
  },
  searchInputContainerFocused: {
    borderColor: '#5A6D57',
    boxShadow: '0 0 0 3px rgba(90,109,87,0.12)',
    background: '#fff',
  },
  searchInput: {
    border: 'none',
    outline: 'none',
    background: 'transparent',
    fontSize: '15px',
    fontFamily: 'Montserrat, sans-serif',
    color: '#202020',
    letterSpacing: '0.02em',
    width: '100%',
    minWidth: 0,
  },
  iconBtn: {
    background: 'none',
    border: 'none',
    cursor: 'pointer',
    padding: '0',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: 0,
    opacity: 1,
    transition: 'opacity 0.2s',
  },
  searchChip: {
    display: 'inline-flex',
    alignItems: 'center',
    gap: '8px',
    background: '#5A6D57',
    color: '#fff',
    borderRadius: '50px',
    padding: '5px 14px 5px 16px',
    fontSize: '13px',
    fontFamily: 'Montserrat, sans-serif',
    fontWeight: 500,
    letterSpacing: '0.04em',
    whiteSpace: 'nowrap',
  },
  chipClose: {
    background: 'rgba(255,255,255,0.25)',
    border: 'none',
    borderRadius: '50%',
    width: '18px',
    height: '18px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    cursor: 'pointer',
    padding: 0,
    transition: 'background 0.2s',
  },
}

function SearchSvg({ color = '#5A6D57', size = 20 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <circle cx="11" cy="11" r="7" stroke={color} strokeWidth="1.8" />
      <path d="M16.5 16.5L21 21" stroke={color} strokeWidth="1.8" strokeLinecap="round" />
    </svg>
  )
}

function CloseSmSvg({ color = '#fff' }) {
  return (
    <svg width="10" height="10" viewBox="0 0 12 12" fill="none" aria-hidden="true">
      <path d="M10 2L2 10M2 2L10 10" stroke={color} strokeWidth="1.8" strokeLinecap="round" />
    </svg>
  )
}

function ClearSvg() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d="M18 6L6 18M6 6L18 18" stroke="#9E9E9E" strokeWidth="2" strokeLinecap="round" />
    </svg>
  )
}

/**
 * Reducer to manage page state with auto-reset when a "resetKey" changes.
 */
function pageReducer(state, action) {
  switch (action.type) {
    case 'SET_PAGE':
      return { ...state, page: action.page }
    case 'RESET_KEY_CHANGED':
      return { page: 0, resetKey: action.resetKey }
    default:
      return state
  }
}

export default function SearchPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const initialQuery = searchParams.get('q') || ''
  const genderParam = (searchParams.get('gender') || '').toLowerCase()
  const genderFilter = genderParam === 'men' || genderParam === 'male'
    ? 'MALE'
    : genderParam === 'women' || genderParam === 'female'
      ? 'FEMALE'
      : ''

  // Track inputValue: use a key-based approach to reset when URL query changes
  const [inputState, setInputState] = useState({ value: initialQuery, queryKey: initialQuery })
  if (inputState.queryKey !== initialQuery) {
    // React allows calling setState during render if the value is different (similar to getDerivedStateFromProps)
    setInputState({ value: initialQuery, queryKey: initialQuery })
  }
  const inputValue = inputState.value
  const setInputValue = (v) => setInputState(prev => ({ ...prev, value: v }))

  const [focused, setFocused] = useState(false)
  const inputRef = useRef(null)

  const handleSubmit = (e) => {
    e.preventDefault()
    if (inputValue.trim()) {
      const genderQuery = genderParam ? `&gender=${encodeURIComponent(genderParam)}` : ''
      navigate(`/search?q=${encodeURIComponent(inputValue.trim())}${genderQuery}`)
      inputRef.current?.blur()
    }
  }

  const handleClear = () => {
    setInputValue('')
    const genderQuery = genderParam ? `?gender=${encodeURIComponent(genderParam)}` : ''
    navigate(`/search${genderQuery}`)
    setTimeout(() => inputRef.current?.focus(), 50)
  }

  const { filters, setFilter, toggleArrayFilter, clearFilters, hasActiveFilters } = useFilters()

  // Build a key that changes whenever the filter dependencies change
  const resetKey = `${initialQuery}|${genderFilter}|${JSON.stringify(filters)}`

  const [pageState, dispatchPage] = useReducer(pageReducer, { page: 0, resetKey })

  // When resetKey changes, the page auto-resets to 0 via the reducer
  const effectivePage = pageState.resetKey === resetKey ? pageState.page : 0
  const setPage = (p) => dispatchPage({ type: 'SET_PAGE', page: typeof p === 'function' ? p(effectivePage) : p })

  // If the key changed, dispatch the reset (will be processed on next render)
  if (pageState.resetKey !== resetKey) {
    dispatchPage({ type: 'RESET_KEY_CHANGED', resetKey })
  }

  const {
    products,
    loading,
    error,
    total,
    totalPages,
    currentPage,
  } = useProducts({
    query: initialQuery,
    filters: { ...filters, page: effectivePage, pageSize: 12 },
    gender: genderFilter,
  })

  const pageNumbers = useMemo(() => {
    const pages = []
    for (let i = 0; i < totalPages; i += 1) pages.push(i)
    return pages
  }, [totalPages])

  return (
    <div style={{ minHeight: '100vh', background: '#fff', fontFamily: 'Montserrat, sans-serif' }}>

      {/* ══ Search bar ══════════════════════════════════════════ */}
      <div style={{ borderBottom: '1px solid #EBEBEB', background: '#fff' }}>
        <div style={{
          maxWidth: '1440px',
          margin: '0 auto',
          padding: '0 24px',
          height: '72px',
          display: 'flex',
          alignItems: 'center',
          gap: '16px',
        }}>

          {/* ── Search input pill ── */}
          <form
            onSubmit={handleSubmit}
            style={{ flex: 1, display: 'flex', alignItems: 'center', gap: '12px' }}
          >
            <div
              style={{
                ...styles.searchInputContainer,
                ...(focused ? styles.searchInputContainerFocused : {}),
                flex: 1,
              }}
              onClick={() => inputRef.current?.focus()}
            >
              {/* Icon */}
              <button type="submit" style={styles.iconBtn} aria-label="Search">
                <SearchSvg color={focused ? '#5A6D57' : '#9E9E9E'} />
              </button>

              {/* Input */}
              <input
                ref={inputRef}
                type="text"
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
                onFocus={() => setFocused(true)}
                onBlur={() => setFocused(false)}
                placeholder="Tìm kiếm sản phẩm..."
                style={styles.searchInput}
                autoFocus
              />

              {/* Clear X inside pill */}
              {inputValue && (
                <button
                  type="button"
                  onClick={(e) => { e.stopPropagation(); handleClear() }}
                  style={{ ...styles.iconBtn, marginLeft: '4px' }}
                  aria-label="Clear"
                >
                  <ClearSvg />
                </button>
              )}
            </div>
          </form>

          {/* ── Searched keyword chip ── */}
          {initialQuery && !focused && (
            <div style={styles.searchChip}>
              <span>"{initialQuery}"</span>
              <button
                style={styles.chipClose}
                onClick={handleClear}
                aria-label="Remove search"
              >
                <CloseSmSvg />
              </button>
            </div>
          )}
        </div>
      </div>

      {/* ══ Body ════════════════════════════════════════════════ */}
      <div style={{ maxWidth: '1440px', margin: '0 auto', padding: '32px 24px' }}>

        {/* Item count */}
        <p style={{
          textAlign: 'center',
          marginBottom: '32px',
          fontFamily: 'Montserrat, sans-serif',
          fontSize: '13px',
          fontWeight: 400,
          color: '#8A8A8A',
          letterSpacing: '0.08em',
          textTransform: 'uppercase',
        }}>
          {loading ? 'Đang tải…' : `${total} sản phẩm`}
        </p>

        <div style={{ display: 'flex', gap: '32px', alignItems: 'flex-start' }}>

          {/* ── Filter sidebar ───────────────────────────────── */}
          <div style={{ width: '240px', flexShrink: 0 }}>
            <FilterSidebar
              filters={filters}
              setFilter={setFilter}
              toggleArrayFilter={toggleArrayFilter}
              clearFilters={clearFilters}
              hasActiveFilters={hasActiveFilters}
            />
          </div>

          {/* ── Product grid ─────────────────────────────────── */}
          <div style={{ flex: 1, minWidth: 0 }}>
            {loading ? (
              <div className="grid grid-cols-2 gap-x-5 gap-y-10">
                <SkeletonProductCollection displayCount={4} />
              </div>
            ) : error ? (
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '96px 0', gap: '8px', textAlign: 'center' }}>
                <p style={{ fontFamily: 'Montserrat, sans-serif', fontSize: '16px', fontWeight: 600, color: '#202020' }}>
                  Không thể tải sản phẩm
                </p>
                <p style={{ fontSize: '14px', color: '#888', fontFamily: 'Montserrat, sans-serif' }}>{error}</p>
              </div>
            ) : products.length === 0 ? (
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '96px 0', gap: '12px', textAlign: 'center' }}>
                {/* Empty state illustration */}
                <div style={{ width: '64px', height: '64px', borderRadius: '50%', background: '#F7F7F5', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '8px' }}>
                  <SearchSvg color="#CBCBCB" size={28} />
                </div>
                <p style={{ fontFamily: 'Montserrat, sans-serif', fontSize: '16px', fontWeight: 600, color: '#202020' }}>
                  Không tìm thấy sản phẩm
                </p>
                {initialQuery && (
                  <p style={{ fontSize: '14px', color: '#888', fontFamily: 'Montserrat, sans-serif' }}>
                    Thử từ khóa khác hoặc xóa bộ lọc.
                  </p>
                )}
                <button
                  onClick={clearFilters}
                  style={{ marginTop: '4px', fontSize: '13px', color: '#5A6D57', fontFamily: 'Montserrat, sans-serif', background: 'none', border: '1px solid #5A6D57', borderRadius: '50px', padding: '8px 20px', cursor: 'pointer', letterSpacing: '0.04em', transition: 'all 0.2s' }}
                >
                  Xóa bộ lọc
                </button>
              </div>
            ) : (
              <>
                <div className="grid grid-cols-2 gap-x-5 gap-y-10">
                  {products.map((product) => (
                    <ProductCard key={product.id} product={product} />
                  ))}
                </div>

                {totalPages > 1 && (
                  <div style={{ marginTop: '40px', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', flexWrap: 'wrap' }}>
                    <button
                      type="button"
                      onClick={() => setPage((prev) => Math.max(0, prev - 1))}
                      disabled={currentPage === 0}
                      className="px-3 py-1.5 border border-[#D7D7D7] text-[12px] text-[#202020] disabled:opacity-40"
                    >
                      Prev
                    </button>

                    {pageNumbers.map((pageNum) => (
                      <button
                        key={pageNum}
                        type="button"
                        onClick={() => setPage(pageNum)}
                        className={`min-w-9 h-9 px-2 border text-[12px] transition-colors ${
                          pageNum === currentPage
                            ? 'border-[#202020] bg-[#202020] text-white'
                            : 'border-[#D7D7D7] text-[#202020] hover:border-[#202020]'
                        }`}
                      >
                        {pageNum + 1}
                      </button>
                    ))}

                    <button
                      type="button"
                      onClick={() => setPage((prev) => Math.min(totalPages - 1, prev + 1))}
                      disabled={currentPage === totalPages - 1}
                      className="px-3 py-1.5 border border-[#D7D7D7] text-[12px] text-[#202020] disabled:opacity-40"
                    >
                      Next
                    </button>
                  </div>
                )}
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
