import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import Box from '@mui/material/Box';
import MuiButton from '@mui/material/Button';
import Paper from '@mui/material/Paper';
import Stack from '@mui/material/Stack';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Typography from '@mui/material/Typography';
import { Alert, Chip, Loading, PageHeader } from 'perf-ui-components';
import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { getTestRunSummary } from '@/api';
import type { TestRunDetailResponse } from '@/types/api';

const statusColor: Record<string, 'success' | 'error' | 'warning' | 'info' | 'default'> = {
  COMPLETED: 'success',
  FAILED: 'error',
  RUNNING: 'info',
  TIMEOUT: 'warning',
};

function formatTps(v: number | null): string {
  return v != null ? v.toFixed(1) : '-';
}

function formatLatency(v: number | null): string {
  return v != null ? `${v.toFixed(1)} ms` : '-';
}

function formatDuration(v: number | null): string {
  return v != null ? `${(v / 1000).toFixed(2)}s` : '-';
}

function formatDate(v: string | null): string {
  return v ? new Date(v).toLocaleString() : '-';
}

interface DeltaProps {
  a: number | null;
  b: number | null;
  higherIsBetter: boolean;
}

function Delta({ a, b, higherIsBetter }: DeltaProps) {
  if (a == null || b == null || b === 0) {
    return <Typography variant="body2" color="text.secondary">—</Typography>;
  }
  const pct = ((a - b) / Math.abs(b)) * 100;
  const better = higherIsBetter ? pct > 0 : pct < 0;
  const worse = higherIsBetter ? pct < 0 : pct > 0;
  const color = better ? 'success.main' : worse ? 'error.main' : 'text.secondary';
  const sign = pct >= 0 ? '+' : '';
  return (
    <Typography variant="body2" color={color} fontWeight={better || worse ? 600 : 400}>
      {sign}{pct.toFixed(1)}%
    </Typography>
  );
}

