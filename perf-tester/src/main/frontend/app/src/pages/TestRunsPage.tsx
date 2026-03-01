import CompareArrowsIcon from '@mui/icons-material/CompareArrows';
import DeleteIcon from '@mui/icons-material/Delete';
import DeleteSweepIcon from '@mui/icons-material/DeleteSweep';
import InboxIcon from '@mui/icons-material/Inbox';
import Box from '@mui/material/Box';
import MuiButton from '@mui/material/Button';
import Checkbox from '@mui/material/Checkbox';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';
import FormControl from '@mui/material/FormControl';
import IconButton from '@mui/material/IconButton';
import InputLabel from '@mui/material/InputLabel';
import MenuItem from '@mui/material/MenuItem';
import Pagination from '@mui/material/Pagination';
import Select from '@mui/material/Select';
import Snackbar from '@mui/material/Snackbar';
import Stack from '@mui/material/Stack';
import { Alert, Chip, DataTable, EmptyState, Loading, PageHeader } from 'perf-ui-components';
import type { DataTableColumn } from 'perf-ui-components';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { bulkDeleteTestRuns, deleteTestRun, getAllTestRunTags, getTestRuns } from '@/api';
import { useApi } from '@/hooks';
import type { TestRunResponse } from '@/types/api';

const statusColor: Record<string, 'success' | 'error' | 'warning' | 'info' | 'default'> = {
  COMPLETED: 'success',
  FAILED: 'error',
  RUNNING: 'info',
  TIMEOUT: 'warning',
};

const testTypeColor: Record<string, 'info' | 'primary' | 'warning' | 'default' | 'error'> = {
  SMOKE: 'info',
  LOAD: 'primary',
  STRESS: 'warning',
  SOAK: 'default',
  SPIKE: 'error',
};

export default function TestRunsPage() {
  const [page, setPage] = useState(0);
  const [pageSize] = useState(20);
  const [selectedTag, setSelectedTag] = useState('');

  const { data, loading, error, refetch } = useApi(
    () => getTestRuns({ page, size: pageSize, tag: selectedTag || undefined }),
    [page, pageSize, selectedTag],
  );
  const { data: allTags } = useApi(() => getAllTestRunTags());

  const navigate = useNavigate();
  const [confirmId, setConfirmId] = useState<number | null>(null);
  const [bulkConfirm, setBulkConfirm] = useState(false);
  const [snackbarError, setSnackbarError] = useState<string | null>(null);
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());

  const runs = data?.content ?? [];

  const toggleSelect = (id: number) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  };

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

  const handleBulkDelete = async () => {
    setBulkConfirm(false);
    try {
      await bulkDeleteTestRuns([...selectedIds]);
      setSelectedIds(new Set());
      refetch();
    } catch {
      setSnackbarError('Failed to delete selected test runs.');
    }
  };

  const columns: DataTableColumn<TestRunResponse>[] = [
    {
      id: 'select',
      label: '',
      minWidth: 50,
      render: (row) => (
        <Checkbox
          size="small"
          checked={selectedIds.has(row.id)}
          onClick={(e) => { e.stopPropagation(); toggleSelect(row.id); }}
        />
      ),
    },
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
      id: 'p95',
      label: 'P95',
      align: 'right',
      render: (row) => (row.p95LatencyMs != null ? `${row.p95LatencyMs.toFixed(0)} ms` : '-'),
    },
    {
      id: 'testType',
      label: 'Type',
      render: (row) =>
        row.testType ? (
          <Chip
            label={row.testType}
            size="small"
            color={testTypeColor[row.testType] ?? 'default'}
          />
        ) : null,
    },
    {
      id: 'tags',
      label: 'Tags',
      render: (row) => (
        <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap' }}>
          {(row.tags ?? []).map((t) => (
            <Chip key={t} label={t} size="small" variant="outlined" />
          ))}
        </Box>
      ),
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
      <Stack direction="row" alignItems="flex-start" justifyContent="space-between" sx={{ mb: 2 }}>
        <PageHeader title="Test Runs" subtitle="Recent performance test results" />
        <Stack direction="row" gap={1} sx={{ mt: 1 }}>
          <MuiButton
            variant="outlined"
            color="error"
            disabled={selectedIds.size < 1}
            startIcon={<DeleteSweepIcon />}
            onClick={() => setBulkConfirm(true)}
          >
            Delete Selected
          </MuiButton>
          <MuiButton
            variant="outlined"
            disabled={selectedIds.size !== 2}
            startIcon={<CompareArrowsIcon />}
            onClick={() => {
              const [id1, id2] = [...selectedIds];
              void navigate(`/test-runs/compare?id1=${id1}&id2=${id2}`);
            }}
          >
            Compare Selected
          </MuiButton>
        </Stack>
      </Stack>

      <Stack direction="row" alignItems="center" gap={2} sx={{ mb: 2 }}>
        <FormControl size="small" sx={{ minWidth: 180 }}>
          <InputLabel>Filter by tag</InputLabel>
          <Select
            value={selectedTag}
            label="Filter by tag"
            onChange={(e) => { setSelectedTag(e.target.value); setPage(0); }}
          >
            <MenuItem value="">All tags</MenuItem>
            {(allTags ?? []).map((t) => (
              <MenuItem key={t} value={t}>{t}</MenuItem>
            ))}
          </Select>
        </FormControl>
      </Stack>

      {runs.length > 0 ? (
        <>
          <DataTable
            columns={columns}
            rows={runs}
            keyExtractor={(row) => row.id}
            onRowClick={(row) => navigate(`/test-runs/${row.id}`)}
          />
          {data && data.totalPages > 1 && (
            <Box sx={{ display: 'flex', justifyContent: 'center', mt: 2 }}>
              <Pagination
                count={data.totalPages}
                page={page + 1}
                onChange={(_, value) => setPage(value - 1)}
                color="primary"
              />
            </Box>
          )}
        </>
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

      <Dialog open={bulkConfirm} onClose={() => setBulkConfirm(false)}>
        <DialogTitle>Delete Test Runs</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Delete {selectedIds.size} test run{selectedIds.size !== 1 ? 's' : ''}? This will also
            remove any associated ZIP export files. This action cannot be undone.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <MuiButton onClick={() => setBulkConfirm(false)}>Cancel</MuiButton>
          <MuiButton color="error" variant="contained" onClick={() => void handleBulkDelete()}>
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
