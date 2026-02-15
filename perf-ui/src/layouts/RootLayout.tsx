import Box from '@mui/material/Box';
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';

export default function RootLayout() {
  return (
    <Box sx={{ display: 'flex', height: '100vh', overflow: 'hidden' }}>
      <Sidebar />
      <Box component="main" sx={{ flex: 1, overflowY: 'auto', bgcolor: 'grey.50', p: 4 }}>
        <Outlet />
      </Box>
    </Box>
  );
}
