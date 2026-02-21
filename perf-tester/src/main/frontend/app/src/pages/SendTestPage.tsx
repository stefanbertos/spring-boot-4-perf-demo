import DownloadIcon from '@mui/icons-material/Download';
import Box from '@mui/material/Box';
import Checkbox from '@mui/material/Checkbox';
import FormControlLabel from '@mui/material/FormControlLabel';
import Grid from '@mui/material/Grid';
import LinearProgress from '@mui/material/LinearProgress';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import {
  Alert,
  Button,
  Card,
  Chip,
  PageHeader,
  Select,
  TextField,
} from 'perf-ui-components';
import type { SelectChangeEvent } from 'perf-ui-components';
import type { FormEvent } from 'react';
import { useCallback, useEffect, useRef, useState } from 'react';
import {
  downloadTestRunUrl,
  listNamespaces,
  listTestScenarios,
  sendTest,
  subscribeTestProgress,
} from '@/api';
import { TestScenarioManager } from '@/components';
import type { TestProgressEvent, TestScenarioSummary } from '@/types/api';

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

// ── Main Page ─────────────────────────────────────────────────────

export default function SendTestPage() {
  const [timeout] = useState('0');
  const [delay] = useState('0');
  const [testId, setTestId] = useState('');
  const [exportGrafana, setExportGrafana] = useState(false);
  const [exportPrometheus, setExportPrometheus] = useState(false);
  const [exportKubernetes, setExportKubernetes] = useState(false);
  const [exportLogs, setExportLogs] = useState(false);
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

  const refreshScenarios = useCallback(async () => {
    try {
      setTestScenarios(await listTestScenarios());
    } catch {
      // silently ignore — user can still open manager to see error
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
    const anyExport = exportGrafana || exportPrometheus || exportKubernetes || exportLogs;
    setExportWasRequested(anyExport);

    try {
      const { id, testRunId } = await sendTest({
        timeoutSeconds: Number(timeout),
        delayMs: Number(delay),
        testId: testId || undefined,
        scenarioId: selectedScenarioId ?? undefined,
        exportGrafana,
        exportPrometheus,
        exportKubernetes,
        exportLogs,
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
              <Stack direction="row" flexWrap="wrap">
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
                {kubernetesAvailable && (
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
                )}
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
              </Stack>
            </Box>
            <Box>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 0.5 }}>
                Debug
              </Typography>
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
