import DeleteIcon from '@mui/icons-material/Delete';
import InboxIcon from '@mui/icons-material/Inbox';
import Box from '@mui/material/Box';
import MuiButton from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';
import IconButton from '@mui/material/IconButton';
import Snackbar from '@mui/material/Snackbar';
import { Alert, Chip, DataTable, EmptyState, Loading, PageHeader } from 'perf-ui-components';
import type { DataTableColumn } from 'perf-ui-components';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { deleteTestRun, getTestRuns } from '@/api';
import { useApi } from '@/hooks';
import type { TestRunResponse } from '@/types/api';

const statusColor: Record<string, 'success' | 'error' | 'warning' | 'info' | 'default'> = {
  COMPLETED: 'success',
  FAILED: 'error',
  RUNNING: 'info',
  TIMEOUT: 'warning',
};

export default function TestRunsPage() {
  const { data: runs, loading, error, refetch } = useApi(() => getTestRuns());
  const navigate = useNavigate();
  const [confirmId, setConfirmId] = useState<number | null>(null);
  const [snackbarError, setSnackbarError] = useState<string | null>(null);

  const handleDeleteConfirm = async () => {
    if (confirmId == null) {
      return;
    }
    const idToDelete = confirmId;
    setConfirmId(null);
    try {
      await deleteTestRun(idToDelete);
      refetch();
    } catch {
      setSnackbarError('Failed to delete test run.');
    }
  };

  const columns: DataTableColumn<TestRunResponse>[] = [
    { id: 'id', label: 'ID', render: (row) => row.id },
    { id: 'testId', label: 'Test ID', render: (row) => row.testId ?? '-' },
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
      id: 'startedAt',
      label: 'Start Time',
      render: (row) => new Date(row.startedAt).toLocaleString(),
    },
    {
      id: 'delete',
      label: '',
      render: (row) => (
        <IconButton
          size="small"
          color="error"
          onClick={(e) => {
            e.stopPropagation();
            setConfirmId(row.id);
          }}
          aria-label="Delete test run"
        >
          <DeleteIcon fontSize="small" />
        </IconButton>
      ),
    },
  ];

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

      <Dialog open={confirmId != null} onClose={() => setConfirmId(null)}>
        <DialogTitle>Delete Test Run</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Are you sure you want to delete this test run? This will also remove any associated ZIP
            export file. This action cannot be undone.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <MuiButton onClick={() => setConfirmId(null)}>Cancel</MuiButton>
          <MuiButton color="error" variant="contained" onClick={() => void handleDeleteConfirm()}>
            Delete
          </MuiButton>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={snackbarError != null}
        autoHideDuration={4000}
        onClose={() => setSnackbarError(null)}
      >
        <Alert severity="error" onClose={() => setSnackbarError(null)}>
          {snackbarError}
        </Alert>
      </Snackbar>
    </Box>
  );
}
