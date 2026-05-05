import { useEffect, useState, useMemo } from 'react'
import { Link } from 'react-router-dom'
import { wishlistService } from '../services/wishlistService'
import { normalizeProduct } from '../../product/services/productService'
import ProductCard from '../../product/components/ProductCard'
import { useWishlistContext } from '../context/useWishlistContext'

function buildPaginationItems(totalPages, currentPage, maxVisible = 5) {
  if (totalPages <= maxVisible) {
    return Array.from({ length: totalPages }, (_, index) => index)
  }

  const halfWindow = Math.floor(maxVisible / 2)
  let start = Math.max(0, currentPage - halfWindow)
  let end = start + maxVisible - 1

  if (end >= totalPages) {
    end = totalPages - 1
    start = end - maxVisible + 1
  }

  const items = []
  if (start > 0) {
    items.push(0)
    if (start > 1) {
      items.push('ellipsis-start')
    }
  }

  for (let pageNumber = start; pageNumber <= end; pageNumber += 1) {
    items.push(pageNumber)
  }

  if (end < totalPages - 1) {
    if (end < totalPages - 2) {
      items.push('ellipsis-end')
    }
    items.push(totalPages - 1)
  }

  return items
}

export default function WishlistPage() {
  const [wishlistProducts, setWishlistProducts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  const { wishlistIds } = useWishlistContext()

  useEffect(() => {
    let mounted = true
    const fetchWishlist = async () => {
      setLoading(true)
      try {
        const data = await wishlistService.getWishlist({ page, size: 12 })
        if (mounted) {
          const rawProducts = data.content || []
          setWishlistProducts(rawProducts.map(normalizeProduct))
          setTotalPages(data.totalPages || 0)
        }
      } catch {
        if (mounted) setError('Could not load wishlist')
      } finally {
        if (mounted) setLoading(false)
      }
    }
    fetchWishlist()
    return () => { mounted = false }
  }, [page])

  const pageItems = useMemo(() => buildPaginationItems(totalPages, page), [page, totalPages])

  // Filter out products that were un-wishlisted in the current session
  const visibleProducts = wishlistProducts.filter(product => wishlistIds.has(product.id))

  return (
    <div className="min-h-screen bg-white" style={{ fontFamily: 'Montserrat, sans-serif' }}>
      <div className="max-w-[1440px] mx-auto px-6 py-12">
        <h1 className="text-center text-[24px] md:text-[28px] font-semibold text-[#202020] tracking-wide mb-8">
          My Wishlist
        </h1>

        {loading ? (
          <p className="text-center mt-10 text-[14px]">Loading...</p>
        ) : error ? (
           <p className="text-center mt-10 text-[14px] text-red-500">{error}</p>
        ) : visibleProducts.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-20">
            <p className="text-[16px] text-[#202020] mb-4">Your wishlist is currently empty.</p>
            <Link
              to="/products"
              className="inline-block px-8 py-3 bg-[#5A6D57] text-white text-[13px] font-medium tracking-wide uppercase hover:opacity-90 transition-opacity"
            >
              Continue Shopping
            </Link>
          </div>
        ) : (
          <>
            <p className="text-center mb-10 text-[14px] text-[#888]">
              {visibleProducts.length} Item{visibleProducts.length !== 1 ? 's' : ''}
            </p>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-x-5 gap-y-10">
              {visibleProducts.map((product) => (
                <ProductCard key={product.id} product={product} />
              ))}
            </div>

            {totalPages > 1 && (
              <div className="mt-16 flex items-center justify-center gap-2 flex-wrap">
                <button
                  type="button"
                  onClick={() => setPage((prev) => Math.max(0, prev - 1))}
                  disabled={page === 0}
                  className="px-3 py-1.5 border border-[#D7D7D7] text-[12px] text-[#202020] disabled:opacity-40"
                >
                  Prev
                </button>
                {pageItems.map((pageItem) => (
                  typeof pageItem === 'number' ? (
                    <button
                      key={pageItem}
                      type="button"
                      onClick={() => setPage(pageItem)}
                      className={`min-w-9 h-9 px-2 border text-[12px] transition-colors ${
                        pageItem === page
                          ? 'border-[#202020] bg-[#202020] text-white'
                          : 'border-[#D7D7D7] text-[#202020] hover:border-[#202020]'
                      }`}
                    >
                      {pageItem + 1}
                    </button>
                  ) : (
                    <span key={pageItem} className="min-w-9 h-9 px-2 flex items-center justify-center text-[12px] text-[#888]">
                      ...
                    </span>
                  )
                ))}
                <button
                  type="button"
                  onClick={() => setPage((prev) => Math.min(totalPages - 1, prev + 1))}
                  disabled={page === totalPages - 1}
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
  )
}
