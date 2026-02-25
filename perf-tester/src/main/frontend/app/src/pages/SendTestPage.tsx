import DownloadIcon from '@mui/icons-material/Download';
import Box from '@mui/material/Box';
import Checkbox from '@mui/material/Checkbox';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import FormControlLabel from '@mui/material/FormControlLabel';
import Grid from '@mui/material/Grid';
import LinearProgress from '@mui/material/LinearProgress';
import Stack from '@mui/material/Stack';
import MuiTextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import {
  Alert,
  Button,
  Card,
  Chip,
  PageHeader,
  Select,
  TextField,
  Tooltip,
} from 'perf-ui-components';
import type { SelectChangeEvent } from 'perf-ui-components';
import type { FormEvent } from 'react';
import { useCallback, useEffect, useRef, useState } from 'react';
import {
  addLokiServiceLabel,
  createDbExportQuery,
  deleteDbExportQuery,
  deleteLokiServiceLabel,
  downloadTestRunUrl,
  listDbExportQueries,
  listLokiServiceLabels,
  listNamespaces,
  listTestScenarios,
  sendTest,
  subscribeTestProgress,
  updateDbExportQuery,
} from '@/api';
import { TestScenarioManager } from '@/components';
import type { DbExportQuery, TestProgressEvent, TestScenarioSummary } from '@/types/api';

// ── Progress Panel ─────────────────────────────────────────────────

interface StatItemProps {
  label: string;
  value: string;
}

function StatItem({ label, value }: StatItemProps) {
  return (
    <Box>
      <Typography variant="caption" color="text.secondary" display="block">
        {label}
      </Typography>
      <Typography variant="body2" fontWeight="bold">
        {value}
      </Typography>
    </Box>
  );
}

interface ProgressPanelProps {
  progress: TestProgressEvent;
}

function ProgressPanel({ progress }: ProgressPanelProps) {
  const isActive = progress.status === 'RUNNING' || progress.status === 'IDLE';
  const isExporting = progress.status === 'EXPORTING';
  const statusColor =
    progress.status === 'COMPLETED'
      ? 'success'
      : progress.status === 'TIMEOUT'
        ? 'warning'
        : progress.status === 'FAILED'
          ? 'error'
          : 'primary';
  const statusLabel =
    progress.status === 'IDLE'
      ? 'Starting...'
      : progress.status === 'RUNNING'
        ? 'Running...'
        : progress.status === 'EXPORTING'
          ? 'Exporting...'
          : progress.status;

  return (
    <Card sx={{ mt: 3 }}>
      <Stack spacing={2}>
        <Stack direction="row" alignItems="center" justifyContent="space-between">
          <Typography variant="subtitle1" fontWeight="bold">
            Test Progress
          </Typography>
          <Chip label={statusLabel} color={statusColor} size="small" />
        </Stack>

        <Box>
          <Stack direction="row" justifyContent="space-between" sx={{ mb: 0.5 }}>
            <Typography variant="caption" color="text.secondary">
              {progress.completedCount.toLocaleString()} / {progress.totalCount.toLocaleString()}{' '}
              messages received
            </Typography>
            <Typography variant="caption" color="text.secondary">
              {progress.progressPercent.toFixed(1)}%
            </Typography>
          </Stack>
          <LinearProgress
            variant={isExporting || (isActive && progress.progressPercent === 0) ? 'indeterminate' : 'determinate'}
            value={Math.min(progress.progressPercent, 100)}
            color={statusColor}
            sx={{ height: 8, borderRadius: 4 }}
          />
        </Box>

        <Grid container spacing={2}>
          <Grid size={{ xs: 6, sm: 4, md: 2 }}>
            <StatItem
              label="Sent"
              value={`${progress.sentCount.toLocaleString()} / ${progress.totalCount.toLocaleString()}`}
            />
          </Grid>
          <Grid size={{ xs: 6, sm: 4, md: 2 }}>
            <StatItem label="TPS" value={progress.tps.toFixed(1)} />
          </Grid>
          <Grid size={{ xs: 6, sm: 4, md: 2 }}>
            <StatItem
              label="Avg Latency"
              value={progress.avgLatencyMs > 0 ? `${progress.avgLatencyMs.toFixed(1)} ms` : '—'}
            />
          </Grid>
          <Grid size={{ xs: 6, sm: 4, md: 2 }}>
            <StatItem
              label="Min Latency"
              value={progress.minLatencyMs > 0 ? `${progress.minLatencyMs.toFixed(1)} ms` : '—'}
            />
          </Grid>
          <Grid size={{ xs: 6, sm: 4, md: 2 }}>
            <StatItem
              label="Max Latency"
              value={progress.maxLatencyMs > 0 ? `${progress.maxLatencyMs.toFixed(1)} ms` : '—'}
            />
          </Grid>
          <Grid size={{ xs: 6, sm: 4, md: 2 }}>
            <StatItem label="Elapsed" value={`${progress.elapsedSeconds.toFixed(1)} s`} />
          </Grid>
        </Grid>
      </Stack>
    </Card>
  );
}

