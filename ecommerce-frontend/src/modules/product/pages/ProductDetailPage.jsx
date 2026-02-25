import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  Box,
  Container,
  Typography,
  Grid,
  Button,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Breadcrumbs,
  Link as MuiLink,
  IconButton,
  Divider,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import FavoriteBorderIcon from '@mui/icons-material/FavoriteBorder';
import FavoriteIcon from '@mui/icons-material/Favorite';
import LocalShippingOutlinedIcon from '@mui/icons-material/LocalShippingOutlined';
import LoopIcon from '@mui/icons-material/Loop';
import ProductGallery from '../components/ProductGallery';
import ColorSelector from '../components/ColorSelector';
import SizeSelector from '../components/SizeSelector';
import ProductCard from '../components/ProductCard';

// xử lý sau: fetch từ backend API theo slug
const MOCK_PRODUCT = {
  id: 1,
  name: 'Recycled Cotton Ribbed Tank Top',
  price: 45,
  originalPrice: null,
  description:
    'A versatile essential crafted from recycled cotton with a ribbed texture. Features a classic tank silhouette perfect for layering or wearing alone. Sustainably made with eco-friendly materials.',
  images: [
    '/assets/images/product-detail-1.jpg',
    '/assets/images/product-detail-2.jpg',
    '/assets/images/product-detail-3.jpg',
    '/assets/images/product-detail-4.jpg',
  ],
  colors: [
    { id: 'black', name: 'Black', hex: '#2D2D2D' },
    { id: 'olive', name: 'Olive', hex: '#748C70' },
    { id: 'beige', name: 'Beige', hex: '#C4A77D' },
  ],
  sizes: [
    { id: 'xs', label: 'XS', available: true },
    { id: 's', label: 'S', available: true },
    { id: 'm', label: 'M', available: true },
    { id: 'l', label: 'L', available: false },
    { id: 'xl', label: 'XL', available: true },
  ],
  materials: '95% Recycled Cotton, 5% Elastane',
  care: ['Machine wash cold', 'Do not bleach', 'Tumble dry low', 'Iron on low heat'],
  sustainability:
    'This product is made with recycled materials, reducing waste and supporting a circular economy.',
};

// Mock related products
const RELATED_PRODUCTS = [
  {
    id: 2,
    name: 'Organic Linen Blend Blazer',
    price: 120,
    images: ['/assets/images/product-2.jpg'],
    colors: ['#C4A77D', '#2D2D2D'],
    slug: 'organic-linen-blend-blazer',
  },
  {
    id: 3,
    name: 'Sustainable Wool Midi Skirt',
    price: 85,
    images: ['/assets/images/product-3.jpg'],
    colors: ['#748C70'],
    slug: 'sustainable-wool-midi-skirt',
  },
  {
    id: 4,
    name: 'Eco-Friendly Jersey Dress',
    price: 95,
    images: ['/assets/images/product-4.jpg'],
    colors: ['#2D2D2D'],
    slug: 'eco-friendly-jersey-dress',
  },
  {
    id: 5,
    name: 'Bamboo Fiber Wide Leg Pants',
    price: 78,
    images: ['/assets/images/product-5.jpg'],
    colors: ['#C4A77D', '#748C70'],
    slug: 'bamboo-fiber-wide-leg-pants',
  },
];

