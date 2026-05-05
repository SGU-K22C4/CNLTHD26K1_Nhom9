import { useState, useEffect } from 'react'
import { loyaltyService } from '../services/loyaltyService'

/**
 * Custom hook for loyalty points management during checkout.
 * Handles wallet balance fetching, point application preview, clear, and max usage.
 */
export function useLoyaltyPoints() {
  const [walletPoints, setWalletPoints] = useState(0)
  const [pointInput, setPointInput] = useState('')
  const [appliedPoints, setAppliedPoints] = useState(0)
  const [loyaltyDiscount, setLoyaltyDiscount] = useState(0)
  const [loyaltyMessage, setLoyaltyMessage] = useState('')
  const [loyaltyError, setLoyaltyError] = useState('')
  const [applyingPoints, setApplyingPoints] = useState(false)

  /* ── Fetch wallet balance on mount ── */
  useEffect(() => {
    let mounted = true
    loyaltyService.getWallet()
      .then((wallet) => {
        if (!mounted) return
        setWalletPoints(Number(wallet?.currentPoints) || 0)
      })
      .catch((err) => {
        if (!mounted) return
        setWalletPoints(0)
        setLoyaltyError(err?.message || 'Không tải được số dư điểm')
      })

    return () => {
      mounted = false
    }
  }, [])

  /**
   * Apply loyalty points with server-side preview validation.
   * @param {number} subtotal - Current order subtotal for preview
   */
  const handleApplyPoints = async (subtotal) => {
    const requested = Number(pointInput)
    if (!Number.isInteger(requested) || requested <= 0) {
      setLoyaltyError('Số điểm không hợp lệ')
      return
    }

    setApplyingPoints(true)
    setLoyaltyError('')
    setLoyaltyMessage('')

    try {
      const preview = await loyaltyService.previewRedeem({
        orderAmount: subtotal,
        requestedPoints: requested,
      })

      if (!preview?.valid) {
        setAppliedPoints(0)
        setLoyaltyDiscount(0)
        setLoyaltyError(preview?.message || 'Không thể áp dụng điểm')
        return
      }

      setAppliedPoints(Number(preview?.appliedPoints) || 0)
      setLoyaltyDiscount(Number(preview?.discountAmount) || 0)
      setWalletPoints(Number(preview?.currentPoints) || walletPoints)
      setLoyaltyMessage(preview?.message || 'Áp dụng điểm thành công')
      setLoyaltyError('')
    } catch (err) {
      setAppliedPoints(0)
      setLoyaltyDiscount(0)
      setLoyaltyError(err?.message || 'Không thể áp dụng điểm')
    } finally {
      setApplyingPoints(false)
    }
  }

  const handleClearPoints = () => {
    setPointInput('')
    setAppliedPoints(0)
    setLoyaltyDiscount(0)
    setLoyaltyMessage('')
    setLoyaltyError('')
  }

  const handleUseMaxPoints = () => {
    if (walletPoints <= 0) {
      setLoyaltyError('Bạn chưa có điểm để sử dụng')
      return
    }
    setPointInput(String(walletPoints))
    setLoyaltyError('')
  }

  return {
    walletPoints,
    pointInput,
    setPointInput,
    appliedPoints,
    loyaltyDiscount,
    loyaltyMessage,
    loyaltyError,
    setLoyaltyError,
    setLoyaltyMessage,
    applyingPoints,
    handleApplyPoints,
    handleClearPoints,
    handleUseMaxPoints,
  }
}
