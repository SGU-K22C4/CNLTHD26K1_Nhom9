import { Box, Container, Typography, Grid, useTheme, useMediaQuery } from '@mui/material';
import Masonry from '@mui/lab/Masonry';
import { ImagesMansory } from '../../../shared/utils/ImageData';  // <-- Điều chỉnh đường dẫn đến file utils
import React from 'react';

function FollowUs() {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'));
  const images = isMobile ? ImagesMansory?.slice(0, 4) : ImagesMansory;

  return (
    <Container sx={{ pb: '5rem' }}>
      <Box sx={{ mt: '6rem', mb: '1.5rem' }}>
        <Typography variant="h5" fontWeight="600" fontFamily="inherit">
          Follow us @modimal
        </Typography>
      </Box>

      {isMobile ? (
        <Grid container spacing={2}>
          {images?.map((item) => (
            <Grid size={{ xs: 6, md: 4 }} key={item.id}>
              <img
                src={item.image}
                alt="Image galerrey"
                style={{ objectFit: 'cover', width: '100%', display: 'block' }}
              />
            </Grid>
          ))}
        </Grid>
      ) : (
        <Masonry spacing={{ lg: 0, xs: 0 }} columns={{ xs: 2, md: 3 }}>
          {ImagesMansory?.map((item) => (
            <React.Fragment key={item.id}>
              <img
                src={item.image}
                alt={`Image for ${item.image}`}
                style={{
                  cursor: 'pointer',
                  objectFit: 'cover',
                  width: '100%',
                  margin: 0,
                  display: 'block'
                }}
              />
            </React.Fragment>
          ))}
        </Masonry>
      )}
    </Container>
  );
}

export default FollowUs;
