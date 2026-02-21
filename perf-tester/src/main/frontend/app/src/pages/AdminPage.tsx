import Box from '@mui/material/Box';
import Stack from '@mui/material/Stack';
import MuiTextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { Alert, Button, Card, DataTable, Loading, PageHeader, Select, Tabs, TextField } from 'perf-ui-components';
import type { DataTableColumn, SelectChangeEvent, TabItem } from 'perf-ui-components';
import type { SelectOption } from 'perf-ui-components';
import type { FormEvent } from 'react';
import { useEffect, useMemo, useState } from 'react';
import { changeQueueMaxDepth, listDeployments, listNamespaces, listQueues, listTopics, resizeTopic, scaleDeployment, setLogLevel } from '@/api';
import type { DeploymentInfo, NamespaceInfo, TopicInfo } from '@/types/api';

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
  const [topics, setTopics] = useState<TopicInfo[]>([]);
  const [topicName, setTopicName] = useState('');
  const [partitions, setPartitions] = useState('1');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  useEffect(() => {
    listTopics()
      .then((data) => {
        setTopics(data);
        if (data.length > 0) {
          setTopicName(data[0].topicName);
          setPartitions(String(data[0].partitions));
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

  const handleTopicChange = (e: SelectChangeEvent) => {
    const name = e.target.value;
    setTopicName(name);
    const topic = topics.find((t) => t.topicName === name);
    if (topic) setPartitions(String(topic.partitions));
  };

  const topicOptions = topics.map((t) => ({
    value: t.topicName,
    label: `${t.topicName} (${t.partitions} partitions)`,
  }));

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
      setTopics((prev) => prev.map((t) => (t.topicName === res.topicName ? res : t)));
      setPartitions(String(res.partitions));
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
            onChange={handleTopicChange}
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

function DeploymentScaleAction({ name, namespace }: { name: string; namespace: string }) {
  const [replicas, setReplicas] = useState('4');
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const handleScale = async () => {
    setSubmitting(true);
    setResult(null);
    try {
      await scaleDeployment(name, namespace, Number(replicas));
      setResult({ type: 'success', text: `Scaled to ${replicas}` });
    } catch (err) {
      setResult({ type: 'error', text: err instanceof Error ? err.message : 'Failed to scale' });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Stack direction="row" spacing={1} alignItems="center">
      <MuiTextField
        type="number"
        value={replicas}
        onChange={(e) => setReplicas(e.target.value)}
        size="small"
        sx={{ width: 80 }}
        slotProps={{ htmlInput: { min: 0 } }}
      />
      <Button disabled={submitting} onClick={handleScale}>
        {submitting ? 'Scaling...' : 'Scale'}
      </Button>
      {result && (
        <Typography variant="caption" color={result.type === 'success' ? 'success.main' : 'error.main'}>
          {result.text}
        </Typography>
      )}
    </Stack>
  );
}

function KubernetesTab() {
  const [namespaces, setNamespaces] = useState<NamespaceInfo[]>([]);
  const [selectedNamespace, setSelectedNamespace] = useState('');
  const [namespacesLoading, setNamespacesLoading] = useState(true);
  const [deployments, setDeployments] = useState<DeploymentInfo[]>([]);
  const [deploymentsLoading, setDeploymentsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listNamespaces()
      .then((ns) => {
        setNamespaces(ns);
        if (ns.length > 0) {
          setSelectedNamespace(ns[0].name);
        }
      })
      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to load namespaces'))
      .finally(() => setNamespacesLoading(false));
  }, []);

  useEffect(() => {
    if (!selectedNamespace) return;
    setDeploymentsLoading(true);
    setDeployments([]);
    listDeployments(selectedNamespace)
      .then(setDeployments)
      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to load deployments'))
      .finally(() => setDeploymentsLoading(false));
  }, [selectedNamespace]);

  const namespaceOptions = namespaces.map((ns) => ({ value: ns.name, label: ns.name }));

  const columns: DataTableColumn<DeploymentInfo>[] = [
    { id: 'name', label: 'Deployment', minWidth: 150, render: (row) => row.name },
    { id: 'desired', label: 'Desired', render: (row) => String(row.desiredReplicas) },
    { id: 'ready', label: 'Ready', render: (row) => String(row.readyReplicas) },
    {
      id: 'scale',
      label: 'Scale To',
      minWidth: 240,
      render: (row) => <DeploymentScaleAction name={row.name} namespace={row.namespace} />,
    },
  ];

  if (namespacesLoading) return <Loading message="Loading namespaces..." />;

  if (error) return <Alert severity="error">{error}</Alert>;

  if (namespaces.length === 0) {
    return (
      <Alert severity="info">
        Kubernetes is not available in this environment. Start the app inside a cluster to manage deployments.
      </Alert>
    );
  }

  return (
    <Stack spacing={3}>
      <Box sx={{ maxWidth: 400 }}>
        <Select
          label="Namespace"
          value={selectedNamespace}
          options={namespaceOptions}
          onChange={(e: SelectChangeEvent) => setSelectedNamespace(e.target.value)}
          fullWidth
        />
      </Box>
      {deploymentsLoading && <Loading message="Loading deployments..." />}
      {!deploymentsLoading && deployments.length === 0 && (
        <Alert severity="info">No deployments found in namespace &quot;{selectedNamespace}&quot;.</Alert>
      )}
      {!deploymentsLoading && deployments.length > 0 && (
        <DataTable columns={columns} rows={deployments} keyExtractor={(row) => row.name} />
      )}
    </Stack>
  );
}

export default function AdminPage() {
  const [kubernetesAvailable, setKubernetesAvailable] = useState(false);

  useEffect(() => {
    listNamespaces().then((ns) => setKubernetesAvailable(ns.length > 0)).catch(() => {});
  }, []);

  const tabs: TabItem[] = useMemo(() => {
    const items: TabItem[] = [
      { label: 'Logging', content: <LoggingTab /> },
      { label: 'Kafka', content: <KafkaTab /> },
      { label: 'IBM MQ', content: <IbmMqTab /> },
    ];
    if (kubernetesAvailable) {
      items.push({ label: 'Kubernetes', content: <KubernetesTab /> });
    }
    return items;
  }, [kubernetesAvailable]);

  return (
    <Box>
      <PageHeader title="Admin" subtitle="Logging, Kafka, IBM MQ, and Kubernetes administration" />
      <Tabs tabs={tabs} />
    </Box>
  );
}
