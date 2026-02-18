import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import DownloadIcon from '@mui/icons-material/Download';
import Box from '@mui/material/Box';
import MuiButton from '@mui/material/Button';
import Grid from '@mui/material/Grid';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemText from '@mui/material/ListItemText';
import Typography from '@mui/material/Typography';
import { Alert, Card, Chip, Loading, PageHeader, Tabs } from 'perf-ui-components';
import type { TabItem } from 'perf-ui-components';
import { Link, useParams } from 'react-router-dom';
import { downloadTestRunUrl, getTestRunLogs, getTestRunSummary } from '@/api';
import { useApi } from '@/hooks';
import type { LogEntry } from '@/types/api';

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

export default function TestRunDetailPage() {
  const { id } = useParams<{ id: string }>();
  const numericId = Number(id);

  const {
    data: summary,
    loading: loadingSummary,
    error: errorSummary,
  } = useApi(() => getTestRunSummary(numericId), [numericId]);

  const { data: logs, loading: loadingLogs } = useApi(() => getTestRunLogs(numericId), [numericId]);

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
                value={summary.avgLatencyMs != null ? `${summary.avgLatencyMs.toFixed(0)} ms` : '-'}
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
        </Card>
      ),
    },
    {
      label: 'Logs',
      content: <LogsPanel logs={logs} loading={loadingLogs} />,
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
        <PageHeader
          title={`Test Run #${id}`}
          subtitle={summary.testId ? `Test ID: ${summary.testId}` : `Run ID: ${summary.testRunId}`}
        />
        <Chip label={summary.status} color={statusColor} sx={{ mt: -1 }} />
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
      <Tabs tabs={tabs} />
    </Box>
  );
}
