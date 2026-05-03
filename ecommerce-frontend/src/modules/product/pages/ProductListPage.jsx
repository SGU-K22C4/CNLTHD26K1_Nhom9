import { useMemo, useReducer } from 'react'
import { useParams, useSearchParams } from 'react-router-dom'
import { useProducts } from '../hooks/useProducts'
import { useFilters } from '../hooks/useFilters'
import FilterSidebar from '../components/FilterSidebar'
import ProductCard from '../components/ProductCard'
import SkeletonProductCollection from '../components/SkeletonProductCollection'

/* ── category param → display label ─────────────────────────── */
const CATEGORY_LABELS = {
  'new-in': 'New In',
  'collection': 'Collection',
  'plus-size': 'Plus Size',
  'modiweek': 'Modiweek',
}

/**
 * Reducer to manage page state with auto-reset when a "resetKey" changes.
 * This avoids calling setState in useEffect (react-hooks/set-state-in-effect)
 * and avoids accessing refs during render (react-hooks/refs).
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

export default function ProductListPage() {
  const { gender: genderSlug } = useParams()
  const [searchParams] = useSearchParams()
  const category = searchParams.get('category') || ''
  const genderParam = searchParams.get('gender') || ''
  const searchQuery = searchParams.get('q') || ''

  const normalizedGender = (genderSlug || genderParam).toLowerCase()
  const genderFilter = normalizedGender === 'men' || normalizedGender === 'male'
    ? 'MALE'
    : normalizedGender === 'women' || normalizedGender === 'female'
      ? 'FEMALE'
      : ''

  const pageTitle = searchQuery
    ? `Kết quả tìm kiếm: "${searchQuery}"`
    : genderFilter === 'MALE'
      ? 'Nam Collection'
      : genderFilter === 'FEMALE'
        ? 'Nu Collection'
        : (CATEGORY_LABELS[category] || 'All Products')

  const { filters, setFilter, toggleArrayFilter, clearFilters, hasActiveFilters } = useFilters(
    category ? { collections: [CATEGORY_LABELS[category]] } : {}
  )

  // Build a key that changes whenever the filter dependencies change
  const resetKey = `${genderFilter}|${category}|${searchQuery}|${JSON.stringify(filters)}`

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
    availableFilters,
  } = useProducts({
    query: searchQuery,
    filters: { ...filters, page: effectivePage, pageSize: 12 },
    gender: genderFilter,
  })

  const pageNumbers = useMemo(() => {
    const pages = []
    for (let i = 0; i < totalPages; i += 1) pages.push(i)
    return pages
  }, [totalPages])

  return (
    <div className="min-h-screen bg-white">
      {/* ── Page heading ─────────────────────────────────────── */}
      <div className="border-b border-[#E0E0E0]">
        <div className="max-w-screen-xl mx-auto px-6 py-4">
          {/* Breadcrumb */}
          <p className="text-[11px] text-[#888] tracking-wide uppercase mb-1">
            Home &nbsp;/&nbsp; {pageTitle}
          </p>
          <h1 className="text-[22px] font-semibold text-[#202020] tracking-wide">
            {pageTitle}
          </h1>
        </div>
      </div>

      {/* ── Body ─────────────────────────────────────────────── */}
      <div className="max-w-screen-xl mx-auto px-6 py-6">
        {/* Item count */}
        <p className="text-center text-[13px] text-[#666] mb-6 tracking-wide">
          {loading ? 'Loading…' : `${total} Item${total !== 1 ? 's' : ''}`}
        </p>

        <div className="flex gap-8">
          {/* ── Filter sidebar ─── */}
          <div className="w-auto md:w-[230px] md:flex-shrink-0">
            <FilterSidebar
              filters={filters}
              setFilter={setFilter}
              toggleArrayFilter={toggleArrayFilter}
              clearFilters={clearFilters}
              hasActiveFilters={hasActiveFilters}
              availableFilters={availableFilters}
            />
          </div>

          {/* ── Product grid ─── */}
          <div className="flex-1">
            {loading ? (
              <div className="grid grid-cols-2 md:grid-cols-3 gap-x-5 gap-y-8">
                <SkeletonProductCollection displayCount={6} />
              </div>
            ) : error ? (
              <div className="flex flex-col items-center justify-center py-24 text-center">
                <p className="text-lg font-medium text-[#2C2C2C]">Could not load products</p>
                <p className="text-sm text-[#888] mt-2">{error}</p>
              </div>
            ) : products.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-24 text-center">
                <p className="text-lg font-medium text-[#2C2C2C]">No products found</p>
                <button
                  onClick={clearFilters}
                  className="mt-4 text-sm underline text-[#5A6D57] hover:text-[#404040] transition-colors"
                >
                  Clear filters
                </button>
              </div>
            ) : (
              <>
                <div className="grid grid-cols-2 md:grid-cols-3 gap-x-5 gap-y-8">
                  {products.map((product) => (
                    <ProductCard key={product.id} product={product} />
                  ))}
                </div>

                {totalPages > 1 && (
                  <div className="mt-10 flex items-center justify-center gap-2 flex-wrap">
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
