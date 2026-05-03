import { useEffect, useMemo, useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { productService } from '../services/productService'
import ProductGallery from '../components/ProductGallery'
import SizeSelector from '../components/SizeSelector'
import ColorSelector from '../components/ColorSelector'
import ProductCard from '../components/ProductCard'
import { formatCurrency } from '../../../shared/utils/format'
import { useCartContext } from '../../cart/hooks/useCartContext'
import { useWishlistContext } from '../../wishlist/context/WishlistContext'
import ProductReviewSection from '../../review/components/ProductReviewSection'
import { useAuth } from '../../auth/hooks/useAuth'

const PRIMARY = '#5A6D57'

function HeartOutlineDark() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d="M12 21C12 21 3 14.5 3 8.5C3 5.42 5.42 3 8.5 3C10.24 3 11.91 3.81 13 5.08C14.09 3.81 15.76 3 17.5 3C20.58 3 23 5.42 23 8.5C23 14.5 12 21 12 21Z" stroke="#CBCBCB" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}
function HeartFilledDark() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d="M12 21C12 21 3 14.5 3 8.5C3 5.42 5.42 3 8.5 3C10.24 3 11.91 3.81 13 5.08C14.09 3.81 15.76 3 17.5 3C20.58 3 23 5.42 23 8.5C23 14.5 12 21 12 21Z" fill="#C0392B" stroke="#C0392B" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

/* ── Quantity control ─────────────────────────────────────── */
function QtyControl({ qty, onDecrement, onIncrement }) {
  return (
    <div className="flex items-center border border-[#CBCBCB] w-fit">
      <button
        onClick={onDecrement}
        disabled={qty <= 1}
        className="w-10 h-10 flex items-center justify-center text-[18px] text-[#202020] hover:bg-[#F5F5F3] disabled:opacity-30 transition-colors"
        aria-label="Decrease quantity"
      >
        −
      </button>
      <span className="w-10 h-10 flex items-center justify-center text-[14px] font-medium text-[#202020] border-x border-[#CBCBCB]">
        {qty}
      </span>
      <button
        onClick={onIncrement}
        className="w-10 h-10 flex items-center justify-center text-[18px] text-[#202020] hover:bg-[#F5F5F3] transition-colors"
        aria-label="Increase quantity"
      >
        +
      </button>
    </div>
  )
}

/* ── Detail accordion row ─────────────────────────────────── */
function DetailRow({ label, content, defaultOpen = false }) {
  const [open, setOpen] = useState(defaultOpen)
  return (
    <div className="border-t border-[#E8E8E8]">
      <button
        onClick={() => setOpen((p) => !p)}
        className="w-full flex items-center justify-between py-4 text-[13px] font-medium tracking-[0.08em] uppercase text-[#202020]"
      >
        <span>{label}</span>
        <span className="text-[18px] leading-none text-[#888]">{open ? '−' : '+'}</span>
      </button>
      {open && (
        <p className="pb-4 text-[13px] text-[#555] leading-relaxed">{content}</p>
      )}
    </div>
  )
}

