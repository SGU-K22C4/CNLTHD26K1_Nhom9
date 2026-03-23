import { useTheme } from '@mui/material/styles';
import { Button, Typography, useMediaQuery } from '@mui/material';
import { Box } from '@mui/material';

function HeroSection() {
  const theme = useTheme();
  const matches = useMediaQuery(theme.breakpoints.down('sm'));
  // xử lý sau: thay bằng data từ backend

  return (
    <Box
      position="relative"
      width="100%"
      sx={{
        minHeight: { xs: 360, md: 600 },
        overflow: 'hidden',
        mb: { xs: 4, md: 8 },
      }}
    >
      <img
        src="../../../../public/assets/images/hero-desktop.webp" // thêm sau
        alt="Image for hero"
        style={{
          objectFit: 'cover',
          width: '100%',
          height: '100%',
          display: 'block',
          objectPosition: '30% 40%',
        }}
        width={1441}
        height={600}
        quality={100}
        priority
      />
      <Box
        sx={{
          position: 'absolute',
          top: '50%',
          [theme.breakpoints.down('sm')]: {
            top: '67%',
            left: '4%',
          },
          left: '10%',
         
        }}
      >
        <Typography variant={matches ? 'h5' : 'h4'}>
          Elegance in simplicity,
          <br />
          Earth’s Harmony
        </Typography>

        <Button
          sx={{
            bgcolor: '#ffff',
            color: '#0C0C0C',
            textTransform: 'none',
            px: { xs: 3, sm: 4, md: 9 },
            py: { xs: 1, sm: 1.5 },
            mt: 2,
            borderRadius: 0,
            border: '1px solid transparent',
            transition: 'all 0.3s ease',
            '&:hover': {
              bgcolor: 'transparent',
              color: '#ffff',
              border: '1px solid #ffff',
            },
          }}
        >
          New In
        </Button>
      </Box>
    </Box>
  );
}

export default HeroSection;
