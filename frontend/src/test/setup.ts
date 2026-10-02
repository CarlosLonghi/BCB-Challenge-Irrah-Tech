import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterEach } from 'vitest';

// Sem `globals: true` no Vitest, o Testing Library não desmonta os componentes sozinho.
afterEach(cleanup);
