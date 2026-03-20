import BestSellerHeader from "../../../shared/components/layout/headers/BestSellerHeader";

// xử lý sau: thay bằng data từ backend
const mockItems = [
    { id: 1, name: "Classic Blazer", price: "1,290,000 VND" },
    { id: 2, name: "Linen Shirt", price: "790,000 VND" },
    { id: 3, name: "Wide Leg Pants", price: "990,000 VND" },
    { id: 4, name: "Minimal Dress", price: "1,150,000 VND" },
];

const BestSellers = () => {
    return (
        <section className="mx-auto w-full max-w-[1440px] px-6 py-10 lg:px-12">
            <BestSellerHeader />
            <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
                {mockItems.map((item) => (
                    <div key={item.id} className="rounded-lg border border-gray-200 p-4">
                        <div className="mb-3 h-40 w-full rounded-md bg-gray-100" />
                        <h3 className="text-sm font-semibold text-gray-900 md:text-base">
                            {item.name}
                        </h3>
                        <p className="mt-1 text-sm text-gray-500">{item.price}</p>
                    </div>
                ))}
            </div>
            <p className="mt-6 text-sm text-gray-500">Data will be updated later.</p>
        </section>
    )
}

export default BestSellers;
