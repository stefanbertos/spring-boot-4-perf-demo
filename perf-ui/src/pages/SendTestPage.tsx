import Box from '@mui/material/Box';
import Stack from '@mui/material/Stack';
import { Alert, Button, Card, PageHeader, TextField } from 'perf-ui-components';
import type { FormEvent } from 'react';
import { useState } from 'react';
import { sendTest } from '@/api';

export default function SendTestPage() {
  const [message, setMessage] = useState('Hello, performance test!');
  const [count, setCount] = useState('10');
  const [timeout, setTimeout] = useState('30');
  const [delay, setDelay] = useState('0');
  const [testId, setTestId] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

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

  return (
    <Box>
      <PageHeader title="Send Test" subtitle="Configure and run a performance test" />
      <Card sx={{ maxWidth: 600 }}>
        <Box component="form" onSubmit={handleSubmit}>
          <Stack spacing={2.5}>
            <TextField
              label="Message Content"
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              fullWidth
              required
              multiline
              rows={3}
            />
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
    </Box>
  );
}
