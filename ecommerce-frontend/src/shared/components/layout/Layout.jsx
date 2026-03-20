import Footer from './Footer'
import BannerHeader from './headers/BannerHeader'
import NavBar from './headers/NavBar'

export default function Layout({ children }) {
  return (
    <div className="flex min-h-screen flex-col bg-white text-gray-900">
      <BannerHeader />
      <NavBar />
      <main className="flex-1">
        {children}
      </main>
      
      <Footer />
    </div>
  )
}