import { useState } from 'react';
import {
  Box,
  Typography,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Checkbox,
  FormControlLabel,
  Slider,
  IconButton,
  Drawer,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import CloseIcon from '@mui/icons-material/Close';

// xử lý sau: kết nối với backend để lấy filter options và apply filters
const FILTER_DATA = {
  categories: [
    { id: 'tops', label: 'Tops', count: 24 },
    { id: 'dresses', label: 'Dresses', count: 18 },
    { id: 'bottoms', label: 'Bottoms', count: 12 },
    { id: 'outerwear', label: 'Outerwear', count: 8 },
    { id: 'accessories', label: 'Accessories', count: 15 },
  ],
  sizes: ['XXS', 'XS', 'S', 'M', 'L', 'XL', 'XXL'],
  colors: [
    { id: 'black', label: 'Black', hex: '#2D2D2D' },
    { id: 'white', label: 'White', hex: '#FFFFFF' },
    { id: 'green', label: 'Green', hex: '#748C70' },
    { id: 'beige', label: 'Beige', hex: '#C4A77D' },
    { id: 'navy', label: 'Navy', hex: '#1E3A5F' },
    { id: 'gray', label: 'Gray', hex: '#9E9E9E' },
  ],
  priceRange: [0, 500],
};

export default function ProductFilters({ open, onClose, filters, onFilterChange }) {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  
  const [localFilters, setLocalFilters] = useState({
    categories: filters?.categories || [],
    sizes: filters?.sizes || [],
    colors: filters?.colors || [],
    priceRange: filters?.priceRange || [0, 500],
  });

  const handleCategoryChange = (categoryId) => {
    const updated = localFilters.categories.includes(categoryId)
      ? localFilters.categories.filter((c) => c !== categoryId)
      : [...localFilters.categories, categoryId];
    setLocalFilters({ ...localFilters, categories: updated });
    onFilterChange?.({ ...localFilters, categories: updated });
  };

  const handleSizeChange = (size) => {
    const updated = localFilters.sizes.includes(size)
      ? localFilters.sizes.filter((s) => s !== size)
      : [...localFilters.sizes, size];
    setLocalFilters({ ...localFilters, sizes: updated });
    onFilterChange?.({ ...localFilters, sizes: updated });
  };

  const handleColorChange = (colorId) => {
    const updated = localFilters.colors.includes(colorId)
      ? localFilters.colors.filter((c) => c !== colorId)
      : [...localFilters.colors, colorId];
    setLocalFilters({ ...localFilters, colors: updated });
    onFilterChange?.({ ...localFilters, colors: updated });
  };

  const handlePriceChange = (event, newValue) => {
    setLocalFilters({ ...localFilters, priceRange: newValue });
    onFilterChange?.({ ...localFilters, priceRange: newValue });
  };

  const FilterContent = () => (
    <Box className="space-y-2">
      {/* Category Filter */}
      <Accordion defaultExpanded elevation={0} disableGutters>
        <AccordionSummary
          expandIcon={<ExpandMoreIcon />}
          sx={{ px: 0, minHeight: 48 }}
        >
          <Typography fontWeight={500} fontSize={14}>
            Category
          </Typography>
        </AccordionSummary>
        <AccordionDetails sx={{ px: 0, pt: 0 }}>
          <Box className="space-y-1">
            {FILTER_DATA.categories.map((category) => (
              <FormControlLabel
                key={category.id}
                control={
                  <Checkbox
                    size="small"
                    checked={localFilters.categories.includes(category.id)}
                    onChange={() => handleCategoryChange(category.id)}
                    sx={{
                      color: '#748C70',
                      '&.Mui-checked': { color: '#748C70' },
                    }}
                  />
                }
                label={
                  <Typography variant="body2" className="text-gray-600">
                    {category.label} ({category.count})
                  </Typography>
                }
                sx={{ ml: 0 }}
              />
            ))}
          </Box>
        </AccordionDetails>
      </Accordion>

      {/* Size Filter */}
      <Accordion defaultExpanded elevation={0} disableGutters>
        <AccordionSummary
          expandIcon={<ExpandMoreIcon />}
          sx={{ px: 0, minHeight: 48 }}
        >
          <Typography fontWeight={500} fontSize={14}>
            Size
          </Typography>
        </AccordionSummary>
        <AccordionDetails sx={{ px: 0, pt: 0 }}>
          <Box className="flex flex-wrap gap-2">
            {FILTER_DATA.sizes.map((size) => (
              <Box
                key={size}
                onClick={() => handleSizeChange(size)}
                sx={{
                  px: 2,
                  py: 1,
                  border: '1px solid',
                  borderColor: localFilters.sizes.includes(size)
                    ? '#748C70'
                    : '#e0e0e0',
                  bgcolor: localFilters.sizes.includes(size)
                    ? '#748C70'
                    : 'transparent',
                  color: localFilters.sizes.includes(size) ? 'white' : 'inherit',
                  cursor: 'pointer',
                  fontSize: 12,
                  transition: 'all 0.2s',
                  '&:hover': {
                    borderColor: '#748C70',
                  },
                }}
              >
                {size}
              </Box>
            ))}
          </Box>
        </AccordionDetails>
      </Accordion>

      {/* Color Filter */}
      <Accordion defaultExpanded elevation={0} disableGutters>
        <AccordionSummary
          expandIcon={<ExpandMoreIcon />}
          sx={{ px: 0, minHeight: 48 }}
        >
          <Typography fontWeight={500} fontSize={14}>
            Color
          </Typography>
        </AccordionSummary>
        <AccordionDetails sx={{ px: 0, pt: 0 }}>
          <Box className="flex flex-wrap gap-3">
            {FILTER_DATA.colors.map((color) => (
              <Box
                key={color.id}
                onClick={() => handleColorChange(color.id)}
                sx={{
                  width: 28,
                  height: 28,
                  borderRadius: '50%',
                  bgcolor: color.hex,
                  border: localFilters.colors.includes(color.id)
                    ? '2px solid #748C70'
                    : '1px solid #e0e0e0',
                  cursor: 'pointer',
                  outline: localFilters.colors.includes(color.id)
                    ? '2px solid white'
                    : 'none',
                  outlineOffset: -3,
                  transition: 'all 0.2s',
                }}
                title={color.label}
              />
            ))}
          </Box>
        </AccordionDetails>
      </Accordion>

      {/* Price Filter */}
      <Accordion defaultExpanded elevation={0} disableGutters>
        <AccordionSummary
          expandIcon={<ExpandMoreIcon />}
          sx={{ px: 0, minHeight: 48 }}
        >
          <Typography fontWeight={500} fontSize={14}>
            Price
          </Typography>
        </AccordionSummary>
        <AccordionDetails sx={{ px: 0, pt: 0 }}>
          <Box sx={{ px: 1 }}>
            <Slider
              value={localFilters.priceRange}
              onChange={handlePriceChange}
              valueLabelDisplay="auto"
              min={0}
              max={500}
              sx={{
                color: '#748C70',
                '& .MuiSlider-thumb': {
                  bgcolor: '#748C70',
                },
              }}
            />
            <Box className="flex justify-between mt-2">
              <Typography variant="body2" className="text-gray-600">
                ${localFilters.priceRange[0]}
              </Typography>
              <Typography variant="body2" className="text-gray-600">
                ${localFilters.priceRange[1]}
              </Typography>
            </Box>
          </Box>
        </AccordionDetails>
      </Accordion>
    </Box>
  );

  // Mobile: Drawer
  if (isMobile) {
    return (
      <Drawer
        anchor="left"
        open={open}
        onClose={onClose}
        PaperProps={{
          sx: { width: '85%', maxWidth: 360, p: 3 },
        }}
      >
        <Box className="flex justify-between items-center mb-4">
          <Typography variant="h6" fontWeight={600}>
            Filters
          </Typography>
          <IconButton onClick={onClose}>
            <CloseIcon />
          </IconButton>
        </Box>
        <FilterContent />
      </Drawer>
    );
  }

  // Desktop: Sidebar
  return (
    <Box className="w-full">
      <Typography variant="h6" fontWeight={600} className="mb-4">
        Filters
      </Typography>
      <FilterContent />
    </Box>
  );
}
