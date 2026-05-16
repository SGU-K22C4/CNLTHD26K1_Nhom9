import { useState } from 'react'
import { orderService } from '../services/orderService'
import { paymentService } from '../services/paymentService'
import { chatbotService } from '../../chatbot/services/chatbotService'

/**
 * Build the order payload matching the DB Order entity structure.
 * Pure function — no side effects.
 *
 * @param {Object} form - Checkout form state
 * @param {Array} items - Cart items
 * @param {number} appliedPoints - Redeemed loyalty points count
 * @returns {Object} Order payload for POST /api/v1/orders
 */
export function buildOrderPayload(form, items, appliedPoints) {
  return {
    recipientName: form.firstName?.trim() || '',
    recipientPhone: form.phone,
    shippingAddress: [form.street, form.ward, form.city]
      .filter(Boolean).join(', '),
    paymentMethod: form.paymentMethod,
    note: form.note || null,
    email: form.email || null,
    usedPoints: appliedPoints,
    items: items.map(item => ({
      productId: item.productId || item.id,
      productName: item.name,
      productSlug: item.slug || item.productSlug || '',
      imageUrl: item.imageUrl || item.image || '',
      color: item.color,
      size: item.size,
      quantity: item.quantity,
      unitPrice: item.price,
    })),
  }
}

/**
 * Custom hook for checkout submission orchestration.
 * Handles order creation, VNPay redirect, and COD navigation.
 */
export function useCheckoutSubmit() {
  const [isSubmitting, setIsSubmitting] = useState(false)

  const buildAttributionMetadata = (items, form, savedOrder, attributedItems = []) => ({
    paymentMethod: form.paymentMethod,
    orderId: savedOrder?.id || '',
    orderNumber: savedOrder?.orderNumber || '',
    totalItems: items.reduce((sum, item) => sum + (Number(item.quantity) || 0), 0),
    subtotal: items.reduce((sum, item) => sum + ((Number(item.price) || 0) * (Number(item.quantity) || 0)), 0),
    attributedProductIds: attributedItems.map((item) => item?.productId).filter(Boolean),
    attributedItems: attributedItems.map((item) => ({
      productId: item?.productId || '',
      productName: item?.productName || '',
      sourceMessageId: item?.sourceMessageId || '',
      surface: item?.surface || '',
      quantity: item?.quantity || 1,
    })),
  })

  /**
   * Submit the checkout form.
   * @param {Object} params
   * @param {Object} params.form - Form state from useCheckoutForm
   * @param {Array} params.items - Cart items from CartContext
   * @param {number} params.appliedPoints - Applied loyalty points
   * @param {string} params.pointInput - Raw point input (for pre-submit validation)
   * @param {Function} params.validateForm - Validation function from useCheckoutForm
   * @param {Function} params.setLoyaltyError - Error setter from useLoyaltyPoints
   * @param {Function} params.clearCart - Clear cart function from CartContext
   * @param {Function} params.navigate - React Router navigate function
   */
  const handleSubmit = async ({
    form,
    items,
    appliedPoints,
    pointInput,
    validateForm,
    setLoyaltyError,
    clearCart,
    navigate,
  }) => {
    if (!validateForm()) return

    // Guard: user typed points but didn't click "Áp dụng"
    if (pointInput && Number(pointInput) > 0 && appliedPoints <= 0) {
      setLoyaltyError('Hãy nhấn "Áp dụng" điểm trước khi đặt hàng')
      return
    }

    try {
      setIsSubmitting(true)
      const payload = buildOrderPayload(form, items, appliedPoints)
      const chatbotAttributions = chatbotService.getCheckoutAttributions()
      const savedOrder = await orderService.create(payload)

      if (chatbotAttributions.length > 0) {
        const primaryAttribution = chatbotAttributions[0]
        await chatbotService.sendFeedbackEvent({
          sessionId: primaryAttribution.sessionId,
          eventType: 'checkout_submit',
          sourceMessageId: primaryAttribution.sourceMessageId,
          metadata: buildAttributionMetadata(items, form, savedOrder, chatbotAttributions),
        })
      }

      // If VNPAY, redirect to VNPay payment page
      if (form.paymentMethod === 'VNPAY') {
        const { paymentUrl } = await paymentService.createVnpayPayment(savedOrder.id)
        window.location.href = paymentUrl
        return // Don't clear cart or navigate — VNPay will redirect back
      }

      // COD or other methods: clear cart and go to order detail page
      await clearCart()
      if (chatbotAttributions.length > 0) {
        const primaryAttribution = chatbotAttributions[0]
        await chatbotService.sendFeedbackEvent({
          sessionId: primaryAttribution.sessionId,
          eventType: 'order_success',
          sourceMessageId: primaryAttribution.sourceMessageId,
          metadata: buildAttributionMetadata(items, form, savedOrder, chatbotAttributions),
        })
        chatbotService.clearCheckoutAttributions()
      }
      navigate(`/orders/${savedOrder.id}?from=payment`, { replace: true })
    } catch (err) {
      console.error('Submit order failed:', err)
      alert(err?.message || 'Tạo đơn hàng thất bại. Vui lòng thử lại.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return { isSubmitting, handleSubmit }
}
