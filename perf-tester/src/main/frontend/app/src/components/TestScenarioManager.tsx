import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import Box from '@mui/material/Box';
import Checkbox from '@mui/material/Checkbox';
import Divider from '@mui/material/Divider';
import FormControlLabel from '@mui/material/FormControlLabel';
import MenuItem from '@mui/material/MenuItem';
import MuiTab from '@mui/material/Tab';
import MuiTabs from '@mui/material/Tabs';
import MuiTextField from '@mui/material/TextField';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import {
  Alert,
  Button,
  DataTable,
  Dialog,
  IconButton,
  TextField,
  Tooltip,
} from 'perf-ui-components';
import type { DataTableColumn } from 'perf-ui-components';
import { useCallback, useEffect, useState } from 'react';
import {
  createHeaderTemplate,
  createResponseTemplate,
  createTestCase,
  createTestScenario,
  deleteHeaderTemplate,
  deleteResponseTemplate,
  deleteTestCase,
  deleteTestScenario,
  getHeaderTemplate,
  getResponseTemplate,
  getTestCase,
  getTestScenario,
  listHeaderTemplates,
  listInfraProfiles,
  listResponseTemplates,
  listTestCases,
  listTestScenarios,
  updateHeaderTemplate,
  updateResponseTemplate,
  updateTestCase,
  updateTestScenario,
} from '@/api';
import type {
  HeaderTemplateField,
  HeaderTemplateSummary,
  InfraProfileSummary,
  ResponseTemplateField,
  ResponseTemplateSummary,
  TestCaseSummary,
  TestScenarioSummary,
  ThresholdDef,
  ThinkTimeConfig,
} from '@/types/api';
import InfraProfileManager from './InfraProfileManager';

// ── Shared Types ───────────────────────────────────────────────────

type FieldType = 'STRING' | 'NUMBER' | 'UUID' | 'MESSAGE_LENGTH' | 'TRANSACTION_ID';

interface HeaderFieldForm {
  name: string;
  size: number;
  value: string;
  type: FieldType;
  paddingChar: string;
  uuidPrefix: string;
  uuidSeparator: string;
  correlationKey: boolean;
}

const DEFAULT_HEADER_FIELD: HeaderFieldForm = {
  name: '', size: 10, value: '', type: 'STRING', paddingChar: ' ', uuidPrefix: '', uuidSeparator: '-',
  correlationKey: false,
};

interface FieldValidation {
  severity: 'error' | 'warning';
  message: string;
}

function getFieldValidation(field: HeaderFieldForm): FieldValidation | null {
  if (field.type === 'UUID') {
    const total = field.uuidPrefix.length + field.uuidSeparator.length + 36;
    if (total > field.size) {
      return { severity: 'warning', message: `UUID is ${total} chars — will be truncated to ${field.size}.` };
    }
    if (total < field.size) {
      return { severity: 'warning', message: `UUID is ${total} chars, will be padded to ${field.size} with '${field.paddingChar || ' '}'.` };
    }
    return null;
  }
  if (field.value.length > field.size) {
    return { severity: 'error', message: `Value (${field.value.length} chars) exceeds size (${field.size}) and will be truncated.` };
  }
  if (field.value.length > 0 && field.value.length < field.size) {
    return {
      severity: 'warning',
      message: `Value is ${field.value.length} of ${field.size} chars. Will be padded with '${field.paddingChar || ' '}'. Enter ${field.size} chars or accept padding.`,
    };
  }
  return null;
}

interface EntryForm {
  id?: number;
  testCaseId: number | null;
  percentage: number;
}

type TestType = 'SMOKE' | 'LOAD' | 'STRESS' | 'SOAK' | 'SPIKE';

interface ScenarioForm {
  id?: number;
  name: string;
  count: number;
  entries: EntryForm[];
  scheduledEnabled: boolean;
  scheduledTime: string;
  warmupCount: number;
  testType: TestType | '';
  infraProfileId: number | null;
  thinkTimeEnabled: boolean;
  thinkTimeDistribution: 'CONSTANT' | 'UNIFORM' | 'GAUSSIAN';
  thinkTimeMinMs: number;
  thinkTimeMaxMs: number;
  thinkTimeMeanMs: number;
  thinkTimeStdDevMs: number;
  thresholds: ThresholdDef[];
}

interface TemplateForm {
  id?: number;
  name: string;
  fields: HeaderFieldForm[];
}

interface TestCaseFormState {
  mode: 'create' | 'edit';
  id?: number;
  name: string;
  message: string;
  headerTemplateId: number | null;
  responseTemplateId: number | null;
}

const EMPTY_SCENARIO: ScenarioForm = {
  name: '', count: 100, entries: [], scheduledEnabled: false, scheduledTime: '',
  warmupCount: 0, testType: '', infraProfileId: null, thinkTimeEnabled: false,
  thinkTimeDistribution: 'CONSTANT', thinkTimeMinMs: 0, thinkTimeMaxMs: 1000,
  thinkTimeMeanMs: 500, thinkTimeStdDevMs: 100, thresholds: [],
};
const EMPTY_TEMPLATE: TemplateForm = { name: '', fields: [] };
const EMPTY_TC: TestCaseFormState = { mode: 'create', name: '', message: '', headerTemplateId: null, responseTemplateId: null };

// ── Test Cases Tab ─────────────────────────────────────────────────

interface TestCasesTabProps {
  onChanged: () => void;
}