export default function TestRunComparePage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const id1 = Number(searchParams.get('id1'));
  const id2 = Number(searchParams.get('id2'));

  const [runA, setRunA] = useState<TestRunDetailResponse | null>(null);
  const [runB, setRunB] = useState<TestRunDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id1 || !id2) {
      setError('Invalid test run IDs');
      setLoading(false);
      return;
    }
    Promise.all([getTestRunSummary(id1), getTestRunSummary(id2)])
      .then(([a, b]) => {
        setRunA(a);
        setRunB(b);
      })
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, [id1, id2]);

  if (loading) return <Loading message="Loading test runs..." />;
  if (error) return <Alert severity="error">{error}</Alert>;
  if (!runA || !runB) return null;

  return (
    <Box>
      <Stack direction="row" alignItems="center" sx={{ mb: 2 }}>
        <MuiButton startIcon={<ArrowBackIcon />} size="small" onClick={() => void navigate('/test-runs')}>
          Back
        </MuiButton>
      </Stack>
      <PageHeader title="Compare Test Runs" subtitle={`Run #${runA.id} vs Run #${runB.id}`} />
      <TableContainer component={Paper} variant="outlined">
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>
                <Typography variant="body2" fontWeight={600}>Metric</Typography>
              </TableCell>
              <TableCell align="right">
                <Typography variant="body2" fontWeight={600}>Run #{runA.id}</Typography>
              </TableCell>
              <TableCell align="right">
                <Typography variant="body2" fontWeight={600}>Run #{runB.id}</Typography>
              </TableCell>
              <TableCell align="right">
                <Typography variant="body2" fontWeight={600}>Δ (A vs B)</Typography>
              </TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            <TableRow>
              <TableCell>Status</TableCell>
              <TableCell align="right">
                <Chip label={runA.status} size="small" color={statusColor[runA.status] ?? 'default'} />
              </TableCell>
              <TableCell align="right">
                <Chip label={runB.status} size="small" color={statusColor[runB.status] ?? 'default'} />
              </TableCell>
              <TableCell align="right">
                <Typography variant="body2" color="text.secondary">—</Typography>
              </TableCell>
            </TableRow>
            <TableRow>
              <TableCell>Messages</TableCell>
              <TableCell align="right">{runA.messageCount.toLocaleString()}</TableCell>
              <TableCell align="right">{runB.messageCount.toLocaleString()}</TableCell>
              <TableCell align="right">
                <Typography variant="body2" color="text.secondary">—</Typography>
              </TableCell>
            </TableRow>
            <TableRow>
              <TableCell>TPS</TableCell>
              <TableCell align="right">{formatTps(runA.tps)}</TableCell>
              <TableCell align="right">{formatTps(runB.tps)}</TableCell>
              <TableCell align="right">
                <Delta a={runA.tps} b={runB.tps} higherIsBetter={true} />
              </TableCell>
            </TableRow>
            <TableRow>
              <TableCell>Avg Latency</TableCell>
              <TableCell align="right">{formatLatency(runA.avgLatencyMs)}</TableCell>
              <TableCell align="right">{formatLatency(runB.avgLatencyMs)}</TableCell>
              <TableCell align="right">
                <Delta a={runA.avgLatencyMs} b={runB.avgLatencyMs} higherIsBetter={false} />
              </TableCell>
            </TableRow>
            <TableRow>
              <TableCell>Min Latency</TableCell>
              <TableCell align="right">{formatLatency(runA.minLatencyMs)}</TableCell>
              <TableCell align="right">{formatLatency(runB.minLatencyMs)}</TableCell>
              <TableCell align="right">
                <Delta a={runA.minLatencyMs} b={runB.minLatencyMs} higherIsBetter={false} />
              </TableCell>
            </TableRow>
            <TableRow>
              <TableCell>Max Latency</TableCell>
              <TableCell align="right">{formatLatency(runA.maxLatencyMs)}</TableCell>
              <TableCell align="right">{formatLatency(runB.maxLatencyMs)}</TableCell>
              <TableCell align="right">
                <Delta a={runA.maxLatencyMs} b={runB.maxLatencyMs} higherIsBetter={false} />
              </TableCell>
            </TableRow>
            <TableRow>
              <TableCell>P50</TableCell>
              <TableCell align="right">{formatLatency(runA.p50LatencyMs)}</TableCell>
              <TableCell align="right">{formatLatency(runB.p50LatencyMs)}</TableCell>
              <TableCell align="right">
                <Delta a={runA.p50LatencyMs} b={runB.p50LatencyMs} higherIsBetter={false} />
              </TableCell>
            </TableRow>
            <TableRow>
              <TableCell>P90</TableCell>
              <TableCell align="right">{formatLatency(runA.p90LatencyMs)}</TableCell>
              <TableCell align="right">{formatLatency(runB.p90LatencyMs)}</TableCell>
              <TableCell align="right">
                <Delta a={runA.p90LatencyMs} b={runB.p90LatencyMs} higherIsBetter={false} />
              </TableCell>
            </TableRow>
            <TableRow>
              <TableCell>P95</TableCell>
              <TableCell align="right">{formatLatency(runA.p95LatencyMs)}</TableCell>
              <TableCell align="right">{formatLatency(runB.p95LatencyMs)}</TableCell>
              <TableCell align="right">
                <Delta a={runA.p95LatencyMs} b={runB.p95LatencyMs} higherIsBetter={false} />
              </TableCell>
            </TableRow>
            <TableRow>
              <TableCell>P99</TableCell>
              <TableCell align="right">{formatLatency(runA.p99LatencyMs)}</TableCell>
              <TableCell align="right">{formatLatency(runB.p99LatencyMs)}</TableCell>
              <TableCell align="right">
                <Delta a={runA.p99LatencyMs} b={runB.p99LatencyMs} higherIsBetter={false} />
              </TableCell>
            </TableRow>
            <TableRow>
              <TableCell>Duration</TableCell>
              <TableCell align="right">{formatDuration(runA.durationMs)}</TableCell>
              <TableCell align="right">{formatDuration(runB.durationMs)}</TableCell>
              <TableCell align="right">
                <Typography variant="body2" color="text.secondary">—</Typography>
              </TableCell>
            </TableRow>
            <TableRow>
              <TableCell>Started</TableCell>
              <TableCell align="right">{formatDate(runA.startedAt)}</TableCell>
              <TableCell align="right">{formatDate(runB.startedAt)}</TableCell>
              <TableCell align="right">
                <Typography variant="body2" color="text.secondary">—</Typography>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  );
}
