const withAlpha = (variable) => `rgb(var(${variable}) / <alpha-value>)`

/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        // Површини
        page: withAlpha('--page'),
        surface: withAlpha('--surface'),
        'surface-2': withAlpha('--surface-2'),
        line: withAlpha('--line'),

        // Текст
        ink: {
          DEFAULT: withAlpha('--ink'),
          soft: withAlpha('--ink-soft'),
          mute: withAlpha('--ink-mute'),
        },

        // Акценти
        brand: {
          DEFAULT: withAlpha('--brand'),
          on: withAlpha('--on-brand'),
        },
        accent: withAlpha('--accent'),
        warm: withAlpha('--warm'),
        good: withAlpha('--good'),
        bad: withAlpha('--bad'),
      },
      fontFamily: {
        display: ['"Space Grotesk"', 'system-ui', 'sans-serif'],
        body: ['"Inter"', 'system-ui', 'sans-serif'],
        mono: ['"JetBrains Mono"', 'monospace'],
      },
      boxShadow: {
        soft: '0 1px 2px rgb(var(--shadow) / 0.06), 0 6px 16px -8px rgb(var(--shadow) / 0.16)',
        lift: '0 2px 6px rgb(var(--shadow) / 0.08), 0 18px 36px -18px rgb(var(--shadow) / 0.30)',
        brand: '0 1px 2px rgb(var(--brand) / 0.30), 0 8px 20px -8px rgb(var(--brand) / 0.55)',
      },
      keyframes: {
        'fade-in': {
          from: { opacity: '0', transform: 'translateY(6px)' },
          to: { opacity: '1', transform: 'translateY(0)' },
        },
        'pop-in': {
          from: { opacity: '0', transform: 'scale(0.97)' },
          to: { opacity: '1', transform: 'scale(1)' },
        },
      },
      animation: {
        'fade-in': 'fade-in 0.25s ease-out',
        'pop-in': 'pop-in 0.2s ease-out',
      },
    },
  },
  plugins: [],
}
