import { AppBar, Toolbar, Box, IconButton, Button } from '@mui/material';
import LogoWebsite from './LogoWebsite';
import SearchOutlinedIcon from '@mui/icons-material/SearchOutlined';
import DesktopMenu from './DesktopMenu';
import LogoMobileWebsite from './LogoMobileWebsite';
import MenuOutlinedIcon from '@mui/icons-material/MenuOutlined';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import SearchField from './SearchField';
import NavigationDropMenuMobile from './NavigationDropMenuMobile';
import BadgeNumberShopping from './BadgeNumberShopping';
import FavoriteBorderOutlinedIcon from '@mui/icons-material/FavoriteBorderOutlined';
import PersonOutlinedIcon from '@mui/icons-material/PersonOutlined';
import CloseIcon from '@mui/icons-material/Close';

function MainNavBar(props) {
  const { options, setIsHovered, setIsOpen } = props;
  const navigate = useNavigate();

  // Temporarily hardcoded cart count (will be replaced with Redux later)
  const cartCount = 0;

  const [open, setOpen] = useState(false);
  const [isOpenSearch, setIsOpenSearch] = useState(false);

  const handleDrawerOpen = () => setOpen(true);
  const handleDrawerClose = () => setOpen(false);
  const handleCloseSearch = () => setIsOpenSearch(false);
  const handleOpenSearch = () => {
    if (!isOpenSearch) {
      setIsOpenSearch(true);
    }
  };
  
  // Temporarily empty function for cart modal
  const handleOpenModal = () => {
    // TODO: Open cart drawer/modal when Redux integration is ready
  };

  return (
    <AppBar
      position="sticky"
      sx={{
        backgroundColor: '#ffff',
        boxShadow: 'none',
        color: '#404040',
        width: '100%',
      }}
    >
      <Toolbar
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: { xs: 'space-between', md: 'space-around' },
          p: { xs: '0px 20px', md: '0' },
        }}
      >
        <LogoWebsite />
        
        {/* Mobile Menu Icon + Search */}
        <Box sx={{ display: { xs: 'flex', md: 'none' }, alignItems: 'center' }}>
          <IconButton onClick={handleDrawerOpen}>
            <MenuOutlinedIcon sx={{ cursor: 'pointer', color: '#000000' }} />
          </IconButton>
          {isOpenSearch ? (
            <Button
              sx={{ color: 'inherit', padding: 0, minWidth: '0px' }}
              onClick={handleCloseSearch}
            >
              <CloseIcon />
            </Button>
          ) : (
            <Button
              onClick={handleOpenSearch}
              sx={{ color: 'inherit', padding: 0, minWidth: '0px' }}
            >
              <SearchOutlinedIcon />
            </Button>
          )}
        </Box>
        
        {/* Desktop Menu */}
        <DesktopMenu options={options} setIsHovered={setIsHovered} setIsOpen={setIsOpen} />
        
        {/* Right Side Icons - Desktop Only */}
        <Box sx={{ gap: { xs: 1, md: 2 }, display: { xs: 'none', md: 'flex' }, alignItems: 'center' }}>
          {isOpenSearch ? (
            <Button
              sx={{ color: 'inherit', padding: '0px', margin: '0px', display: 'block', minWidth: '0px' }}
              onClick={handleCloseSearch}
            >
              <CloseIcon />
            </Button>
          ) : (
            <Button
              onClick={handleOpenSearch}
              sx={{ color: 'inherit', padding: '0px', margin: '0px', display: 'block', minWidth: '0px' }}
            >
              <SearchOutlinedIcon />
            </Button>
          )}
          <FavoriteBorderOutlinedIcon sx={{ cursor: 'pointer' }} />
          <PersonOutlinedIcon sx={{ cursor: 'pointer' }} onClick={() => navigate('/login')} />
          <BadgeNumberShopping badgetItem={cartCount.toString()} handleOpenModal={handleOpenModal} />
        </Box>
        
        {/* Mobile Logo */}
        <Box>
          <LogoMobileWebsite />
        </Box>
        
        {/* Mobile Right Icons */}
        <Box sx={{ display: { xs: 'flex', md: 'none' }, gap: '9px' }}>
          <PersonOutlinedIcon sx={{ cursor: 'pointer' }} onClick={() => navigate('/login')} />
          <FavoriteBorderOutlinedIcon sx={{ cursor: 'pointer' }} />
          <BadgeNumberShopping badgetItem={cartCount.toString()} handleOpenModal={handleOpenModal} />
        </Box>
        
        {/* Mobile Menu Drawer */}
        <NavigationDropMenuMobile open={open} handleDrawerClose={handleDrawerClose} options={options} />
      </Toolbar>
      
      {/* Search Field Below Navbar */}
      {isOpenSearch && <SearchField />}
    </AppBar>
  );
}

export default MainNavBar;