export default function ProductDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { addItem, openDrawer } = useCartContext()
  const { user } = useAuth()

  const [product, setProduct] = useState(null)
  const [related, setRelated] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [cartError, setCartError] = useState('')

  const [selectedColor, setSelectedColor] = useState(null)
  const [selectedSize, setSelectedSize] = useState(null)
  const [qty, setQty] = useState(1)
  const [addedToCart, setAddedToCart] = useState(false)

  const { isWishlisted, toggleWishlist } = useWishlistContext()
  const wishlisted = product ? isWishlisted(product.id) : false

  useEffect(() => {
    let isMounted = true

    const loadProduct = async () => {
      setLoading(true)
      setError('')
      try {
        const detail = await productService.getById(id)
        if (!isMounted) return

        setProduct(detail)
        setSelectedColor(detail?.colors?.[0] || null)

        const list = await productService.getAll({
          categoryId: detail?.categoryId || undefined,
          size: 20,
        })

        if (!isMounted) return
        setRelated((list.items || []).filter((p) => p.id !== detail.id).slice(0, 4))
      } catch (err) {
        if (!isMounted) return
        setError(err?.response?.data?.message || err?.message || 'Failed to load product')
      } finally {
        if (isMounted) setLoading(false)
      }
    }

    loadProduct()

    return () => {
      isMounted = false
    }
  }, [id])

  const galleryImages = useMemo(() => {
    if (!product) return []
    if (product.images?.length) return product.images
    return product.image ? [product.image] : []
  }, [product])

  if (loading) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center gap-4">
        <p className="text-lg font-medium text-[#202020]">Loading product...</p>
      </div>
    )
  }

  if (error) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center gap-4">
        <p className="text-lg font-medium text-[#202020]">Could not load product</p>
        <p className="text-sm text-[#888]">{error}</p>
        <button
          onClick={() => navigate('/products')}
          className="text-[13px] underline text-[#5A6D57]"
        >
          Back to Collection
        </button>
      </div>
    )
  }

  if (!product) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center gap-4">
        <p className="text-lg font-medium text-[#202020]">Product not found</p>
        <button
          onClick={() => navigate('/products')}
          className="text-[13px] underline text-[#5A6D57]"
        >
          Back to Collection
        </button>
      </div>
    )
  }

  const { name, category, price, isNew, colors, sizes, collection, fabric, description, variants, colorLabels, inStock, stockBySize } = product

  // Sizes that are out of stock (quantity = 0)
  const disabledSizes = sizes.filter((s) => !(stockBySize?.[s] > 0))

  const handleAddToCart = async (e) => {
    if (!user) {
      navigate('/login')
      return
    }
    if (!selectedSize) return
    setCartError('')

    // Find the matching variant by selected color
    const selectedColorIndex = colors?.indexOf(selectedColor) ?? 0
    const selectedColorLabel = colorLabels?.[selectedColorIndex] || ''
    const variant = variants?.find((v) => v.colorName === selectedColorLabel) || variants?.[0]

    // Find the size ID within that variant
    const sizeObj = variant?.sizes?.find((s) => s.sizeName === selectedSize)
    const variantSizeId = sizeObj?.id

    if (!variantSizeId) {
      console.error('Could not find variantSizeId for', selectedColorLabel, selectedSize)
      setCartError('Không tìm thấy sản phẩm với kích thước này')
      return
    }

    try {
      setAddedToCart(true)
      await addItem({ variantSizeId, quantity: qty })
      openDrawer(e)
      setTimeout(() => setAddedToCart(false), 2000)
    } catch (err) {
      console.error('Add to cart failed:', err)
      setCartError(err?.message || 'Không thể thêm vào giỏ hàng')
      setAddedToCart(false)
    }
  }

  return (
    <div className="min-h-screen bg-white">
      {/* ── Breadcrumb ─────────────────────────────────────── */}
      <div className="max-w-screen-xl mx-auto px-6 pt-5 pb-2">
        <nav className="text-[11px] text-[#888] tracking-wide uppercase flex items-center gap-1.5">
          <Link to="/" className="hover:text-[#202020] transition-colors">Home</Link>
          <span>/</span>
          <Link to="/products" className="hover:text-[#202020] transition-colors">Collection</Link>
          <span>/</span>
          <span className="text-[#202020]">{name}</span>
        </nav>
      </div>

      {/* ── Main content ───────────────────────────────────── */}
      <div className="max-w-screen-xl mx-auto px-6 py-6">
        <div className="flex flex-col md:flex-row gap-10 lg:gap-16">

          {/* LEFT — Gallery */}
          <div className="w-full md:w-[55%] lg:w-[60%] flex-shrink-0">
            <ProductGallery images={galleryImages} productName={name} />
          </div>

          {/* RIGHT — Product info */}
          <div className="flex-1 flex flex-col min-w-0">
            {/* New badge */}
            {isNew && (
              <span className="text-[11px] tracking-[0.12em] uppercase text-[#5A6D57] font-medium mb-2">
                New Arrival
              </span>
            )}

            {/* Name + wishlist row */}
            <div className="flex items-start justify-between gap-3 mb-1">
              <h1 className="text-[22px] md:text-[26px] font-semibold text-[#202020] leading-snug">
                {name}
              </h1>
              <button
                onClick={() => toggleWishlist(product.id)}
                className="flex-shrink-0 mt-0.5 hover:scale-110 transition-transform"
                aria-label="Toggle wishlist"
              >
                {wishlisted ? <HeartFilledDark /> : <HeartOutlineDark />}
              </button>
            </div>

            <p className="text-[13px] text-[#888] mb-3">{category}</p>

            {/* Price */}
            <p className="text-[20px] font-semibold text-[#202020] mb-5">
              {formatCurrency(price)}
            </p>

            <div className="border-t border-[#E8E8E8] mb-5" />

            {/* Out-of-stock banner */}
            {!inStock && (
              <div className="mb-5 px-4 py-3 bg-[#FFF3F3] border border-[#F5C6CB] rounded text-[13px] text-[#721C24]">
                Sản phẩm hiện đã hết hàng
              </div>
            )}

            {/* Color selector */}
            {colors?.length > 0 && (
              <div className="mb-5">
                <p className="text-[12px] tracking-[0.08em] uppercase text-[#202020] font-medium mb-2.5">
                  Color
                </p>
                <ColorSelector
                  colors={colors}
                  selected={selectedColor}
                  onSelect={setSelectedColor}
                />
              </div>
            )}

            {/* Size selector */}
            {sizes?.length > 0 && (
              <div className="mb-5">
                <div className="flex items-center justify-between mb-2.5">
                  <p className="text-[12px] tracking-[0.08em] uppercase text-[#202020] font-medium">
                    Size
                  </p>
                  <button className="text-[12px] underline text-[#888] hover:text-[#202020] transition-colors">
                    Size Guide
                  </button>
                </div>
                <SizeSelector
                  sizes={sizes}
                  selected={selectedSize}
                  onSelect={setSelectedSize}
                  disabledSizes={disabledSizes}
                />
                {!selectedSize && (
                  <p className="text-[11px] text-[#CBCBCB] mt-1.5">Please select a size</p>
                )}
              </div>
            )}

            {/* Quantity */}
            <div className="mb-6">
              <p className="text-[12px] tracking-[0.08em] uppercase text-[#202020] font-medium mb-2.5">
                Quantity
              </p>
              <QtyControl
                qty={qty}
                onDecrement={() => setQty((q) => Math.max(1, q - 1))}
                onIncrement={() => setQty((q) => q + 1)}
              />
            </div>

            {/* Add to Cart button */}
            <button
              onClick={handleAddToCart}
              disabled={!selectedSize || !inStock}
              className="w-full h-[52px] text-[13px] font-medium tracking-[0.1em] uppercase text-white transition-opacity disabled:opacity-40 hover:opacity-90 mb-3"
              style={{ backgroundColor: PRIMARY }}
            >
              {!inStock ? 'Hết hàng' : addedToCart ? 'Added to Cart ✓' : 'Add to Cart'}
            </button>

            {cartError && (
              <p className="text-[13px] text-red-500 mb-3 text-center">{cartError}</p>
            )}

            {/* Add to Wishlist text button */}
            <button
              onClick={() => toggleWishlist(product.id)}
              className="w-full h-[52px] text-[13px] font-medium tracking-[0.1em] uppercase text-[#202020] border border-[#CBCBCB] hover:border-[#202020] transition-colors mb-6"
            >
              {wishlisted ? 'Remove from Wishlist' : 'Add to Wishlist'}
            </button>

            {/* Detail accordions */}
            <DetailRow
              label="Product Details"
              defaultOpen
              content={description || `Fabric: ${fabric}. Part of the ${collection} collection. This versatile piece is designed for everyday comfort without compromising on style. True to size and easy to mix with your everyday wardrobe.`}
            />
            <DetailRow
              label="Shipping & Returns"
              content="Free standard shipping on all orders. Express shipping available. Returns accepted within 30 days of delivery — items must be unworn and in original condition."
            />
            <DetailRow
              label="Care Instructions"
              content="Machine wash cold with similar colours. Do not bleach. Tumble dry low. Cool iron if needed. Do not dry clean."
            />
          </div>
        </div>

        <ProductReviewSection productId={product.id} />

        {/* ── Related Products ───────────────────────────── */}
        {related.length > 0 && (
          <section className="mt-16">
            <h2 className="text-[16px] font-semibold tracking-[0.08em] uppercase text-[#202020] mb-6 text-center">
              You May Also Like
            </h2>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-x-5 gap-y-8">
              {related.map((p) => (
                <ProductCard key={p.id} product={p} />
              ))}
            </div>
          </section>
        )}
      </div>
    </div>
  )
}
