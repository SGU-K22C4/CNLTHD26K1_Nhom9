import { AppBar, Toolbar, Box, IconButton, Button, Menu, MenuItem, Typography } from '@mui/material';
import { Link } from 'react-router-dom';
import LogoWebsite from './LogoWebsite';
import { useCartContext } from '../../../../../modules/cart/hooks/useCartContext';
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
import CartDrawer from '../../../../../modules/cart/components/CartDrawer';
import { useAuth } from '../../../../../modules/auth/hooks/useAuth';

function MainNavBar(props) {
  const { options, setIsHovered, setIsOpen } = props;
  const navigate = useNavigate();
  const { user, logout } = useAuth();

  const { openDrawer, closeDrawer, totalItems } = useCartContext();

  const [open, setOpen] = useState(false);
  const [isOpenSearch, setIsOpenSearch] = useState(false);
  // Dropdown anchor cho user menu
  const [userMenuAnchor, setUserMenuAnchor] = useState(null);

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

  const handleUserIconClick = (e) => {
    if (user) {
      // Đã login → mở dropdown
      setUserMenuAnchor(e.currentTarget);
    } else {
      // Chưa login → về trang đăng nhập
      navigate('/login');
    }
  };

  const handleUserMenuClose = () => setUserMenuAnchor(null);

  const handleLogout = () => {
    handleUserMenuClose();
    logout(navigate);
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
          justifyContent: 'space-between',
          p: { xs: '0px 20px', md: '0px 40px' },
        }}
      >
        {/* === LEFT SECTION: Logo (Desktop) + Menu Icon & Search (Mobile) === */}
        <Box sx={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'flex-start' }}>
          <LogoWebsite />
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
        </Box>

        {/* === CENTER SECTION: Desktop Menu (Desktop) + Mobile Logo (Mobile) === */}
        <Box sx={{ display: 'flex', justifyContent: 'center' }}>
          <DesktopMenu options={options} setIsHovered={setIsHovered} setIsOpen={setIsOpen} />
          <LogoMobileWebsite />
        </Box>

        {/* === RIGHT SECTION: Icons === */}
        <Box sx={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: { xs: '9px', md: 2 } }}>
          <Button
            onClick={handleToggleSearch}
            sx={{ color: 'inherit', padding: '0px', margin: '0px', display: { xs: 'none', md: 'block' }, minWidth: '0px' }}
          >
            {isOpenSearch ? <CloseIcon /> : <SearchOutlinedIcon />}
          </Button>

          {/* User Icon — thông minh: đã login → dropdown, chưa login → về /login */}
          <IconButton
            onClick={handleUserIconClick}
            sx={{ color: 'inherit', padding: '4px' }}
            aria-label={user ? 'Tài khoản' : 'Đăng nhập'}
          >
            <PersonOutlinedIcon sx={{ cursor: 'pointer' }} />
          </IconButton>
          <Menu
            anchorEl={userMenuAnchor}
            open={Boolean(userMenuAnchor)}
            onClose={handleUserMenuClose}
            anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
            transformOrigin={{ vertical: 'top', horizontal: 'right' }}
            PaperProps={{ sx: { mt: 1, minWidth: 160, boxShadow: '0 4px 20px rgba(0,0,0,0.1)' } }}
          >
            {user && (
              <Box sx={{ px: 2, py: 1, borderBottom: '1px solid #f0f0f0' }}>
                <Typography variant="caption" color="text.secondary">Xin chào,</Typography>
                <Typography variant="body2" fontWeight={600}>{user.firstName} {user.lastName}</Typography>
              </Box>
            )}
            <MenuItem onClick={() => { handleUserMenuClose(); navigate('/profile'); }} sx={{ fontSize: 14 }}>
              Thông tin tài khoản
            </MenuItem>
            <MenuItem onClick={() => { handleUserMenuClose(); navigate('/orders'); }} sx={{ fontSize: 14 }}>
              Đơn hàng của tôi
            </MenuItem>
            <MenuItem onClick={() => { handleUserMenuClose(); navigate('/wallet'); }} sx={{ fontSize: 14 }}>
              Ví điểm tích lũy
            </MenuItem>
            <MenuItem onClick={handleLogout} sx={{ fontSize: 14, color: 'error.main' }}>
              Đăng xuất
            </MenuItem>
          </Menu>

          <Link to="/wishlist" style={{ color: 'inherit', display: 'flex' }}>
            <FavoriteBorderOutlinedIcon sx={{ cursor: 'pointer' }} />
          </Link>
          <Box className="relative z-[10]">
            <BadgeNumberShopping badgetItem={totalItems.toString()} handleOpenModal={handleOpenCart} />
          </Box>
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
