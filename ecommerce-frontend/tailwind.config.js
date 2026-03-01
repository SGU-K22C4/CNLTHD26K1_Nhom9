/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#eff6ff',
          100: '#dbeafe',
          200: '#bfdbfe',
          300: '#93c5fd',
          400: '#60a5fa',
          500: '#3b82f6',
          600: '#2563eb',
          700: '#1d4ed8',
          800: '#1e40af',
          900: '#1e3a8a',
        },
        brand: {
          olive: '#5A6D57',      // Figma primary-600 — banner, buttons, accents
          green: '#748C70',      // Figma Primary — hover states, scrollbar
          charcoal: '#2C2C2C',   // Footer background
          light: '#FAFAFA',      // Header background
          sage: '#E8EBE4',       // Figma stepper / quantity background
          'sage-hover': '#D8DBD4', // Figma stepper hover
        },
        // Figma neutral grays
        neutral: {
          202020: '#202020',
          404040: '#404040',
          cbcbcb: '#CBCBCB',
        },
        // Figma semantic blacks/whites
        ink: '#0C0C0C',
      },
      fontFamily: {
        sans: ['Montserrat', 'Inter', 'system-ui', 'sans-serif'],
        display: ['League Spartan', 'sans-serif'],
      },
      maxWidth: {
        '8xl': '1440px',
      },
    },
  },
  plugins: [],
}
