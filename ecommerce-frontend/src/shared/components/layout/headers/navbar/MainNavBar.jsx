import { AppBar, Toolbar, Box, IconButton, Button } from '@mui/material';
import { Link } from 'react-router-dom';
import LogoWebsite from './LogoWebsite';
import { useCartContext } from '../../../../../modules/cart/context/CartContext';
import SearchOutlinedIcon from '@mui/icons-material/SearchOutlined';
import DesktopMenu from './DesktopMenu';
import LogoMobileWebsite from './LogoMobileWebsite';
import MenuOutlinedIcon from '@mui/icons-material/MenuOutlined';
import { useState } from 'react';
import SearchField from './SearchField';
import NavigationDropMenuMobile from './NavigationDropMenuMobile';
import BadgeNumberShopping from './BadgeNumberShopping';
import FavoriteBorderOutlinedIcon from '@mui/icons-material/FavoriteBorderOutlined';
import CloseIcon from '@mui/icons-material/Close';
import CartDrawer from '../../../../../modules/cart/components/CartDrawer';

function MainNavBar(props) {
  const { options, setIsHovered, setIsOpen } = props;

  const { openDrawer, closeDrawer, totalItems } = useCartContext();

  const [open, setOpen] = useState(false);
  const [isOpenSearch, setIsOpenSearch] = useState(false);

  const handleDrawerOpen = () => setOpen(true);
  const handleDrawerClose = () => setOpen(false);
  const handleCloseSearch = () => setIsOpenSearch(false);
  const handleToggleSearch = () => {
    setIsOpenSearch((prev) => {
      if (!prev) closeDrawer();   // mở search → đóng cart
      return !prev;
    });
  };
  const handleOpenCart = (e) => {
    setIsOpenSearch(false);        // đóng search → mở cart
    openDrawer(e);
  };

  return (
    <AppBar
      position="sticky"
      sx={{
        backgroundColor: '#ffff',
        boxShadow: 'none',
        color: '#404040',
        width: '100%',
        overflow: 'visible',
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
          <Button
            onClick={handleToggleSearch}
            sx={{ color: 'inherit', padding: 0, minWidth: '0px' }}
          >
            {isOpenSearch ? <CloseIcon /> : <SearchOutlinedIcon />}
          </Button>
        </Box>

        {/* Desktop Menu */}
        <DesktopMenu options={options} setIsHovered={setIsHovered} setIsOpen={setIsOpen} />

        {/* Right Side Icons - Desktop Only */}
        <Box sx={{ gap: { xs: 1, md: 2 }, display: { xs: 'none', md: 'flex' }, alignItems: 'center' }}>
          <Button
            onClick={handleToggleSearch}
            sx={{ color: 'inherit', padding: '0px', margin: '0px', display: 'block', minWidth: '0px' }}
          >
            {isOpenSearch ? <CloseIcon /> : <SearchOutlinedIcon />}
          </Button>
          <Link to="/wishlist" style={{ color: 'inherit', display: 'flex' }}>
            <FavoriteBorderOutlinedIcon sx={{ cursor: 'pointer' }} />
          </Link>
          <div className="relative">
            <BadgeNumberShopping badgetItem={totalItems.toString()} handleOpenModal={handleOpenCart} />
          </div>
        </Box>

        {/* Mobile Logo */}
        <Box>
          <LogoMobileWebsite />
        </Box>

        {/* Mobile Right Icons */}
        <Box sx={{ display: { xs: 'flex', md: 'none' }, gap: '9px', alignItems: 'center' }}>
          <Link to="/wishlist" style={{ color: 'inherit', display: 'flex' }}>
            <FavoriteBorderOutlinedIcon sx={{ cursor: 'pointer' }} />
          </Link>
          <div className="relative z-[10]">
            <BadgeNumberShopping badgetItem={totalItems.toString()} handleOpenModal={openDrawer} />
          </div>
        </Box>

        {/* Mobile Menu Drawer */}
        <NavigationDropMenuMobile open={open} handleDrawerClose={handleDrawerClose} options={options} />
      </Toolbar>

      {/* Search Dropdown — beautiful animated */}
      {isOpenSearch && <SearchField onClose={handleCloseSearch} />}

      {/* Cart Drawer */}
      <CartDrawer />
    </AppBar>
  );
}

export default MainNavBar;
