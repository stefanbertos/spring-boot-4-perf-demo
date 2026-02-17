import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import Box from '@mui/material/Box';
import Checkbox from '@mui/material/Checkbox';
import Collapse from '@mui/material/Collapse';
import FormControlLabel from '@mui/material/FormControlLabel';
import type { SelectChangeEvent } from '@mui/material/Select';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import {
  Alert,
  Button,
  Card,
  DataTable,
  Dialog,
  IconButton,
  PageHeader,
  Select,
  TextField,
} from 'perf-ui-components';
import type { DataTableColumn } from 'perf-ui-components';
import type { FormEvent } from 'react';
import { useCallback, useEffect, useState } from 'react';
import {
  createTestCase,
  deleteTestCase,
  getTestCase,
  listTestCases,
  sendTest,
  updateTestCase,
  uploadTestCase,
} from '@/api';
import type { TestCaseSummary } from '@/types/api';

// ── Test Case Manager ─────────────────────────────────────────────

function useTestCases() {
  const [testCases, setTestCases] = useState<TestCaseSummary[]>([]);
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      setTestCases(await listTestCases());
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  return { testCases, loading, refresh };
}

interface TestCaseFormState {
  mode: 'create' | 'edit' | 'upload';
  id?: number;
  name: string;
  message: string;
  file: File | null;
}

const INITIAL_FORM: TestCaseFormState = { mode: 'create', name: '', message: '', file: null };

// ── Main Page ─────────────────────────────────────────────────────

