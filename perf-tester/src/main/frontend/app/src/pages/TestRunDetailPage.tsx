import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import DownloadIcon from '@mui/icons-material/Download';
import Box from '@mui/material/Box';
import MuiButton from '@mui/material/Button';
import Grid from '@mui/material/Grid';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemText from '@mui/material/ListItemText';
import Snackbar from '@mui/material/Snackbar';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { Alert, Card, Chip, DataTable, Loading, PageHeader, Tabs } from 'perf-ui-components';
import type { DataTableColumn, TabItem } from 'perf-ui-components';
import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { downloadTestRunUrl, getTestRunLogs, getTestRunSnapshots, getTestRunSummary, setTestRunTags } from '@/api';
import { useApi } from '@/hooks';
import type { LogEntry, TestRunSnapshotResponse, ThresholdResult } from '@/types/api';

function SummaryField({ label, value }: { label: string; value: string }) {
  return (
    <Box>
      <Typography variant="body2" color="text.secondary">
        {label}
      </Typography>
      <Typography variant="body1" fontWeight="medium">
        {value}
      </Typography>
    </Box>
  );
}

function LogsPanel({ logs, loading }: { logs: LogEntry[] | null; loading: boolean }) {
  if (loading) return <Loading message="Loading logs..." />;
  if (!logs || logs.length === 0) {
    return <Typography color="text.secondary">No logs available.</Typography>;
  }
  return (
    <List
      dense
      sx={{ fontFamily: 'monospace', fontSize: '0.8rem', maxHeight: 600, overflow: 'auto' }}
    >
      {logs.map((entry, idx) => (
        <ListItem key={idx} disableGutters sx={{ py: 0 }}>
          <ListItemText
            primary={`${entry.timestamp}  [${entry.level}]  ${entry.message}`}
            primaryTypographyProps={{
              sx: {
                fontFamily: 'monospace',
                fontSize: '0.8rem',
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-all',
              },
            }}
          />
        </ListItem>
      ))}
    </List>
  );
}

function MonitoringPanel({
  snapshots,
  loading,
}: {
  snapshots: TestRunSnapshotResponse[] | null;
  loading: boolean;
}) {
  if (loading) return <Loading message="Loading monitoring data..." />;
  if (!snapshots || snapshots.length === 0) {
    return <Typography color="text.secondary">No monitoring snapshots available.</Typography>;
  }
  const columns: DataTableColumn<TestRunSnapshotResponse>[] = [
    {
      id: 'time',
      label: 'Time',
      render: (row) => new Date(row.sampledAt).toLocaleTimeString(),
    },
    {
      id: 'outbound',
      label: 'Outbound Q',
      align: 'right',
      render: (row) => (row.outboundQueueDepth != null ? row.outboundQueueDepth : '-'),
    },
    {
      id: 'inbound',
      label: 'Inbound Q',
      align: 'right',
      render: (row) => (row.inboundQueueDepth != null ? row.inboundQueueDepth : '-'),
    },
    {
      id: 'reqLag',
      label: 'Requests Lag',
      align: 'right',
      render: (row) => (row.kafkaRequestsLag != null ? row.kafkaRequestsLag : '-'),
    },
    {
      id: 'resLag',
      label: 'Responses Lag',
      align: 'right',
      render: (row) => (row.kafkaResponsesLag != null ? row.kafkaResponsesLag : '-'),
    },
  ];
  return (
    <DataTable
      columns={columns}
      rows={snapshots}
      keyExtractor={(row) => row.id}
    />
  );
}

const testTypeColor: Record<string, 'info' | 'primary' | 'warning' | 'default' | 'error'> = {
  SMOKE: 'info',
  LOAD: 'primary',
  STRESS: 'warning',
  SOAK: 'default',
  SPIKE: 'error',
};

