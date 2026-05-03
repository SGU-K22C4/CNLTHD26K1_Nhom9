import { Link } from 'react-router-dom'
import { useHomeProducts } from '../hooks/useHomeProducts'

function formatCurrency(value) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(Number(value) || 0)
}

export default function FeaturedProducts() {
  const { featuredProducts: products, loading, error } = useHomeProducts()

  return (
    <section className="mx-auto w-full max-w-[1440px] px-6 py-8 lg:px-12">
      <div className="mb-4 mt-4 flex items-center justify-between md:mt-12">
        <h2 className="text-[22px] font-extrabold md:text-[33px]">Featured Products</h2>
        <Link
          to="/products"
          className="font-medium text-[#5A6D57] transition-colors hover:text-[#4a5547] hover:underline"
        >
          View all
        </Link>
      </div>

      {loading ? (
        <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
          {Array.from({ length: 4 }).map((_, idx) => (
            <div key={idx} className="rounded-lg border border-gray-200 p-4 animate-pulse">
              <div className="mb-3 h-44 w-full rounded-md bg-gray-100" />
              <div className="h-4 w-4/5 rounded bg-gray-100" />
              <div className="mt-2 h-4 w-2/5 rounded bg-gray-100" />
            </div>
          ))}
        </div>
      ) : error ? (
        <p className="mt-4 text-sm text-red-500">{error}</p>
      ) : (
        <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
          {products.map((item) => (
            <Link
              key={item.id}
              to={`/products/${item.id}`}
              className="rounded-lg border border-gray-200 p-4 transition-shadow hover:shadow-md"
            >
              {item.image ? (
                <img
                  src={item.image}
                  alt={item.name}
                  className="mb-3 h-44 w-full rounded-md object-cover"
                />
              ) : (
                <div className="mb-3 h-44 w-full rounded-md bg-gray-100" />
              )}
              <h3 className="text-sm font-semibold text-gray-900 md:text-base line-clamp-2 min-h-10">
                {item.name}
              </h3>
              <p className="mt-1 text-sm text-gray-500">{formatCurrency(item.price)}</p>
            </Link>
          ))}
        </div>
      )}
    </section>
  )
}