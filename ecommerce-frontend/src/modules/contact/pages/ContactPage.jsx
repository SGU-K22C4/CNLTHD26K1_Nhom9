import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Mail, MessageSquareText, IdCard, ChevronRight, Plus, Minus, X, ChevronDown } from 'lucide-react'

const initialForm = {
  fullName: '',
  email: '',
  subject: '',
  orderNumber: '',
  message: '',
  acceptedPolicy: false,
}

const subjectOptions = [
  'Order Support',
  'Shipping Question',
  'Payment Issue',
  'Product Information',
  'Returns & Refunds',
]

function InputLine({ type = 'text', name, value, onChange, placeholder }) {
  return (
    <input
      type={type}
      name={name}
      value={value}
      onChange={onChange}
      placeholder={placeholder}
      className="w-full border-0 border-b border-[#B7B7B7] bg-transparent px-0 pb-2 pt-1 text-[14px] text-[#232323] outline-none placeholder:text-[#8E8E8E] focus:border-[#5A6D57]"
    />
  )
}

function ContactForm({ form, onChange, onCheck, onSubmit, compact = false }) {
  return (
    <form onSubmit={onSubmit} className="space-y-4">
      <InputLine
        name="fullName"
        value={form.fullName}
        onChange={onChange}
        placeholder="Full Name"
      />

      <InputLine
        type="email"
        name="email"
        value={form.email}
        onChange={onChange}
        placeholder="Email"
      />

      <div className="relative">
        <select
          name="subject"
          value={form.subject}
          onChange={onChange}
          className="w-full appearance-none border-0 border-b border-[#B7B7B7] bg-transparent px-0 pb-2 pt-1 text-[14px] text-[#232323] outline-none focus:border-[#5A6D57]"
        >
          <option value="">Subject</option>
          {subjectOptions.map((subject) => (
            <option key={subject} value={subject}>
              {subject}
            </option>
          ))}
        </select>
        <ChevronDown size={16} className="pointer-events-none absolute right-1 top-1.5 text-[#7A7A7A]" />
      </div>

      <InputLine
        name="orderNumber"
        value={form.orderNumber}
        onChange={onChange}
        placeholder="Order Number"
      />

      <InputLine
        name="message"
        value={form.message}
        onChange={onChange}
        placeholder="Message"
      />

      <label className="flex items-start gap-2 pt-1 text-[12px] leading-[1.45] text-[#404040]">
        <input
          type="checkbox"
          name="acceptedPolicy"
          checked={form.acceptedPolicy}
          onChange={onCheck}
          className="mt-0.5 h-4 w-4 rounded-none border border-[#B7B7B7] accent-[#5A6D57]"
        />
        <span>I Have Read And Understood The Contact Us Privacy And Policy.</span>
      </label>

      <div className={compact ? 'pt-2' : 'pt-4'}>
        <button
          type="submit"
          className="h-[46px] w-full bg-[#5A6D57] text-[14px] font-medium text-white transition-colors hover:bg-[#4D5F4A]"
        >
          Send
        </button>
      </div>
    </form>
  )
}

function SupportCard({ icon: Icon, title, description, actionText }) {
  return (
    <article className="bg-[#EAEEEA] px-6 py-6 text-center">
      <Icon size={20} className="mx-auto mb-3 text-[#1F1F1F]" />
      <h3 className="text-[23px] font-semibold text-[#202020]">{title}</h3>
      <p className="mt-2 text-[13px] text-[#555]">{description}</p>
      <button
        type="button"
        className="mt-5 h-[42px] w-full border border-[#9AA498] bg-[#F8F9F7] text-[15px] text-[#687666] transition-colors hover:bg-white"
      >
        {actionText}
      </button>
    </article>
  )
}