export default function TestRunDetailPage() {
  const { id } = useParams<{ id: string }>();
  const numericId = Number(id);

  const {
    data: summary,
    loading: loadingSummary,
    error: errorSummary,
  } = useApi(() => getTestRunSummary(numericId), [numericId]);

  const { data: logs, loading: loadingLogs } = useApi(() => getTestRunLogs(numericId), [numericId]);
  const { data: snapshots, loading: loadingSnapshots } = useApi(
    () => getTestRunSnapshots(numericId),
    [numericId],
  );

  const [tags, setTagsState] = useState<string[]>([]);
  const [newTag, setNewTag] = useState('');
  const [tagError, setTagError] = useState<string | null>(null);

  useEffect(() => {
    if (summary) {
      setTagsState(summary.tags ?? []);
    }
  }, [summary]);

  const handleAddTag = async () => {
    const trimmed = newTag.trim();
    if (!trimmed) {
      return;
    }
    const updated = [...tags, trimmed];
    setTagsState(updated);
    setNewTag('');
    try {
      await setTestRunTags(numericId, updated);
    } catch {
      setTagError('Failed to update tags.');
      setTagsState(tags);
    }
  };

  const handleRemoveTag = async (tag: string) => {
    const updated = tags.filter((t) => t !== tag);
    setTagsState(updated);
    try {
      await setTestRunTags(numericId, updated);
    } catch {
      setTagError('Failed to update tags.');
      setTagsState(tags);
    }
  };

  if (loadingSummary) return <Loading message="Loading test run..." />;
  if (errorSummary) return <Alert severity="error">{errorSummary.message}</Alert>;
  if (!summary) return <Alert severity="warning">Test run not found</Alert>;

  const statusColor =
    summary.status === 'COMPLETED'
      ? 'success'
      : summary.status === 'FAILED'
        ? 'error'
        : summary.status === 'RUNNING'
          ? 'info'
          : ('default' as const);

  const parsedThresholds: ThresholdResult[] = summary.thresholdResults
    ? (JSON.parse(summary.thresholdResults) as ThresholdResult[])
    : [];

  const tabs: TabItem[] = [
    {
      label: 'Summary',
      content: (
        <Card>
          <Grid container spacing={3}>
            <Grid size={{ xs: 6, md: 3 }}>
              <SummaryField label="Status" value={summary.status} />
            </Grid>
            <Grid size={{ xs: 6, md: 3 }}>
              <SummaryField label="Messages" value={String(summary.messageCount)} />
            </Grid>
            <Grid size={{ xs: 6, md: 3 }}>
              <SummaryField label="Completed" value={String(summary.completedCount)} />
            </Grid>
            <Grid size={{ xs: 6, md: 3 }}>
              <SummaryField
                label="TPS"
                value={summary.tps != null ? summary.tps.toFixed(1) : '-'}
              />
            </Grid>
            <Grid size={{ xs: 6, md: 3 }}>
              <SummaryField
                label="Avg Latency"
                value={summary.avgLatencyMs != null ? `${summary.avgLatencyMs.toFixed(1)} ms` : '-'}
              />
            </Grid>
            <Grid size={{ xs: 6, md: 3 }}>
              <SummaryField
                label="P25"
                value={summary.p25LatencyMs != null ? `${summary.p25LatencyMs.toFixed(1)} ms` : '-'}
              />
            </Grid>
            <Grid size={{ xs: 6, md: 3 }}>
              <SummaryField
                label="P50"
                value={summary.p50LatencyMs != null ? `${summary.p50LatencyMs.toFixed(1)} ms` : '-'}
              />
            </Grid>
            <Grid size={{ xs: 6, md: 3 }}>
              <SummaryField
                label="P75"
                value={summary.p75LatencyMs != null ? `${summary.p75LatencyMs.toFixed(1)} ms` : '-'}
              />
            </Grid>
            <Grid size={{ xs: 6, md: 3 }}>
              <SummaryField
                label="P90"
                value={summary.p90LatencyMs != null ? `${summary.p90LatencyMs.toFixed(1)} ms` : '-'}
              />
            </Grid>
            <Grid size={{ xs: 6, md: 3 }}>
              <SummaryField
                label="P95"
                value={summary.p95LatencyMs != null ? `${summary.p95LatencyMs.toFixed(1)} ms` : '-'}
              />
            </Grid>
            <Grid size={{ xs: 6, md: 3 }}>
              <SummaryField
                label="P99"
                value={summary.p99LatencyMs != null ? `${summary.p99LatencyMs.toFixed(1)} ms` : '-'}
              />
            </Grid>
            <Grid size={{ xs: 6, md: 3 }}>
              <SummaryField
                label="Min Latency"
                value={summary.minLatencyMs != null ? `${summary.minLatencyMs.toFixed(0)} ms` : '-'}
              />
            </Grid>
            <Grid size={{ xs: 6, md: 3 }}>
              <SummaryField
                label="Max Latency"
                value={summary.maxLatencyMs != null ? `${summary.maxLatencyMs.toFixed(0)} ms` : '-'}
              />
            </Grid>
            <Grid size={{ xs: 6, md: 3 }}>
              <SummaryField
                label="Duration"
                value={
                  summary.durationMs != null ? `${(summary.durationMs / 1000).toFixed(1)}s` : '-'
                }
              />
            </Grid>
            <Grid size={{ xs: 6, md: 3 }}>
              <SummaryField
                label="Start Time"
                value={new Date(summary.startedAt).toLocaleString()}
              />
            </Grid>
            <Grid size={{ xs: 6, md: 3 }}>
              <SummaryField
                label="End Time"
                value={summary.completedAt ? new Date(summary.completedAt).toLocaleString() : '-'}
              />
            </Grid>
            {summary.testRunId && (
              <Grid size={{ xs: 12 }}>
                <SummaryField label="Test Run ID" value={summary.testRunId} />
              </Grid>
            )}
          </Grid>
          {parsedThresholds.length > 0 && (
            <Box sx={{ mt: 3 }}>
              <Typography variant="body2" fontWeight={600} sx={{ mb: 1 }}>
                SLA Thresholds
              </Typography>
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                {parsedThresholds.map((t, i) => (
                  <Chip
                    key={i}
                    size="small"
                    color={t.passed ? 'success' : 'error'}
                    label={`${t.metric} ${t.operator} ${t.threshold} (actual: ${t.actual.toFixed(1)})`}
                  />
                ))}
              </Box>
            </Box>
          )}
        </Card>
      ),
    },
    {
      label: 'Logs',
      content: <LogsPanel logs={logs} loading={loadingLogs} />,
    },
    {
      label: 'Monitoring',
      content: <MonitoringPanel snapshots={snapshots} loading={loadingSnapshots} />,
    },
  ];

  return (
    <Box>
      <MuiButton
        component={Link}
        to="/test-runs"
        variant="text"
        startIcon={<ArrowBackIcon />}
        disableElevation
        sx={{ mb: 1 }}
      >
        Back to Test Runs
      </MuiButton>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
        <PageHeader
          title={`Test Run #${id}`}
          subtitle={summary.testId ? `Test ID: ${summary.testId}` : `Run ID: ${summary.testRunId}`}
        />
        <Chip label={summary.status} color={statusColor} sx={{ mt: -1 }} />
        {summary.testType && (
          <Chip
            label={summary.testType}
            size="small"
            color={testTypeColor[summary.testType] ?? 'default'}
            sx={{ mt: -1 }}
          />
        )}
        {summary.thresholdStatus && (
          <Chip
            label={summary.thresholdStatus}
            size="small"
            color={summary.thresholdStatus === 'PASSED' ? 'success' : 'error'}
            sx={{ mt: -1 }}
          />
        )}
        {summary.zipFilePath && (
          <MuiButton
            component="a"
            href={downloadTestRunUrl(numericId)}
            download
            variant="outlined"
            size="small"
            startIcon={<DownloadIcon />}
            sx={{ mt: -1 }}
          >
            Download ZIP
          </MuiButton>
        )}
      </Box>

      <Box sx={{ display: 'flex', alignItems: 'center', flexWrap: 'wrap', gap: 1, mb: 3 }}>
        {tags.map((tag) => (
          <Chip
            key={tag}
            label={tag}
            size="small"
            onDelete={() => void handleRemoveTag(tag)}
          />
        ))}
        <TextField
          size="small"
          placeholder="Add tag"
          value={newTag}
          onChange={(e) => setNewTag(e.target.value)}
          onKeyDown={(e) => { if (e.key === 'Enter') void handleAddTag(); }}
          sx={{ width: 140 }}
        />
        <MuiButton size="small" variant="outlined" onClick={() => void handleAddTag()}>
          Add
        </MuiButton>
      </Box>

      <Tabs tabs={tabs} />

      <Snackbar
        open={tagError != null}
        autoHideDuration={4000}
        onClose={() => setTagError(null)}
      >
        <Alert severity="error" onClose={() => setTagError(null)}>
          {tagError}
        </Alert>
      </Snackbar>
    </Box>
  );
}
