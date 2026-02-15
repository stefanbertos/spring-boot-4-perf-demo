import { CssBaseline, theme, ThemeProvider } from 'perf-ui-components';
import { RouterProvider } from 'react-router-dom';
import router from './router';

export default function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <RouterProvider router={router} />
    </ThemeProvider>
  );
}
