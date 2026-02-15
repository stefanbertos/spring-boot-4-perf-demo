import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import Box from '@mui/material/Box';
import MuiButton from '@mui/material/Button';
import Grid from '@mui/material/Grid';
import Typography from '@mui/material/Typography';
import { Alert, Card, Chip, DataTable, Loading, PageHeader, Tabs } from 'perf-ui-components';
import type { DataTableColumn, TabItem } from 'perf-ui-components';
import { Link, useParams } from 'react-router-dom';
import { getTestRunMessages, getTestRunMetrics, getTestRunSummary } from '@/api';
import { useApi } from '@/hooks';
import type { MessageResponse, MetricResponse } from '@/types/api';

const messageColumns: DataTableColumn<MessageResponse>[] = [
  { id: 'id', label: 'ID', render: (row) => row.id },
  { id: 'messageId', label: 'Message ID', render: (row) => row.messageId },
  { id: 'correlationId', label: 'Correlation ID', render: (row) => row.correlationId },
  {
    id: 'status',
    label: 'Status',
    render: (row) => (
      <Chip
        label={row.status}
        size="small"
        color={
          row.status === 'RECEIVED' ? 'success' : row.status === 'FAILED' ? 'error' : 'default'
        }
      />
    ),
  },
  {
    id: 'latency',
    label: 'Latency',
    align: 'right',
    render: (row) => (row.latencyMs != null ? `${row.latencyMs} ms` : '-'),
  },
  {
    id: 'sentAt',
    label: 'Sent At',
    render: (row) => new Date(row.sentAt).toLocaleTimeString(),
  },
];

const metricColumns: DataTableColumn<MetricResponse>[] = [
  { id: 'name', label: 'Metric', render: (row) => row.metricName },
  { id: 'type', label: 'Type', render: (row) => row.metricType },
  {
    id: 'value',
    label: 'Value',
    align: 'right',
    render: (row) => row.metricValue.toFixed(2),
  },
  { id: 'unit', label: 'Unit', render: (row) => row.metricUnit ?? '-' },
  {
    id: 'time',
    label: 'Collected At',
    render: (row) => new Date(row.collectedAt).toLocaleTimeString(),
  },
];

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

export default function TestRunDetailPage() {
  const { id } = useParams<{ id: string }>();
  const numericId = Number(id);

  const {
    data: summary,
    loading: loadingSummary,
    error: errorSummary,
  } = useApi(() => getTestRunSummary(numericId), [numericId]);
  const { data: messages, loading: loadingMessages } = useApi(
    () => getTestRunMessages(numericId),
    [numericId],
  );
  const { data: metrics, loading: loadingMetrics } = useApi(
    () => getTestRunMetrics(numericId),
    [numericId],
  );

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
              <SummaryField label="Total Messages" value={String(summary.totalMessages)} />
            </Grid>
            <Grid size={{ xs: 6, md: 3 }}>
              <SummaryField label="Sent" value={String(summary.sentCount)} />
            </Grid>
            <Grid size={{ xs: 6, md: 3 }}>
              <SummaryField label="Received" value={String(summary.receivedCount)} />
            </Grid>
            <Grid size={{ xs: 6, md: 3 }}>
              <SummaryField label="Failed" value={String(summary.failedCount)} />
            </Grid>
            <Grid size={{ xs: 6, md: 3 }}>
              <SummaryField label="Timeouts" value={String(summary.timeoutCount)} />
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
                value={summary.avgLatencyMs != null ? `${summary.avgLatencyMs.toFixed(0)} ms` : '-'}
              />
            </Grid>
            <Grid size={{ xs: 6, md: 3 }}>
              <SummaryField label="Duration" value={summary.duration ?? '-'} />
            </Grid>
            <Grid size={{ xs: 6, md: 3 }}>
              <SummaryField
                label="Start Time"
                value={new Date(summary.startTime).toLocaleString()}
              />
            </Grid>
            <Grid size={{ xs: 6, md: 3 }}>
              <SummaryField
                label="End Time"
                value={summary.endTime ? new Date(summary.endTime).toLocaleString() : '-'}
              />
            </Grid>
          </Grid>
        </Card>
      ),
    },
    {
      label: 'Messages',
      content: loadingMessages ? (
        <Loading message="Loading messages..." />
      ) : messages && messages.length > 0 ? (
        <DataTable columns={messageColumns} rows={messages} keyExtractor={(row) => row.id} />
      ) : (
        <Typography color="text.secondary">No messages recorded.</Typography>
      ),
    },
    {
      label: 'Metrics',
      content: loadingMetrics ? (
        <Loading message="Loading metrics..." />
      ) : metrics && metrics.length > 0 ? (
        <DataTable columns={metricColumns} rows={metrics} keyExtractor={(row) => row.id} />
      ) : (
        <Typography color="text.secondary">No metrics recorded.</Typography>
      ),
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
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 3 }}>
        <PageHeader title={`Test Run #${id}`} subtitle={`Test ID: ${summary.testId}`} />
        <Chip label={summary.status} color={statusColor} sx={{ mt: -1 }} />
      </Box>
      <Tabs tabs={tabs} />
    </Box>
  );
}
