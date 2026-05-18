import { SendHorizonal } from 'lucide-react'
import { useRef, useState } from 'react'

export default function ChatInput({
  onSend,
  placeholder = 'Nhập câu hỏi của bạn...',
  disabled = false,
  compact = false,
}) {
  const [value, setValue] = useState('')
  const submitLockRef = useRef(false)

  const submit = async () => {
    const message = value.trim()
    if (!message || disabled || submitLockRef.current) return
    submitLockRef.current = true
    try {
      const sent = await onSend(message)
      if (sent !== null) {
        setValue('')
      }
    } finally {
      submitLockRef.current = false
    }
  }

  return (
    <div className={`flex items-end gap-2 rounded-[20px] border border-[#ddd2c4] bg-[#fffdf9] px-3 py-2 shadow-sm ${compact ? 'min-h-14' : 'min-h-16'}`}>
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
        className="max-h-36 min-h-8 flex-1 resize-none bg-transparent text-sm leading-6 text-[#25201a] outline-none placeholder:text-[#9f9486]"
      />

      <button
        type="button"
        onClick={submit}
        disabled={disabled || !value.trim()}
        className="inline-flex h-10 w-10 items-center justify-center rounded-full bg-[#18120f] text-white transition hover:bg-[#090909] disabled:cursor-not-allowed disabled:bg-[#c8c1b8]"
        aria-label="Gửi tin nhắn"
      >
        <SendHorizonal size={16} />
      </button>
    </div>
  )
}
