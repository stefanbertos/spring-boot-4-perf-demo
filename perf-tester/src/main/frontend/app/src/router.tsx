import { createBrowserRouter, Navigate } from 'react-router-dom';
import { RootLayout } from '@/layouts';
import {
  AdminPage,
  DashboardsPage,
  NotFoundPage,
  SendTestPage,
  TestRunComparePage,
  TestRunDetailPage,
  TestRunsPage,
} from '@/pages';

const router = createBrowserRouter([
  {
    element: <RootLayout />,
    children: [
      { index: true, element: <Navigate to="/dashboards" replace /> },
      { path: 'send', element: <SendTestPage /> },
      { path: 'test-runs', element: <TestRunsPage /> },
      { path: 'test-runs/compare', element: <TestRunComparePage /> },
      { path: 'test-runs/:id', element: <TestRunDetailPage /> },
      { path: 'dashboards', element: <DashboardsPage /> },
      { path: 'admin', element: <AdminPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]);

export default router;
