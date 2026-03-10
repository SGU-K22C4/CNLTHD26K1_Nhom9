import { Box, Modal } from '@mui/material'
import { useState } from 'react'
import { NAV_DATA } from '../../../../lib/navData'
import MainNavBar from './navbar/MainNavBar'

export default function NavBar() {
  const [isHoverd, setIsHovered] = useState('')
  const [isOpen, setIsOpen] = useState(false)

  return (
    <>
      <MainNavBar
        setIsOpen={setIsOpen}
        setIsHovered={setIsHovered}
        options={NAV_DATA}
      />

      {/* Modal for hover menus disabled - simplified navigation */}
      {/* Will re-enable when implementing detailed category dropdowns */}
      {isOpen && isHoverd && (
        <Modal 
          open={false}
          sx={{ backdropFilter: 'blur(5px)', border: 'none' }}
        >
          <Box onMouseLeave={() => setIsOpen(false)}>
            {/* Mega menu content would go here */}
          </Box>
        </Modal>
      )}
    </>
  )
}

