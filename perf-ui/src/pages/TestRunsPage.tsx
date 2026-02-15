import InboxIcon from '@mui/icons-material/Inbox';
import Box from '@mui/material/Box';
import { Alert, Chip, DataTable, EmptyState, Loading, PageHeader } from 'perf-ui-components';
import type { DataTableColumn } from 'perf-ui-components';
import { useNavigate } from 'react-router-dom';
import { getTestRuns } from '@/api';
import { useApi } from '@/hooks';
import type { TestRunResponse } from '@/types/api';

const statusColor: Record<string, 'success' | 'error' | 'warning' | 'info' | 'default'> = {
  COMPLETED: 'success',
  FAILED: 'error',
  RUNNING: 'info',
  TIMEOUT: 'warning',
};

const columns: DataTableColumn<TestRunResponse>[] = [
  { id: 'id', label: 'ID', render: (row) => row.id },
  { id: 'testId', label: 'Test ID', render: (row) => row.testId },
  {
    id: 'status',
    label: 'Status',
    render: (row) => (
      <Chip label={row.status} size="small" color={statusColor[row.status] ?? 'default'} />
    ),
  },
  { id: 'messages', label: 'Messages', align: 'right', render: (row) => row.messageCount },
  {
    id: 'tps',
    label: 'TPS',
    align: 'right',
    render: (row) => (row.tps != null ? row.tps.toFixed(1) : '-'),
  },
  {
    id: 'avgLatency',
    label: 'Avg Latency',
    align: 'right',
    render: (row) => (row.avgLatencyMs != null ? `${row.avgLatencyMs.toFixed(0)} ms` : '-'),
  },
  {
    id: 'duration',
    label: 'Duration',
    align: 'right',
    render: (row) => (row.durationMs != null ? `${(row.durationMs / 1000).toFixed(1)}s` : '-'),
  },
  {
    id: 'startTime',
    label: 'Start Time',
    render: (row) => new Date(row.startTime).toLocaleString(),
  },
];

export default function TestRunsPage() {
  const { data: runs, loading, error } = useApi(() => getTestRuns());
  const navigate = useNavigate();

  if (loading) return <Loading message="Loading test runs..." />;
  if (error) return <Alert severity="error">{error.message}</Alert>;

  return (
    <Box>
      <PageHeader title="Test Runs" subtitle="Recent performance test results" />
      {runs && runs.length > 0 ? (
        <DataTable
          columns={columns}
          rows={runs}
          keyExtractor={(row) => row.id}
          onRowClick={(row) => navigate(`/test-runs/${row.id}`)}
        />
      ) : (
        <EmptyState
          icon={<InboxIcon sx={{ fontSize: 48 }} />}
          title="No test runs yet"
          description="Run a performance test to see results here."
        />
      )}
    </Box>
  );
}
