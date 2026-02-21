import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import Box from '@mui/material/Box';
import Divider from '@mui/material/Divider';
import MuiTextField from '@mui/material/TextField';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import {
  Alert,
  Button,
  Card,
  DataTable,
  Dialog,
  IconButton,
  Select,
  Tooltip,
} from 'perf-ui-components';
import type { DataTableColumn, SelectChangeEvent } from 'perf-ui-components';
import { useCallback, useEffect, useState } from 'react';
import {
  createInfraProfile,
  deleteInfraProfile,
  getInfraProfile,
  listDeployments,
  listNamespaces,
  listInfraProfiles,
  listQueues,
  listTopics,
  updateInfraProfile,
} from '@/api';
import type { InfraProfileSummary, QueueInfo, TopicInfo } from '@/types/api';

const LOG_LEVELS = ['TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR', 'OFF'].map((l) => ({
  value: l,
  label: l,
}));

interface KvEntry {
  key: string;
  value: string;
}

interface ProfileForm {
  id?: number;
  name: string;
  logLevels: KvEntry[];
  kafkaTopics: KvEntry[];
  kubernetesReplicas: KvEntry[];
  ibmMqQueues: KvEntry[];
}

const EMPTY_FORM: ProfileForm = {
  name: '',
  logLevels: [],
  kafkaTopics: [],
  kubernetesReplicas: [],
  ibmMqQueues: [],
};

function KvEditor({
  label,
  entries,
  onChange,
  valueType,
  valuePlaceholder,
  defaultKey,
  keyLabel,
  keyOptions,
  onKeyChange,
}: {
  label: string;
  entries: KvEntry[];
  onChange: (entries: KvEntry[]) => void;
  valueType: 'text' | 'number' | 'loglevel';
  valuePlaceholder?: string;
  defaultKey?: string;
  keyLabel?: string;
  keyOptions?: { value: string; label: string }[];
  onKeyChange?: (key: string) => string;
}) {
  const defaultEntryKey = defaultKey ?? (keyOptions && keyOptions.length > 0 ? keyOptions[0].value : '');
  const add = () => {
    const defaultValue = valueType === 'loglevel' ? 'INFO' : (onKeyChange ? onKeyChange(defaultEntryKey) : '');
    onChange([...entries, { key: defaultEntryKey, value: defaultValue }]);
  };
  const remove = (i: number) => onChange(entries.filter((_, idx) => idx !== i));
  const update = (i: number, field: 'key' | 'value', val: string) => {
    if (field === 'key' && onKeyChange) {
      onChange(entries.map((e, idx) => (idx === i ? { ...e, key: val, value: onKeyChange(val) } : e)));
    } else {
      onChange(entries.map((e, idx) => (idx === i ? { ...e, [field]: val } : e)));
    }
  };

  return (
    <Box>
      <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 1 }}>
        <Typography variant="body2" color="text.secondary" fontWeight={500}>
          {label}
        </Typography>
        <Button size="small" onClick={add}>
          <AddIcon fontSize="small" sx={{ mr: 0.5 }} />
          Add
        </Button>
      </Stack>
      {entries.length === 0 ? (
        <Typography variant="caption" color="text.disabled">
          No entries
        </Typography>
      ) : (
        <Stack spacing={1}>
          {entries.map((entry, i) => (
            <Stack key={i} direction="row" spacing={1} alignItems="center">
              {keyOptions ? (
                <Box sx={{ flex: 1 }}>
                  <Select
                    label={keyLabel ?? 'Key'}
                    value={entry.key}
                    options={keyOptions}
                    onChange={(e: SelectChangeEvent) => update(i, 'key', e.target.value)}
                    size="small"
                    fullWidth
                  />
                </Box>
              ) : (
                <MuiTextField
                  size="small"
                  placeholder={keyLabel ?? 'Key'}
                  value={entry.key}
                  onChange={(e) => update(i, 'key', e.target.value)}
                  sx={{ flex: 1 }}
                />
              )}
              {valueType === 'loglevel' ? (
                <Select
                  label=""
                  value={entry.value}
                  options={LOG_LEVELS}
                  onChange={(e: SelectChangeEvent) => update(i, 'value', e.target.value)}
                  size="small"
                />
              ) : (
                <MuiTextField
                  size="small"
                  placeholder={valuePlaceholder ?? (valueType === 'number' ? 'Value' : 'Value')}
                  type={valueType}
                  value={entry.value}
                  onChange={(e) => update(i, 'value', e.target.value)}
                  sx={{ flex: 1 }}
                />
              )}
              <Tooltip title="Remove">
                <IconButton size="small" onClick={() => remove(i)}>
                  <DeleteIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            </Stack>
          ))}
        </Stack>
      )}
    </Box>
  );
}

