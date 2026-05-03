import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { loyaltyService } from '../services/loyaltyService'
import { formatCurrency } from '../../../shared/utils/format'

const MEMBERSHIP_TIERS = [
  {
    id: 'tier-1',
    name: 'Bronze',
    minSpending: 0,
    discountPercent: 0,
    pointRate: 1.0,
  },
  {
    id: 'tier-2',
    name: 'Silver',
    minSpending: 5000000,
    discountPercent: 3,
    pointRate: 1.2,
  },
  {
    id: 'tier-3',
    name: 'Gold',
    minSpending: 20000000,
    discountPercent: 5,
    pointRate: 1.5,
  },
  {
    id: 'tier-4',
    name: 'Platinum',
    minSpending: 50000000,
    discountPercent: 8,
    pointRate: 2.0,
  },
]

function formatDate(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  return new Intl.DateTimeFormat('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(date)
}

function getTransactionTypeLabel(type) {
  const map = {
    EARN_ORDER: 'Cộng điểm từ đơn hàng',
    EARN_REVIEW: 'Cộng điểm từ đánh giá',
    REDEEM: 'Sử dụng điểm',
    REFUND: 'Hoàn điểm',
  }
  return map[type] || type || '-'
}

function getTierProgress(wallet) {
  if (!wallet) {
    return {
      currentTier: null,
      nextTier: null,
      amountToNextTier: 0,
    }
  }

  const totalSpending = Number(wallet.totalSpending) || 0
  const currentTier = MEMBERSHIP_TIERS.find((tier) => tier.name === wallet.tierName)
    || MEMBERSHIP_TIERS
      .slice()
      .reverse()
      .find((tier) => totalSpending >= tier.minSpending)
    || MEMBERSHIP_TIERS[0]

  const currentTierIndex = MEMBERSHIP_TIERS.findIndex((tier) => tier.name === currentTier.name)
  const nextTier = currentTierIndex >= 0 && currentTierIndex < MEMBERSHIP_TIERS.length - 1
    ? MEMBERSHIP_TIERS[currentTierIndex + 1]
    : null

  const amountToNextTier = nextTier ? Math.max(nextTier.minSpending - totalSpending, 0) : 0

  return {
    currentTier,
    nextTier,
    amountToNextTier,
  }
}

export default function LoyaltyWalletPage() {
  const [loading, setLoading] = useState(true)
  const [warning, setWarning] = useState('')
  const [wallet, setWallet] = useState(null)
  const [pointTransactions, setPointTransactions] = useState([])

  const { currentTier, nextTier, amountToNextTier } = getTierProgress(wallet)

  useEffect(() => {
    let mounted = true

    const loadWalletData = async () => {
      setLoading(true)
      setWarning('')

      const [walletResult, txResult] = await Promise.allSettled([
        loyaltyService.getWallet(),
        loyaltyService.getTransactions(20),
      ])

      if (!mounted) return

      if (walletResult.status === 'fulfilled') {
        setWallet(walletResult.value)
      } else {
        setWallet(null)
        setWarning('Không tải được thông tin ví điểm lúc này.')
      }

      if (txResult.status === 'fulfilled') {
        setPointTransactions(Array.isArray(txResult.value) ? txResult.value : [])
      } else {
        setPointTransactions([])
      }

      setLoading(false)
    }

    loadWalletData()

    return () => {
      mounted = false
    }
  }, [])

  return (
    <div className="min-h-screen bg-[#FAFAFA] py-8">
      <div className="max-w-screen-xl mx-auto px-4 sm:px-6">
        <div className="flex items-start justify-between gap-4 flex-wrap mb-6">
          <div>
            <h1 className="text-[24px] font-semibold text-[#202020]">Ví điểm tích lũy</h1>
            <p className="text-[13px] text-[#666] mt-1">Theo dõi số dư điểm, hạng thành viên và lịch sử giao dịch điểm của bạn.</p>
          </div>
          <Link to="/orders" className="text-[12px] uppercase tracking-[0.08em] border border-[#D9D9D9] px-4 h-10 inline-flex items-center hover:border-[#202020] transition-colors">
            Xem đơn hàng
          </Link>
        </div>

        {warning && <p className="text-[13px] text-amber-600 mb-4">{warning}</p>}

        {loading ? (
          <p className="text-[13px] text-[#666]">Đang tải ví điểm...</p>
        ) : (
          <section className="mb-6 border border-[#E8E8E8] bg-white p-5">
            <h2 className="text-[16px] font-semibold text-[#202020] mb-3">Thông tin ví</h2>
            {wallet ? (
              <>
                <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                  <div className="border border-[#EFEFEF] p-3">
                    <p className="text-[12px] text-[#666]">Số dư điểm</p>
                    <p className="text-[22px] font-semibold text-[#202020] mt-1">{Number(wallet?.currentPoints) || 0}</p>
                  </div>
                  <div className="border border-[#EFEFEF] p-3">
                    <p className="text-[12px] text-[#666]">Hạng thành viên</p>
                    <p className="text-[22px] font-semibold text-[#202020] mt-1">{wallet?.tierName || '-'}</p>
                  </div>
                  <div className="border border-[#EFEFEF] p-3">
                    <p className="text-[12px] text-[#666]">Tổng chi tiêu</p>
                    <p className="text-[22px] font-semibold text-[#202020] mt-1">{formatCurrency(Number(wallet?.totalSpending) || 0)}</p>
                  </div>
                  <div className="border border-[#EFEFEF] p-3">
                    <p className="text-[12px] text-[#666]">Tỷ lệ quy đổi</p>
                    <p className="text-[22px] font-semibold text-[#202020] mt-1">1 điểm = {formatCurrency(Number(wallet?.pointToVnd) || 0)}</p>
                  </div>
                </div>

                {pointTransactions.length > 0 ? (
                  <div className="mt-4 overflow-x-auto">
                    <table className="min-w-full border border-[#EFEFEF] text-[12px]">
                      <thead className="bg-[#FAFAFA]">
                        <tr>
                          <th className="text-left p-2 border-b border-[#EFEFEF]">Thời gian</th>
                          <th className="text-left p-2 border-b border-[#EFEFEF]">Loại</th>
                          <th className="text-right p-2 border-b border-[#EFEFEF]">Điểm</th>
                          <th className="text-left p-2 border-b border-[#EFEFEF]">Mô tả</th>
                        </tr>
                      </thead>
                      <tbody>
                        {pointTransactions.map((tx) => (
                          <tr key={tx.transactionId}>
                            <td className="p-2 border-b border-[#F5F5F5]">{formatDate(tx.createdAt)}</td>
                            <td className="p-2 border-b border-[#F5F5F5]">{getTransactionTypeLabel(tx.type)}</td>
                            <td className={`p-2 border-b border-[#F5F5F5] text-right font-semibold ${(Number(tx.points) || 0) >= 0 ? 'text-[#0f766e]' : 'text-[#b91c1c]'}`}>
                              {(Number(tx.points) || 0) > 0 ? '+' : ''}{Number(tx.points) || 0}
                            </td>
                            <td className="p-2 border-b border-[#F5F5F5]">{tx.description || tx.refId || '-'}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <p className="text-[13px] text-[#666] mt-4">Bạn chưa có giao dịch điểm nào.</p>
                )}
              </>
            ) : (
              <p className="text-[13px] text-[#666]">Chưa có thông tin ví điểm.</p>
            )}
          </section>
        )}

        <section className="mb-6 border border-[#E8E8E8] bg-white p-5">
          <h2 className="text-[16px] font-semibold text-[#202020] mb-3">Cơ chế hạng thành viên</h2>
          <p className="text-[13px] text-[#666] mb-4">
            Hệ thống xác định hạng dựa trên tổng chi tiêu tích lũy. Hạng càng cao thì ưu đãi giảm giá và hệ số tích điểm càng lớn.
          </p>

          {wallet && (
            <div className="mb-4 p-3 border border-[#EFEFEF] bg-[#FAFAFA]">
              <p className="text-[13px] text-[#404040]">
                Hạng hiện tại: <span className="font-semibold">{wallet.tierName || '-'}</span>
              </p>
              {nextTier ? (
                <p className="text-[13px] text-[#404040] mt-1">
                  Còn thiếu <span className="font-semibold">{formatCurrency(amountToNextTier)}</span> để lên hạng <span className="font-semibold">{nextTier.name}</span>.
                </p>
              ) : (
                <p className="text-[13px] text-[#0f766e] mt-1 font-medium">
                  Bạn đã đạt hạng cao nhất.
                </p>
              )}
            </div>
          )}

          <div className="overflow-x-auto">
            <table className="min-w-full border border-[#EFEFEF] text-[12px]">
              <thead className="bg-[#FAFAFA]">
                <tr>
                  <th className="text-left p-2 border-b border-[#EFEFEF]">Hạng</th>
                  <th className="text-left p-2 border-b border-[#EFEFEF]">Chi tiêu tối thiểu</th>
                  <th className="text-left p-2 border-b border-[#EFEFEF]">Giảm giá theo hạng</th>
                  <th className="text-left p-2 border-b border-[#EFEFEF]">Hệ số tích điểm</th>
                </tr>
              </thead>
              <tbody>
                {MEMBERSHIP_TIERS.map((tier) => {
                  const isCurrentTier = currentTier?.name === tier.name
                  return (
                    <tr key={tier.id} className={isCurrentTier ? 'bg-[#F3F8F2]' : undefined}>
                      <td className="p-2 border-b border-[#F5F5F5] font-semibold text-[#202020]">
                        {tier.name}
                        {isCurrentTier && <span className="ml-2 text-[11px] text-[#0f766e]">(Hiện tại)</span>}
                      </td>
                      <td className="p-2 border-b border-[#F5F5F5]">{formatCurrency(tier.minSpending)}</td>
                      <td className="p-2 border-b border-[#F5F5F5]">{tier.discountPercent}%</td>
                      <td className="p-2 border-b border-[#F5F5F5]">x{tier.pointRate}</td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        </section>
      </div>
    </div>
  )
}