function TestCasesTab({ onChanged }: TestCasesTabProps) {
  const [testCases, setTestCases] = useState<TestCaseSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [form, setForm] = useState<TestCaseFormState>(EMPTY_TC);
  const [confirmDeleteId, setConfirmDeleteId] = useState<number | null>(null);
  const [saving, setSaving] = useState(false);
  const [templates, setTemplates] = useState<HeaderTemplateSummary[]>([]);
  const [expandedTemplateId, setExpandedTemplateId] = useState<number | null>(null);
  const [expandedFields, setExpandedFields] = useState<HeaderTemplateField[]>([]);
  const [responseTemplates, setResponseTemplates] = useState<ResponseTemplateSummary[]>([]);
  const [expandedResponseTemplateId, setExpandedResponseTemplateId] = useState<number | null>(null);
  const [expandedResponseFields, setExpandedResponseFields] = useState<ResponseTemplateField[]>([]);

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      setTestCases(await listTestCases());
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const loadTemplates = () => {
    listHeaderTemplates().then(setTemplates).catch(() => {});
    listResponseTemplates().then(setResponseTemplates).catch(() => {});
  };

  const toggleTemplate = async (id: number) => {
    if (expandedTemplateId === id) {
      setExpandedTemplateId(null);
      setExpandedFields([]);
    } else {
      const detail = await getHeaderTemplate(id);
      setExpandedTemplateId(id);
      setExpandedFields(detail.fields);
    }
  };

  const toggleResponseTemplate = async (id: number) => {
    if (expandedResponseTemplateId === id) {
      setExpandedResponseTemplateId(null);
      setExpandedResponseFields([]);
    } else {
      const detail = await getResponseTemplate(id);
      setExpandedResponseTemplateId(id);
      setExpandedResponseFields(detail.fields);
    }
  };

  const openCreate = () => {
    setForm({ ...EMPTY_TC, mode: 'create' });
    setExpandedTemplateId(null);
    setExpandedFields([]);
    setExpandedResponseTemplateId(null);
    setExpandedResponseFields([]);
    loadTemplates();
    setDialogOpen(true);
  };

  const openEdit = async (tc: TestCaseSummary) => {
    setExpandedTemplateId(null);
    setExpandedFields([]);
    setExpandedResponseTemplateId(null);
    setExpandedResponseFields([]);
    loadTemplates();
    const detail = await getTestCase(tc.id);
    setForm({
      mode: 'edit',
      id: detail.id,
      name: detail.name,
      message: detail.message,
      headerTemplateId: detail.headerTemplateId ?? null,
      responseTemplateId: detail.responseTemplateId ?? null,
    });
    setDialogOpen(true);
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      if (form.mode === 'edit' && form.id != null) {
        await updateTestCase(form.id, form.name, form.message, form.headerTemplateId, form.responseTemplateId);
      } else {
        await createTestCase(form.name, form.message, form.headerTemplateId, form.responseTemplateId);
      }
      setDialogOpen(false);
      await refresh();
      onChanged();
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (confirmDeleteId == null) return;
    await deleteTestCase(confirmDeleteId);
    setConfirmDeleteId(null);
    await refresh();
    onChanged();
  };

  const canSave = form.name.trim() !== '' && form.message.trim() !== '';

  const columns: DataTableColumn<TestCaseSummary>[] = [
    { id: 'name', label: 'Name', render: (row) => row.name },
    { id: 'headerTemplateName', label: 'Header Template', render: (row) => row.headerTemplateName ?? '—' },
    { id: 'responseTemplateName', label: 'Response Template', render: (row) => row.responseTemplateName ?? '—' },
    { id: 'updatedAt', label: 'Updated', render: (row) => new Date(row.updatedAt).toLocaleString() },
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
    <>
      <Stack direction="row" justifyContent="flex-end" sx={{ mb: 2 }}>
        <Button size="small" onClick={openCreate}>New</Button>
      </Stack>
      {loading ? (
        <Typography variant="body2" color="text.secondary">Loading test cases...</Typography>
      ) : testCases.length === 0 ? (
        <Typography variant="body2" color="text.secondary">No test cases yet.</Typography>
      ) : (
        <DataTable columns={columns} rows={testCases} keyExtractor={(row) => row.id} />
      )}

      <Dialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        title={form.mode === 'edit' ? 'Edit Test Case' : 'New Test Case'}
        maxWidth="md"
        actions={
          <>
            <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
            <Button onClick={handleSave} disabled={saving || !canSave}>
              {saving ? 'Saving...' : 'Save'}
            </Button>
          </>
        }
      >
        <Stack spacing={2} sx={{ mt: 1 }}>
          <TextField
            label="Name"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            fullWidth
            required
          />
          {templates.length > 0 && (
            <Box>
              <Typography variant="body2" fontWeight={500} color="text.secondary" sx={{ mb: 1 }}>
                Header Templates
              </Typography>
              <Stack spacing={0.5}>
                {templates.map((t) => (
                  <Box key={t.id}>
                    <Stack
                      direction="row"
                      alignItems="center"
                      justifyContent="space-between"
                      sx={{
                        px: 1.5,
                        py: 0.75,
                        border: '1px solid',
                        borderColor: expandedTemplateId === t.id ? 'primary.main' : 'divider',
                        borderRadius: 1,
                        cursor: 'pointer',
                        '&:hover': { borderColor: 'primary.main' },
                      }}
                      onClick={() => void toggleTemplate(t.id)}
                    >
                      <Typography variant="body2">{t.name}</Typography>
                      <Stack direction="row" spacing={1} alignItems="center">
                        <Typography variant="caption" color="text.secondary">
                          {t.fieldCount} {t.fieldCount === 1 ? 'field' : 'fields'}
                        </Typography>
                        <Typography variant="caption" color="primary">
                          {expandedTemplateId === t.id ? 'hide' : 'show'}
                        </Typography>
                      </Stack>
                    </Stack>
                    {expandedTemplateId === t.id && expandedFields.length > 0 && (
                      <Box
                        sx={{
                          mt: 0.5,
                          px: 1.5,
                          py: 1,
                          borderLeft: '2px solid',
                          borderColor: 'primary.main',
                          bgcolor: 'action.hover',
                          borderRadius: '0 4px 4px 0',
                        }}
                      >
                        <Stack spacing={0.25}>
                          {expandedFields.map((f, fi) => (
                            <Typography key={fi} variant="caption" color="text.secondary">
                              <strong>{f.name}</strong> — size: {f.size}
                              {f.value ? `, default: "${f.value}"` : ''}
                            </Typography>
                          ))}
                        </Stack>
                      </Box>
                    )}
                  </Box>
                ))}
              </Stack>
            </Box>
          )}
          {responseTemplates.length > 0 && (
            <Box>
              <Typography variant="body2" fontWeight={500} color="text.secondary" sx={{ mb: 1 }}>
                Response Templates
              </Typography>
              <Stack spacing={0.5}>
                {responseTemplates.map((t) => (
                  <Box key={t.id}>
                    <Stack
                      direction="row"
                      alignItems="center"
                      justifyContent="space-between"
                      sx={{
                        px: 1.5,
                        py: 0.75,
                        border: '1px solid',
                        borderColor: expandedResponseTemplateId === t.id ? 'secondary.main' : 'divider',
                        borderRadius: 1,
                        cursor: 'pointer',
                        '&:hover': { borderColor: 'secondary.main' },
                      }}
                      onClick={() => void toggleResponseTemplate(t.id)}
                    >
                      <Typography variant="body2">{t.name}</Typography>
                      <Stack direction="row" spacing={1} alignItems="center">
                        <Typography variant="caption" color="text.secondary">
                          {t.fieldCount} {t.fieldCount === 1 ? 'field' : 'fields'}
                        </Typography>
                        <Typography variant="caption" color="secondary">
                          {expandedResponseTemplateId === t.id ? 'hide' : 'show'}
                        </Typography>
                      </Stack>
                    </Stack>
                    {expandedResponseTemplateId === t.id && expandedResponseFields.length > 0 && (
                      <Box
                        sx={{
                          mt: 0.5,
                          px: 1.5,
                          py: 1,
                          borderLeft: '2px solid',
                          borderColor: 'secondary.main',
                          bgcolor: 'action.hover',
                          borderRadius: '0 4px 4px 0',
                        }}
                      >
                        <Stack spacing={0.25}>
                          {expandedResponseFields.map((f, fi) => (
                            <Typography key={fi} variant="caption" color="text.secondary">
                              <strong>{f.name}</strong> — size: {f.size}, type: {f.type ?? 'STATIC'}
                              {f.value ? `, expected: "${f.value}"` : ''}
                            </Typography>
                          ))}
                        </Stack>
                      </Box>
                    )}
                  </Box>
                ))}
              </Stack>
            </Box>
          )}
          <TextField
            label="Message"
            value={form.message}
            onChange={(e) => setForm({ ...form, message: e.target.value })}
            fullWidth
            required
            multiline
            rows={8}
          />
          <MuiTextField
            select
            label="Header Template"
            value={form.headerTemplateId ?? ''}
            onChange={e => setForm(f => ({ ...f, headerTemplateId: e.target.value ? Number(e.target.value) : null }))}
            fullWidth
            size="small"
          >
            <MenuItem value="">None</MenuItem>
            {templates.map(t => (
              <MenuItem key={t.id} value={t.id}>{t.name}</MenuItem>
            ))}
          </MuiTextField>
          <MuiTextField
            select
            label="Response Template"
            value={form.responseTemplateId ?? ''}
            onChange={e => setForm(f => ({ ...f, responseTemplateId: e.target.value ? Number(e.target.value) : null }))}
            fullWidth
            size="small"
          >
            <MenuItem value="">None</MenuItem>
            {responseTemplates.map(t => (
              <MenuItem key={t.id} value={t.id}>{t.name}</MenuItem>
            ))}
          </MuiTextField>
        </Stack>
      </Dialog>

      <Dialog
        open={confirmDeleteId != null}
        onClose={() => setConfirmDeleteId(null)}
        title="Delete Test Case"
        maxWidth="xs"
        actions={
          <>
            <Button onClick={() => setConfirmDeleteId(null)}>Cancel</Button>
            <Button onClick={handleDelete}>Delete</Button>
          </>
        }
      >
        <Typography>Are you sure you want to delete this test case?</Typography>
      </Dialog>
    </>
  );
}

