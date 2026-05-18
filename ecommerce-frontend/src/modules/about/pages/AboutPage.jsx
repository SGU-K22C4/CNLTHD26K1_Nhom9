import { Link } from 'react-router-dom'
import { Leaf, Truck, ShieldCheck, Heart } from 'lucide-react'

const VALUES = [
  {
    icon: Leaf,
    title: 'Bền vững',
    desc: 'Chúng tôi ưu tiên sử dụng chất liệu thân thiện với môi trường và quy trình sản xuất có trách nhiệm.',
  },
  {
    icon: ShieldCheck,
    title: 'Chất lượng',
    desc: 'Mỗi sản phẩm đều được kiểm tra kỹ lưỡng, đảm bảo tiêu chuẩn cao nhất trước khi đến tay khách hàng.',
  },
  {
    icon: Heart,
    title: 'Phong cách',
    desc: 'Thiết kế hiện đại, tối giản nhưng vẫn giữ nét riêng — phù hợp với mọi dịp trong cuộc sống.',
  },
  {
    icon: Truck,
    title: 'Giao hàng toàn quốc',
    desc: 'Miễn phí vận chuyển cho tất cả đơn hàng. Đổi trả dễ dàng trong vòng 30 ngày.',
  },
]

const TEAM = [
  { name: 'Phúc Mạnh', role: 'Team Lead & Backend Developer' },
  { name: 'Thành Viên 2', role: 'Frontend Developer' },
  { name: 'Thành Viên 3', role: 'UI/UX Designer' },
  { name: 'Thành Viên 4', role: 'Backend Developer' },
  { name: 'Thành Viên 5', role: 'DevOps & Testing' },
]