// ── Loki Config Dialog ─────────────────────────────────────────────

function LokiConfigDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const [labels, setLabels] = useState<string[]>([]);
  const [newName, setNewName] = useState('');
  const [adding, setAdding] = useState(false);
  const [deletingName, setDeletingName] = useState<string | null>(null);
  const [result, setResult] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  useEffect(() => {
    if (!open) return;
    listLokiServiceLabels()
      .then(setLabels)
      .catch((err) => {
        setResult({ type: 'error', text: err instanceof Error ? err.message : 'Failed to load labels' });
      });
  }, [open]);

  const handleAdd = async (e: FormEvent) => {
    e.preventDefault();
    const trimmed = newName.trim();
    if (!trimmed) return;
    setAdding(true);
    setResult(null);
    try {
      await addLokiServiceLabel(trimmed);
      setLabels((prev) => [...prev, trimmed].sort());
      setNewName('');
      setResult({ type: 'success', text: `Added "${trimmed}"` });
    } catch (err) {
      setResult({ type: 'error', text: err instanceof Error ? err.message : 'Failed to add label' });
    } finally {
      setAdding(false);
    }
  };

  const handleDelete = async (name: string) => {
    setDeletingName(name);
    setResult(null);
    try {
      await deleteLokiServiceLabel(name);
      setLabels((prev) => prev.filter((l) => l !== name));
    } catch (err) {
      setResult({ type: 'error', text: err instanceof Error ? err.message : 'Failed to delete label' });
    } finally {
      setDeletingName(null);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Loki Service Labels</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          <Typography variant="body2" color="text.secondary">
            Log export queries Loki for all listed service names using the <code>job</code> label.
            Changes apply immediately on the next export.
          </Typography>
          <Stack spacing={1}>
            {labels.length === 0 && (
              <Typography variant="body2" color="text.secondary">
                No service labels configured.
              </Typography>
            )}
            {labels.map((label) => (
              <Stack key={label} direction="row" alignItems="center" spacing={1}>
                <Typography variant="body2" sx={{ flex: 1, fontFamily: 'monospace' }}>
                  {label}
                </Typography>
                <Button disabled={deletingName === label} onClick={() => handleDelete(label)}>
                  {deletingName === label ? 'Removing...' : 'Remove'}
                </Button>
              </Stack>
            ))}
          </Stack>
          <Box component="form" onSubmit={handleAdd}>
            <Stack direction="row" spacing={1} alignItems="flex-start">
              <TextField
                label="Service name"
                value={newName}
                onChange={(e) => setNewName(e.target.value)}
                placeholder="e.g. perf-tester"
                size="small"
                sx={{ flex: 1 }}
              />
              <Button type="submit" disabled={adding || !newName.trim()}>
                {adding ? 'Adding...' : 'Add'}
              </Button>
            </Stack>
          </Box>
          {result && (
            <Alert severity={result.type} onClose={() => setResult(null)}>
              {result.text}
            </Alert>
          )}
        </Stack>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose}>Close</Button>
      </DialogActions>
    </Dialog>
  );
}

// ── DB Queries Dialog ──────────────────────────────────────────────

type DbQueryEdit = { name: string; sqlQuery: string; displayOrder: string };

function DbQueriesDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const [queries, setQueries] = useState<DbExportQuery[]>([]);
  const [editingId, setEditingId] = useState<number | 'new' | null>(null);
  const [form, setForm] = useState<DbQueryEdit>({ name: '', sqlQuery: '', displayOrder: '0' });
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [result, setResult] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  useEffect(() => {
    if (!open) return;
    listDbExportQueries()
      .then(setQueries)
      .catch((err) => {
        setResult({ type: 'error', text: err instanceof Error ? err.message : 'Failed to load queries' });
      });
  }, [open]);

  const startEdit = (q: DbExportQuery) => {
    setEditingId(q.id);
    setForm({ name: q.name, sqlQuery: q.sqlQuery, displayOrder: String(q.displayOrder) });
    setResult(null);
  };

  const cancelEdit = () => { setEditingId(null); setResult(null); };

  const handleSave = async () => {
    setSaving(true);
    setResult(null);
    try {
      const data = { name: form.name, sqlQuery: form.sqlQuery, displayOrder: Number(form.displayOrder) };
      if (editingId === 'new') {
        const created = await createDbExportQuery(data);
        setQueries((prev) => [...prev, created].sort((a, b) => a.displayOrder - b.displayOrder || a.name.localeCompare(b.name)));
        setResult({ type: 'success', text: `Query "${created.name}" created` });
      } else if (editingId != null) {
        const updated = await updateDbExportQuery(editingId, data);
        setQueries((prev) => prev.map((q) => (q.id === updated.id ? updated : q)).sort((a, b) => a.displayOrder - b.displayOrder || a.name.localeCompare(b.name)));
        setResult({ type: 'success', text: `Query "${updated.name}" updated` });
      }
      setEditingId(null);
    } catch (err) {
      setResult({ type: 'error', text: err instanceof Error ? err.message : 'Failed to save query' });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: number, name: string) => {
    setDeletingId(id);
    setResult(null);
    try {
      await deleteDbExportQuery(id);
      setQueries((prev) => prev.filter((q) => q.id !== id));
      setResult({ type: 'success', text: `Query "${name}" deleted` });
      if (editingId === id) setEditingId(null);
    } catch (err) {
      setResult({ type: 'error', text: err instanceof Error ? err.message : 'Failed to delete query' });
    } finally {
      setDeletingId(null);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>Database Export Queries</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          <Typography variant="body2" color="text.secondary">
            Configure SELECT queries to execute during export. Results are saved as CSV files in
            the <code>db/</code> folder of the ZIP.
          </Typography>

          {queries.length === 0 && editingId === null && (
            <Typography variant="body2" color="text.secondary">
              No queries configured yet. Click &quot;Add Query&quot; to get started.
            </Typography>
          )}

          {queries.map((q) => (
            <Card key={q.id}>
              <Stack spacing={1}>
                <Stack direction="row" alignItems="center" justifyContent="space-between">
                  <Stack direction="row" spacing={1} alignItems="center">
                    <Typography variant="body2" fontWeight="bold">{q.name}</Typography>
                    <Typography variant="caption" color="text.secondary">order: {q.displayOrder}</Typography>
                  </Stack>
                  <Stack direction="row" spacing={1}>
                    <Button size="small" disabled={deletingId === q.id} onClick={() => startEdit(q)}>Edit</Button>
                    <Button size="small" disabled={deletingId === q.id} onClick={() => handleDelete(q.id, q.name)}>
                      {deletingId === q.id ? 'Deleting...' : 'Delete'}
                    </Button>
                  </Stack>
                </Stack>
                <Typography
                  variant="caption"
                  sx={{ fontFamily: 'monospace', whiteSpace: 'pre-wrap', wordBreak: 'break-all', color: 'text.secondary' }}
                >
                  {q.sqlQuery.length > 200 ? q.sqlQuery.slice(0, 200) + '…' : q.sqlQuery}
                </Typography>
              </Stack>
            </Card>
          ))}

          {editingId !== null && (
            <Card>
              <Typography variant="subtitle2" fontWeight="bold" sx={{ mb: 2 }}>
                {editingId === 'new' ? 'Add New Query' : 'Edit Query'}
              </Typography>
              <Stack spacing={2}>
                <Stack direction="row" spacing={2}>
                  <TextField
                    label="Name"
                    value={form.name}
                    onChange={(e) => setForm((p) => ({ ...p, name: e.target.value }))}
                    fullWidth
                    required
                  />
                  <MuiTextField
                    label="Display Order"
                    type="number"
                    value={form.displayOrder}
                    onChange={(e) => setForm((p) => ({ ...p, displayOrder: e.target.value }))}
                    sx={{ width: 140 }}
                    size="small"
                  />
                </Stack>
                <MuiTextField
                  label="SQL Query (SELECT only)"
                  value={form.sqlQuery}
                  onChange={(e) => setForm((p) => ({ ...p, sqlQuery: e.target.value }))}
                  multiline
                  rows={5}
                  fullWidth
                  required
                  slotProps={{ htmlInput: { style: { fontFamily: 'monospace', fontSize: 13 } } }}
                />
                <Stack direction="row" spacing={1}>
                  <Button disabled={saving || !form.name.trim() || !form.sqlQuery.trim()} onClick={handleSave}>
                    {saving ? 'Saving...' : 'Save'}
                  </Button>
                  <Button onClick={cancelEdit}>Cancel</Button>
                </Stack>
              </Stack>
            </Card>
          )}

          {editingId === null && (
            <Box>
              <Button onClick={() => { setEditingId('new'); setForm({ name: '', sqlQuery: '', displayOrder: '0' }); setResult(null); }}>
                Add Query
              </Button>
            </Box>
          )}

          {result && (
            <Alert severity={result.type} onClose={() => setResult(null)}>
              {result.text}
            </Alert>
          )}
        </Stack>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose}>Close</Button>
      </DialogActions>
    </Dialog>
  );
}

