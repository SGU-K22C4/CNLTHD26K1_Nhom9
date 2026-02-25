import { useState } from 'react';
import { Box, IconButton, useMediaQuery, useTheme } from '@mui/material';
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';
import ZoomInIcon from '@mui/icons-material/ZoomIn';

export default function ProductGallery({ images }) {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));

  // Mock images nếu không có prop
  const defaultImages = [
    '/assets/images/product-detail-1.jpg',
    '/assets/images/product-detail-2.jpg',
    '/assets/images/product-detail-3.jpg',
    '/assets/images/product-detail-4.jpg',
  ];

  const galleryImages = images?.length > 0 ? images : defaultImages;
  const [selectedIndex, setSelectedIndex] = useState(0);
  const [isZoomed, setIsZoomed] = useState(false);

  const handlePrev = () => {
    setSelectedIndex((prev) => (prev === 0 ? galleryImages.length - 1 : prev - 1));
  };

  const handleNext = () => {
    setSelectedIndex((prev) => (prev === galleryImages.length - 1 ? 0 : prev + 1));
  };

  // Mobile: Swipable Gallery
  if (isMobile) {
    return (
      <Box className="relative">
        {/* Main Image */}
        <Box
          className="relative bg-gray-100 overflow-hidden"
          sx={{ aspectRatio: '3/4' }}
        >
          <img
            src={galleryImages[selectedIndex]}
            alt={`Product image ${selectedIndex + 1}`}
            className="w-full h-full object-cover"
          />

          {/* Navigation Arrows */}
          <IconButton
            onClick={handlePrev}
            sx={{
              position: 'absolute',
              left: 8,
              top: '50%',
              transform: 'translateY(-50%)',
              bgcolor: 'rgba(255,255,255,0.9)',
              '&:hover': { bgcolor: 'white' },
            }}
            size="small"
          >
            <ChevronLeftIcon />
          </IconButton>
          <IconButton
            onClick={handleNext}
            sx={{
              position: 'absolute',
              right: 8,
              top: '50%',
              transform: 'translateY(-50%)',
              bgcolor: 'rgba(255,255,255,0.9)',
              '&:hover': { bgcolor: 'white' },
            }}
            size="small"
          >
            <ChevronRightIcon />
          </IconButton>
        </Box>

        {/* Dots Indicator */}
        <Box className="flex justify-center gap-2 mt-4">
          {galleryImages.map((_, index) => (
            <Box
              key={index}
              onClick={() => setSelectedIndex(index)}
              sx={{
                width: 8,
                height: 8,
                borderRadius: '50%',
                bgcolor: index === selectedIndex ? '#748C70' : '#e0e0e0',
                cursor: 'pointer',
                transition: 'all 0.2s',
              }}
            />
          ))}
        </Box>
      </Box>
    );
  }

  // Desktop: Thumbnail Gallery
  return (
    <Box className="flex gap-4">
      {/* Thumbnails */}
      <Box className="flex flex-col gap-2 w-20">
        {galleryImages.map((img, index) => (
          <Box
            key={index}
            onClick={() => setSelectedIndex(index)}
            sx={{
              aspectRatio: '3/4',
              cursor: 'pointer',
              border: '2px solid',
              borderColor: index === selectedIndex ? '#748C70' : 'transparent',
              opacity: index === selectedIndex ? 1 : 0.6,
              transition: 'all 0.2s',
              '&:hover': { opacity: 1 },
              overflow: 'hidden',
              bgcolor: '#f5f5f5',
            }}
          >
            <img
              src={img}
              alt={`Thumbnail ${index + 1}`}
              className="w-full h-full object-cover"
            />
          </Box>
        ))}
      </Box>

      {/* Main Image */}
      <Box
        className="flex-1 relative bg-gray-100 overflow-hidden"
        sx={{
          aspectRatio: '3/4',
          cursor: isZoomed ? 'zoom-out' : 'zoom-in',
        }}
        onClick={() => setIsZoomed(!isZoomed)}
      >
        <img
          src={galleryImages[selectedIndex]}
          alt={`Product image ${selectedIndex + 1}`}
          className="w-full h-full object-cover transition-transform duration-300"
          style={{ transform: isZoomed ? 'scale(1.5)' : 'scale(1)' }}
        />

        {/* Zoom Icon */}
        <IconButton
          sx={{
            position: 'absolute',
            bottom: 16,
            right: 16,
            bgcolor: 'rgba(255,255,255,0.9)',
            '&:hover': { bgcolor: 'white' },
          }}
          size="small"
        >
          <ZoomInIcon />
        </IconButton>
      </Box>
    </Box>
  );
}