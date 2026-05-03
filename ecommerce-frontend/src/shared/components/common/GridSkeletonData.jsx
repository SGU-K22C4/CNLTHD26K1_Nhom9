import { Grid } from '@mui/material';

import SkeletonData from './SkeletonData';

function GridSkeletonData() {
  return (
    <Grid container spacing={4}>
      <Grid size={{ xs: 6, md: 4 }}>
        <SkeletonData />
      </Grid>
      <Grid size={{ xs: 6, md: 4 }}>
        <SkeletonData />
      </Grid>
      <Grid size={{ xs: 6, md: 4 }}>
        <SkeletonData />
      </Grid>
    </Grid>
  );
}

export default GridSkeletonData;