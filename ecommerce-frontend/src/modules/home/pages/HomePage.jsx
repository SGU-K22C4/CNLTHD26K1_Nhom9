import BestSellers from '../components/BestSellers'
import Collection from '../components/Collection'
import FeaturedProducts from '../components/FeaturedProducts'
import FollowUs from '../components/FollowUs'
import HeroSection from '../components/HeroSection'
import MoodiWeek from '../components/MoodiWeek'
import Sustainability from '../components/Sustainability'

export default function HomePage() {
  return (
    <>
      <HeroSection />
      <BestSellers />
      <FeaturedProducts />
      <Collection />
      <MoodiWeek />
      <Sustainability />
      <FollowUs />
    </>
  )
}