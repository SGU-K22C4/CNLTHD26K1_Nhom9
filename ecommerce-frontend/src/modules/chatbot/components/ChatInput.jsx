import { SendHorizonal } from 'lucide-react'
import { useState } from 'react'

export default function ChatInput({
  onSend,
  placeholder = 'Nhập câu hỏi của bạn...',
  disabled = false,
  compact = false,
}) {
  const [value, setValue] = useState('')

  const submit = async () => {
    const message = value.trim()
    if (!message || disabled) return
    const sent = await onSend(message)
    if (sent !== null) {
      setValue('')
    }
  }

  return (
    <div className={`flex items-end gap-2 rounded-xl border border-[#d3d3d3] bg-white px-3 py-2 ${compact ? 'min-h-14' : 'min-h-16'}`}>
      <textarea
        value={value}
        onChange={(event) => setValue(event.target.value)}
        onKeyDown={(event) => {
          if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault()
            submit()
          }
        }}
        disabled={disabled}
        rows={1}
        placeholder={placeholder}
        className="max-h-36 min-h-8 flex-1 resize-none bg-transparent text-sm text-[#252525] outline-none placeholder:text-[#9a9a9a]"
      />

      <button
        type="button"
        onClick={submit}
        disabled={disabled || !value.trim()}
        className="inline-flex h-9 w-9 items-center justify-center rounded-lg bg-[#141414] text-white transition hover:bg-black disabled:cursor-not-allowed disabled:bg-[#c8c8c8]"
        aria-label="Gửi tin nhắn"
      >
        <SendHorizonal size={16} />
      </button>
    </div>
  )
}