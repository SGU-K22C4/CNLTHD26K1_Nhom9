import SearchIcon from '@mui/icons-material/Search'
import { Container, InputAdornment, TextField } from '@mui/material'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

export default function SearchField({ initialQuery = '' }) {
  const navigate = useNavigate()
  const [query, setQuery] = useState(initialQuery)

  const handleChangeSearch = (event) => {
    event.preventDefault()
    navigate(`/search?q=${encodeURIComponent(query)}`)
  }

  return (
    <Container>
      <form onSubmit={handleChangeSearch} style={{ paddingBottom: '1rem' }}>
        <TextField
          defaultValue={initialQuery}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Search"
          sx={{ borderBottom: '1px solid #ADADAD', paddingLeft: '1rem', paddingBottom: '0.7rem' }}
          fullWidth
          variant="standard"
          focused
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon />
              </InputAdornment>
            ),
          }}
        />
      </form>
    </Container>
  )
}
