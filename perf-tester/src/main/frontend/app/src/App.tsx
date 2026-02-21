import { CssBaseline, ThemeProvider } from 'perf-ui-components';
import { RouterProvider } from 'react-router-dom';
import { ThemeContextProvider, useThemeContext } from './contexts/ThemeContext';
import router from './router';

function AppContent() {
  const { activeTheme } = useThemeContext();
  return (
    <ThemeProvider theme={activeTheme.muiTheme}>
      <CssBaseline />
      <RouterProvider router={router} />
    </ThemeProvider>
  );
}

export default function App() {
  return (
    <ThemeContextProvider>
      <AppContent />
    </ThemeContextProvider>
  );
}
