import Box from '@mui/material/Box';
import Stack from '@mui/material/Stack';
import MuiSwitch from '@mui/material/Switch';
import MuiTextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { Alert, Button, Card, DataTable, Loading, PageHeader, Select, Tabs, TextField } from 'perf-ui-components';
import type { DataTableColumn, SelectChangeEvent, TabItem } from 'perf-ui-components';
import type { SelectOption } from 'perf-ui-components';
import type { FormEvent } from 'react';
import { useEffect, useMemo, useState } from 'react';
import { addLokiServiceLabel, changeQueueMaxDepth, createDbExportQuery, deleteDbExportQuery, deleteLokiServiceLabel, listDbExportQueries, listDeployments, listHealthCheckConfigs, listLokiServiceLabels, listNamespaces, listQueues, listTopics, resizeTopic, scaleDeployment, setLogLevel, updateDbExportQuery, updateHealthCheckConfig } from '@/api';
import type { DbExportQuery, DeploymentInfo, HealthCheckConfig, NamespaceInfo, TopicInfo } from '@/types/api';

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

function LokiTab() {
  const [labels, setLabels] = useState<string[]>([]);
  const [newName, setNewName] = useState('');
  const [loading, setLoading] = useState(true);
  const [adding, setAdding] = useState(false);
  const [deletingName, setDeletingName] = useState<string | null>(null);
  const [result, setResult] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  useEffect(() => {
    listLokiServiceLabels()
      .then(setLabels)
      .catch((err) => setResult({ type: 'error', text: err instanceof Error ? err.message : 'Failed to load service labels' }))
      .finally(() => setLoading(false));
  }, []);

  const handleAdd = async (e: FormEvent) => {
    e.preventDefault();
    const trimmed = newName.trim();
    if (!trimmed) return;
    setAdding(true);
    setResult(null);
    try {
      await addLokiServiceLabel(trimmed);
      setLabels((prev) => [...prev, trimmed].sort());
      setNewName('');
      setResult({ type: 'success', text: `Added "${trimmed}"` });
    } catch (err) {
      setResult({ type: 'error', text: err instanceof Error ? err.message : 'Failed to add label' });
    } finally {
      setAdding(false);
    }
  };

  const handleDelete = async (name: string) => {
    setDeletingName(name);
    setResult(null);
    try {
      await deleteLokiServiceLabel(name);
      setLabels((prev) => prev.filter((l) => l !== name));
    } catch (err) {
      setResult({ type: 'error', text: err instanceof Error ? err.message : 'Failed to delete label' });
    } finally {
      setDeletingName(null);
    }
  };

  if (loading) return <Loading message="Loading Loki service labels..." />;

  return (
    <Card sx={{ maxWidth: 500 }}>
      <Typography variant="subtitle1" fontWeight="bold" sx={{ mb: 1 }}>
        Loki Service Labels
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        Log export queries Loki for all listed service names using the <code>job</code> label.
        Changes apply immediately on the next export.
      </Typography>
      <Stack spacing={1} sx={{ mb: 2 }}>
        {labels.length === 0 && (
          <Typography variant="body2" color="text.secondary">
            No service labels configured.
          </Typography>
        )}
        {labels.map((label) => (
          <Stack key={label} direction="row" alignItems="center" spacing={1}>
            <Typography variant="body2" sx={{ flex: 1, fontFamily: 'monospace' }}>
              {label}
            </Typography>
            <Button
              disabled={deletingName === label}
              onClick={() => handleDelete(label)}
            >
              {deletingName === label ? 'Removing...' : 'Remove'}
            </Button>
          </Stack>
        ))}
      </Stack>
      <Box component="form" onSubmit={handleAdd}>
        <Stack direction="row" spacing={1} alignItems="flex-start">
          <TextField
            label="Service name"
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
            placeholder="e.g. fluentd"
            size="small"
            sx={{ flex: 1 }}
          />
          <Button type="submit" disabled={adding || !newName.trim()}>
            {adding ? 'Adding...' : 'Add'}
          </Button>
        </Stack>
      </Box>
      {result && (
        <Box sx={{ mt: 2 }}>
          <Alert severity={result.type} onClose={() => setResult(null)}>
            {result.text}
          </Alert>
        </Box>
      )}
    </Card>
  );
}