// ── Header Templates Tab ───────────────────────────────────────────

interface HeadersTabProps {
  onChanged: () => void;
}

function HeadersTab({ onChanged }: HeadersTabProps) {
  const [templates, setTemplates] = useState<HeaderTemplateSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [form, setForm] = useState<TemplateForm>(EMPTY_TEMPLATE);
  const [confirmDeleteId, setConfirmDeleteId] = useState<number | null>(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      setTemplates(await listHeaderTemplates());
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load header templates');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const openCreate = () => {
    setForm(EMPTY_TEMPLATE);
    setDialogOpen(true);
  };

  const openEdit = async (t: HeaderTemplateSummary) => {
    const detail = await getHeaderTemplate(t.id);
    setForm({
      id: detail.id,
      name: detail.name,
      fields: detail.fields.map((f) => ({
        name: f.name,
        size: f.size,
        value: f.value,
        type: (f.type ?? 'STRING') as FieldType,
        paddingChar: f.paddingChar ?? ' ',
        uuidPrefix: f.uuidPrefix ?? '',
        uuidSeparator: f.uuidSeparator ?? '-',
        correlationKey: f.correlationKey ?? false,
      })),
    });
    setDialogOpen(true);
  };

  const addField = () => {
    setForm((f) => ({ ...f, fields: [...f.fields, { ...DEFAULT_HEADER_FIELD }] }));
  };

  const removeField = (i: number) => {
    setForm((f) => ({ ...f, fields: f.fields.filter((_, idx) => idx !== i) }));
  };

  const updateField = (i: number, field: keyof HeaderFieldForm, value: string | number | boolean) => {
    setForm((f) => ({
      ...f,
      fields: f.fields.map((fld, idx) => (idx === i ? { ...fld, [field]: value } as HeaderFieldForm : fld)),
    }));
  };

  const updateFieldType = (i: number, newType: FieldType) => {
    setForm((f) => ({
      ...f,
      fields: f.fields.map((fld, idx) => {
        if (idx !== i) return fld;
        const updated = { ...fld, type: newType };
        if (newType === 'TRANSACTION_ID') {
          updated.correlationKey = true;
        }
        return updated;
      }),
    }));
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      const request = {
        name: form.name,
        fields: form.fields.map((f) => ({
          name: f.name,
          size: f.size,
          value: f.value,
          type: f.type,
          paddingChar: f.paddingChar,
          uuidPrefix: f.uuidPrefix,
          uuidSeparator: f.uuidSeparator,
          correlationKey: f.correlationKey,
        })),
      };
      if (form.id != null) {
        await updateHeaderTemplate(form.id, request);
      } else {
        await createHeaderTemplate(request);
      }
      setDialogOpen(false);
      await refresh();
      onChanged();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to save header template');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (confirmDeleteId == null) return;
    await deleteHeaderTemplate(confirmDeleteId);
    setConfirmDeleteId(null);
    await refresh();
    onChanged();
  };

  const columns: DataTableColumn<HeaderTemplateSummary>[] = [
    { id: 'name', label: 'Name', minWidth: 160, render: (row) => row.name },
    { id: 'fieldCount', label: 'Fields', render: (row) => String(row.fieldCount) },
    { id: 'updated', label: 'Updated', render: (row) => new Date(row.updatedAt).toLocaleString() },
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
    <>
      <Stack direction="row" justifyContent="flex-end" sx={{ mb: 2 }}>
        <Button size="small" onClick={openCreate}>New</Button>
      </Stack>

      {error && (
        <Alert severity="error" onClose={() => setError(null)} sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {loading ? (
        <Typography variant="body2" color="text.secondary">Loading header templates...</Typography>
      ) : templates.length === 0 ? (
        <Typography variant="body2" color="text.secondary">
          No header templates yet. Create one to reuse fixed-width header columns across entries.
        </Typography>
      ) : (
        <DataTable columns={columns} rows={templates} keyExtractor={(row) => row.id} />
      )}

      <Dialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        title={form.id != null ? 'Edit Header Template' : 'New Header Template'}
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
            label="Template Name"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            fullWidth
            required
            size="small"
          />
          <Divider />
          <Stack spacing={1}>
            <Stack direction="row" alignItems="center" justifyContent="space-between">
              <Typography variant="body2" color="text.secondary" fontWeight={500}>
                Fields
              </Typography>
              <Button size="small" onClick={addField}>
                <AddIcon fontSize="small" sx={{ mr: 0.5 }} />
                Add Field
              </Button>
            </Stack>
            {form.fields.length === 0 ? (
              <Typography variant="caption" color="text.disabled">No fields yet</Typography>
            ) : (
              <Stack spacing={1.5}>
                {form.fields.map((field, i) => {
                  const validation = getFieldValidation(field);
                  const isUuid = field.type === 'UUID';
                  const isMessageLength = field.type === 'MESSAGE_LENGTH';
                  const isTransactionId = field.type === 'TRANSACTION_ID';
                  const uuidTotal = field.uuidPrefix.length + field.uuidSeparator.length + 36;
                  return (
                    <Box key={i} sx={{ border: '1px solid', borderColor: field.correlationKey ? 'primary.main' : 'divider', borderRadius: 1, p: 1.5 }}>
                      <Stack spacing={1}>
                        <Stack direction="row" spacing={1} alignItems="center">
                          <MuiTextField
                            size="small"
                            label="Name"
                            value={field.name}
                            onChange={(e) => updateField(i, 'name', e.target.value)}
                            sx={{ flex: 2 }}
                          />
                          <MuiTextField
                            select
                            size="small"
                            label="Type"
                            value={field.type}
                            onChange={(e) => updateFieldType(i, e.target.value as FieldType)}
                            sx={{ width: 148 }}
                          >
                            <MenuItem value="STRING">String</MenuItem>
                            <MenuItem value="NUMBER">Number</MenuItem>
                            <MenuItem value="UUID">UUID</MenuItem>
                            <MenuItem value="MESSAGE_LENGTH">Msg Length</MenuItem>
                            <MenuItem value="TRANSACTION_ID">Transaction ID</MenuItem>
                          </MuiTextField>
                          <MuiTextField
                            size="small"
                            label="Size"
                            type="number"
                            value={field.size}
                            onChange={(e) => updateField(i, 'size', Number(e.target.value))}
                            sx={{ width: 75 }}
                            slotProps={{ htmlInput: { min: 1 } }}
                          />
                          <Tooltip title="Remove field">
                            <IconButton size="small" onClick={() => removeField(i)}>
                              <DeleteIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        </Stack>
                        {isUuid ? (
                          <Stack direction="row" spacing={1} alignItems="center">
                            <MuiTextField
                              size="small"
                              label="Prefix"
                              value={field.uuidPrefix}
                              onChange={(e) => updateField(i, 'uuidPrefix', e.target.value)}
                              sx={{ flex: 1 }}
                            />
                            <MuiTextField
                              size="small"
                              label="Separator"
                              value={field.uuidSeparator}
                              onChange={(e) => updateField(i, 'uuidSeparator', e.target.value)}
                              sx={{ width: 90 }}
                            />
                            <Typography
                              variant="caption"
                              color={validation?.severity === 'error' ? 'error.main' : validation?.severity === 'warning' ? 'warning.main' : 'success.main'}
                              sx={{ whiteSpace: 'nowrap' }}
                            >
                              {`${field.uuidPrefix.length}+${field.uuidSeparator.length}+36=${uuidTotal}/${field.size}`}
                              {!validation ? ' ✓' : ''}
                            </Typography>
                            {validation && (
                              <Typography variant="caption" color={validation.severity === 'error' ? 'error.main' : 'warning.main'}>
                                {validation.message}
                              </Typography>
                            )}
                          </Stack>
                        ) : isTransactionId ? (
                          <Typography variant="caption" color="text.secondary">
                            Auto-generates a unique UUID per message. Forwarded as a JMS property for downstream correlation.
                          </Typography>
                        ) : isMessageLength ? (
                          <Typography variant="caption" color="text.secondary">
                            Auto-populated with the byte length of the message body, padded to {field.size} chars.
                          </Typography>
                        ) : (
                          <Stack direction="row" spacing={1} alignItems="flex-start">
                            <MuiTextField
                              size="small"
                              label="Default Value"
                              value={field.value}
                              onChange={(e) => updateField(i, 'value', e.target.value)}
                              error={validation?.severity === 'error'}
                              sx={{ flex: 1 }}
                            />
                            <MuiTextField
                              size="small"
                              label="Pad char"
                              value={field.paddingChar}
                              onChange={(e) => {
                                const v = e.target.value;
                                updateField(i, 'paddingChar', v.length > 0 ? v[v.length - 1] : ' ');
                              }}
                              sx={{ width: 80 }}
                              slotProps={{ htmlInput: { maxLength: 1 } }}
                            />
                          </Stack>
                        )}
                        {!isUuid && !isMessageLength && !isTransactionId && validation && (
                          <Typography
                            variant="caption"
                            color={validation.severity === 'error' ? 'error.main' : 'warning.main'}
                          >
                            {validation.message}
                          </Typography>
                        )}
                        <FormControlLabel
                          control={
                            <Checkbox
                              size="small"
                              checked={field.correlationKey || isTransactionId}
                              disabled={isTransactionId}
                              onChange={(e) => updateField(i, 'correlationKey', e.target.checked)}
                            />
                          }
                          label={
                            <Typography variant="caption">
                              Correlation key — echo expected in matching response field
                            </Typography>
                          }
                        />
                      </Stack>
                    </Box>
                  );
                })}
              </Stack>
            )}
          </Stack>
        </Stack>
      </Dialog>

      <Dialog
        open={confirmDeleteId != null}
        onClose={() => setConfirmDeleteId(null)}
        title="Delete Header Template"
        maxWidth="xs"
        actions={
          <>
            <Button onClick={() => setConfirmDeleteId(null)}>Cancel</Button>
            <Button onClick={handleDelete}>Delete</Button>
          </>
        }
      >
        <Typography>Are you sure you want to delete this header template?</Typography>
      </Dialog>
    </>
  );
}

// ── Response Templates Tab ─────────────────────────────────────────

type ResponseFieldType = 'STATIC' | 'IGNORE' | 'REGEX' | 'ECHO';

interface ResponseFieldForm {
  name: string;
  size: number;
  value: string;
  type: ResponseFieldType;
  paddingChar: string;
}

const DEFAULT_RESPONSE_FIELD: ResponseFieldForm = {
  name: '', size: 10, value: '', type: 'STATIC', paddingChar: ' ',
};

interface ResponseTemplateForm {
  id?: number;
  name: string;
  fields: ResponseFieldForm[];
}

const EMPTY_RESPONSE_TEMPLATE: ResponseTemplateForm = { name: '', fields: [] };

interface ResponseTemplatesTabProps {
  onChanged: () => void;
}

function ResponseTemplatesTab({ onChanged }: ResponseTemplatesTabProps) {
  const [templates, setTemplates] = useState<ResponseTemplateSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [form, setForm] = useState<ResponseTemplateForm>(EMPTY_RESPONSE_TEMPLATE);
  const [confirmDeleteId, setConfirmDeleteId] = useState<number | null>(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      setTemplates(await listResponseTemplates());
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load response templates');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const openCreate = () => {
    setForm(EMPTY_RESPONSE_TEMPLATE);
    setDialogOpen(true);
  };

  const openEdit = async (t: ResponseTemplateSummary) => {
    const detail = await getResponseTemplate(t.id);
    setForm({
      id: detail.id,
      name: detail.name,
      fields: detail.fields.map((f) => ({
        name: f.name,
        size: f.size,
        value: f.value ?? '',
        type: (f.type ?? 'STATIC') as ResponseFieldType,
        paddingChar: f.paddingChar ?? ' ',
      })),
    });
    setDialogOpen(true);
  };

  const addField = () => {
    setForm((f) => ({ ...f, fields: [...f.fields, { ...DEFAULT_RESPONSE_FIELD }] }));
  };

  const removeField = (i: number) => {
    setForm((f) => ({ ...f, fields: f.fields.filter((_, idx) => idx !== i) }));
  };

  const updateField = (i: number, field: keyof ResponseFieldForm, value: string | number) => {
    setForm((f) => ({
      ...f,
      fields: f.fields.map((fld, idx) => (idx === i ? { ...fld, [field]: value } as ResponseFieldForm : fld)),
    }));
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      const request = {
        name: form.name,
        fields: form.fields.map((f) => ({
          name: f.name,
          size: f.size,
          value: f.value,
          type: f.type,
          paddingChar: f.paddingChar,
        })),
      };
      if (form.id != null) {
        await updateResponseTemplate(form.id, request);
      } else {
        await createResponseTemplate(request);
      }
      setDialogOpen(false);
      await refresh();
      onChanged();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to save response template');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (confirmDeleteId == null) return;
    await deleteResponseTemplate(confirmDeleteId);
    setConfirmDeleteId(null);
    await refresh();
    onChanged();
  };

  const columns: DataTableColumn<ResponseTemplateSummary>[] = [
    { id: 'name', label: 'Name', minWidth: 160, render: (row) => row.name },
    { id: 'fieldCount', label: 'Fields', render: (row) => String(row.fieldCount) },
    { id: 'updated', label: 'Updated', render: (row) => new Date(row.updatedAt).toLocaleString() },
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
    <>
      <Stack direction="row" justifyContent="flex-end" sx={{ mb: 2 }}>
        <Button size="small" onClick={openCreate}>New</Button>
      </Stack>

      {error && (
        <Alert severity="error" onClose={() => setError(null)} sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {loading ? (
        <Typography variant="body2" color="text.secondary">Loading response templates...</Typography>
      ) : templates.length === 0 ? (
        <Typography variant="body2" color="text.secondary">
          No response templates yet. Create one to define expected response field structure.
        </Typography>
      ) : (
        <DataTable columns={columns} rows={templates} keyExtractor={(row) => row.id} />
      )}

      <Dialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        title={form.id != null ? 'Edit Response Template' : 'New Response Template'}
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
            label="Template Name"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            fullWidth
            required
            size="small"
          />
          <Divider />
          <Stack spacing={1}>
            <Stack direction="row" alignItems="center" justifyContent="space-between">
              <Typography variant="body2" color="text.secondary" fontWeight={500}>
                Fields
              </Typography>
              <Button size="small" onClick={addField}>
                <AddIcon fontSize="small" sx={{ mr: 0.5 }} />
                Add Field
              </Button>
            </Stack>
            {form.fields.length === 0 ? (
              <Typography variant="caption" color="text.disabled">No fields yet</Typography>
            ) : (
              <Stack spacing={1.5}>
                {form.fields.map((field, i) => (
                  <Box key={i} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 1, p: 1.5 }}>
                    <Stack spacing={1}>
                      <Stack direction="row" spacing={1} alignItems="center">
                        <MuiTextField
                          size="small"
                          label="Name"
                          value={field.name}
                          onChange={(e) => updateField(i, 'name', e.target.value)}
                          sx={{ flex: 2 }}
                        />
                        <MuiTextField
                          select
                          size="small"
                          label="Type"
                          value={field.type}
                          onChange={(e) => updateField(i, 'type', e.target.value)}
                          sx={{ width: 110 }}
                        >
                          <MenuItem value="STATIC">Static</MenuItem>
                          <MenuItem value="IGNORE">Ignore</MenuItem>
                          <MenuItem value="REGEX">Regex</MenuItem>
                          <MenuItem value="ECHO">Echo</MenuItem>
                        </MuiTextField>
                        <MuiTextField
                          size="small"
                          label="Size"
                          type="number"
                          value={field.size}
                          onChange={(e) => updateField(i, 'size', Number(e.target.value))}
                          sx={{ width: 75 }}
                          slotProps={{ htmlInput: { min: 1 } }}
                        />
                        <Tooltip title="Remove field">
                          <IconButton size="small" onClick={() => removeField(i)}>
                            <DeleteIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      </Stack>
                      {field.type === 'ECHO' ? (
                        <Stack direction="row" spacing={1} alignItems="center">
                          <MuiTextField
                            size="small"
                            label="Request field name"
                            value={field.value}
                            onChange={(e) => updateField(i, 'value', e.target.value)}
                            sx={{ flex: 1 }}
                            helperText="Name of the request header field marked as correlation key"
                          />
                        </Stack>
                      ) : field.type !== 'IGNORE' && (
                        <Stack direction="row" spacing={1}>
                          <MuiTextField
                            size="small"
                            label={field.type === 'REGEX' ? 'Pattern' : 'Expected Value'}
                            value={field.value}
                            onChange={(e) => updateField(i, 'value', e.target.value)}
                            sx={{ flex: 1 }}
                          />
                          {field.type === 'STATIC' && (
                            <MuiTextField
                              size="small"
                              label="Pad char"
                              value={field.paddingChar}
                              onChange={(e) => {
                                const v = e.target.value;
                                updateField(i, 'paddingChar', v.length > 0 ? v[v.length - 1] : ' ');
                              }}
                              sx={{ width: 80 }}
                              slotProps={{ htmlInput: { maxLength: 1 } }}
                            />
                          )}
                        </Stack>
                      )}
                    </Stack>
                  </Box>
                ))}
              </Stack>
            )}
          </Stack>
        </Stack>
      </Dialog>

      <Dialog
        open={confirmDeleteId != null}
        onClose={() => setConfirmDeleteId(null)}
        title="Delete Response Template"
        maxWidth="xs"
        actions={
          <>
            <Button onClick={() => setConfirmDeleteId(null)}>Cancel</Button>
            <Button onClick={handleDelete}>Delete</Button>
          </>
        }
      >
        <Typography>Are you sure you want to delete this response template?</Typography>
      </Dialog>
    </>
  );
}

// ── Entry Row ──────────────────────────────────────────────────────

function EntryRow({ entry, testCases, onChange, onRemove }: {
  entry: EntryForm;
  testCases: TestCaseSummary[];
  onChange: (updated: EntryForm) => void;
  onRemove: () => void;
}) {
  return (
    <Stack direction="row" spacing={1} alignItems="center">
      <MuiTextField
        select
        label="Test Case"
        value={entry.testCaseId ?? ''}
        onChange={e => onChange({ ...entry, testCaseId: e.target.value ? Number(e.target.value) : null })}
        fullWidth
        size="small"
      >
        {testCases.map(tc => (
          <MenuItem key={tc.id} value={tc.id}>{tc.name}</MenuItem>
        ))}
      </MuiTextField>
      <MuiTextField
        label="% of messages"
        type="number"
        value={entry.percentage}
        onChange={e => onChange({ ...entry, percentage: Number(e.target.value) })}
        size="small"
        sx={{ width: 140 }}
        inputProps={{ min: 0, max: 100 }}
      />
      <IconButton onClick={onRemove} size="small" color="error">
        <DeleteIcon />
      </IconButton>
    </Stack>
  );
}

// ── Scenarios Tab ──────────────────────────────────────────────────

interface ScenariosTabProps {
  onChanged: () => void;
}

function ScenariosTab({ onChanged }: ScenariosTabProps) {
  const [scenarios, setScenarios] = useState<TestScenarioSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [testCases, setTestCases] = useState<TestCaseSummary[]>([]);
  const [infraProfiles, setInfraProfiles] = useState<InfraProfileSummary[]>([]);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [form, setForm] = useState<ScenarioForm>(EMPTY_SCENARIO);
  const [saving, setSaving] = useState(false);
  const [confirmDeleteId, setConfirmDeleteId] = useState<number | null>(null);

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      setScenarios(await listTestScenarios());
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load scenarios');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const refreshSupportingData = () => {
    listTestCases().then(setTestCases).catch(() => {});
    listInfraProfiles().then(setInfraProfiles).catch(() => {});
  };

  const openCreate = () => {
    setForm(EMPTY_SCENARIO);
    refreshSupportingData();
    setDialogOpen(true);
  };

  const openEdit = async (scenario: TestScenarioSummary) => {
    setDialogOpen(true);
    const [detail, tcs, profiles] = await Promise.all([
      getTestScenario(scenario.id),
      listTestCases().catch(() => [] as TestCaseSummary[]),
      listInfraProfiles().catch(() => [] as InfraProfileSummary[]),
    ]);
    setTestCases(tcs);
    setInfraProfiles(profiles);
    setForm({
      id: detail.id,
      name: detail.name,
      count: detail.count,
      scheduledEnabled: detail.scheduledEnabled ?? false,
      scheduledTime: detail.scheduledTime ?? '',
      warmupCount: detail.warmupCount ?? 0,
      testType: (detail.testType as TestType | '') ?? '',
      infraProfileId: detail.infraProfileId ?? null,
      thinkTimeEnabled: detail.thinkTime != null,
      thinkTimeDistribution: detail.thinkTime?.distribution ?? 'CONSTANT',
      thinkTimeMinMs: detail.thinkTime?.minMs ?? 0,
      thinkTimeMaxMs: detail.thinkTime?.maxMs ?? 1000,
      thinkTimeMeanMs: detail.thinkTime?.meanMs ?? 500,
      thinkTimeStdDevMs: detail.thinkTime?.stdDevMs ?? 100,
      thresholds: detail.thresholds ?? [],
      entries: detail.entries.map((e) => ({
        id: e.id,
        testCaseId: e.testCaseId ?? null,
        percentage: e.percentage,
      })),
    });
  };

  const addEntry = () => {
    setForm((f) => ({
      ...f,
      entries: [
        ...f.entries,
        { testCaseId: null, percentage: 0 },
      ],
    }));
  };

  const updateEntry = (i: number, updated: EntryForm) => {
    setForm((f) => ({ ...f, entries: f.entries.map((e, idx) => (idx === i ? updated : e)) }));
  };

  const removeEntry = (i: number) => {
    setForm((f) => ({ ...f, entries: f.entries.filter((_, idx) => idx !== i) }));
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      const thinkTime: ThinkTimeConfig | null = form.thinkTimeEnabled
        ? {
            distribution: form.thinkTimeDistribution,
            minMs: form.thinkTimeMinMs,
            maxMs: form.thinkTimeMaxMs,
            meanMs: form.thinkTimeMeanMs,
            stdDevMs: form.thinkTimeStdDevMs,
          }
        : null;
      const request = {
        name: form.name,
        count: form.count,
        scheduledEnabled: form.scheduledEnabled,
        scheduledTime: form.scheduledEnabled && form.scheduledTime ? form.scheduledTime : null,
        warmupCount: form.warmupCount,
        testType: form.testType || null,
        infraProfileId: form.infraProfileId,
        thinkTime,
        thresholds: form.thresholds,
        entries: form.entries
          .filter((e) => e.testCaseId != null)
          .map((e, index) => ({
            testCaseId: e.testCaseId as number,
            percentage: e.percentage,
            displayOrder: index,
          })),
      };
      if (form.id != null) {
        await updateTestScenario(form.id, request);
      } else {
        await createTestScenario(request);
      }
      setDialogOpen(false);
      await refresh();
      onChanged();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to save scenario');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (confirmDeleteId == null) return;
    await deleteTestScenario(confirmDeleteId);
    setConfirmDeleteId(null);
    await refresh();
    onChanged();
  };

  const columns: DataTableColumn<TestScenarioSummary>[] = [
    { id: 'name', label: 'Name', minWidth: 140, render: (row) => row.name },
    { id: 'count', label: 'Messages', render: (row) => row.count.toLocaleString() },
    { id: 'updated', label: 'Updated', render: (row) => new Date(row.updatedAt).toLocaleString() },
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
    <>
      <Stack direction="row" justifyContent="flex-end" sx={{ mb: 2 }}>
        <Button size="small" onClick={openCreate}>New</Button>
      </Stack>

      {error && (
        <Alert severity="error" onClose={() => setError(null)} sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {loading ? (
        <Typography variant="body2" color="text.secondary">Loading scenarios...</Typography>
      ) : scenarios.length === 0 ? (
        <Typography variant="body2" color="text.secondary">
          No scenarios yet. Create one to define message content, percentages and headers.
        </Typography>
      ) : (
        <DataTable columns={columns} rows={scenarios} keyExtractor={(row) => row.id} />
      )}

      <Dialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        title={form.id != null ? 'Edit Test Scenario' : 'New Test Scenario'}
        maxWidth="lg"
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
          <Stack direction="row" spacing={2}>
            <MuiTextField
              label="Scenario Name"
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              fullWidth
              required
              size="small"
            />
            <MuiTextField
              label="Total Messages"
              type="number"
              value={form.count}
              onChange={(e) => setForm({ ...form, count: Number(e.target.value) })}
              required
              size="small"
              sx={{ width: 160 }}
              slotProps={{ htmlInput: { min: 1 } }}
            />
          </Stack>
          <Stack direction="row" spacing={2} alignItems="center">
            <FormControlLabel
              control={
                <Checkbox
                  size="small"
                  checked={form.scheduledEnabled}
                  onChange={(e) => setForm({ ...form, scheduledEnabled: e.target.checked })}
                />
              }
              label={<Typography variant="body2">Run at specific time daily</Typography>}
            />
            {form.scheduledEnabled && (
              <MuiTextField
                size="small"
                label="Time (HH:mm)"
                type="time"
                value={form.scheduledTime}
                onChange={(e) => setForm({ ...form, scheduledTime: e.target.value })}
                sx={{ width: 140 }}
                slotProps={{ htmlInput: { step: 60 } }}
              />
            )}
          </Stack>
          <Stack direction="row" spacing={2} alignItems="center">
            <MuiTextField
              select
              size="small"
              label="Test Type"
              value={form.testType}
              onChange={(e) => setForm({ ...form, testType: e.target.value as TestType | '' })}
              sx={{ width: 160 }}
            >
              <MenuItem value=""><em>None</em></MenuItem>
              <MenuItem value="SMOKE">Smoke</MenuItem>
              <MenuItem value="LOAD">Load</MenuItem>
              <MenuItem value="STRESS">Stress</MenuItem>
              <MenuItem value="SOAK">Soak</MenuItem>
              <MenuItem value="SPIKE">Spike</MenuItem>
            </MuiTextField>
            <MuiTextField
              size="small"
              label="Warmup Messages"
              type="number"
              value={form.warmupCount}
              onChange={(e) => setForm({ ...form, warmupCount: Number(e.target.value) })}
              sx={{ width: 160 }}
              slotProps={{ htmlInput: { min: 0 } }}
            />
            <MuiTextField
              select
              size="small"
              label="Infra Profile"
              value={form.infraProfileId ?? ''}
              onChange={(e) => setForm({ ...form, infraProfileId: e.target.value ? Number(e.target.value) : null })}
              sx={{ width: 200 }}
            >
              <MenuItem value=""><em>None</em></MenuItem>
              {infraProfiles.map((p) => (
                <MenuItem key={p.id} value={p.id}>{p.name}</MenuItem>
              ))}
            </MuiTextField>
          </Stack>
          <Box>
            <Stack direction="row" alignItems="center" spacing={2} sx={{ mb: 1 }}>
              <FormControlLabel
                control={
                  <Checkbox
                    size="small"
                    checked={form.thinkTimeEnabled}
                    onChange={(e) => setForm({ ...form, thinkTimeEnabled: e.target.checked })}
                  />
                }
                label={<Typography variant="body2">Think time between messages</Typography>}
              />
            </Stack>
            {form.thinkTimeEnabled && (
              <Stack direction="row" spacing={1} flexWrap="wrap">
                <MuiTextField
                  select
                  size="small"
                  label="Distribution"
                  value={form.thinkTimeDistribution}
                  onChange={(e) => setForm({ ...form, thinkTimeDistribution: e.target.value as 'CONSTANT' | 'UNIFORM' | 'GAUSSIAN' })}
                  sx={{ width: 140 }}
                >
                  <MenuItem value="CONSTANT">Constant</MenuItem>
                  <MenuItem value="UNIFORM">Uniform</MenuItem>
                  <MenuItem value="GAUSSIAN">Gaussian</MenuItem>
                </MuiTextField>
                <MuiTextField
                  size="small"
                  label="Min ms"
                  type="number"
                  value={form.thinkTimeMinMs}
                  onChange={(e) => setForm({ ...form, thinkTimeMinMs: Number(e.target.value) })}
                  sx={{ width: 90 }}
                  slotProps={{ htmlInput: { min: 0 } }}
                />
                <MuiTextField
                  size="small"
                  label="Max ms"
                  type="number"
                  value={form.thinkTimeMaxMs}
                  onChange={(e) => setForm({ ...form, thinkTimeMaxMs: Number(e.target.value) })}
                  sx={{ width: 90 }}
                  slotProps={{ htmlInput: { min: 0 } }}
                />
                {form.thinkTimeDistribution === 'GAUSSIAN' && (
                  <>
                    <MuiTextField
                      size="small"
                      label="Mean ms"
                      type="number"
                      value={form.thinkTimeMeanMs}
                      onChange={(e) => setForm({ ...form, thinkTimeMeanMs: Number(e.target.value) })}
                      sx={{ width: 90 }}
                    />
                    <MuiTextField
                      size="small"
                      label="StdDev ms"
                      type="number"
                      value={form.thinkTimeStdDevMs}
                      onChange={(e) => setForm({ ...form, thinkTimeStdDevMs: Number(e.target.value) })}
                      sx={{ width: 100 }}
                    />
                  </>
                )}
              </Stack>
            )}
          </Box>
          <Box>
            <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 1 }}>
              <Typography variant="body2" color="text.secondary" fontWeight={500}>
                SLA Thresholds
              </Typography>
              <Button
                size="small"
                onClick={() => setForm((f) => ({
                  ...f,
                  thresholds: [...f.thresholds, { metric: 'P95', operator: 'LT', value: 1000 }],
                }))}
              >
                <AddIcon fontSize="small" sx={{ mr: 0.5 }} />
                Add Threshold
              </Button>
            </Stack>
            {form.thresholds.length === 0 ? (
              <Typography variant="caption" color="text.disabled">No thresholds defined</Typography>
            ) : (
              <Stack spacing={1}>
                {form.thresholds.map((t, i) => (
                  <Stack key={i} direction="row" spacing={1} alignItems="center">
                    <MuiTextField
                      select
                      size="small"
                      label="Metric"
                      value={t.metric}
                      onChange={(e) => {
                        const updated = [...form.thresholds];
                        updated[i] = { ...t, metric: e.target.value };
                        setForm({ ...form, thresholds: updated });
                      }}
                      sx={{ width: 130 }}
                    >
                      {['TPS', 'AVG_LATENCY', 'P50', 'P90', 'P95', 'P99'].map((m) => (
                        <MenuItem key={m} value={m}>{m}</MenuItem>
                      ))}
                    </MuiTextField>
                    <MuiTextField
                      select
                      size="small"
                      label="Op"
                      value={t.operator}
                      onChange={(e) => {
                        const updated = [...form.thresholds];
                        updated[i] = { ...t, operator: e.target.value };
                        setForm({ ...form, thresholds: updated });
                      }}
                      sx={{ width: 80 }}
                    >
                      {['LT', 'LTE', 'GT', 'GTE'].map((op) => (
                        <MenuItem key={op} value={op}>{op}</MenuItem>
                      ))}
                    </MuiTextField>
                    <MuiTextField
                      size="small"
                      label="Value"
                      type="number"
                      value={t.value}
                      onChange={(e) => {
                        const updated = [...form.thresholds];
                        updated[i] = { ...t, value: Number(e.target.value) };
                        setForm({ ...form, thresholds: updated });
                      }}
                      sx={{ width: 100 }}
                    />
                    <Tooltip title="Remove">
                      <IconButton
                        size="small"
                        onClick={() => setForm((f) => ({
                          ...f,
                          thresholds: f.thresholds.filter((_, idx) => idx !== i),
                        }))}
                      >
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </Stack>
                ))}
              </Stack>
            )}
          </Box>
          <Divider />
          <Stack spacing={1}>
            <Stack direction="row" alignItems="center" justifyContent="space-between">
              <Typography variant="body2" color="text.secondary" fontWeight={500}>
                Test Cases
              </Typography>
              <Button size="small" onClick={addEntry} disabled={testCases.length === 0}>
                <AddIcon fontSize="small" sx={{ mr: 0.5 }} />
                Add Test Case
              </Button>
            </Stack>
            {testCases.length === 0 && (
              <Typography variant="caption" color="text.secondary">
                No test cases available — create some in the Test Cases tab first.
              </Typography>
            )}
            {form.entries.length === 0 && testCases.length > 0 ? (
              <Typography variant="caption" color="text.disabled">No test cases added yet</Typography>
            ) : (
              <Stack spacing={1}>
                {form.entries.map((entry, i) => (
                  <EntryRow
                    key={i}
                    entry={entry}
                    testCases={testCases}
                    onChange={(updated) => updateEntry(i, updated)}
                    onRemove={() => removeEntry(i)}
                  />
                ))}
              </Stack>
            )}
            {form.entries.length > 0 && (() => {
              const total = form.entries.reduce((sum, e) => sum + e.percentage, 0);
              return (
                <Stack direction="row" justifyContent="flex-end">
                  <Typography
                    variant="caption"
                    color={total === 100 ? 'success.main' : 'warning.main'}
                    fontWeight={500}
                  >
                    Total: {total}%{total !== 100 ? ' (should be 100%)' : ''}
                  </Typography>
                </Stack>
              );
            })()}
          </Stack>
        </Stack>
      </Dialog>

      <Dialog
        open={confirmDeleteId != null}
        onClose={() => setConfirmDeleteId(null)}
        title="Delete Test Scenario"
        maxWidth="xs"
        actions={
          <>
            <Button onClick={() => setConfirmDeleteId(null)}>Cancel</Button>
            <Button onClick={handleDelete}>Delete</Button>
          </>
        }
      >
        <Typography>Are you sure you want to delete this scenario?</Typography>
      </Dialog>
    </>
  );
}

// ── Props ──────────────────────────────────────────────────────────

interface Props {
  open: boolean;
  onClose: () => void;
  onChanged?: () => void;
}

// ── Main Component ─────────────────────────────────────────────────

export default function TestScenarioManager({ open, onClose, onChanged }: Props) {
  const [activeTab, setActiveTab] = useState(0);

  const handleChanged = () => {
    onChanged?.();
  };

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title="Manage Scenarios"
      maxWidth="xl"
      actions={<Button onClick={onClose}>Close</Button>}
    >
      <MuiTabs
        value={activeTab}
        onChange={(_, v) => setActiveTab(v as number)}
        sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }}
      >
        <MuiTab label="Scenarios" />
        <MuiTab label="Test Cases" />
        <MuiTab label="Header Templates" />
        <MuiTab label="Response Templates" />
        <MuiTab label="Infra Profiles" />
      </MuiTabs>

      {activeTab === 0 && <ScenariosTab onChanged={handleChanged} />}
      {activeTab === 1 && <TestCasesTab onChanged={handleChanged} />}
      {activeTab === 2 && <HeadersTab onChanged={handleChanged} />}
      {activeTab === 3 && <ResponseTemplatesTab onChanged={handleChanged} />}
      {activeTab === 4 && <InfraProfileManager />}
    </Dialog>
  );
}
