import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';
import AssessmentIcon from '@mui/icons-material/Assessment';
import DashboardIcon from '@mui/icons-material/Dashboard';
import MonitorIcon from '@mui/icons-material/Monitor';
import ScienceIcon from '@mui/icons-material/Science';
import SpeedIcon from '@mui/icons-material/Speed';
import Box from '@mui/material/Box';
import List from '@mui/material/List';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import Typography from '@mui/material/Typography';
import { NavLink, useLocation } from 'react-router-dom';

const navigation = [
  { name: 'Dashboard', to: '/', icon: DashboardIcon },
  { name: 'Send Test', to: '/send', icon: ScienceIcon },
  { name: 'Test Runs', to: '/test-runs', icon: AssessmentIcon },
  { name: 'Dashboards', to: '/dashboards', icon: MonitorIcon },
  { name: 'Admin', to: '/admin', icon: AdminPanelSettingsIcon },
];

export default function Sidebar() {
  const location = useLocation();

  const isActive = (to: string) => {
    if (to === '/') return location.pathname === '/';
    return location.pathname.startsWith(to);
  };

  return (
    <Box
      component="aside"
      sx={{
        width: 256,
        height: '100vh',
        display: 'flex',
        flexDirection: 'column',
        bgcolor: 'grey.900',
        color: 'common.white',
      }}
    >
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, height: 64, px: 3 }}>
        <SpeedIcon sx={{ fontSize: 28, color: 'primary.light' }} />
        <Typography variant="subtitle1" fontWeight="bold" letterSpacing="-0.025em">
          Perf Tester
        </Typography>
      </Box>

      <List component="nav" sx={{ mt: 1, px: 1.5, flex: 1 }}>
        {navigation.map((item) => (
          <ListItemButton
            key={item.to}
            component={NavLink}
            to={item.to}
            selected={isActive(item.to)}
            sx={{
              borderRadius: 2,
              mb: 0.5,
              color: 'grey.400',
              '&.Mui-selected': {
                bgcolor: 'grey.800',
                color: 'common.white',
                '&:hover': { bgcolor: 'grey.700' },
              },
              '&:hover': { bgcolor: 'grey.800', color: 'common.white' },
            }}
          >
            <ListItemIcon sx={{ color: 'inherit', minWidth: 36 }}>
              <item.icon fontSize="small" />
            </ListItemIcon>
            <ListItemText
              primary={item.name}
              primaryTypographyProps={{ variant: 'body2', fontWeight: 500 }}
            />
          </ListItemButton>
        ))}
      </List>
    </Box>
  );
}
