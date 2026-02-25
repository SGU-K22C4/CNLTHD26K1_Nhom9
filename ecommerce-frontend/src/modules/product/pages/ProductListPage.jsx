import { useState } from 'react';
import { useParams } from 'react-router-dom';
import {
  Box,
  Container,
  Typography,
  Grid,
  Select,
  MenuItem,
  FormControl,
  Button,
  useMediaQuery,
  useTheme,
  Breadcrumbs,
  Link as MuiLink,
} from '@mui/material';
import { Link } from 'react-router-dom';
import TuneIcon from '@mui/icons-material/Tune';
import GridViewIcon from '@mui/icons-material/GridView';
import ViewListIcon from '@mui/icons-material/ViewList';
import ProductCard from '../components/ProductCard';
import ProductFilters from '../components/ProductFilters';

// xử lý sau: fetch từ backend API
const MOCK_PRODUCTS = [
  {
    id: 1,
    name: 'Recycled Cotton Ribbed Tank Top',
    price: 45,
    images: ['/assets/images/product-1.jpg'],
    colors: ['#2D2D2D', '#748C70'],
    slug: 'recycled-cotton-ribbed-tank-top',
    isNew: true,
  },
  {
    id: 2,
    name: 'Organic Linen Blend Blazer',
    price: 120,
    originalPrice: 150,
    images: ['/assets/images/product-2.jpg'],
    colors: ['#C4A77D', '#2D2D2D'],
    slug: 'organic-linen-blend-blazer',
    isSale: true,
  },
  {
    id: 3,
    name: 'Sustainable Wool Midi Skirt',
    price: 85,
    images: ['/assets/images/product-3.jpg'],
    colors: ['#748C70', '#9E9E9E'],
    slug: 'sustainable-wool-midi-skirt',
  },
  {
    id: 4,
    name: 'Eco-Friendly Jersey Dress',
    price: 95,
    images: ['/assets/images/product-4.jpg'],
    colors: ['#2D2D2D'],
    slug: 'eco-friendly-jersey-dress',
    isNew: true,
  },
  {
    id: 5,
    name: 'Bamboo Fiber Wide Leg Pants',
    price: 78,
    images: ['/assets/images/product-5.jpg'],
    colors: ['#C4A77D', '#748C70', '#2D2D2D'],
    slug: 'bamboo-fiber-wide-leg-pants',
  },
  {
    id: 6,
    name: 'Organic Cotton Oversized Shirt',
    price: 65,
    images: ['/assets/images/product-6.jpg'],
    colors: ['#FFFFFF', '#1E3A5F'],
    slug: 'organic-cotton-oversized-shirt',
  },
  {
    id: 7,
    name: 'Recycled Cashmere Sweater',
    price: 145,
    images: ['/assets/images/product-7.jpg'],
    colors: ['#9E9E9E', '#C4A77D'],
    slug: 'recycled-cashmere-sweater',
  },
  {
    id: 8,
    name: 'Hemp Blend Cargo Trousers',
    price: 88,
    originalPrice: 110,
    images: ['/assets/images/product-8.jpg'],
    colors: ['#748C70', '#2D2D2D'],
    slug: 'hemp-blend-cargo-trousers',
    isSale: true,
  },
];