export default function SendTestPage() {
  // Send test form state
  const [message, setMessage] = useState('Hello, performance test!');
  const [count, setCount] = useState('10');
  const [timeout, setTimeout] = useState('30');
  const [delay, setDelay] = useState('0');
  const [testId, setTestId] = useState('');
  const [exportStatistics, setExportStatistics] = useState(false);
  const [debug, setDebug] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // Test case manager state
  const { testCases, loading: testCasesLoading, refresh } = useTestCases();
  const [managerOpen, setManagerOpen] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [form, setForm] = useState<TestCaseFormState>(INITIAL_FORM);
  const [confirmDeleteId, setConfirmDeleteId] = useState<number | null>(null);
  const [saving, setSaving] = useState(false);
  const [selectedTestCaseId, setSelectedTestCaseId] = useState('');

  // ── Send test handler ────────────────────────────────────────────

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setResult(null);

    try {
      await sendTest({
        message,
        count: Number(count),
        timeoutSeconds: Number(timeout),
        delayMs: Number(delay),
        testId: testId || undefined,
        exportStatistics,
        debug,
      });
      setResult({ type: 'success', text: `Test started with ${count} messages` });
    } catch (err) {
      setResult({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to send test',
      });
    } finally {
      setSubmitting(false);
    }
  };

  // ── Test case CRUD handlers ──────────────────────────────────────

  const openCreateDialog = () => {
    setForm({ ...INITIAL_FORM, mode: 'create' });
    setDialogOpen(true);
  };

  const openUploadDialog = () => {
    setForm({ ...INITIAL_FORM, mode: 'upload' });
    setDialogOpen(true);
  };

  const openEditDialog = async (tc: TestCaseSummary) => {
    const detail = await getTestCase(tc.id);
    setForm({
      mode: 'edit',
      id: detail.id,
      name: detail.name,
      message: detail.message,
      file: null,
    });
    setDialogOpen(true);
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      if (form.mode === 'upload' && form.file) {
        await uploadTestCase(form.name, form.file);
      } else if (form.mode === 'edit' && form.id != null) {
        await updateTestCase(form.id, form.name, form.message);
      } else {
        await createTestCase(form.name, form.message);
      }
      setDialogOpen(false);
      await refresh();
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (confirmDeleteId == null) return;
    await deleteTestCase(confirmDeleteId);
    setConfirmDeleteId(null);
    if (selectedTestCaseId === String(confirmDeleteId)) {
      setSelectedTestCaseId('');
    }
    await refresh();
  };

  const handleLoadTestCase = async (tc: TestCaseSummary) => {
    const detail = await getTestCase(tc.id);
    setMessage(detail.message);
    setSelectedTestCaseId(String(tc.id));
  };

  const handleSelectChange = async (event: SelectChangeEvent) => {
    const value = event.target.value;
    setSelectedTestCaseId(value);
    if (value === '' || value === '__freetext__') {
      return;
    }
    const detail = await getTestCase(Number(value));
    setMessage(detail.message);
  };

  const handleSaveAsTestCase = () => {
    setForm({ mode: 'create', name: '', message, file: null });
    setDialogOpen(true);
  };

  // ── Table columns ────────────────────────────────────────────────

  const columns: DataTableColumn<TestCaseSummary>[] = [
    { id: 'name', label: 'Name', render: (row) => row.name },
    {
      id: 'updatedAt',
      label: 'Updated',
      render: (row) => new Date(row.updatedAt).toLocaleString(),
    },
    {
      id: 'actions',
      label: 'Actions',
      align: 'right',
      render: (row) => (
        <Stack direction="row" spacing={0.5} justifyContent="flex-end">
          <IconButton size="small" tooltip="Load" onClick={() => handleLoadTestCase(row)}>
            <PlayArrowIcon fontSize="small" />
          </IconButton>
          <IconButton size="small" tooltip="Edit" onClick={() => openEditDialog(row)}>
            <EditIcon fontSize="small" />
          </IconButton>
          <IconButton size="small" tooltip="Delete" onClick={() => setConfirmDeleteId(row.id)}>
            <DeleteIcon fontSize="small" />
          </IconButton>
        </Stack>
      ),
    },
  ];

  // ── Select options ───────────────────────────────────────────────

  const selectOptions = [
    { value: '__freetext__', label: 'Freetext' },
    ...testCases.map((tc) => ({ value: String(tc.id), label: tc.name })),
  ];

  // ── Render ───────────────────────────────────────────────────────

  const dialogTitle =
    form.mode === 'upload'
      ? 'Upload Test Case'
      : form.mode === 'edit'
        ? 'Edit Test Case'
        : 'New Test Case';

  const canSave =
    form.mode === 'upload'
      ? form.name.trim() !== '' && form.file != null
      : form.name.trim() !== '' && form.message.trim() !== '';

  return (
    <Box>
      <PageHeader title="Send Test" subtitle="Configure and run a performance test" />

      {/* Test Case Manager */}
      <Card sx={{ mb: 3 }}>
        <Stack spacing={2}>
          <Stack direction="row" alignItems="center" justifyContent="space-between">
            <Typography
              variant="subtitle1"
              sx={{ fontWeight: 'bold', cursor: 'pointer' }}
              onClick={() => setManagerOpen(!managerOpen)}
            >
              Test Cases {managerOpen ? '(collapse)' : '(expand)'}
            </Typography>
            <Stack direction="row" spacing={1}>
              <Button size="small" onClick={openCreateDialog}>
                New
              </Button>
              <Button size="small" onClick={openUploadDialog}>
                <UploadFileIcon fontSize="small" sx={{ mr: 0.5 }} />
                Upload
              </Button>
            </Stack>
          </Stack>
          <Collapse in={managerOpen}>
            {testCasesLoading ? (
              <Typography variant="body2" color="text.secondary">
                Loading test cases...
              </Typography>
            ) : testCases.length === 0 ? (
              <Typography variant="body2" color="text.secondary">
                No test cases yet. Create one or upload a file.
              </Typography>
            ) : (
              <DataTable columns={columns} rows={testCases} keyExtractor={(row) => row.id} />
            )}
          </Collapse>
        </Stack>
      </Card>

      {/* Send Test Form */}
      <Card sx={{ maxWidth: 600 }}>
        <Box component="form" onSubmit={handleSubmit}>
          <Stack spacing={2.5}>
            <Select
              label="Load Test Case"
              value={selectedTestCaseId || '__freetext__'}
              options={selectOptions}
              onChange={handleSelectChange}
              fullWidth
              size="small"
            />
            <Stack direction="row" spacing={1} alignItems="flex-start">
              <TextField
                label="Message Content"
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                fullWidth
                required
                multiline
                rows={3}
              />
              <Button
                size="small"
                onClick={handleSaveAsTestCase}
                sx={{ whiteSpace: 'nowrap', mt: 1 }}
              >
                Save As...
              </Button>
            </Stack>
            <Stack direction="row" spacing={2}>
              <TextField
                label="Message Count"
                type="number"
                value={count}
                onChange={(e) => setCount(e.target.value)}
                fullWidth
                required
                slotProps={{ htmlInput: { min: 1 } }}
              />
              <TextField
                label="Timeout (seconds)"
                type="number"
                value={timeout}
                onChange={(e) => setTimeout(e.target.value)}
                fullWidth
                required
                slotProps={{ htmlInput: { min: 1 } }}
              />
            </Stack>
            <Stack direction="row" spacing={2}>
              <TextField
                label="Delay (ms)"
                type="number"
                value={delay}
                onChange={(e) => setDelay(e.target.value)}
                fullWidth
                slotProps={{ htmlInput: { min: 0 } }}
              />
              <TextField
                label="Test ID (optional)"
                value={testId}
                onChange={(e) => setTestId(e.target.value)}
                fullWidth
              />
            </Stack>
            <Stack direction="row" spacing={2}>
              <FormControlLabel
                control={
                  <Checkbox
                    checked={exportStatistics}
                    onChange={(e) => setExportStatistics(e.target.checked)}
                  />
                }
                label="Export Statistics"
              />
              <FormControlLabel
                control={<Checkbox checked={debug} onChange={(e) => setDebug(e.target.checked)} />}
                label="Debug Mode"
              />
            </Stack>
            {result && (
              <Alert severity={result.type} onClose={() => setResult(null)}>
                {result.text}
              </Alert>
            )}
            <Button type="submit" disabled={submitting} sx={{ alignSelf: 'flex-start' }}>
              {submitting ? 'Sending...' : 'Send Test'}
            </Button>
          </Stack>
        </Box>
      </Card>

      {/* Create/Edit/Upload Dialog */}
      <Dialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        title={dialogTitle}
        maxWidth="md"
        actions={
          <>
            <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
            <Button onClick={handleSave} disabled={saving || !canSave}>
              {saving ? 'Saving...' : 'Save'}
            </Button>
          </>
        }
      >
        <Stack spacing={2} sx={{ mt: 1 }}>
          <TextField
            label="Name"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            fullWidth
            required
          />
          {form.mode === 'upload' ? (
            <Button component="label">
              {form.file ? form.file.name : 'Choose File'}
              <input
                type="file"
                hidden
                onChange={(e) => {
                  const file = e.target.files?.[0] ?? null;
                  setForm({ ...form, file });
                }}
              />
            </Button>
          ) : (
            <TextField
              label="Message"
              value={form.message}
              onChange={(e) => setForm({ ...form, message: e.target.value })}
              fullWidth
              required
              multiline
              rows={8}
            />
          )}
        </Stack>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <Dialog
        open={confirmDeleteId != null}
        onClose={() => setConfirmDeleteId(null)}
        title="Delete Test Case"
        maxWidth="xs"
        actions={
          <>
            <Button onClick={() => setConfirmDeleteId(null)}>Cancel</Button>
            <Button onClick={handleDelete}>Delete</Button>
          </>
        }
      >
        <Typography>Are you sure you want to delete this test case?</Typography>
      </Dialog>
    </Box>
  );
}
