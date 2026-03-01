import Box from '@mui/material/Box';
import FormControl from '@mui/material/FormControl';
import InputLabel from '@mui/material/InputLabel';
import MenuItem from '@mui/material/MenuItem';
import Select from '@mui/material/Select';
import Typography from '@mui/material/Typography';
import { Alert, Card, Loading, PageHeader } from 'perf-ui-components';
import { useState } from 'react';
import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { getTrendData } from '@/api';
import { useApi } from '@/hooks';

export default function TrendsPage() {
  const { data: trendData, loading, error } = useApi(() => getTrendData());
  const [selectedTestId, setSelectedTestId] = useState('');

  if (loading) return <Loading message="Loading trend data..." />;
  if (error) return <Alert severity="error">{error.message}</Alert>;

  const allTestIds = trendData
    ? [...new Set(trendData.map((p) => p.testId).filter(Boolean))].sort()
    : [];

  const filtered = trendData
    ? (selectedTestId ? trendData.filter((p) => p.testId === selectedTestId) : trendData)
    : [];

  const chartData = filtered.map((p) => ({
    date: new Date(p.startedAt).toLocaleDateString(),
    tps: p.tps,
    p99: p.p99LatencyMs,
  }));

  return (
    <Box>
      <PageHeader title="Trends" subtitle="Historical performance trend charts" />

      <FormControl size="small" sx={{ mb: 3, minWidth: 200 }}>
        <InputLabel>Test ID</InputLabel>
        <Select
          value={selectedTestId}
          label="Test ID"
          onChange={(e) => setSelectedTestId(e.target.value)}
        >
          <MenuItem value="">All Tests</MenuItem>
          {allTestIds.map((tid) => (
            <MenuItem key={tid} value={tid}>{tid}</MenuItem>
          ))}
        </Select>
      </FormControl>

      <Box sx={{ mb: 3 }}>
        <Card>
          <Typography variant="subtitle1" fontWeight={600} sx={{ mb: 2 }}>
            TPS Over Time
          </Typography>
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Line type="monotone" dataKey="tps" stroke="#1976d2" name="TPS" dot={false} />
            </LineChart>
          </ResponsiveContainer>
        </Card>
      </Box>

      <Card>
        <Typography variant="subtitle1" fontWeight={600} sx={{ mb: 2 }}>
          P99 Latency Over Time (ms)
        </Typography>
        <ResponsiveContainer width="100%" height={300}>
          <LineChart data={chartData}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="date" />
            <YAxis />
            <Tooltip />
            <Legend />
            <Line type="monotone" dataKey="p99" stroke="#d32f2f" name="P99 Latency (ms)" dot={false} />
          </LineChart>
        </ResponsiveContainer>
      </Card>
    </Box>
  );
}
