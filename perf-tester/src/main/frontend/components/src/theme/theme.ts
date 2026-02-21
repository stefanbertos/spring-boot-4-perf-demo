import { createTheme } from '@mui/material/styles';
import type { Theme } from '@mui/material/styles';

export interface AppTheme {
  muiTheme: Theme;
  navColors: { bg: string; active: string; hover: string };
}

const font =
  'Inter, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif';

const baseComponents = {
  MuiButton: { defaultProps: { disableElevation: true } },
  MuiCard: {
    defaultProps: { elevation: 0 },
    styleOverrides: {
      root: { border: '1px solid', borderColor: 'rgba(0, 0, 0, 0.12)' },
    },
  },
  MuiTextField: { defaultProps: { variant: 'outlined' as const, size: 'small' as const } },
};

/**
 * Default theme — indigo/navy primary, purple secondary, dark-grey sidebar.
 * This is the out-of-the-box colour scheme.
 */
export const themeDefault: AppTheme = {
  muiTheme: createTheme({
    palette: { mode: 'light', primary: { main: '#1e40af' }, secondary: { main: '#7c3aed' } },
    typography: { fontFamily: font },
    shape: { borderRadius: 8 },
    components: baseComponents,
  }),
  navColors: { bg: '#111827', active: '#1f2937', hover: '#1f2937' },
};

/**
 * Blue theme — sky-blue primary, dark-blue sidebar.
 */
export const themeBlue: AppTheme = {
  muiTheme: createTheme({
    palette: { mode: 'light', primary: { main: '#0369a1' }, secondary: { main: '#0284c7' } },
    typography: { fontFamily: font },
    shape: { borderRadius: 8 },
    components: baseComponents,
  }),
  navColors: { bg: '#0c4a6e', active: '#075985', hover: '#075985' },
};

/**
 * Red theme — crimson primary, dark-red sidebar.
 */
export const themeRed: AppTheme = {
  muiTheme: createTheme({
    palette: { mode: 'light', primary: { main: '#b91c1c' }, secondary: { main: '#dc2626' } },
    typography: { fontFamily: font },
    shape: { borderRadius: 8 },
    components: baseComponents,
  }),
  navColors: { bg: '#450a0a', active: '#7f1d1d', hover: '#7f1d1d' },
};