export default function ProductDetailPage() {
  const { slug } = useParams();
  const [selectedColor, setSelectedColor] = useState(MOCK_PRODUCT.colors[0]?.id);
  const [selectedSize, setSelectedSize] = useState(null);
  const [isWishlisted, setIsWishlisted] = useState(false);
  const [quantity, setQuantity] = useState(1);

  // xử lý sau: fetch product by slug từ API
  const product = MOCK_PRODUCT;

  const handleAddToCart = () => {
    if (!selectedSize) {
      alert('Please select a size');
      return;
    }
    // xử lý sau: thêm vào cart qua context/API
    console.log('Add to cart:', { product, selectedColor, selectedSize, quantity });
  };

  return (
    <Box className="min-h-screen bg-white">
      {/* Breadcrumbs */}
      <Container maxWidth="xl" sx={{ pt: 3 }}>
        <Breadcrumbs separator="/" sx={{ fontSize: 14 }}>
          <MuiLink component={Link} to="/" underline="hover" color="inherit">
            Home
          </MuiLink>
          <MuiLink component={Link} to="/collection/women" underline="hover" color="inherit">
            Women
          </MuiLink>
          <Typography color="text.primary" fontSize={14}>
            {product.name}
          </Typography>
        </Breadcrumbs>
      </Container>

      {/* Product Section */}
      <Container maxWidth="xl" sx={{ py: { xs: 4, md: 6 } }}>
        <Grid container spacing={{ xs: 4, md: 6 }}>
          {/* Product Gallery */}
          <Grid item xs={12} md={7}>
            <ProductGallery images={product.images} />
          </Grid>

          {/* Product Info */}
          <Grid item xs={12} md={5}>
            <Box className="md:sticky md:top-24">
              {/* Title & Price */}
              <Typography
                variant="h5"
                component="h1"
                fontWeight={600}
                sx={{ fontSize: { xs: '1.25rem', md: '1.5rem' } }}
              >
                {product.name}
              </Typography>

              <Box className="flex items-center gap-3 mt-2">
                <Typography variant="h6" fontWeight={500}>
                  ${product.price}
                </Typography>
                {product.originalPrice && (
                  <Typography
                    variant="body1"
                    className="line-through text-gray-400"
                  >
                    ${product.originalPrice}
                  </Typography>
                )}
              </Box>

              {/* Description */}
              <Typography
                variant="body2"
                color="text.secondary"
                sx={{ mt: 3, lineHeight: 1.7 }}
              >
                {product.description}
              </Typography>

              <Divider sx={{ my: 3 }} />

              {/* Color Selector */}
              <Box sx={{ mb: 3 }}>
                <ColorSelector
                  colors={product.colors}
                  selectedColor={selectedColor}
                  onColorChange={setSelectedColor}
                />
              </Box>

              {/* Size Selector */}
              <Box sx={{ mb: 3 }}>
                <SizeSelector
                  sizes={product.sizes}
                  selectedSize={selectedSize}
                  onSizeChange={setSelectedSize}
                  onSizeGuideClick={() => console.log('Open size guide')}
                />
              </Box>

              {/* Quantity & Add to Cart */}
              <Box className="flex gap-3 mt-6">
                {/* Quantity Selector */}
                <Box className="flex items-center border border-gray-200">
                  <Button
                    onClick={() => setQuantity(Math.max(1, quantity - 1))}
                    sx={{ minWidth: 40, color: '#2D2D2D' }}
                  >
                    -
                  </Button>
                  <Typography sx={{ px: 2, minWidth: 40, textAlign: 'center' }}>
                    {quantity}
                  </Typography>
                  <Button
                    onClick={() => setQuantity(quantity + 1)}
                    sx={{ minWidth: 40, color: '#2D2D2D' }}
                  >
                    +
                  </Button>
                </Box>

                {/* Add to Cart Button */}
                <Button
                  variant="contained"
                  fullWidth
                  onClick={handleAddToCart}
                  sx={{
                    bgcolor: '#2D2D2D',
                    color: 'white',
                    py: 1.5,
                    textTransform: 'none',
                    fontSize: '1rem',
                    '&:hover': {
                      bgcolor: '#748C70',
                    },
                  }}
                >
                  Add to Cart
                </Button>

                {/* Wishlist Button */}
                <IconButton
                  onClick={() => setIsWishlisted(!isWishlisted)}
                  sx={{
                    border: '1px solid #e0e0e0',
                    borderRadius: 0,
                    px: 2,
                  }}
                >
                  {isWishlisted ? (
                    <FavoriteIcon sx={{ color: '#748C70' }} />
                  ) : (
                    <FavoriteBorderIcon />
                  )}
                </IconButton>
              </Box>

              {/* Shipping Info */}
              <Box className="mt-6 space-y-3">
                <Box className="flex items-center gap-3">
                  <LocalShippingOutlinedIcon sx={{ color: '#748C70' }} />
                  <Typography variant="body2">
                    Free shipping on orders over $100
                  </Typography>
                </Box>
                <Box className="flex items-center gap-3">
                  <LoopIcon sx={{ color: '#748C70' }} />
                  <Typography variant="body2">
                    Free 30-day returns
                  </Typography>
                </Box>
              </Box>

              <Divider sx={{ my: 3 }} />

              {/* Product Details Accordions */}
              <Box>
                <Accordion elevation={0} disableGutters defaultExpanded>
                  <AccordionSummary expandIcon={<ExpandMoreIcon />} sx={{ px: 0 }}>
                    <Typography fontWeight={500}>Materials</Typography>
                  </AccordionSummary>
                  <AccordionDetails sx={{ px: 0 }}>
                    <Typography variant="body2" color="text.secondary">
                      {product.materials}
                    </Typography>
                  </AccordionDetails>
                </Accordion>

                <Accordion elevation={0} disableGutters>
                  <AccordionSummary expandIcon={<ExpandMoreIcon />} sx={{ px: 0 }}>
                    <Typography fontWeight={500}>Care Instructions</Typography>
                  </AccordionSummary>
                  <AccordionDetails sx={{ px: 0 }}>
                    <ul className="list-disc list-inside space-y-1">
                      {product.care.map((item, index) => (
                        <li key={index}>
                          <Typography variant="body2" component="span" color="text.secondary">
                            {item}
                          </Typography>
                        </li>
                      ))}
                    </ul>
                  </AccordionDetails>
                </Accordion>

                <Accordion elevation={0} disableGutters>
                  <AccordionSummary expandIcon={<ExpandMoreIcon />} sx={{ px: 0 }}>
                    <Typography fontWeight={500}>Sustainability</Typography>
                  </AccordionSummary>
                  <AccordionDetails sx={{ px: 0 }}>
                    <Typography variant="body2" color="text.secondary">
                      {product.sustainability}
                    </Typography>
                  </AccordionDetails>
                </Accordion>
              </Box>
            </Box>
          </Grid>
        </Grid>
      </Container>

      {/* Related Products */}
      <Box sx={{ bgcolor: '#fafafa', py: { xs: 6, md: 8 } }}>
        <Container maxWidth="xl">
          <Typography
            variant="h5"
            fontWeight={600}
            sx={{ mb: 4, textAlign: 'center' }}
          >
            You May Also Like
          </Typography>

          <Grid container spacing={3}>
            {RELATED_PRODUCTS.map((item) => (
              <Grid item xs={6} sm={3} key={item.id}>
                <ProductCard product={item} />
              </Grid>
            ))}
          </Grid>
        </Container>
      </Box>
    </Box>
  );
}