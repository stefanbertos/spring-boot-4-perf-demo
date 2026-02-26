import AccessTimeIcon from '@mui/icons-material/AccessTime';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ErrorIcon from '@mui/icons-material/Error';
import MessageIcon from '@mui/icons-material/Message';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import SpeedIcon from '@mui/icons-material/Speed';
import Box from '@mui/material/Box';
import Grid from '@mui/material/Grid';
import Typography from '@mui/material/Typography';
import { Alert, Card, Loading, PageHeader, Tabs } from 'perf-ui-components';
import type { TabItem } from 'perf-ui-components';
import type { ReactNode } from 'react';
import { getDashboardLinks, getTestRuns } from '@/api';
import { useApi } from '@/hooks';

const IFRAME_HEIGHT = 'calc(100vh - 200px)';

function IframePanel({ src, title }: { src: string; title: string }) {
  return (
    <Box
      component="iframe"
      src={src}
      title={title}
      sx={{
        width: '100%',
        height: IFRAME_HEIGHT,
        border: 'none',
        borderRadius: 1,
      }}
    />
  );
}

function StatCard({ icon, label, value }: { icon: ReactNode; label: string; value: string }) {
  return (
    <Card>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
        <Box sx={{ color: 'primary.main', display: 'flex' }}>{icon}</Box>
        <Box>
          <Typography variant="body2" color="text.secondary">
            {label}
          </Typography>
          <Typography variant="h5" fontWeight="bold">
            {value}
          </Typography>
        </Box>
      </Box>
    </Card>
  );
}

function OverviewPanel() {
  const { data: runs, loading, error } = useApi(() => getTestRuns());

  if (loading) return <Loading message="Loading overview..." />;
  if (error) return <Alert severity="error">{error.message}</Alert>;

  const totalRuns = runs?.length ?? 0;
  const completed = runs?.filter((r) => r.status === 'COMPLETED').length ?? 0;
  const failed = runs?.filter((r) => r.status === 'FAILED').length ?? 0;
  const totalMessages = runs?.reduce((sum, r) => sum + r.messageCount, 0) ?? 0;
  const tpsValues = runs?.filter((r) => r.tps != null).map((r) => r.tps!) ?? [];
  const avgTps = tpsValues.length > 0 ? tpsValues.reduce((a, b) => a + b, 0) / tpsValues.length : 0;
  const latencyValues =
    runs?.filter((r) => r.avgLatencyMs != null).map((r) => r.avgLatencyMs!) ?? [];
  const avgLatency =
    latencyValues.length > 0 ? latencyValues.reduce((a, b) => a + b, 0) / latencyValues.length : 0;

  return (
    <Grid container spacing={3}>
      <Grid size={{ xs: 12, sm: 6, md: 4 }}>
        <StatCard icon={<PlayArrowIcon />} label="Total Runs" value={String(totalRuns)} />
      </Grid>
      <Grid size={{ xs: 12, sm: 6, md: 4 }}>
        <StatCard icon={<CheckCircleIcon />} label="Completed" value={String(completed)} />
      </Grid>
      <Grid size={{ xs: 12, sm: 6, md: 4 }}>
        <StatCard icon={<ErrorIcon />} label="Failed" value={String(failed)} />
      </Grid>
      <Grid size={{ xs: 12, sm: 6, md: 4 }}>
        <StatCard icon={<MessageIcon />} label="Total Messages" value={String(totalMessages)} />
      </Grid>
      <Grid size={{ xs: 12, sm: 6, md: 4 }}>
        <StatCard icon={<SpeedIcon />} label="Avg TPS" value={avgTps.toFixed(1)} />
      </Grid>
      <Grid size={{ xs: 12, sm: 6, md: 4 }}>
        <StatCard
          icon={<AccessTimeIcon />}
          label="Avg Latency"
          value={`${avgLatency.toFixed(0)} ms`}
        />
      </Grid>
    </Grid>
  );
}

const OVERVIEW_TAB: TabItem = {
  label: 'Overview',
  content: <OverviewPanel />,
};

export default function DashboardsPage() {
  const { data: links, error } = useApi(() => getDashboardLinks());

  const tabs: TabItem[] = [
    OVERVIEW_TAB,
    ...(links ?? []).map((link) => ({
      label: link.label,
      content: <IframePanel src={link.url} title={link.label} />,
    })),
  ];

  return (
    <Box>
      <PageHeader title="Dashboards" subtitle="Overview and monitoring dashboards" />
      {error && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          Could not load dashboard links: {error.message}
        </Alert>
      )}
      <Tabs tabs={tabs} />
    </Box>
  );
}