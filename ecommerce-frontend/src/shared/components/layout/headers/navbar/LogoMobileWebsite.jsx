import { Box } from '@mui/material'
import { Link } from 'react-router-dom'

function LogoMobileWebsite() {
  return (
    <Box
      sx={{
        display: { xs: 'flex', md: 'none' },
        alignItems: 'center',
        justifyContent: 'center',
      }}
    >
      <Link to="/">
        <img alt="Logo for shop" src="/assets/icons/Logo.png" width={120} height={30} />
      </Link>
    </Box>
  )
}

export default LogoMobileWebsite
