import { Box, Typography, Link as MuiLink } from '@mui/material';

export default function SizeSelector({ sizes, selectedSize, onSizeChange, onSizeGuideClick }) {
  // Mock sizes nếu không có prop
  const defaultSizes = [
    { id: 'xs', label: 'XS', available: true },
    { id: 's', label: 'S', available: true },
    { id: 'm', label: 'M', available: true },
    { id: 'l', label: 'L', available: false },
    { id: 'xl', label: 'XL', available: true },
  ];

  const sizeOptions = sizes || defaultSizes;
  const selected = selectedSize || null;

  return (
    <Box>
      <Box className="flex items-center justify-between mb-3">
        <Box className="flex items-center gap-2">
          <Typography variant="body2" fontWeight={500}>
            Size:
          </Typography>
          {selected && (
            <Typography variant="body2" className="text-gray-600">
              {sizeOptions.find((s) => s.id === selected)?.label || ''}
            </Typography>
          )}
        </Box>
        <MuiLink
          component="button"
          variant="body2"
          underline="always"
          onClick={onSizeGuideClick}
          sx={{ color: '#748C70', cursor: 'pointer' }}
        >
          Size Guide
        </MuiLink>
      </Box>

      <Box className="flex flex-wrap gap-2">
        {sizeOptions.map((size) => (
          <Box
            key={size.id}
            onClick={() => size.available && onSizeChange?.(size.id)}
            sx={{
              minWidth: 48,
              height: 48,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              border: '1px solid',
              borderColor: selected === size.id ? '#748C70' : '#e0e0e0',
              bgcolor: selected === size.id ? '#748C70' : 'transparent',
              color: selected === size.id ? 'white' : size.available ? '#2D2D2D' : '#bdbdbd',
              cursor: size.available ? 'pointer' : 'not-allowed',
              position: 'relative',
              transition: 'all 0.2s ease',
              '&:hover': size.available
                ? {
                    borderColor: '#748C70',
                  }
                : {},
              // Strike-through for unavailable sizes
              ...((!size.available) && {
                '&::after': {
                  content: '""',
                  position: 'absolute',
                  top: '50%',
                  left: 0,
                  right: 0,
                  height: '1px',
                  bgcolor: '#bdbdbd',
                  transform: 'rotate(-45deg)',
                },
              }),
            }}
          >
            <Typography variant="body2" fontWeight={500}>
              {size.label}
            </Typography>
          </Box>
        ))}
      </Box>
    </Box>
  );
}