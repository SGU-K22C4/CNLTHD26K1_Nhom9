import React from 'react';
import { Drawer, IconButton, Box } from '@mui/material';
import BannerHeader from '../BannerHeader'; 
import MobileMenu from './MobileMenu'; 
import CloseIcon from '@mui/icons-material/Close';
import SearchIcon from '@mui/icons-material/Search';
import LogoMobileWebsite from './LogoMobileWebsite';
import FavoriteBorderOutlinedIcon from '@mui/icons-material/FavoriteBorderOutlined';
import ShoppingBagOutlinedIcon from '@mui/icons-material/ShoppingBagOutlined';

export default function NavigationDropMenuMobile(props) {
  const { open, handleDrawerClose, options = [] } = props;
  
  if (!open) return null;
  
  return (
    <Drawer
      sx={{
        zIndex: 9999,
        display: { xs: 'block', md: 'none' },
        '& .MuiDrawer-paper': {
          width: '100%',
          backgroundColor: 'white',
        },
      }}
      variant="persistent"
      anchor="left"
      open={open}
    >
      <BannerHeader />
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '0.2rem 1rem',
        }}
      >
        <Box
          sx={{
            display: { xs: 'flex', md: 'none' },
            alignItems: 'center',
            cursor: 'pointer',
            padding: '7px 10px',
          }}
        >
          <IconButton onClick={handleDrawerClose} sx={{ cursor: 'pointer' }}>
            <CloseIcon sx={{ color: '#000000' }} />
          </IconButton>
          <SearchIcon />
        </Box>
        <LogoMobileWebsite />
        <Box
          sx={{
            display: { xs: 'flex', md: 'none' },
            gap: '9px',
          }}
        >
          <FavoriteBorderOutlinedIcon sx={{ cursor: 'pointer' }} />
          <ShoppingBagOutlinedIcon sx={{ cursor: 'pointer' }} />
        </Box>
      </Box>
      <MobileMenu options={options} onNavigate={handleDrawerClose} />
    </Drawer>
  );
}
