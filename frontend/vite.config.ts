import { fileURLToPath } from 'node:url'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

export default defineConfig({
  plugins: [react()],
  resolve: {
    // @/ aponta para src/ (o mesmo alias está em tsconfig.app.json)
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
  },
  server: { port: 5173, strictPort: true }, // o CORS do backend libera exatamente esta porta
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
  },
})
