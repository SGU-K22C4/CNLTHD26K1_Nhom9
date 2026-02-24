import { Button, Grid, Skeleton, Container, Typography, useMediaQuery, useTheme, Box } from "@mui/material";
import Masonry from "@mui/lab/Masonry";
import { Link } from "react-router-dom"; // <-- Thay Next Link
import { ImagesMansory } from "../../../shared/utils/ImageData"; // <-- Đổi đường dẫn phù hợp
import { useState, useEffect } from "react";

function Collection() {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down("sm"));
  const [isLoading, setIsLoading] = useState(true);
  // xử lý sau: thay bằng data từ backend
  const fallbackItems = [
    { id: 1, name: "Women", src: "", height: 360 },
    { id: 2, name: "Men", src: "", height: 360 },
    { id: 3, name: "Kids", src: "", height: 360 },
    { id: 4, name: "Accessories", src: "", height: 360 },
  ];
  const collectionItems = ImagesMansory?.length ? ImagesMansory : fallbackItems;

  useEffect(() => {
    setIsLoading(false);
  }, []);

  return (
    <Container>
      {isLoading ? (
        <Box sx={{ mb: 4, mt: 5 }}>
          <Grid container spacing={2}>
            <Grid item xs={12} sm={6} md={6}>
              <Skeleton variant="rectangular" height={"100%"} />
            </Grid>
            <Grid item xs={12} sm={6} md={6}>
              <Skeleton variant="rectangular" height={640} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <Skeleton variant="rectangular" height={340} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <Skeleton variant="rectangular" height={"100%"} />
            </Grid>
          </Grid>
        </Box>
      ) : (
        <>
          <Box sx={{ mt: "6rem", mb: "1.5rem" }}>
            <Typography variant="h5" fontWeight="600" fontFamily="inherit">
              Collection
            </Typography>
          </Box>
          <Masonry columns={2} spacing={{ lg: 6, xs: 2 }} style={{ columnGap: "10px", rowGap: "1rem" }}>
            {collectionItems.map((item, index) => (
              <Link key={item.id} to={`/collection/${item.name.toLowerCase()}`} style={{textDecoration: 'none', color: '#000'}}>
                <Box sx={{ position: "relative" }}>
                  {item.src ? (
                    <img
                      src={item.src}
                      alt={`Image for ${item.name}`}
                      style={{
                        objectFit: isMobile ? "contain" : "cover",
                        maxWidth: "100%",
                        width: "100%",
                        height: isMobile ? "auto" : item.height || "auto",
                        display: "block"
                      }}
                    />
                  ) : (
                    <Box
                      sx={{
                        height: item.height || 360,
                        backgroundColor: "#F4F4F4",
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                      }}
                    >
                      <Typography sx={{ color: "#666" }}>{item.name}</Typography>
                    </Box>
                  )}
                  <Typography>{isMobile && `${item.name}`}</Typography>

                  {!isMobile && (
                    <Button
                      sx={{
                        position: "absolute",
                        bottom: "2rem",
                        right: "3rem",
                        textTransform: "capitalize",
                        color: "#000",
                        padding: "0.5rem 3rem",
                        background: "#fff",
                      }}
                    >
                      {item.name}
                    </Button>
                  )}
                </Box>
              </Link>
            ))}
          </Masonry>
        </>
      )}
    </Container>
  );
}

export default Collection;
