import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import Box from '@mui/material/Box';
import Divider from '@mui/material/Divider';
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
  createTestCase,
  createTestScenario,
  deleteHeaderTemplate,
  deleteTestCase,
  deleteTestScenario,
  getHeaderTemplate,
  getTestCase,
  getTestScenario,
  listHeaderTemplates,
  listTestCases,
  listTestScenarios,
  updateHeaderTemplate,
  updateTestCase,
  updateTestScenario,
} from '@/api';
import type {
  HeaderTemplateSummary,
  TestCaseSummary,
  TestScenarioSummary,
} from '@/types/api';
import InfraProfileManager from './InfraProfileManager';

// ── Shared Types ───────────────────────────────────────────────────

interface HeaderFieldForm {
  name: string;
  size: number;
  value: string;
}

interface EntryForm {
  content: string;
  percentage: number;
  headerFields: HeaderFieldForm[];
}

interface ScenarioForm {
  id?: number;
  name: string;
  count: number;
  entries: EntryForm[];
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
}

const EMPTY_SCENARIO: ScenarioForm = { name: '', count: 100, entries: [] };
const EMPTY_TEMPLATE: TemplateForm = { name: '', fields: [] };
const EMPTY_TC: TestCaseFormState = { mode: 'create', name: '', message: '' };

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

  const openCreate = () => {
    setForm({ ...EMPTY_TC, mode: 'create' });
    setDialogOpen(true);
  };

  const openEdit = async (tc: TestCaseSummary) => {
    const detail = await getTestCase(tc.id);
    setForm({ mode: 'edit', id: detail.id, name: detail.name, message: detail.message });
    setDialogOpen(true);
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      if (form.mode === 'edit' && form.id != null) {
        await updateTestCase(form.id, form.name, form.message);
      } else {
        await createTestCase(form.name, form.message);
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
          <TextField
            label="Message"
            value={form.message}
            onChange={(e) => setForm({ ...form, message: e.target.value })}
            fullWidth
            required
            multiline
            rows={8}
          />
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
      fields: detail.fields.map((f) => ({ name: f.name, size: f.size, value: f.value })),
    });
    setDialogOpen(true);
  };

  const addField = () => {
    setForm((f) => ({ ...f, fields: [...f.fields, { name: '', size: 10, value: '' }] }));
  };

  const removeField = (i: number) => {
    setForm((f) => ({ ...f, fields: f.fields.filter((_, idx) => idx !== i) }));
  };

  const updateField = (i: number, field: keyof HeaderFieldForm, value: string | number) => {
    setForm((f) => ({
      ...f,
      fields: f.fields.map((fld, idx) => (idx === i ? { ...fld, [field]: value } : fld)),
    }));
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      const request = {
        name: form.name,
        fields: form.fields.map((f) => ({ name: f.name, size: f.size, value: f.value })),
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
              <Stack spacing={1}>
                {form.fields.map((field, i) => (
                  <Stack key={i} direction="row" spacing={1} alignItems="center">
                    <MuiTextField
                      size="small"
                      label="Name"
                      value={field.name}
                      onChange={(e) => updateField(i, 'name', e.target.value)}
                      sx={{ flex: 2 }}
                    />
                    <MuiTextField
                      size="small"
                      label="Size"
                      type="number"
                      value={field.size}
                      onChange={(e) => updateField(i, 'size', Number(e.target.value))}
                      sx={{ width: 80 }}
                      slotProps={{ htmlInput: { min: 1 } }}
                    />
                    <MuiTextField
                      size="small"
                      label="Value"
                      value={field.value}
                      onChange={(e) => updateField(i, 'value', e.target.value)}
                      sx={{ flex: 2 }}
                    />
                    <Tooltip title="Remove field">
                      <IconButton size="small" onClick={() => removeField(i)}>
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </Stack>
                ))}
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

// ── Entry Editor ───────────────────────────────────────────────────

interface EntryEditorProps {
  entry: EntryForm;
  index: number;
  testCases: TestCaseSummary[];
  templates: HeaderTemplateSummary[];
  onChange: (entry: EntryForm) => void;
  onRemove: () => void;
}

function EntryEditor({ entry, index, testCases, templates, onChange, onRemove }: EntryEditorProps) {
  const handleLoadTestCase = async (id: number) => {
    const detail = await getTestCase(id);
    onChange({ ...entry, content: detail.message });
  };

  const handleLoadTemplate = async (id: number) => {
    const detail = await getHeaderTemplate(id);
    onChange({
      ...entry,
      headerFields: detail.fields.map((f) => ({ name: f.name, size: f.size, value: f.value })),
    });
  };

  const addField = () =>
    onChange({ ...entry, headerFields: [...entry.headerFields, { name: '', size: 10, value: '' }] });

  const removeField = (i: number) =>
    onChange({ ...entry, headerFields: entry.headerFields.filter((_, idx) => idx !== i) });

  const updateField = (i: number, field: keyof HeaderFieldForm, value: string | number) =>
    onChange({
      ...entry,
      headerFields: entry.headerFields.map((f, idx) => (idx === i ? { ...f, [field]: value } : f)),
    });

  return (
    <Box sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 1, p: 2 }}>
      <Stack spacing={2}>
        <Stack direction="row" alignItems="center" justifyContent="space-between">
          <Typography variant="caption" color="text.secondary" fontWeight={500}>
            Entry {index + 1}
          </Typography>
          <Tooltip title="Remove entry">
            <IconButton size="small" onClick={onRemove}>
              <DeleteIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </Stack>

        {testCases.length > 0 && (
          <MuiTextField
            select
            size="small"
            label="Load from test case"
            value=""
            onChange={(e) => {
              if (e.target.value) void handleLoadTestCase(Number(e.target.value));
            }}
            fullWidth
          >
            {testCases.map((tc) => (
              <MenuItem key={tc.id} value={tc.id}>{tc.name}</MenuItem>
            ))}
          </MuiTextField>
        )}

        <Stack direction="row" spacing={2} alignItems="flex-start">
          <MuiTextField
            label="Content"
            value={entry.content}
            onChange={(e) => onChange({ ...entry, content: e.target.value })}
            multiline
            rows={3}
            fullWidth
            size="small"
          />
          <MuiTextField
            label="%"
            type="number"
            value={entry.percentage}
            onChange={(e) => onChange({ ...entry, percentage: Number(e.target.value) })}
            size="small"
            sx={{ width: 80 }}
            slotProps={{ htmlInput: { min: 1, max: 100 } }}
          />
        </Stack>

        <Box>
          <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 1 }}>
            <Typography variant="caption" color="text.secondary">
              Header Fields (fixed-width columns, prepended as first line)
            </Typography>
            <Stack direction="row" spacing={1} alignItems="center">
              {templates.length > 0 && (
                <MuiTextField
                  select
                  size="small"
                  label="From template"
                  value=""
                  onChange={(e) => {
                    if (e.target.value) void handleLoadTemplate(Number(e.target.value));
                  }}
                  sx={{ width: 160 }}
                >
                  {templates.map((t) => (
                    <MenuItem key={t.id} value={t.id}>{t.name}</MenuItem>
                  ))}
                </MuiTextField>
              )}
              <Button size="small" onClick={addField}>
                <AddIcon fontSize="small" sx={{ mr: 0.5 }} />
                Add Field
              </Button>
            </Stack>
          </Stack>
          {entry.headerFields.length === 0 ? (
            <Typography variant="caption" color="text.disabled">No header fields</Typography>
          ) : (
            <Stack spacing={1}>
              {entry.headerFields.map((field, fi) => (
                <Stack key={fi} direction="row" spacing={1} alignItems="center">
                  <MuiTextField
                    size="small"
                    label="Name"
                    value={field.name}
                    onChange={(e) => updateField(fi, 'name', e.target.value)}
                    sx={{ flex: 2 }}
                  />
                  <MuiTextField
                    size="small"
                    label="Size"
                    type="number"
                    value={field.size}
                    onChange={(e) => updateField(fi, 'size', Number(e.target.value))}
                    sx={{ width: 100 }}
                    slotProps={{ htmlInput: { min: 1 } }}
                  />
                  <MuiTextField
                    size="small"
                    label="Value"
                    value={field.value}
                    onChange={(e) => updateField(fi, 'value', e.target.value)}
                    sx={{ flex: 2 }}
                  />
                  <Tooltip title="Remove field">
                    <IconButton size="small" onClick={() => removeField(fi)}>
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  </Tooltip>
                </Stack>
              ))}
            </Stack>
          )}
        </Box>
      </Stack>
    </Box>
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
  const [templates, setTemplates] = useState<HeaderTemplateSummary[]>([]);
  const [testCases, setTestCases] = useState<TestCaseSummary[]>([]);

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
    listHeaderTemplates().then(setTemplates).catch(() => {});
  };

  const openCreate = () => {
    setForm(EMPTY_SCENARIO);
    refreshSupportingData();
    setDialogOpen(true);
  };

  const openEdit = async (scenario: TestScenarioSummary) => {
    refreshSupportingData();
    setDialogOpen(true);
    const detail = await getTestScenario(scenario.id);
    setForm({
      id: detail.id,
      name: detail.name,
      count: detail.count,
      entries: detail.entries.map((e) => ({
        content: e.content,
        percentage: e.percentage,
        headerFields: e.headerFields.map((h) => ({ name: h.name, size: h.size, value: h.value })),
      })),
    });
  };

  const addEntry = () => {
    setForm((f) => ({
      ...f,
      entries: [...f.entries, { content: '', percentage: 100, headerFields: [] }],
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
      const request = {
        name: form.name,
        count: form.count,
        entries: form.entries.map((e) => ({
          content: e.content,
          percentage: e.percentage,
          headerFields: e.headerFields.map((h) => ({ name: h.name, size: h.size, value: h.value })),
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
          <Divider />
          <Stack spacing={1}>
            <Stack direction="row" alignItems="center" justifyContent="space-between">
              <Typography variant="body2" color="text.secondary" fontWeight={500}>
                Message Variants
              </Typography>
              <Button size="small" onClick={addEntry}>
                <AddIcon fontSize="small" sx={{ mr: 0.5 }} />
                Add Variant
              </Button>
            </Stack>
            {form.entries.length === 0 ? (
              <Typography variant="caption" color="text.disabled">No variants yet</Typography>
            ) : (
              <Stack spacing={2}>
                {form.entries.map((entry, i) => (
                  <EntryEditor
                    key={i}
                    entry={entry}
                    index={i}
                    testCases={testCases}
                    templates={templates}
                    onChange={(updated) => updateEntry(i, updated)}
                    onRemove={() => removeEntry(i)}
                  />
                ))}
              </Stack>
            )}
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
        <MuiTab label="Infra Profiles" />
      </MuiTabs>

      {activeTab === 0 && <ScenariosTab onChanged={handleChanged} />}
      {activeTab === 1 && <TestCasesTab onChanged={handleChanged} />}
      {activeTab === 2 && <HeadersTab onChanged={handleChanged} />}
      {activeTab === 3 && <InfraProfileManager />}
    </Dialog>
  );
}
