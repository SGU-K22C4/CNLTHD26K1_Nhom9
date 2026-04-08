import { createContext, useContext, useState, useCallback, useMemo, useEffect, useRef } from 'react'
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

export function CartProvider({ children }) {
  const [items, setItems] = useState([])
  const [isDrawerOpen, setIsDrawerOpen] = useState(false)
  const [drawerAnchor, setDrawerAnchor] = useState({ top: 96, right: 16 })
  const productVariantIndexRef = useRef(null)

  const ensureProductVariantIndex = useCallback(async () => {
    if (productVariantIndexRef.current) return productVariantIndexRef.current

    const { items: products = [] } = await productService.getAll({ page: 0, size: 500 })
    const index = new Map()

    products.forEach((product) => {
      const fallbackImage = product.image || product.images?.[0] || ''

      ;(product.variants || []).forEach((variant) => {
        const variantImage = variant?.images?.find((img) => img.primary)?.imageUrl
          || variant?.images?.[0]?.imageUrl
          || fallbackImage

        ;(variant.sizes || []).forEach((sizeObj) => {
          if (!sizeObj?.id) return
          index.set(sizeObj.id, {
            id: sizeObj.id,
            variantSizeId: sizeObj.id,
            productId: product.id,
            name: product.name || 'Unknown product',
            color: variant.colorName || '',
            size: sizeObj.sizeName || '',
            price: Number(variant.price) || 0,
            image: variantImage,
            imageUrl: variantImage,
            slug: product.slug || '',
          })
        })
      })
    })

    productVariantIndexRef.current = index
    return index
  }, [])

  const hydrateCartItems = useCallback(async (rawItems) => {
    const index = await ensureProductVariantIndex()
    return (rawItems || []).map((raw) => {
      const variantSizeId = String(raw?.variantSizeId || raw?.id || '')
      const quantity = Number(raw?.quantity) || 1
      const base = index.get(variantSizeId)

      if (!base) {
        return {
          id: variantSizeId,
          variantSizeId,
          productId: '',
          name: `Variant ${variantSizeId}`,
          color: '',
          size: '',
          price: 0,
          image: '',
          imageUrl: '',
          slug: '',
          quantity,
        }
      }

      return { ...base, quantity }
    })
  }, [ensureProductVariantIndex])

  const syncCart = useCallback(async () => {
    const raw = await cartService.getCart()
    const hydrated = await hydrateCartItems(raw)
    setItems(hydrated)
  }, [hydrateCartItems])

  useEffect(() => {
    let mounted = true

    const loadInitialCart = async () => {
      try {
        const raw = await cartService.getCart()
        const hydrated = await hydrateCartItems(raw)
        if (mounted) setItems(hydrated)
      } catch (error) {
        console.error('Failed to load cart:', error)
        if (mounted) setItems([])
      }
    }

    loadInitialCart()

    return () => {
      mounted = false
    }
  }, [hydrateCartItems])

  /* ── Drawer controls ─────────────────────────────────────────────────────── */
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

  /* ── Item CRUD ───────────────────────────────────────────────────────────── */
  const addItem = useCallback(async (payload) => {
    const variantSizeId = payload?.variantSizeId || payload?.id
    const quantity = Number(payload?.quantity) || 1

    if (!variantSizeId) {
      throw new Error('variantSizeId is required')
    }

    await cartService.addItem(variantSizeId, quantity)
    await syncCart()
  }, [syncCart])

  const removeItem = useCallback(async (id) => {
    await cartService.removeItem(id)
    await syncCart()
  }, [syncCart])

  const updateQuantity = useCallback(async (id, qty) => {
    if (qty <= 0) {
      await cartService.removeItem(id)
      await syncCart()
      return
    }

    await cartService.updateQuantity(id, qty)
    await syncCart()
  }, [syncCart])

  const clearCart = useCallback(async () => {
    await cartService.clearCart()
    setItems([])
  }, [])

  /* ── Derived values (memoised) ───────────────────────────────────────────── */
  const subtotal = useMemo(
    () => items.reduce((sum, i) => sum + i.price * i.quantity, 0),
    [items],
  )

  const totalItems = useMemo(
    () => items.reduce((sum, i) => sum + i.quantity, 0),
    [items],
  )

  /* ── Context value (stable reference when deps unchanged) ────────────────── */
  const value = useMemo(
    () => ({
      items,
      isDrawerOpen,
      drawerAnchor,
      openDrawer,
      closeDrawer,
      addItem,
      removeItem,
      updateQuantity,
      clearCart,
      subtotal,
      totalItems,
    }),
    [items, isDrawerOpen, drawerAnchor, openDrawer, closeDrawer, addItem, removeItem, updateQuantity, clearCart, subtotal, totalItems],
  )

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>
}

export default CartContext