type PingEdit = {
  host: string;
  port: string;
  enabled: boolean;
  connectionTimeoutMs: string;
  intervalMs: string;
};

function PingTab() {
  const [configs, setConfigs] = useState<HealthCheckConfig[]>([]);
  const [edits, setEdits] = useState<Record<string, PingEdit>>({});
  const [saving, setSaving] = useState<Record<string, boolean>>({});
  const [results, setResults] = useState<Record<string, { type: 'success' | 'error'; text: string } | null>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listHealthCheckConfigs()
      .then((data) => {
        setConfigs(data);
        const initial: Record<string, PingEdit> = {};
        data.forEach((c) => {
          initial[c.service] = {
            host: c.host,
            port: String(c.port),
            enabled: c.enabled,
            connectionTimeoutMs: String(c.connectionTimeoutMs),
            intervalMs: String(c.intervalMs),
          };
        });
        setEdits(initial);
      })
      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to load ping configuration'))
      .finally(() => setLoading(false));
  }, []);

  const updateEdit = (service: string, field: keyof PingEdit, value: string | boolean) => {
    setEdits((prev) => ({ ...prev, [service]: { ...prev[service], [field]: value } }));
  };

  const handleSave = async (service: string) => {
    const edit = edits[service];
    setSaving((prev) => ({ ...prev, [service]: true }));
    setResults((prev) => ({ ...prev, [service]: null }));
    try {
      const updated = await updateHealthCheckConfig(service, {
        host: edit.host,
        port: Number(edit.port),
        enabled: edit.enabled,
        connectionTimeoutMs: Number(edit.connectionTimeoutMs),
        intervalMs: Number(edit.intervalMs),
      });
      setConfigs((prev) => prev.map((c) => (c.service === updated.service ? updated : c)));
      setResults((prev) => ({ ...prev, [service]: { type: 'success', text: 'Saved' } }));
    } catch (err) {
      setResults((prev) => ({
        ...prev,
        [service]: { type: 'error', text: err instanceof Error ? err.message : 'Failed to save' },
      }));
    } finally {
      setSaving((prev) => ({ ...prev, [service]: false }));
    }
  };

  const columns: DataTableColumn<HealthCheckConfig>[] = [
    {
      id: 'service',
      label: 'Service',
      minWidth: 80,
      render: (row) => (
        <Typography variant="body2" fontWeight="bold" sx={{ textTransform: 'uppercase' }}>
          {row.service}
        </Typography>
      ),
    },
    {
      id: 'enabled',
      label: 'Enabled',
      render: (row) => (
        <MuiSwitch
          checked={edits[row.service]?.enabled ?? row.enabled}
          onChange={(e) => updateEdit(row.service, 'enabled', e.target.checked)}
          size="small"
        />
      ),
    },
    {
      id: 'host',
      label: 'Host',
      minWidth: 160,
      render: (row) => (
        <MuiTextField
          value={edits[row.service]?.host ?? row.host}
          onChange={(e) => updateEdit(row.service, 'host', e.target.value)}
          size="small"
          sx={{ width: 160 }}
        />
      ),
    },
    {
      id: 'port',
      label: 'Port',
      render: (row) => (
        <MuiTextField
          type="number"
          value={edits[row.service]?.port ?? String(row.port)}
          onChange={(e) => updateEdit(row.service, 'port', e.target.value)}
          size="small"
          sx={{ width: 90 }}
          slotProps={{ htmlInput: { min: 1, max: 65535 } }}
        />
      ),
    },
    {
      id: 'timeout',
      label: 'Timeout (ms)',
      render: (row) => (
        <MuiTextField
          type="number"
          value={edits[row.service]?.connectionTimeoutMs ?? String(row.connectionTimeoutMs)}
          onChange={(e) => updateEdit(row.service, 'connectionTimeoutMs', e.target.value)}
          size="small"
          sx={{ width: 110 }}
          slotProps={{ htmlInput: { min: 100 } }}
        />
      ),
    },
    {
      id: 'interval',
      label: 'Interval (ms)',
      render: (row) => (
        <MuiTextField
          type="number"
          value={edits[row.service]?.intervalMs ?? String(row.intervalMs)}
          onChange={(e) => updateEdit(row.service, 'intervalMs', e.target.value)}
          size="small"
          sx={{ width: 110 }}
          slotProps={{ htmlInput: { min: 1000 } }}
        />
      ),
    },
    {
      id: 'save',
      label: '',
      render: (row) => (
        <Stack direction="row" spacing={1} alignItems="center">
          <Button disabled={saving[row.service]} onClick={() => handleSave(row.service)}>
            {saving[row.service] ? 'Saving...' : 'Save'}
          </Button>
          {results[row.service] && (
            <Typography
              variant="caption"
              color={results[row.service]?.type === 'success' ? 'success.main' : 'error.main'}
            >
              {results[row.service]?.text}
            </Typography>
          )}
        </Stack>
      ),
    },
  ];

  if (loading) return <Loading message="Loading ping configuration..." />;
  if (error) return <Alert severity="error">{error}</Alert>;

  return (
    <Stack spacing={2}>
      <Typography variant="body2" color="text.secondary">
        Configure TCP connectivity checks per service. Changes take effect within 5 seconds.
      </Typography>
      <DataTable columns={columns} rows={configs} keyExtractor={(row) => row.service} />
    </Stack>
  );
}

