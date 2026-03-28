import { createContext, useContext, useState, useCallback, useMemo, useEffect } from 'react'
import { cartService } from '../services/cartService'
import { productService } from '../../product/services/productService'

const CartContext = createContext(null)

/**
 * Custom hook to consume cart state.
 * Throws if used outside <CartProvider>.
 */
export const useCartContext = () => {
  const ctx = useContext(CartContext)
  if (!ctx) throw new Error('useCartContext must be used inside <CartProvider>')
  return ctx
}

/**
 * Enrich raw cart items (variantSizeId + quantity) with product data
 * so that UI components get: { id, name, color, size, price, image, quantity }
 */
async function enrichCartItems(rawItems) {
  if (!rawItems || rawItems.length === 0) return []

  // Collect unique variantSizeIds
  const variantSizeIds = rawItems.map((item) => item.variantSizeId)

  // Fetch all products to look up variant info
  // In a production app, you'd want a dedicated endpoint for this
  try {
    const { items: products } = await productService.getAll({ size: 200 })

    // Build a lookup: variantSizeId -> { product, variant, size }
    const lookup = new Map()
    for (const product of products) {
      for (const variant of product.variants || []) {
        for (const size of variant.sizes || []) {
          if (size.id && variantSizeIds.includes(size.id)) {
            const primaryImage = (variant.images || []).find((img) => img.primary)
            const firstImage = (variant.images || [])[0]
            lookup.set(size.id, {
              productId: product.id,
              slug: product.slug,
              name: product.name,
              color: variant.colorName || '',
              size: size.sizeName || '',
              price: Number(variant.price) || 0,
              image: primaryImage?.imageUrl || firstImage?.imageUrl || '',
            })
          }
        }
      }
    }

    return rawItems.map((item) => {
      const info = lookup.get(item.variantSizeId)
      return {
        id: item.variantSizeId,
        variantSizeId: item.variantSizeId,
        productId: info?.productId || 0,
        slug: info?.slug || '',
        quantity: item.quantity,
        name: info?.name || 'Unknown Product',
        color: info?.color || '',
        size: info?.size || '',
        price: info?.price || 0,
        image: info?.image || '',
        imageUrl: info?.image || '',
      }
    })
  } catch (error) {
    console.error('Failed to enrich cart items:', error)
    // Return basic items without enrichment
    return rawItems.map((item) => ({
      id: item.variantSizeId,
      variantSizeId: item.variantSizeId,
      quantity: item.quantity,
      name: 'Product',
      color: '',
      size: '',
      price: 0,
      image: '',
    }))
  }
}

export function CartProvider({ children }) {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(false)
  const [isDrawerOpen, setIsDrawerOpen] = useState(false)
  const [drawerAnchor, setDrawerAnchor] = useState({ top: 96, right: 16 })

  /* ── Load cart from API on mount ───────────────────────────────────────── */
  const fetchCart = useCallback(async () => {
    try {
      setLoading(true)
      const rawItems = await cartService.getCart()
      const enrichedItems = await enrichCartItems(rawItems)
      setItems(enrichedItems)
    } catch (error) {
      console.error('Failed to load cart:', error)
      setItems([])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchCart()
  }, [fetchCart])

  /* ── Drawer controls ─────────────────────────────────────────────────── */
  const openDrawer = useCallback((event) => {
    if (event?.currentTarget) {
      const rect = event.currentTarget.getBoundingClientRect()
      setDrawerAnchor({
        top: Math.round(rect.bottom + 8),
        right: Math.max(8, Math.round(window.innerWidth - rect.right)),
      })
    }
    setIsDrawerOpen(true)
  }, [])
  const closeDrawer = useCallback(() => setIsDrawerOpen(false), [])

  /* ── Item CRUD — calls backend API ─────────────────────────────────── */
  const addItem = useCallback(async (product) => {
    try {
      const variantSizeId = product.variantSizeId || product.id
      await cartService.addItem(variantSizeId, product.quantity ?? 1)
      // Re-fetch to get enriched data
      const rawItems = await cartService.getCart()
      const enrichedItems = await enrichCartItems(rawItems)
      setItems(enrichedItems)
    } catch (error) {
      console.error('Failed to add item to cart:', error)
    }
  }, [])

  const removeItem = useCallback(async (id) => {
    try {
      // Optimistic update
      setItems((prev) => prev.filter((i) => i.id !== id))
      await cartService.removeItem(id)
    } catch (error) {
      console.error('Failed to remove item:', error)
      // Re-fetch on error to restore correct state
      fetchCart()
    }
  }, [fetchCart])

  const updateQuantity = useCallback(async (id, qty) => {
    if (qty <= 0) {
      return removeItem(id)
    }

    try {
      // Optimistic update
      setItems((prev) => prev.map((i) => (i.id === id ? { ...i, quantity: qty } : i)))
      await cartService.updateQuantity(id, qty)
    } catch (error) {
      console.error('Failed to update quantity:', error)
      fetchCart()
    }
  }, [fetchCart, removeItem])

  const clearCart = useCallback(async () => {
    try {
      setItems([])
      await cartService.clearCart()
    } catch (error) {
      console.error('Failed to clear cart:', error)
      fetchCart()
    }
  }, [fetchCart])

  /* ── Derived values (memoised) ───────────────────────────────────────── */
  const subtotal = useMemo(
    () => items.reduce((sum, i) => sum + i.price * i.quantity, 0),
    [items],
  )

  const totalItems = useMemo(
    () => items.reduce((sum, i) => sum + i.quantity, 0),
    [items],
  )

  /* ── Context value (stable reference when deps unchanged) ────────────── */
  const value = useMemo(
    () => ({
      items,
      loading,
      isDrawerOpen,
      drawerAnchor,
      openDrawer,
      closeDrawer,
      addItem,
      removeItem,
      updateQuantity,
      clearCart,
      fetchCart,
      subtotal,
      totalItems,
    }),
    [items, loading, isDrawerOpen, drawerAnchor, openDrawer, closeDrawer, addItem, removeItem, updateQuantity, clearCart, fetchCart, subtotal, totalItems],
  )

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>
}

export default CartContext