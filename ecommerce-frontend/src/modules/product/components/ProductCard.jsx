import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Box, Typography, IconButton } from '@mui/material';
import FavoriteBorderIcon from '@mui/icons-material/FavoriteBorder';
import FavoriteIcon from '@mui/icons-material/Favorite';

// xử lý sau: kết nối backend cho wishlist
export default function ProductCard({ product }) {
  const [isWishlisted, setIsWishlisted] = useState(false);
  const [currentImage, setCurrentImage] = useState(0);

  // Mock data nếu không có product prop
  const defaultProduct = {
    id: 1,
    name: 'Recycled Cotton Ribbed Tank Top',
    price: 45,
    originalPrice: null,
    images: ['/assets/images/product-1.jpg', '/assets/images/product-1-hover.jpg'],
    colors: ['#2D2D2D', '#748C70', '#C4A77D'],
    slug: 'recycled-cotton-ribbed-tank-top',
    isNew: false,
    isSale: false,
  };

  const item = product || defaultProduct;

  const handleWishlist = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsWishlisted(!isWishlisted);
    // xử lý sau: gọi API thêm vào wishlist
  };

  return (
    <Box className="group relative">
      {/* Product Image */}
      <Link to={`/product/${item.slug}`}>
        <Box
          className="relative overflow-hidden bg-gray-100"
          sx={{ aspectRatio: '3/4' }}
          onMouseEnter={() => item.images?.length > 1 && setCurrentImage(1)}
          onMouseLeave={() => setCurrentImage(0)}
        >
          <img
            src={item.images?.[currentImage] || '/assets/images/placeholder.jpg'}
            alt={item.name}
            className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
          />
          
          {/* Badges */}
          <Box className="absolute top-3 left-3 flex flex-col gap-2">
            {item.isNew && (
              <span className="bg-[#748C70] text-white text-xs px-2 py-1">NEW</span>
            )}
            {item.isSale && (
              <span className="bg-red-500 text-white text-xs px-2 py-1">SALE</span>
            )}
          </Box>

          {/* Wishlist Button */}
          <IconButton
            onClick={handleWishlist}
            className="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity"
            sx={{
              bgcolor: 'white',
              '&:hover': { bgcolor: 'white' },
              position: 'absolute',
              top: 8,
              right: 8,
              opacity: { xs: 1, md: 0 },
              transition: 'opacity 0.3s',
              '.group:hover &': { opacity: 1 },
            }}
            size="small"
          >
            {isWishlisted ? (
              <FavoriteIcon sx={{ color: '#748C70', fontSize: 20 }} />
            ) : (
              <FavoriteBorderIcon sx={{ fontSize: 20 }} />
            )}
          </IconButton>
        </Box>
      </Link>

      {/* Product Info */}
      <Box className="mt-3 space-y-2">
        {/* Color Options */}
        {item.colors && item.colors.length > 0 && (
          <Box className="flex gap-1">
            {item.colors.map((color, index) => (
              <Box
                key={index}
                sx={{
                  width: 12,
                  height: 12,
                  borderRadius: '50%',
                  bgcolor: color,
                  border: '1px solid #e0e0e0',
                  cursor: 'pointer',
                }}
              />
            ))}
          </Box>
        )}

        {/* Product Name */}
        <Link to={`/product/${item.slug}`}>
          <Typography
            variant="body2"
            className="text-gray-800 hover:text-[#748C70] transition-colors line-clamp-2"
            sx={{ fontSize: { xs: '0.8rem', md: '0.875rem' } }}
          >
            {item.name}
          </Typography>
        </Link>

        {/* Price */}
        <Box className="flex items-center gap-2">
          <Typography
            variant="body2"
            fontWeight={500}
            sx={{ fontSize: { xs: '0.8rem', md: '0.875rem' } }}
          >
            ${item.price}
          </Typography>
          {item.originalPrice && (
            <Typography
              variant="body2"
              className="line-through text-gray-400"
              sx={{ fontSize: { xs: '0.75rem', md: '0.8rem' } }}
            >
              ${item.originalPrice}
            </Typography>
          )}
        </Box>
      </Box>
    </Box>
  );
}