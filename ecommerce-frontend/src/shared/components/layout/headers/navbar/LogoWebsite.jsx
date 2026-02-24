import { Box } from '@mui/material'
import { Link } from 'react-router-dom'

function LogoWebsite() {
  return (
    <Box sx={{ display: { xs: 'none', md: 'block' } }}>
      <Link to="/">
        <img src="/assets/icons/Logo.png" alt="Logo for page" width={184} height={46} />
      </Link>
    </Box>
  )
}

export default LogoWebsite
