import { Button, Grid, Skeleton, Container, Typography, useMediaQuery, useTheme, Box } from '@mui/material'
import Masonry from '@mui/lab/Masonry'
import { Link } from 'react-router-dom'
import { useHomeProducts } from '../hooks/useHomeProducts'

function Collection() {
  const theme = useTheme()
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'))
  const { collections, loading: isLoading } = useHomeProducts()

  const fallbackItems = [
    { id: 'female', name: 'Women', src: '', height: 420, count: 0, to: '/collection/women' },
    { id: 'male', name: 'Men', src: '', height: 420, count: 0, to: '/collection/men' },
  ]

  const collectionItems = collections.length ? collections : fallbackItems

  return (
    <Container>
      {isLoading ? (
        <Box sx={{ mb: 4, mt: 5 }}>
          <Grid container spacing={2}>
            <Grid size={{ xs: 12, sm: 6 }}>
              <Skeleton variant="rectangular" height={420} />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <Skeleton variant="rectangular" height={420} />
            </Grid>
          </Grid>
        </Box>
      ) : (
        <>
          <Box sx={{ mt: '6rem', mb: '1.5rem' }}>
            <Typography variant="h5" fontWeight="600" fontFamily="inherit">
              Collection
            </Typography>
          </Box>
          <Masonry columns={2} spacing={{ lg: 6, xs: 2 }} style={{ columnGap: '10px', rowGap: '1rem' }}>
            {collectionItems.map((item) => (
              <Link key={item.id} to={item.to} style={{ textDecoration: 'none', color: '#000' }}>
                <Box sx={{ position: 'relative' }}>
                  {item.src ? (
                    <img
                      src={item.src}
                      alt={`Image for ${item.name}`}
                      style={{
                        objectFit: isMobile ? 'contain' : 'cover',
                        maxWidth: '100%',
                        width: '100%',
                        height: isMobile ? 'auto' : item.height || 'auto',
                        display: 'block',
                      }}
                    />
                  ) : (
                    <Box
                      sx={{
                        height: item.height || 360,
                        backgroundColor: '#F4F4F4',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                      }}
                    >
                      <Typography sx={{ color: "#666" }}>{item.name}</Typography>
                    </Box>
                  )}
                  <Typography sx={{ mt: 1 }}>{isMobile && `${item.name} (${item.count})`}</Typography>

                  {!isMobile && (
                    <Button
                      sx={{
                        position: 'absolute',
                        bottom: '2rem',
                        right: '3rem',
                        textTransform: 'capitalize',
                        color: '#000',
                        padding: '0.5rem 2rem',
                        background: '#fff',
                      }}
                    >
                      {`${item.name} (${item.count})`}
                    </Button>
                  )}
                </Box>
              </Link>
            ))}
          </Masonry>
        </>
      )}
    </Container>
  )
}

export default Collection
