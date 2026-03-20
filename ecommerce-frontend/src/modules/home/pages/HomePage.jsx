import BestSellers from '../components/BestSellers'
import Collection from '../components/Collection'
import FollowUs from '../components/FollowUs'
import HeroSection from '../components/HeroSection'
import MoodiWeek from '../components/MoodiWeek'
import Sustainability from '../components/Sustainability'

export default function HomePage() {
  return (
    <>
      <HeroSection />
      <BestSellers />
      <Collection />
      <MoodiWeek />
      <Sustainability />
      <FollowUs />
    </>
  )
}