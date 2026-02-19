import Box from '@mui/material/Box';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { Alert, Button, Card, PageHeader, Select, Tabs, TextField } from 'perf-ui-components';
import type { SelectChangeEvent, TabItem } from 'perf-ui-components';
import type { SelectOption } from 'perf-ui-components';
import type { FormEvent } from 'react';
import { useEffect, useState } from 'react';
import { changeQueueMaxDepth, listQueues, listTopics, resizeTopic, setLogLevel } from '@/api';

const logLevels = [
  { value: 'TRACE', label: 'TRACE' },
  { value: 'DEBUG', label: 'DEBUG' },
  { value: 'INFO', label: 'INFO' },
  { value: 'WARN', label: 'WARN' },
  { value: 'ERROR', label: 'ERROR' },
  { value: 'OFF', label: 'OFF' },
];

function LoggingTab() {
  const [loggerName, setLoggerName] = useState('com.example');
  const [level, setLevel] = useState('INFO');
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setResult(null);
    try {
      const res = await setLogLevel(level, loggerName || undefined);
      setResult({
        type: 'success',
        text: `Logger "${res.loggerName}" set to ${res.effectiveLevel}`,
      });
    } catch (err) {
      setResult({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to set log level',
      });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Card sx={{ maxWidth: 500 }}>
      <Typography variant="subtitle1" fontWeight="bold" sx={{ mb: 2 }}>
        Change Log Level
      </Typography>
      <Box component="form" onSubmit={handleSubmit}>
        <Stack spacing={2}>
          <TextField
            label="Logger Name"
            value={loggerName}
            onChange={(e) => setLoggerName(e.target.value)}
            fullWidth
            placeholder="com.example"
          />
          <Select
            label="Level"
            value={level}
            options={logLevels}
            onChange={(e: SelectChangeEvent) => setLevel(e.target.value)}
            fullWidth
          />
          {result && (
            <Alert severity={result.type} onClose={() => setResult(null)}>
              {result.text}
            </Alert>
          )}
          <Button type="submit" disabled={submitting} sx={{ alignSelf: 'flex-start' }}>
            {submitting ? 'Applying...' : 'Set Level'}
          </Button>
        </Stack>
      </Box>
    </Card>
  );
}

function KafkaTab() {
  const [topicOptions, setTopicOptions] = useState<SelectOption[]>([]);
  const [topicName, setTopicName] = useState('');
  const [partitions, setPartitions] = useState('3');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  useEffect(() => {
    listTopics()
      .then((topics) => {
        const options = topics.map((t) => ({
          value: t.topicName,
          label: `${t.topicName} (${t.partitions} partitions)`,
        }));
        setTopicOptions(options);
        if (options.length > 0) {
          setTopicName(options[0].value);
        }
      })
      .catch((err) => {
        setResult({
          type: 'error',
          text: err instanceof Error ? err.message : 'Failed to load topics',
        });
      })
      .finally(() => setLoading(false));
  }, []);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setResult(null);
    try {
      const res = await resizeTopic(topicName, Number(partitions));
      setResult({
        type: 'success',
        text: `Topic "${res.topicName}" resized to ${res.partitions} partitions`,
      });
    } catch (err) {
      setResult({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to resize topic',
      });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Card sx={{ maxWidth: 500 }}>
      <Typography variant="subtitle1" fontWeight="bold" sx={{ mb: 2 }}>
        Resize Kafka Topic
      </Typography>
      <Box component="form" onSubmit={handleSubmit}>
        <Stack spacing={2}>
          <Select
            label="Topic Name"
            value={topicName}
            options={topicOptions}
            onChange={(e: SelectChangeEvent) => setTopicName(e.target.value)}
            fullWidth
            disabled={loading}
          />
          <TextField
            label="Partitions"
            type="number"
            value={partitions}
            onChange={(e) => setPartitions(e.target.value)}
            fullWidth
            required
            slotProps={{ htmlInput: { min: 1 } }}
          />
          {result && (
            <Alert severity={result.type} onClose={() => setResult(null)}>
              {result.text}
            </Alert>
          )}
          <Button
            type="submit"
            disabled={submitting || loading || !topicName}
            sx={{ alignSelf: 'flex-start' }}
          >
            {submitting ? 'Resizing...' : 'Resize Topic'}
          </Button>
        </Stack>
      </Box>
    </Card>
  );
}

function IbmMqTab() {
  const [queueOptions, setQueueOptions] = useState<SelectOption[]>([]);
  const [queueName, setQueueName] = useState('');
  const [maxDepth, setMaxDepth] = useState('5000');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  useEffect(() => {
    listQueues()
      .then((queues) => {
        const options = queues.map((q) => ({
          value: q.queueName,
          label: `${q.queueName} (depth: ${q.currentDepth}/${q.maxDepth})`,
        }));
        setQueueOptions(options);
        if (options.length > 0) {
          setQueueName(options[0].value);
        }
      })
      .catch((err) => {
        setResult({
          type: 'error',
          text: err instanceof Error ? err.message : 'Failed to load queues',
        });
      })
      .finally(() => setLoading(false));
  }, []);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setResult(null);
    try {
      const res = await changeQueueMaxDepth(queueName, Number(maxDepth));
      setResult({
        type: 'success',
        text: `Queue "${res.queueName}" max depth set to ${res.maxDepth}`,
      });
    } catch (err) {
      setResult({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to change queue depth',
      });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Card sx={{ maxWidth: 500 }}>
      <Typography variant="subtitle1" fontWeight="bold" sx={{ mb: 2 }}>
        Change Queue Max Depth
      </Typography>
      <Box component="form" onSubmit={handleSubmit}>
        <Stack spacing={2}>
          <Select
            label="Queue Name"
            value={queueName}
            options={queueOptions}
            onChange={(e: SelectChangeEvent) => setQueueName(e.target.value)}
            fullWidth
            disabled={loading}
          />
          <TextField
            label="Max Depth"
            type="number"
            value={maxDepth}
            onChange={(e) => setMaxDepth(e.target.value)}
            fullWidth
            required
            slotProps={{ htmlInput: { min: 1 } }}
          />
          {result && (
            <Alert severity={result.type} onClose={() => setResult(null)}>
              {result.text}
            </Alert>
          )}
          <Button
            type="submit"
            disabled={submitting || loading || !queueName}
            sx={{ alignSelf: 'flex-start' }}
          >
            {submitting ? 'Applying...' : 'Set Max Depth'}
          </Button>
        </Stack>
      </Box>
    </Card>
  );
}

export default function AdminPage() {
  const tabs: TabItem[] = [
    { label: 'Logging', content: <LoggingTab /> },
    { label: 'Kafka', content: <KafkaTab /> },
    { label: 'IBM MQ', content: <IbmMqTab /> },
  ];

  return (
    <Box>
      <PageHeader title="Admin" subtitle="Logging, Kafka, and IBM MQ administration" />
      <Tabs tabs={tabs} />
    </Box>
  );
}