export default function ProductListPage() {
  const { category } = useParams();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));

  const [filterOpen, setFilterOpen] = useState(false);
  const [filters, setFilters] = useState({
    categories: [],
    sizes: [],
    colors: [],
    priceRange: [0, 500],
  });
  const [sortBy, setSortBy] = useState('newest');
  const [gridCols, setGridCols] = useState(4);

  const handleFilterChange = (newFilters) => {
    setFilters(newFilters);
    // xử lý sau: gọi API với filters mới
  };

  const getCategoryTitle = () => {
    if (!category) return 'Shop All';
    return category.charAt(0).toUpperCase() + category.slice(1);
  };

  return (
    <Box className="min-h-screen bg-white">
      {/* Breadcrumbs */}
      <Container maxWidth="xl" sx={{ pt: 3 }}>
        <Breadcrumbs separator="/" sx={{ fontSize: 14 }}>
          <MuiLink
            component={Link}
            to="/"
            underline="hover"
            color="inherit"
          >
            Home
          </MuiLink>
          <Typography color="text.primary" fontSize={14}>
            {getCategoryTitle()}
          </Typography>
        </Breadcrumbs>
      </Container>

      {/* Page Header */}
      <Container maxWidth="xl" sx={{ py: { xs: 3, md: 5 } }}>
        <Typography
          variant="h4"
          component="h1"
          fontWeight={600}
          sx={{ fontSize: { xs: '1.5rem', md: '2rem' } }}
        >
          {getCategoryTitle()}
        </Typography>
        <Typography
          variant="body2"
          color="text.secondary"
          sx={{ mt: 1 }}
        >
          {MOCK_PRODUCTS.length} products
        </Typography>
      </Container>

      <Container maxWidth="xl" sx={{ pb: 8 }}>
        <Grid container spacing={4}>
          {/* Filters Sidebar - Desktop */}
          {!isMobile && (
            <Grid item md={3} lg={2.5}>
              <Box className="sticky top-24">
                <ProductFilters
                  filters={filters}
                  onFilterChange={handleFilterChange}
                />
              </Box>
            </Grid>
          )}

          {/* Products Grid */}
          <Grid item xs={12} md={9} lg={9.5}>
            {/* Toolbar */}
            <Box className="flex justify-between items-center mb-6 pb-4 border-b">
              {/* Mobile Filter Button */}
              {isMobile && (
                <Button
                  variant="outlined"
                  startIcon={<TuneIcon />}
                  onClick={() => setFilterOpen(true)}
                  sx={{
                    borderColor: '#e0e0e0',
                    color: '#2D2D2D',
                    textTransform: 'none',
                    '&:hover': { borderColor: '#748C70' },
                  }}
                >
                  Filters
                </Button>
              )}

              {/* Sort & View Options */}
              <Box className="flex items-center gap-4 ml-auto">
                {/* Sort Dropdown */}
                <FormControl size="small" sx={{ minWidth: 150 }}>
                  <Select
                    value={sortBy}
                    onChange={(e) => setSortBy(e.target.value)}
                    sx={{
                      fontSize: 14,
                      '& .MuiSelect-select': { py: 1 },
                    }}
                  >
                    <MenuItem value="newest">Newest</MenuItem>
                    <MenuItem value="price-low">Price: Low to High</MenuItem>
                    <MenuItem value="price-high">Price: High to Low</MenuItem>
                    <MenuItem value="popular">Most Popular</MenuItem>
                  </Select>
                </FormControl>

                {/* Grid Toggle - Desktop Only */}
                {!isMobile && (
                  <Box className="flex gap-1">
                    <Button
                      variant={gridCols === 3 ? 'contained' : 'outlined'}
                      onClick={() => setGridCols(3)}
                      sx={{
                        minWidth: 40,
                        p: 1,
                        bgcolor: gridCols === 3 ? '#748C70' : 'transparent',
                        borderColor: '#e0e0e0',
                        '&:hover': {
                          bgcolor: gridCols === 3 ? '#5a7359' : '#f5f5f5',
                        },
                      }}
                    >
                      <ViewListIcon
                        sx={{ color: gridCols === 3 ? 'white' : '#666' }}
                      />
                    </Button>
                    <Button
                      variant={gridCols === 4 ? 'contained' : 'outlined'}
                      onClick={() => setGridCols(4)}
                      sx={{
                        minWidth: 40,
                        p: 1,
                        bgcolor: gridCols === 4 ? '#748C70' : 'transparent',
                        borderColor: '#e0e0e0',
                        '&:hover': {
                          bgcolor: gridCols === 4 ? '#5a7359' : '#f5f5f5',
                        },
                      }}
                    >
                      <GridViewIcon
                        sx={{ color: gridCols === 4 ? 'white' : '#666' }}
                      />
                    </Button>
                  </Box>
                )}
              </Box>
            </Box>

            {/* Products Grid */}
            <Grid container spacing={{ xs: 2, md: 3 }}>
              {MOCK_PRODUCTS.map((product) => (
                <Grid
                  item
                  xs={6}
                  sm={4}
                  md={12 / gridCols}
                  key={product.id}
                >
                  <ProductCard product={product} />
                </Grid>
              ))}
            </Grid>

            {/* Load More */}
            <Box className="text-center mt-10">
              <Button
                variant="outlined"
                sx={{
                  px: 6,
                  py: 1.5,
                  borderColor: '#2D2D2D',
                  color: '#2D2D2D',
                  textTransform: 'none',
                  '&:hover': {
                    borderColor: '#748C70',
                    bgcolor: '#748C70',
                    color: 'white',
                  },
                }}
              >
                Load More
              </Button>
            </Box>
          </Grid>
        </Grid>
      </Container>

      {/* Mobile Filters Drawer */}
      {isMobile && (
        <ProductFilters
          open={filterOpen}
          onClose={() => setFilterOpen(false)}
          filters={filters}
          onFilterChange={handleFilterChange}
        />
      )}
    </Box>
  );
}