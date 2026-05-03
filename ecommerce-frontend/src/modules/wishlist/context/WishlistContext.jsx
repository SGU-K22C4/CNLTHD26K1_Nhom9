import { useState, useCallback, useEffect, useMemo } from 'react'
import { WishlistContext } from './wishlistContextDef'
import { wishlistService } from '../services/wishlistService'

/* ── Confirm‑remove modal ───────────────────────────────────── */
const OVERLAY_STYLE = {
  position: 'fixed',
  inset: 0,
  zIndex: 9999,
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  backgroundColor: 'rgba(0,0,0,0.35)',
  backdropFilter: 'blur(4px)',
  animation: 'fadeIn .15s ease',
}

const CARD_STYLE = {
  background: '#fff',
  borderRadius: '14px',
  padding: '32px 28px 24px',
  maxWidth: '360px',
  width: '90%',
  boxShadow: '0 20px 60px rgba(0,0,0,0.18)',
  textAlign: 'center',
  fontFamily: 'Montserrat, sans-serif',
  animation: 'slideUp .2s ease',
}

const BTN_BASE = {
  flex: 1,
  height: '42px',
  borderRadius: '8px',
  fontSize: '13px',
  fontWeight: 600,
  letterSpacing: '0.04em',
  cursor: 'pointer',
  transition: 'all .15s ease',
  fontFamily: 'Montserrat, sans-serif',
}

function ConfirmModal({ onConfirm, onCancel }) {
  return (
    <div style={OVERLAY_STYLE} onClick={onCancel}>
      <div style={CARD_STYLE} onClick={(e) => e.stopPropagation()}>
        {/* Icon */}
        <div style={{ marginBottom: '16px' }}>
          <svg
            width="48" height="48" viewBox="0 0 48 48" fill="none"
            style={{ margin: '0 auto' }}
          >
            <circle cx="24" cy="24" r="23" stroke="#E8D5D5" strokeWidth="2" fill="#FFF5F5" />
            <path
              d="M24 33C24 33 15 27.5 15 21.5C15 18.42 17.42 16 20.5 16C22.24 16 23.91 16.81 25 18.08C26.09 16.81 27.76 16 29.5 16C32.58 16 35 18.42 35 21.5C35 27.5 24 33 24 33Z"
              fill="#C0392B"
              stroke="#C0392B"
              strokeWidth="1.5"
              strokeLinecap="round"
              strokeLinejoin="round"
              opacity="0.7"
            />
            <line x1="18" y1="18" x2="32" y2="32" stroke="#C0392B" strokeWidth="2" strokeLinecap="round" />
          </svg>
        </div>

        {/* Text */}
        <p style={{ fontSize: '16px', fontWeight: 600, color: '#202020', marginBottom: '8px' }}>
          Xóa khỏi yêu thích?
        </p>
        <p style={{ fontSize: '13px', color: '#888', lineHeight: '1.6', marginBottom: '24px' }}>
          Sản phẩm sẽ bị xóa khỏi danh sách yêu thích của bạn.
        </p>

        {/* Actions */}
        <div style={{ display: 'flex', gap: '12px' }}>
          <button
            onClick={onCancel}
            style={{
              ...BTN_BASE,
              background: '#F5F5F3',
              color: '#505050',
              border: '1px solid #E0E0E0',
            }}
            onMouseEnter={(e) => { e.target.style.background = '#EBEBEB' }}
            onMouseLeave={(e) => { e.target.style.background = '#F5F5F3' }}
          >
            Hủy
          </button>
          <button
            onClick={onConfirm}
            style={{
              ...BTN_BASE,
              background: '#C0392B',
              color: '#fff',
              border: 'none',
            }}
            onMouseEnter={(e) => { e.target.style.background = '#A93226' }}
            onMouseLeave={(e) => { e.target.style.background = '#C0392B' }}
          >
            Xóa
          </button>
        </div>
      </div>

      {/* Inline animations */}
      <style>{`
        @keyframes fadeIn { from { opacity: 0 } to { opacity: 1 } }
        @keyframes slideUp { from { opacity: 0; transform: translateY(16px) } to { opacity: 1; transform: translateY(0) } }
      `}</style>
    </div>
  )
}

/* ── Helper: fetch wishlist IDs from API ─────────────────── */
async function fetchWishlistIds() {
  try {
    const ids = await wishlistService.getWishlistIds()
    return new Set(ids)
  } catch (error) {
    console.error('Failed to load wishlist:', error)
    return null // signal that fetch failed, don't overwrite existing state
  }
}

/* ── Provider (only component export from this file) ──────── */
export function WishlistProvider({ children }) {
  const [wishlistIds, setWishlistIds] = useState(new Set())
  const [pendingRemoveId, setPendingRemoveId] = useState(null)

  // Initial load: fetch wishlist IDs on mount.
  // Using an inline async IIFE so that setState is called inside an async
  // callback, not synchronously in the effect body.
  useEffect(() => {
    let cancelled = false
    ;(async () => {
      const ids = await fetchWishlistIds()
      if (!cancelled && ids) setWishlistIds(ids)
    })()
    return () => { cancelled = true }
  }, [])

  // Re-sync helper for use in event handlers (not in effects)
  const syncWishlist = useCallback(async () => {
    const ids = await fetchWishlistIds()
    if (ids) setWishlistIds(ids)
  }, [])

  /* ── Add to wishlist (instant) ─────────────────────────── */
  const addToWishlist = useCallback(async (productId) => {
    setWishlistIds((prev) => new Set(prev).add(productId))
    try {
      await wishlistService.addToWishlist(productId)
    } catch (error) {
      console.error('Failed to add to wishlist:', error)
      await syncWishlist()
    }
  }, [syncWishlist])

  /* ── Remove from wishlist (after confirm) ──────────────── */
  const doRemove = useCallback(async (productId) => {
    setWishlistIds((prev) => {
      const s = new Set(prev)
      s.delete(productId)
      return s
    })
    try {
      await wishlistService.removeFromWishlist(productId)
    } catch (error) {
      console.error('Failed to remove from wishlist:', error)
      await syncWishlist()
    }
  }, [syncWishlist])

  /* ── Toggle: add instantly, remove with confirm ────────── */
  const toggleWishlist = useCallback((productId) => {
    if (wishlistIds.has(productId)) {
      setPendingRemoveId(productId) // show confirm modal
    } else {
      addToWishlist(productId)
    }
  }, [wishlistIds, addToWishlist])

  const handleConfirmRemove = useCallback(() => {
    if (pendingRemoveId) {
      doRemove(pendingRemoveId)
      setPendingRemoveId(null)
    }
  }, [pendingRemoveId, doRemove])

  const handleCancelRemove = useCallback(() => {
    setPendingRemoveId(null)
  }, [])

  const isWishlisted = useCallback((productId) => {
    return wishlistIds.has(productId)
  }, [wishlistIds])

  const value = useMemo(() => ({
    wishlistIds,
    toggleWishlist,
    isWishlisted,
    syncWishlist
  }), [wishlistIds, toggleWishlist, isWishlisted, syncWishlist])

  return (
    <WishlistContext.Provider value={value}>
      {children}
      {pendingRemoveId && (
        <ConfirmModal
          onConfirm={handleConfirmRemove}
          onCancel={handleCancelRemove}
        />
      )}
    </WishlistContext.Provider>
  )
}
