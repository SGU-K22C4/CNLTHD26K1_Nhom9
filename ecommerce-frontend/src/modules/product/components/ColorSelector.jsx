import { Box, Typography } from '@mui/material';

export default function ColorSelector({ colors, selectedColor, onColorChange }) {
  // Mock colors nếu không có prop
  const defaultColors = [
    { id: 'black', name: 'Black', hex: '#2D2D2D' },
    { id: 'olive', name: 'Olive', hex: '#748C70' },
    { id: 'beige', name: 'Beige', hex: '#C4A77D' },
  ];

  const colorOptions = colors || defaultColors;
  const selected = selectedColor || colorOptions[0]?.id;

  return (
    <Box>
      <Box className="flex items-center gap-2 mb-3">
        <Typography variant="body2" fontWeight={500}>
          Color:
        </Typography>
        <Typography variant="body2" className="text-gray-600">
          {colorOptions.find((c) => c.id === selected)?.name || ''}
        </Typography>
      </Box>

      <Box className="flex gap-3">
        {colorOptions.map((color) => (
          <Box
            key={color.id}
            onClick={() => onColorChange?.(color.id)}
            sx={{
              width: 32,
              height: 32,
              borderRadius: '50%',
              bgcolor: color.hex,
              cursor: 'pointer',
              border: selected === color.id ? '2px solid #748C70' : '1px solid #e0e0e0',
              outline: selected === color.id ? '2px solid white' : 'none',
              outlineOffset: -4,
              transition: 'all 0.2s ease',
              '&:hover': {
                transform: 'scale(1.1)',
              },
            }}
            title={color.name}
          />
        ))}
      </Box>
    </Box>
  );
}