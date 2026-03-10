import { useSearchParams } from 'react-router-dom'
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

export default function ProductListPage() {
  const [searchParams] = useSearchParams()
  const category = searchParams.get('category') || ''
  const pageTitle = CATEGORY_LABELS[category] || 'All Products'

  const { filters, setFilter, toggleArrayFilter, clearFilters, hasActiveFilters } = useFilters(
    category ? { collections: [CATEGORY_LABELS[category]] } : {}
  )
  const { products, loading } = useProducts({ filters })

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
          {loading ? 'Loading…' : `${products.length} Item${products.length !== 1 ? 's' : ''}`}
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
            />
          </div>

          {/* ── Product grid ─── */}
          <div className="flex-1">
            {loading ? (
              <div className="grid grid-cols-2 md:grid-cols-3 gap-x-5 gap-y-8">
                <SkeletonProductCollection displayCount={6} />
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
              <div className="grid grid-cols-2 md:grid-cols-3 gap-x-5 gap-y-8">
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
