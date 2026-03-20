import ShoppingBagOutlinedIcon from '@mui/icons-material/ShoppingBagOutlined'
import { Badge, IconButton } from '@mui/material'

/**
 * @typedef {Object} Types
 * @property {string} badgetItem
 * @property {(event: React.MouseEvent<HTMLButtonElement>) => void} handleOpenModal
 */
//gán cứng fe giá trị là 0, sau này sẽ lấy số lượng sản phẩm trong giỏ hàng từ Redux store
export default function BadgeNumberShopping({ badgetItem = '0', handleOpenModal }) {
    //export default function BadgeNumberShopping(props: PropsWithChildren<Types>) {
    //lấy số lượng sản phẩm trong giỏ hàng từ Redux store
    //const  {badgetItem , handleOpenModal} =props 
  return (
    <Badge
      badgeContent={badgetItem}
      sx={{
        '& .MuiBadge-badge': {
          color: 'white',
          backgroundColor: '#5A6D57',
        },
      }}
    >
      <IconButton color="inherit" onClick={handleOpenModal}>
        <ShoppingBagOutlinedIcon />
      </IconButton>
    </Badge>
  )
}
