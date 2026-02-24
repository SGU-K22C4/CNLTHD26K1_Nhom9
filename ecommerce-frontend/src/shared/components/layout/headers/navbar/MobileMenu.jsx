import { List, ListItem, ListItemText } from '@mui/material'
import { Link } from 'react-router-dom'

export default function MobileMenu({ options = [], onNavigate }) {
  return (
    <List>
      {options.map((option, index) => {
        return (
          <ListItem key={option?.id || index}>
            <Link 
              to={option?.path || '/'} 
              onClick={onNavigate} 
              style={{ width: '100%', textDecoration: 'none', color: 'inherit' }}
            >
              <ListItemText primary={option?.name} />
            </Link>
          </ListItem>
        )
      })}
    </List>
  )
}