// ── Main Page ─────────────────────────────────────────────────────

export default function SendTestPage() {
  const [testId, setTestId] = useState('');
  const [exportGrafana, setExportGrafana] = useState(false);
  const [exportPrometheus, setExportPrometheus] = useState(false);
  const [exportKubernetes, setExportKubernetes] = useState(false);
  const [exportLogs, setExportLogs] = useState(false);
  const [exportDatabase, setExportDatabase] = useState(false);
  const [debug, setDebug] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const [progress, setProgress] = useState<TestProgressEvent | null>(null);
  const [currentTestRunDbId, setCurrentTestRunDbId] = useState<number | null>(null);
  const [exportWasRequested, setExportWasRequested] = useState(false);
  const unsubscribeRef = useRef<(() => void) | null>(null);
  const [selectedScenarioId, setSelectedScenarioId] = useState<number | null>(null);
  const [testScenarios, setTestScenarios] = useState<TestScenarioSummary[]>([]);
  const [scenarioManagerOpen, setScenarioManagerOpen] = useState(false);
  const [kubernetesAvailable, setKubernetesAvailable] = useState(false);
  const [logsDialogOpen, setLogsDialogOpen] = useState(false);
  const [dbDialogOpen, setDbDialogOpen] = useState(false);

  const refreshScenarios = useCallback(async () => {
    try {
      setTestScenarios(await listTestScenarios());
    } catch {
      // silently ignore
    }
  }, []);

  useEffect(() => {
    void refreshScenarios();
    listNamespaces().then((ns) => setKubernetesAvailable(ns.length > 0)).catch(() => {});
    return () => {
      unsubscribeRef.current?.();
    };
  }, [refreshScenarios]);

  const scenarioOptions = testScenarios.map((s) => ({ value: String(s.id), label: s.name }));

  const handleScenarioChange = (e: SelectChangeEvent) => {
    setSelectedScenarioId(e.target.value ? Number(e.target.value) : null);
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    unsubscribeRef.current?.();
    setSubmitting(true);
    setResult(null);
    setProgress(null);
    setCurrentTestRunDbId(null);
    const anyExport = exportGrafana || exportPrometheus || exportKubernetes || exportLogs || exportDatabase;
    setExportWasRequested(anyExport);

    try {
      const { id, testRunId } = await sendTest({
        delayMs: 0,
        testId: testId || undefined,
        scenarioId: selectedScenarioId ?? undefined,
        exportGrafana,
        exportPrometheus,
        exportKubernetes,
        exportLogs,
        exportDatabase,
        debug,
      });
      setCurrentTestRunDbId(id);

      const unsub = subscribeTestProgress(
        testRunId,
        (event) => setProgress(event),
        () => setSubmitting(false),
        () => {
          setSubmitting(false);
          setResult({ type: 'error', text: 'Lost connection to progress stream' });
        },
      );
      unsubscribeRef.current = unsub;
    } catch (err) {
      setSubmitting(false);
      setResult({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to start test',
      });
    }
  };

  return (
    <Box>
      <PageHeader title="Run Test" subtitle="Configure and run a performance test" />

      <Card sx={{ maxWidth: 640 }}>
        <Box component="form" onSubmit={handleSubmit}>
          <Stack spacing={2.5}>
            <Stack direction="row" spacing={2} alignItems="flex-end">
              <Box sx={{ flex: 1 }}>
                <Select
                  label="Test Scenario"
                  value={selectedScenarioId != null ? String(selectedScenarioId) : ''}
                  options={scenarioOptions}
                  onChange={handleScenarioChange}
                  fullWidth
                  disabled={testScenarios.length === 0}
                />
              </Box>
              <Button onClick={() => setScenarioManagerOpen(true)} sx={{ flexShrink: 0 }}>
                Manage
              </Button>
            </Stack>

            <TextField
              label="Test ID (optional)"
              value={testId}
              onChange={(e) => setTestId(e.target.value)}
              fullWidth
            />

            <Box>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 0.5 }}>
                Export
              </Typography>
              <Stack direction="row" flexWrap="wrap" alignItems="center">
                <Tooltip title="Exports Grafana dashboard snapshots as PNG images" placement="top">
                  <FormControlLabel
                    control={
                      <Checkbox
                        size="small"
                        checked={exportGrafana}
                        onChange={(e) => setExportGrafana(e.target.checked)}
                      />
                    }
                    label="Grafana"
                  />
                </Tooltip>
                <Tooltip title="Exports a Prometheus metrics snapshot at the end of the test" placement="top">
                  <FormControlLabel
                    control={
                      <Checkbox
                        size="small"
                        checked={exportPrometheus}
                        onChange={(e) => setExportPrometheus(e.target.checked)}
                      />
                    }
                    label="Prometheus"
                  />
                </Tooltip>
                {kubernetesAvailable && (
                  <Tooltip title="Exports Kubernetes deployment and pod state at the end of the test" placement="top">
                    <FormControlLabel
                      control={
                        <Checkbox
                          size="small"
                          checked={exportKubernetes}
                          onChange={(e) => setExportKubernetes(e.target.checked)}
                        />
                      }
                      label="Kubernetes"
                    />
                  </Tooltip>
                )}
                <Stack direction="row" alignItems="center">
                  <Tooltip title="Queries Loki for logs from configured services during the test window" placement="top">
                    <FormControlLabel
                      control={
                        <Checkbox
                          size="small"
                          checked={exportLogs}
                          onChange={(e) => setExportLogs(e.target.checked)}
                        />
                      }
                      label="Logs"
                    />
                  </Tooltip>
                  {exportLogs && (
                    <Button size="small" sx={{ ml: -0.5, mr: 1, mb: 0.25 }} onClick={() => setLogsDialogOpen(true)}>
                      Configure
                    </Button>
                  )}
                </Stack>
                <Stack direction="row" alignItems="center">
                  <Tooltip title="Runs configured SQL queries and saves results as CSV files in the export ZIP" placement="top">
                    <FormControlLabel
                      control={
                        <Checkbox
                          size="small"
                          checked={exportDatabase}
                          onChange={(e) => setExportDatabase(e.target.checked)}
                        />
                      }
                      label="Database"
                    />
                  </Tooltip>
                  {exportDatabase && (
                    <Button size="small" sx={{ ml: -0.5, mr: 1, mb: 0.25 }} onClick={() => setDbDialogOpen(true)}>
                      Configure
                    </Button>
                  )}
                </Stack>
              </Stack>
            </Box>

            <Box>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 0.5 }}>
                Debug
              </Typography>
              <Tooltip title="Temporarily sets com.example log level to DEBUG for the duration of the test" placement="top">
                <FormControlLabel
                  control={
                    <Checkbox
                      size="small"
                      checked={debug}
                      onChange={(e) => setDebug(e.target.checked)}
                    />
                  }
                  label="Enable debug logging"
                />
              </Tooltip>
            </Box>

            {result && (
              <Alert severity={result.type} onClose={() => setResult(null)}>
                {result.text}
              </Alert>
            )}

            {!selectedScenarioId && (
              <Typography variant="caption" color="text.secondary">
                Select a scenario to run
              </Typography>
            )}

            <Button
              type="submit"
              disabled={submitting || !selectedScenarioId}
              sx={{ alignSelf: 'flex-start' }}
            >
              {submitting
                ? progress?.status === 'EXPORTING'
                  ? 'Exporting...'
                  : 'Running...'
                : 'Run Test'}
            </Button>
          </Stack>
        </Box>
      </Card>

      <TestScenarioManager
        open={scenarioManagerOpen}
        onClose={() => setScenarioManagerOpen(false)}
        onChanged={() => void refreshScenarios()}
      />

      <LokiConfigDialog open={logsDialogOpen} onClose={() => setLogsDialogOpen(false)} />
      <DbQueriesDialog open={dbDialogOpen} onClose={() => setDbDialogOpen(false)} />

      {progress && <ProgressPanel progress={progress} />}

      {progress &&
        (progress.status === 'COMPLETED' || progress.status === 'TIMEOUT') &&
        exportWasRequested &&
        currentTestRunDbId !== null && (
          <Card sx={{ mt: 2 }}>
            <Stack direction="row" alignItems="center" spacing={2}>
              <Typography variant="body2" color="text.secondary">
                Export package ready
              </Typography>
              <Button
                component="a"
                href={downloadTestRunUrl(currentTestRunDbId)}
                startIcon={<DownloadIcon />}
                size="small"
              >
                Download Results ZIP
              </Button>
            </Stack>
          </Card>
        )}
    </Box>
  );
}