export default function ContactPage() {
  const [form, setForm] = useState(initialForm)
  const [activeSupport, setActiveSupport] = useState('chat')
  const [writeUsOpen, setWriteUsOpen] = useState(false)

  useEffect(() => {
    if (!writeUsOpen) return undefined

    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'

    return () => {
      document.body.style.overflow = previousOverflow
    }
  }, [writeUsOpen])

  const handleFieldChange = (e) => {
    const { name, value } = e.target
    setForm((prev) => ({ ...prev, [name]: value }))
  }

  const handleCheck = (e) => {
    const { name, checked } = e.target
    setForm((prev) => ({ ...prev, [name]: checked }))
  }

  const handleSubmit = (e) => {
    e.preventDefault()
  }

  const supportItems = [
    {
      id: 'chat',
      icon: MessageSquareText,
      title: 'Chat With Us',
      body: 'We Are Here And Ready To Chat',
      action: 'Start Chat',
    },
    {
      id: 'call',
      icon: IdCard,
      title: 'Call Us',
      body: "We're Here To Talk To You",
      action: '+1(929)460-3208',
    },
    {
      id: 'email',
      icon: Mail,
      title: 'Email Us',
      body: 'You Are Welcome To Send Us An Email',
      action: 'Send Email',
    },
  ]

  return (
    <div className="bg-[#F4F5F3] pb-12 pt-6 sm:pt-10 lg:pt-12">
      <div className="mx-auto w-full max-w-[1220px] px-4 sm:px-6 lg:px-10">
        <nav className="mb-7 flex items-center gap-2 text-[13px] text-[#7B7B7B] sm:text-[14px]">
          <Link to="/" className="transition-colors hover:text-[#202020]">Home</Link>
          <span>/</span>
          <span className="text-[#202020]">Contact Us</span>
        </nav>

        <h1 className="text-[40px] font-semibold leading-none text-[#202020] sm:text-[46px] lg:text-[54px]">Contact Us</h1>

        <section className="mt-6 bg-[#E9EBE8] px-5 py-5 text-[14px] leading-[1.8] text-[#3E3E3E] sm:px-7 sm:py-6 sm:text-[15px] lg:text-[25px]">
          <p>
            We Always Love Hearing From Our Customers! Please Do Not Hesitate To Contact Us Should You Have Any
            Questions Regarding Our Products And Sizing Recommendations Or Inquiries About Your Current Order.
          </p>
          <p className="mt-4">
            Contact Our Customer Care Team Through The Contact Form Below, Email Us At Hello@Modimal.Com Or Live
            Chat With Us Via Our Chat Widget On The Bottom Right Hand Corner Of This Page.
          </p>
          <p className="mt-4">We Will Aim To Respond To You Within 1-2 Business Days.</p>
        </section>

        <section className="mt-9 hidden lg:block">
          <div className="mb-7 flex items-center gap-2 text-[29px] font-semibold text-[#202020]">
            <Mail size={24} />
            <h2>Write Us</h2>
          </div>

          <h3 className="mb-4 text-[28px] font-semibold text-[#202020]">Your Information</h3>
          <div className="max-w-[760px]">
            <ContactForm
              form={form}
              onChange={handleFieldChange}
              onCheck={handleCheck}
              onSubmit={handleSubmit}
            />
          </div>

          <div className="mt-12 grid grid-cols-3 gap-4 xl:gap-6">
            {supportItems.map((item) => (
              <SupportCard
                key={item.id}
                icon={item.icon}
                title={item.title}
                description={item.body}
                actionText={item.action}
              />
            ))}
          </div>
        </section>

        <section className="mt-8 lg:hidden">
          <div className="border border-[#D6DAD5] bg-[#F8F9F7]">
            <button
              type="button"
              onClick={() => setWriteUsOpen(true)}
              className="flex w-full items-center justify-between px-4 py-4 text-left"
            >
              <span className="flex items-center gap-2 text-[18px] font-semibold text-[#202020]">
                <Mail size={18} />
                Write Us
              </span>
              <ChevronRight size={18} className="text-[#202020]" />
            </button>
          </div>

          <div className="mt-3 space-y-0">
            {supportItems.map((item, index) => {
              const isOpen = activeSupport === item.id
              const Icon = item.icon
              return (
                <article key={item.id} className={`${index > 0 ? 'mt-1' : ''} border border-[#D6DAD5] bg-[#E6EAE5]`}>
                  <button
                    type="button"
                    onClick={() => setActiveSupport((prev) => (prev === item.id ? '' : item.id))}
                    className="flex w-full items-center justify-between px-4 py-3"
                  >
                    <span className="flex items-center gap-2 text-[17px] font-semibold text-[#202020]">
                      <Icon size={18} />
                      {item.id === 'chat' && isOpen ? 'Chat With Us/Extend' : item.title}
                    </span>
                    {isOpen ? <Minus size={18} /> : <Plus size={18} />}
                  </button>

                  {isOpen && (
                    <div className="bg-[#EFF1ED] px-4 pb-4">
                      <p className="py-3 text-center text-[14px] text-[#525252]">{item.body}</p>
                      <button
                        type="button"
                        className="h-[40px] w-full border border-[#97A291] bg-[#F8F9F7] text-[14px] text-[#687666]"
                      >
                        {item.action}
                      </button>
                    </div>
                  )}
                </article>
              )
            })}
          </div>
        </section>
      </div>

      {writeUsOpen && (
        <div className="fixed inset-0 z-50 flex items-start justify-center bg-black/45 px-3 pt-16 sm:pt-20">
          <div className="w-full max-w-[560px] bg-[#F8F8F8] px-4 pb-5 pt-4 sm:px-6 sm:pt-5">
            <div className="mb-4 flex items-center justify-between">
              <h2 className="flex items-center gap-2 text-[28px] font-semibold text-[#202020]">
                <Mail size={20} />
                Write Us
              </h2>
              <button
                type="button"
                onClick={() => setWriteUsOpen(false)}
                className="p-1 text-[#202020]"
                aria-label="Close write us form"
              >
                <X size={22} />
              </button>
            </div>

            <ContactForm
              form={form}
              onChange={handleFieldChange}
              onCheck={handleCheck}
              onSubmit={handleSubmit}
              compact
            />
          </div>
        </div>
      )}
    </div>
  )
}
