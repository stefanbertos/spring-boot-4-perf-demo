import CircleIcon from '@mui/icons-material/Circle';
import Box from '@mui/material/Box';
import ToggleButton from '@mui/material/ToggleButton';
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup';
import Tooltip from '@mui/material/Tooltip';
import { Outlet } from 'react-router-dom';
import { useThemeContext } from '../contexts/ThemeContext';
import type { ThemeName } from '../contexts/ThemeContext';
import Sidebar from './Sidebar';

const THEME_OPTIONS: { name: ThemeName; color: string; label: string }[] = [
  { name: 'default', color: '#1e40af', label: 'Default theme (indigo)' },
  { name: 'blue', color: '#0369a1', label: 'Blue theme' },
  { name: 'red', color: '#b91c1c', label: 'Red theme' },
];

export default function RootLayout() {
  const { themeName, setThemeName } = useThemeContext();

  return (
    <Box sx={{ display: 'flex', height: '100vh', overflow: 'hidden' }}>
      <Sidebar />
      <Box component="main" sx={{ flex: 1, overflowY: 'auto', bgcolor: 'grey.50', p: 4 }}>
        <Outlet />
      </Box>

      {/* Theme switcher — fixed in the top-right corner, always visible */}
      <Box sx={{ position: 'fixed', top: 10, right: 14, zIndex: 1200 }}>
        <ToggleButtonGroup
          value={themeName}
          exclusive
          onChange={(_, val: ThemeName | null) => {
            if (val) setThemeName(val);
          }}
          size="small"
          sx={{ bgcolor: 'background.paper', boxShadow: 1, borderRadius: 1 }}
        >
          {THEME_OPTIONS.map(({ name, color, label }) => (
            <Tooltip key={name} title={label} placement="bottom">
              <ToggleButton value={name} sx={{ px: 1, py: 0.5 }}>
                <CircleIcon sx={{ fontSize: 16, color }} />
              </ToggleButton>
            </Tooltip>
          ))}
        </ToggleButtonGroup>
      </Box>
    </Box>
  );
}
