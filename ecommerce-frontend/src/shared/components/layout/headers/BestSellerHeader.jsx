import { Link } from 'react-router-dom';

function BestSellerHeader() {
  return (
    <div className="mb-4 mt-4 flex items-center justify-between md:mt-16">
      <h2 className="text-[22px] font-extrabold md:text-[33px]">
        Best Sellers
      </h2>
      
      <Link 
        to="/collection/best-sellers" 
        className="font-medium text-[#5A6D57] transition-colors hover:text-[#4a5547] hover:underline"
      >
        View all
      </Link>
    </div>
  );
}

export default BestSellerHeader;