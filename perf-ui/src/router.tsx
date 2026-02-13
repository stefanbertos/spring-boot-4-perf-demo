import { createBrowserRouter } from 'react-router-dom';
import { RootLayout } from '@/layouts';
import {
  AdminPage,
  DashboardPage,
  NotFoundPage,
  SendTestPage,
  TestRunDetailPage,
  TestRunsPage,
} from '@/pages';

const router = createBrowserRouter([
  {
    element: <RootLayout />,
    children: [
      { index: true, element: <DashboardPage /> },
      { path: 'send', element: <SendTestPage /> },
      { path: 'test-runs', element: <TestRunsPage /> },
      { path: 'test-runs/:id', element: <TestRunDetailPage /> },
      { path: 'admin', element: <AdminPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]);

export default router;
