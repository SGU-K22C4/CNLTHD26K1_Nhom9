import { useEffect, useState } from 'react'
import { useSearchParams, useNavigate } from 'react-router-dom'
import { useProducts } from '../hooks/useProducts'
import { useFilters } from '../hooks/useFilters'
import ProductCard from '../components/ProductCard'
import FilterSidebar from '../components/FilterSidebar'
import SkeletonProductCollection from '../components/SkeletonProductCollection'

/* ── 24×24 Search SVG ──────────────────────────────────────── */
function SearchSvg() {
  return (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <circle cx="11" cy="11" r="7" stroke="#202020" strokeWidth="1.5" />
      <path d="M16.5 16.5L21 21" stroke="#202020" strokeWidth="1.5" strokeLinecap="round" />
    </svg>
  )
}

/* ── 24×24 Close/X SVG ─────────────────────────────────────── */
function CloseSvg() {
  return (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d="M18 6L6 18M6 6L18 18" stroke="#202020" strokeWidth="1.5" strokeLinecap="round" />
    </svg>
  )
}

export default function SearchPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const initialQuery = searchParams.get('q') || ''

  const [inputValue, setInputValue] = useState(initialQuery)

  useEffect(() => {
    setInputValue(searchParams.get('q') || '')
  }, [searchParams])

  const handleSubmit = (e) => {
    e.preventDefault()
    if (inputValue.trim()) {
      navigate(`/search?q=${encodeURIComponent(inputValue.trim())}`)
    }
  }

  const handleClear = () => {
    setInputValue('')
    navigate('/search')
  }

  const { filters, setFilter, toggleArrayFilter, clearFilters, hasActiveFilters } = useFilters()
  const { products, loading } = useProducts({ query: initialQuery, filters })

  return (
    <div className="min-h-screen bg-white" style={{ fontFamily: 'Montserrat, sans-serif' }}>

      {/* ══ Search bar ══════════════════════════════════════════ */}
      <div className="border-b border-[#E0E0E0]">
        <form
          onSubmit={handleSubmit}
          className="max-w-[1440px] mx-auto flex items-center px-6 h-[56px] gap-4"
        >
          {/* Search icon button */}
          <button
            type="submit"
            className="flex-shrink-0 hover:opacity-70 transition-opacity"
            aria-label="Search"
          >
            <SearchSvg />
          </button>

          {/* Input */}
          <input
            type="text"
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            placeholder="Search"
            className="flex-1 text-[16px] font-normal text-[#202020] placeholder-[#CBCBCB] outline-none bg-transparent leading-none"
            style={{ fontFamily: 'Montserrat, sans-serif' }}
          />

          {/* Clear button */}
          {inputValue && (
            <button
              type="button"
              onClick={handleClear}
              className="flex-shrink-0 hover:opacity-70 transition-opacity"
              aria-label="Clear search"
            >
              <CloseSvg />
            </button>
          )}
        </form>
      </div>

      {/* ══ Body ════════════════════════════════════════════════ */}
      <div className="max-w-[1440px] mx-auto px-6 py-8">

        {/* Item count */}
        <p
          className="text-center mb-8"
          style={{
            fontFamily: 'Montserrat, sans-serif',
            fontSize: '14px',
            fontWeight: 400,
            color: '#202020',
            letterSpacing: '0.06em',
          }}
        >
          {loading
            ? 'Loading…'
            : `${products.length} Item${products.length !== 1 ? 's' : ''}`}
        </p>

        <div className="flex gap-8 items-start">

          {/* ── Filter sidebar ─────────────────────────────── */}
          <div className="w-auto md:w-[240px] flex-shrink-0">
            <FilterSidebar
              filters={filters}
              setFilter={setFilter}
              toggleArrayFilter={toggleArrayFilter}
              clearFilters={clearFilters}
              hasActiveFilters={hasActiveFilters}
            />
          </div>

          {/* ── Product grid ───────────────────────────────── */}
          <div className="flex-1 min-w-0">
            {loading ? (
              <div className="grid grid-cols-2 gap-x-5 gap-y-10">
                <SkeletonProductCollection displayCount={4} />
              </div>
            ) : products.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-24 text-center gap-4">
                <p
                  style={{
                    fontFamily: 'Montserrat, sans-serif',
                    fontSize: '16px',
                    fontWeight: 600,
                    color: '#202020',
                  }}
                >
                  No products found
                </p>
                {initialQuery && (
                  <p style={{ fontSize: '14px', color: '#888', fontFamily: 'Montserrat, sans-serif' }}>
                    Try a different search term or clear your filters.
                  </p>
                )}
                <button
                  onClick={clearFilters}
                  className="underline underline-offset-2 hover:opacity-70 transition-opacity"
                  style={{ fontSize: '14px', color: '#5A6D57', fontFamily: 'Montserrat, sans-serif' }}
                >
                  Clear filters
                </button>
              </div>
            ) : (
              <div className="grid grid-cols-2 gap-x-5 gap-y-10">
                {products.map((product) => (
                  <ProductCard key={product.id} product={product} />
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
