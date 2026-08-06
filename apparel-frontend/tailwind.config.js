/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts}",
  ],
  theme: {
    extend: {
      colors: {
        ivory: '#FAF7F2',
        wine: {
          DEFAULT: '#7A2E38',
          dark: '#5E2129',
          light: '#93414C',
        },
        blush: '#F1DDE0',
        ink: '#2B2420',
        gold: '#C9A24B',
      },
      fontFamily: {
        display: ['"Fraunces"', 'serif'],
        body: ['"Manrope"', 'sans-serif'],
      },
      borderRadius: {
        // the "petal" motif: one corner rounded further than the rest — used on
        // product cards, primary buttons, and OTP boxes as the app's signature shape
        petal: '28px 8px 28px 8px',
        'petal-sm': '14px 4px 14px 4px',
      },
      boxShadow: {
        card: '0 2px 20px -4px rgba(43, 36, 32, 0.12)',
        cardHover: '0 12px 32px -8px rgba(122, 46, 56, 0.25)',
      },
    },
  },
  plugins: [],
}