function toRecord<V>(entries: KvEntry[], parseValue: (v: string) => V): Record<string, V> {
  return Object.fromEntries(
    entries.filter((e) => e.key.trim() !== '').map((e) => [e.key.trim(), parseValue(e.value)]),
  );
}

function fromRecord<V>(record: Record<string, V>): KvEntry[] {
  return Object.entries(record).map(([key, value]) => ({ key, value: String(value) }));
}

export default function InfraProfileManager() {
  const [profiles, setProfiles] = useState<InfraProfileSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [form, setForm] = useState<ProfileForm>(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [confirmDeleteId, setConfirmDeleteId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [topics, setTopics] = useState<TopicInfo[]>([]);
  const [queues, setQueues] = useState<QueueInfo[]>([]);
  const [topicOptions, setTopicOptions] = useState<{ value: string; label: string }[]>([]);
  const [deploymentOptions, setDeploymentOptions] = useState<{ value: string; label: string }[]>([]);
  const [queueOptions, setQueueOptions] = useState<{ value: string; label: string }[]>([]);
  const [kubernetesAvailable, setKubernetesAvailable] = useState(false);

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      setProfiles(await listInfraProfiles());
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load profiles');
    } finally {
      setLoading(false);
    }
  }, []);

  const loadTopicsAndDeployments = useCallback(() => {
    listTopics()
      .then((data) => {
        setTopics(data);
        setTopicOptions(data.map((t) => ({ value: t.topicName, label: `${t.topicName} (${t.partitions} partitions)` })));
      })
      .catch(() => {});
    listQueues()
      .then((data) => {
        setQueues(data);
        setQueueOptions(data.map((q) => ({ value: q.queueName, label: `${q.queueName} (max: ${q.maxDepth})` })));
      })
      .catch(() => {});
    listNamespaces()
      .then((namespaces) => {
        const hasK8s = namespaces.length > 0;
        setKubernetesAvailable(hasK8s);
        if (hasK8s) {
          listDeployments(namespaces[0].name)
            .then((deployments) => setDeploymentOptions(deployments.map((d) => ({ value: d.name, label: d.name }))))
            .catch(() => {});
        }
      })
      .catch(() => {});
  }, []);

  useEffect(() => {
    void refresh();
    loadTopicsAndDeployments();
  }, [refresh, loadTopicsAndDeployments]);

  const openCreate = () => {
    setForm(EMPTY_FORM);
    setDialogOpen(true);
  };

  const openEdit = async (profile: InfraProfileSummary) => {
    setDialogOpen(true);
    const detail = await getInfraProfile(profile.id);
    setForm({
      id: detail.id,
      name: detail.name,
      logLevels: fromRecord(detail.logLevels),
      kafkaTopics: fromRecord(detail.kafkaTopics),
      kubernetesReplicas: fromRecord(detail.kubernetesReplicas),
      ibmMqQueues: fromRecord(detail.ibmMqQueues),
    });
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      const request = {
        name: form.name,
        logLevels: toRecord(form.logLevels, (v) => v),
        kafkaTopics: toRecord(form.kafkaTopics, Number),
        kubernetesReplicas: toRecord(form.kubernetesReplicas, Number),
        ibmMqQueues: toRecord(form.ibmMqQueues, Number),
      };
      if (form.id != null) {
        await updateInfraProfile(form.id, request);
      } else {
        await createInfraProfile(request);
      }
      setDialogOpen(false);
      await refresh();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to save profile');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (confirmDeleteId == null) return;
    await deleteInfraProfile(confirmDeleteId);
    setConfirmDeleteId(null);
    await refresh();
  };

  const columns: DataTableColumn<InfraProfileSummary>[] = [
    { id: 'name', label: 'Name', minWidth: 140, render: (row) => row.name },
    {
      id: 'updated',
      label: 'Updated',
      render: (row) => new Date(row.updatedAt).toLocaleString(),
    },
    {
      id: 'actions',
      label: 'Actions',
      align: 'right',
      render: (row) => (
        <Stack direction="row" spacing={0.5} justifyContent="flex-end">
          <Tooltip title="Edit">
            <IconButton size="small" onClick={() => void openEdit(row)}>
              <EditIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="Delete">
            <IconButton size="small" onClick={() => setConfirmDeleteId(row.id)}>
              <DeleteIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </Stack>
      ),
    },
  ];

  return (
    <Card sx={{ mb: 3 }}>
      <Stack spacing={2}>
        <Stack direction="row" alignItems="center" justifyContent="space-between">
          <Typography variant="subtitle1" sx={{ fontWeight: 'bold' }}>
            Infrastructure Profiles
          </Typography>
          <Button size="small" onClick={openCreate}>
            New
          </Button>
        </Stack>

        {error && (
          <Alert severity="error" onClose={() => setError(null)}>
            {error}
          </Alert>
        )}
        {loading ? (
          <Typography variant="body2" color="text.secondary">
            Loading profiles...
          </Typography>
        ) : profiles.length === 0 ? (
          <Typography variant="body2" color="text.secondary">
            No infra profiles yet. Create one to define log levels, topic partitions and replica counts.
          </Typography>
        ) : (
          <DataTable columns={columns} rows={profiles} keyExtractor={(row) => row.id} />
        )}
      </Stack>

      {/* Create / Edit dialog */}
      <Dialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        title={form.id != null ? 'Edit Infra Profile' : 'New Infra Profile'}
        maxWidth="md"
        actions={
          <>
            <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
            <Button onClick={handleSave} disabled={saving || !form.name.trim()}>
              {saving ? 'Saving...' : 'Save'}
            </Button>
          </>
        }
      >
        <Stack spacing={3} sx={{ mt: 1 }}>
          <MuiTextField
            label="Profile Name"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            fullWidth
            required
            size="small"
          />
          <Divider />
          <KvEditor
            label="Log Levels (logger name → level)"
            entries={form.logLevels}
            onChange={(e) => setForm({ ...form, logLevels: e })}
            valueType="loglevel"
            keyLabel="Logger Name"
            defaultKey="com.example"
          />
          <Divider />
          <KvEditor
            label="Kafka Topics (topic → partitions)"
            entries={form.kafkaTopics}
            onChange={(e) => setForm({ ...form, kafkaTopics: e })}
            valueType="number"
            keyLabel="Topic Name"
            valuePlaceholder="Partitions"
            keyOptions={topicOptions}
            onKeyChange={(key) => {
              const topic = topics.find((t) => t.topicName === key);
              return topic != null ? String(topic.partitions) : '';
            }}
          />
          <Divider />
          <KvEditor
            label="IBM MQ Queues (queue → max depth)"
            entries={form.ibmMqQueues}
            onChange={(e) => setForm({ ...form, ibmMqQueues: e })}
            valueType="number"
            keyLabel="Queue Name"
            valuePlaceholder="Max Depth"
            keyOptions={queueOptions}
            onKeyChange={(key) => {
              const queue = queues.find((q) => q.queueName === key);
              return queue != null ? String(queue.maxDepth) : '';
            }}
          />
          {kubernetesAvailable && (
            <>
              <Divider />
              <KvEditor
                label="Kubernetes Replicas (deployment → replicas)"
                entries={form.kubernetesReplicas}
                onChange={(e) => setForm({ ...form, kubernetesReplicas: e })}
                valueType="number"
                keyLabel="Deployment"
                valuePlaceholder="Replicas"
                keyOptions={deploymentOptions}
              />
            </>
          )}
        </Stack>
      </Dialog>

      {/* Delete confirmation */}
      <Dialog
        open={confirmDeleteId != null}
        onClose={() => setConfirmDeleteId(null)}
        title="Delete Infra Profile"
        maxWidth="xs"
        actions={
          <>
            <Button onClick={() => setConfirmDeleteId(null)}>Cancel</Button>
            <Button onClick={handleDelete}>Delete</Button>
          </>
        }
      >
        <Typography>Are you sure you want to delete this profile?</Typography>
      </Dialog>
    </Card>
  );
}
