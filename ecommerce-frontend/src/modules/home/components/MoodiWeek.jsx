import { Container, Box, Typography } from "@mui/material";
import { Swiper, SwiperSlide } from "swiper/react";
import { Pagination, Autoplay } from "swiper/modules";
import "swiper/css/pagination";
import "swiper/css";

// Giữ lại các biến ảnh mảng import tĩnh của bạn
import mon from "../../../../public/assets/images/hero-desktop.webp"; // <-- Điều chỉnh lại path ảnh
import tue from "../../../../public/assets/images/hero-desktop.webp";
import wed from "../../../../public/assets/images/hero-desktop.webp";
import thu from "../../../../public/assets/images/hero-desktop.webp";
import fri from "../../../../public/assets/images/hero-desktop.webp";
import sat from "../../../../public/assets/images/hero-desktop.webp";
import sun from "../../../../public/assets/images/hero-desktop.webp";

const ImageWeek = [
  { imageSrc: mon, imageWeek: "Monday" },
  { imageSrc: tue, imageWeek: "Tuesday" },
  { imageSrc: wed, imageWeek: "Wednesday" },
  { imageSrc: thu, imageWeek: "Thursday" },
  { imageSrc: fri, imageWeek: "Friday" },
  { imageSrc: sat, imageWeek: "Saturday" },
  { imageSrc: sun, imageWeek: "Sunday" },
];

function MoodiWeek() {
  return (
    <Container>
      <Box sx={{ mt: "6rem", mb: "1.5rem" }}>
        <Typography variant="h5" fontWeight="600" fontFamily="inherit">
          ModiWeek
        </Typography>
        <Swiper
          style={{
            paddingBottom: "4rem",
            marginBottom: "4rem",
          }}
          spaceBetween={20}
          breakpoints={{
            0: { slidesPerView: 2 },
            640: { slidesPerView: 4 },
            1024: { slidesPerView: 4 },
          }}
          autoplay={{
            delay: 2500,
            disableOnInteraction: false,
          }}
          loop={true}
          slidesPerView={2}
          pagination={{ clickable: true }}
          modules={[Pagination, Autoplay]}
        >
          {ImageWeek.map((items, index) => (
            <SwiperSlide key={index}>
              <img
                src={items.imageSrc}
                alt=" group imsgsrd"
                style={{
                  objectFit: "cover",
                  width: "100%",
                  height: "100%", 
                  display: "block"
                }}
              />
              <Typography sx={{ paddingTop: "1rem" }} fontWeight="600">
                {items.imageWeek}
              </Typography>
            </SwiperSlide>
          ))}
        </Swiper>
      </Box>
    </Container>
  );
}

export default MoodiWeek;