export default function AboutPage() {
  return (
    <div className="bg-[#F4F5F3] pb-16 pt-6 sm:pt-10">
      <div className="mx-auto w-full max-w-[1220px] px-4 sm:px-6 lg:px-10">

        {/* ── Breadcrumb ─────────────────────────────────────── */}
        <nav className="mb-7 flex items-center gap-2 text-[13px] text-[#7B7B7B]">
          <Link to="/" className="transition-colors hover:text-[#202020]">Home</Link>
          <span>/</span>
          <span className="text-[#202020]">About Us</span>
        </nav>

        {/* ── Hero ───────────────────────────────────────────── */}
        <section className="mb-12">
          <h1 className="text-[40px] font-semibold leading-none text-[#202020] sm:text-[46px] lg:text-[54px]">
            About Us
          </h1>

          <div className="mt-6 bg-[#E9EBE8] px-5 py-6 sm:px-8 sm:py-8">
            <p className="text-[15px] leading-[1.8] text-[#3E3E3E] sm:text-[16px] lg:text-[18px]">
              <strong className="text-[#202020]">Modimal</strong> là thương hiệu thời trang được thành lập với sứ mệnh
              mang đến những bộ trang phục chất lượng cao, thiết kế tinh tế và giá cả hợp lý cho người Việt.
              Chúng tôi tin rằng thời trang không chỉ là vẻ bề ngoài — mà là cách bạn thể hiện bản thân mỗi ngày.
            </p>
            <p className="mt-4 text-[15px] leading-[1.8] text-[#3E3E3E] sm:text-[16px] lg:text-[18px]">
              Với hệ thống công nghệ hiện đại, Modimal cam kết mang đến trải nghiệm mua sắm trực tuyến tiện lợi,
              nhanh chóng và an toàn — từ lúc bạn chọn sản phẩm đến khi nhận hàng tận tay.
            </p>
          </div>
        </section>

        {/* ── Values ─────────────────────────────────────────── */}
        <section className="mb-14">
          <h2 className="mb-6 text-center text-[24px] font-semibold text-[#202020] sm:text-[28px]">
            Giá trị cốt lõi
          </h2>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {VALUES.map((item) => {
              const Icon = item.icon
              return (
                <div
                  key={item.title}
                  className="bg-[#EAEEEA] px-6 py-7 text-center transition-shadow hover:shadow-md"
                >
                  <Icon size={28} className="mx-auto mb-3 text-[#5A6D57]" />
                  <h3 className="text-[16px] font-semibold text-[#202020]">{item.title}</h3>
                  <p className="mt-2 text-[13px] leading-relaxed text-[#555]">{item.desc}</p>
                </div>
              )
            })}
          </div>
        </section>

        {/* ── Story ──────────────────────────────────────────── */}
        <section className="mb-14">
          <div className="grid grid-cols-1 gap-8 lg:grid-cols-2">
            {/* Image placeholder using a muted bg */}
            <div className="flex items-center justify-center bg-[#DDE1DB] min-h-[320px]">
              <span className="text-[64px] font-bold text-[#C0C7BD] select-none tracking-widest">M</span>
            </div>

            <div className="flex flex-col justify-center">
              <h2 className="mb-4 text-[24px] font-semibold text-[#202020] sm:text-[28px]">
                Câu chuyện của chúng tôi
              </h2>
              <p className="text-[14px] leading-[1.8] text-[#555] sm:text-[15px]">
                Modimal ra đời từ niềm đam mê thời trang và công nghệ. Chúng tôi bắt đầu với một ý tưởng đơn giản:
                tạo ra một nền tảng mua sắm thời trang trực tuyến mà ở đó khách hàng có thể dễ dàng tìm thấy
                những sản phẩm phù hợp với phong cách cá nhân.
              </p>
              <p className="mt-4 text-[14px] leading-[1.8] text-[#555] sm:text-[15px]">
                Dự án này được xây dựng bởi Nhóm 9 — lớp CNLTHD26K1 — như một phần của đồ án môn học,
                áp dụng kiến trúc Microservices với Spring Boot, React, và tích hợp AI thông minh.
              </p>
              <p className="mt-4 text-[14px] leading-[1.8] text-[#555] sm:text-[15px]">
                Mục tiêu của chúng tôi là xây dựng một hệ thống thương mại điện tử hoàn chỉnh,
                có khả năng mở rộng, và mang lại trải nghiệm người dùng tốt nhất.
              </p>
            </div>
          </div>
        </section>

        {/* ── Team ───────────────────────────────────────────── */}
        <section className="mb-10">
          <h2 className="mb-6 text-center text-[24px] font-semibold text-[#202020] sm:text-[28px]">
            Đội ngũ phát triển — Nhóm 9
          </h2>

          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
            {TEAM.map((member) => (
              <div
                key={member.name}
                className="bg-[#EAEEEA] px-4 py-6 text-center transition-shadow hover:shadow-md"
              >
                {/* Avatar placeholder */}
                <div className="mx-auto mb-3 flex h-14 w-14 items-center justify-center rounded-full bg-[#5A6D57] text-[18px] font-bold text-white">
                  {member.name.charAt(0)}
                </div>
                <h3 className="text-[14px] font-semibold text-[#202020]">{member.name}</h3>
                <p className="mt-1 text-[12px] text-[#777]">{member.role}</p>
              </div>
            ))}
          </div>
        </section>

        {/* ── CTA ────────────────────────────────────────────── */}
        <section className="bg-[#5A6D57] px-6 py-10 text-center sm:px-10 sm:py-12">
          <h2 className="text-[22px] font-semibold text-white sm:text-[26px]">
            Khám phá bộ sưu tập mới nhất
          </h2>
          <p className="mx-auto mt-3 max-w-[520px] text-[14px] leading-relaxed text-[#D6DDD4]">
            Tìm kiếm phong cách riêng của bạn từ hàng trăm sản phẩm thời trang nam &amp; nữ.
          </p>
          <Link
            to="/products"
            className="mt-6 inline-block border border-white px-8 py-3 text-[14px] font-medium text-white transition-colors hover:bg-white hover:text-[#5A6D57]"
          >
            Xem sản phẩm
          </Link>
        </section>
      </div>
    </div>
  )
}