type DbQueryEdit = {
  name: string;
  sqlQuery: string;
  displayOrder: string;
};

function DatabaseTab() {
  const [queries, setQueries] = useState<DbExportQuery[]>([]);
  const [loading, setLoading] = useState(true);
  const [editingId, setEditingId] = useState<number | 'new' | null>(null);
  const [form, setForm] = useState<DbQueryEdit>({ name: '', sqlQuery: '', displayOrder: '0' });
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [result, setResult] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  useEffect(() => {
    listDbExportQueries()
      .then(setQueries)
      .catch((err) => setResult({ type: 'error', text: err instanceof Error ? err.message : 'Failed to load queries' }))
      .finally(() => setLoading(false));
  }, []);

  const startAdd = () => {
    setEditingId('new');
    setForm({ name: '', sqlQuery: '', displayOrder: '0' });
    setResult(null);
  };

  const startEdit = (q: DbExportQuery) => {
    setEditingId(q.id);
    setForm({ name: q.name, sqlQuery: q.sqlQuery, displayOrder: String(q.displayOrder) });
    setResult(null);
  };

  const cancelEdit = () => {
    setEditingId(null);
    setResult(null);
  };

  const handleSave = async () => {
    setSaving(true);
    setResult(null);
    try {
      const data = { name: form.name, sqlQuery: form.sqlQuery, displayOrder: Number(form.displayOrder) };
      if (editingId === 'new') {
        const created = await createDbExportQuery(data);
        setQueries((prev) => [...prev, created].sort((a, b) => a.displayOrder - b.displayOrder || a.name.localeCompare(b.name)));
        setResult({ type: 'success', text: `Query "${created.name}" created` });
      } else if (editingId != null) {
        const updated = await updateDbExportQuery(editingId, data);
        setQueries((prev) => prev.map((q) => (q.id === updated.id ? updated : q)).sort((a, b) => a.displayOrder - b.displayOrder || a.name.localeCompare(b.name)));
        setResult({ type: 'success', text: `Query "${updated.name}" updated` });
      }
      setEditingId(null);
    } catch (err) {
      setResult({ type: 'error', text: err instanceof Error ? err.message : 'Failed to save query' });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: number, name: string) => {
    setDeletingId(id);
    setResult(null);
    try {
      await deleteDbExportQuery(id);
      setQueries((prev) => prev.filter((q) => q.id !== id));
      setResult({ type: 'success', text: `Query "${name}" deleted` });
      if (editingId === id) setEditingId(null);
    } catch (err) {
      setResult({ type: 'error', text: err instanceof Error ? err.message : 'Failed to delete query' });
    } finally {
      setDeletingId(null);
    }
  };

  if (loading) return <Loading message="Loading database queries..." />;

  return (
    <Stack spacing={3}>
      <Typography variant="body2" color="text.secondary">
        Configure SELECT queries to execute during export. Results are saved as CSV files in the
        <code> db/</code> folder of the ZIP.
      </Typography>

      {queries.length === 0 && editingId === null && (
        <Typography variant="body2" color="text.secondary">
          No queries configured yet. Click &quot;Add Query&quot; to get started.
        </Typography>
      )}

      {queries.map((q) => (
        <Card key={q.id}>
          <Stack spacing={1}>
            <Stack direction="row" alignItems="center" justifyContent="space-between">
              <Stack direction="row" spacing={1} alignItems="center">
                <Typography variant="body2" fontWeight="bold">
                  {q.name}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  order: {q.displayOrder}
                </Typography>
              </Stack>
              <Stack direction="row" spacing={1}>
                <Button size="small" disabled={deletingId === q.id} onClick={() => startEdit(q)}>
                  Edit
                </Button>
                <Button size="small" disabled={deletingId === q.id} onClick={() => handleDelete(q.id, q.name)}>
                  {deletingId === q.id ? 'Deleting...' : 'Delete'}
                </Button>
              </Stack>
            </Stack>
            <Typography
              variant="caption"
              sx={{ fontFamily: 'monospace', whiteSpace: 'pre-wrap', wordBreak: 'break-all', color: 'text.secondary' }}
            >
              {q.sqlQuery.length > 200 ? q.sqlQuery.slice(0, 200) + '…' : q.sqlQuery}
            </Typography>
          </Stack>
        </Card>
      ))}

      {editingId !== null && (
        <Card>
          <Typography variant="subtitle2" fontWeight="bold" sx={{ mb: 2 }}>
            {editingId === 'new' ? 'Add New Query' : 'Edit Query'}
          </Typography>
          <Stack spacing={2}>
            <Stack direction="row" spacing={2}>
              <TextField
                label="Name"
                value={form.name}
                onChange={(e) => setForm((p) => ({ ...p, name: e.target.value }))}
                fullWidth
                required
              />
              <MuiTextField
                label="Display Order"
                type="number"
                value={form.displayOrder}
                onChange={(e) => setForm((p) => ({ ...p, displayOrder: e.target.value }))}
                sx={{ width: 140 }}
                size="small"
              />
            </Stack>
            <MuiTextField
              label="SQL Query (SELECT only)"
              value={form.sqlQuery}
              onChange={(e) => setForm((p) => ({ ...p, sqlQuery: e.target.value }))}
              multiline
              rows={5}
              fullWidth
              required
              slotProps={{ htmlInput: { style: { fontFamily: 'monospace', fontSize: 13 } } }}
            />
            <Stack direction="row" spacing={1}>
              <Button disabled={saving || !form.name.trim() || !form.sqlQuery.trim()} onClick={handleSave}>
                {saving ? 'Saving...' : 'Save'}
              </Button>
              <Button onClick={cancelEdit}>Cancel</Button>
            </Stack>
          </Stack>
        </Card>
      )}

      {editingId === null && (
        <Box>
          <Button onClick={startAdd}>Add Query</Button>
        </Box>
      )}

      {result && (
        <Alert severity={result.type} onClose={() => setResult(null)}>
          {result.text}
        </Alert>
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
      { label: 'Loki', content: <LokiTab /> },
      { label: 'Ping', content: <PingTab /> },
      { label: 'Database', content: <DatabaseTab /> },
    ];
    if (kubernetesAvailable) {
      items.push({ label: 'Kubernetes', content: <KubernetesTab /> });
    }
    return items;
  }, [kubernetesAvailable]);

  return (
    <Box>
      <PageHeader title="Admin" subtitle="Logging, Kafka, IBM MQ, Loki, Ping, Database, and Kubernetes administration" />
      <Tabs tabs={tabs} />
    </Box>
  );
}
