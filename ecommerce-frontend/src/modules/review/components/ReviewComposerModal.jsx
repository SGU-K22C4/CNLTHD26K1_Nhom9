import { useEffect, useMemo, useState } from 'react'
import StarRating from './StarRating'

function fileToDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result)
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

export default function ReviewComposerModal({
  submitting,
  targetItem,
  errorMessage,
  onClose,
  onSubmit,
}) {
  const [rating, setRating] = useState(5)
  const [comment, setComment] = useState('')
  const [files, setFiles] = useState([])
  const [formError, setFormError] = useState('')

  const previewUrls = useMemo(
    () => files.map((file) => URL.createObjectURL(file)),
    [files],
  )

  useEffect(() => {
    return () => {
      previewUrls.forEach((url) => URL.revokeObjectURL(url))
    }
  }, [previewUrls])

  const handlePickFiles = (event) => {
    const selected = Array.from(event.target.files || [])
    if (selected.length === 0) return
    const merged = [...files, ...selected].slice(0, 4)
    setFiles(merged)
  }

  const handleRemoveFile = (index) => {
    setFiles((prev) => prev.filter((_, fileIndex) => fileIndex !== index))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setFormError('')

    if (!targetItem) return

    if (!rating || rating < 1 || rating > 5) {
      setFormError('Vui lòng chọn số sao từ 1 đến 5')
      return
    }

    if (!comment.trim()) {
      setFormError('Vui lòng nhập nội dung đánh giá')
      return
    }

    const imageUrls = await Promise.all(files.map((file) => fileToDataUrl(file)))

    await onSubmit({
      orderId: targetItem.orderId,
      productId: String(targetItem.productId),
      rating,
      comment: comment.trim(),
      imageUrls,
    })
  }

  if (!targetItem) return null

  return (
    <div className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4">
      <div className="bg-white w-full max-w-xl max-h-[92vh] overflow-y-auto p-6">
        <div className="flex items-start justify-between gap-4 mb-4">
          <div>
            <h2 className="text-[18px] font-semibold text-[#202020]">Đánh giá sản phẩm đã mua</h2>
            <p className="text-[12px] text-[#777] mt-1">{targetItem.productName}</p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="text-[12px] uppercase tracking-[0.08em] text-[#666] hover:text-[#202020]"
          >
            Đóng
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <p className="text-[12px] uppercase tracking-[0.08em] text-[#202020] mb-2">Số sao</p>
            <StarRating value={rating} interactive onChange={setRating} size={24} />
          </div>

          <label className="block">
            <span className="text-[12px] uppercase tracking-[0.08em] text-[#202020] mb-2 block">Nội dung nhận xét</span>
            <textarea
              value={comment}
              onChange={(event) => setComment(event.target.value)}
              rows={5}
              maxLength={1200}
              placeholder="Chia sẻ trải nghiệm thực tế sau khi nhận hàng..."
              className="w-full border border-[#D9D9D9] p-3 text-[13px] leading-relaxed text-[#202020] outline-none focus:border-[#5A6D57] resize-none"
            />
          </label>

          <div>
            <p className="text-[12px] uppercase tracking-[0.08em] text-[#202020] mb-2">Hình ảnh thực tế (tùy chọn)</p>
            <label className="inline-flex items-center justify-center border border-[#D9D9D9] px-4 h-10 text-[12px] uppercase tracking-[0.08em] text-[#202020] cursor-pointer hover:border-[#202020] transition-colors">
              Chọn ảnh
              <input
                type="file"
                accept="image/*"
                multiple
                onChange={handlePickFiles}
                className="hidden"
              />
            </label>
            <p className="text-[11px] text-[#888] mt-1">Tối đa 4 ảnh.</p>

            {previewUrls.length > 0 && (
              <div className="mt-3 grid grid-cols-4 gap-2">
                {previewUrls.map((url, index) => (
                  <div key={url} className="relative">
                    <img src={url} alt={`Preview ${index + 1}`} className="w-full h-16 object-cover border border-[#E8E8E8]" />
                    <button
                      type="button"
                      onClick={() => handleRemoveFile(index)}
                      className="absolute -top-2 -right-2 h-5 w-5 bg-[#202020] text-white text-[10px] rounded-full"
                      aria-label="Remove image"
                    >
                      x
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          {(formError || errorMessage) && (
            <p className="text-[12px] text-red-500">{formError || errorMessage}</p>
          )}

          <div className="flex justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="h-11 px-5 border border-[#D9D9D9] text-[12px] uppercase tracking-[0.08em] text-[#202020]"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="h-11 px-6 bg-[#5A6D57] text-white text-[12px] uppercase tracking-[0.08em] disabled:opacity-50"
            >
              {submitting ? 'Đang gửi...' : 'Gửi đánh giá'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
