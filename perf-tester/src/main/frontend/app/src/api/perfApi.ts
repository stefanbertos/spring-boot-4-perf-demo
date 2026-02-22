import { del, get, post, postFormData, put } from './client';
import type {
  ApplyResult,
  DeploymentInfo,
  HeaderTemplateDetail,
  HeaderTemplateRequest,
  HeaderTemplateSummary,
  NamespaceInfo,
  InfraProfileDetail,
  InfraProfileRequest,
  InfraProfileSummary,
  LogEntry,
  LogLevelResponse,
  QueueInfo,
  ResponseTemplateDetail,
  ResponseTemplateRequest,
  ResponseTemplateSummary,
  TestCaseDetail,
  TestCaseSummary,
  TestProgressEvent,
  TestRunDetailResponse,
  TestRunResponse,
  TestScenarioDetail,
  TestScenarioRequest,
  TestScenarioSummary,
  TestStartResponse,
  TopicInfo,
} from '@/types/api';

// ── Performance Tests ──────────────────────────────────────────────

export function sendTest(params: {
  message?: string;
  count?: number;
  timeoutSeconds?: number;
  delayMs?: number;
  testId?: string;
  scenarioId?: number;
  exportGrafana?: boolean;
  exportPrometheus?: boolean;
  exportKubernetes?: boolean;
  exportLogs?: boolean;
  debug?: boolean;
}): Promise<TestStartResponse> {
  const query = new URLSearchParams();
  if (params.count != null) query.set('count', String(params.count));
  if (params.timeoutSeconds != null) query.set('timeoutSeconds', String(params.timeoutSeconds));
  if (params.delayMs != null) query.set('delayMs', String(params.delayMs));
  if (params.testId) query.set('testId', params.testId);
  if (params.scenarioId != null) query.set('scenarioId', String(params.scenarioId));
  if (params.exportGrafana) query.set('exportGrafana', 'true');
  if (params.exportPrometheus) query.set('exportPrometheus', 'true');
  if (params.exportKubernetes) query.set('exportKubernetes', 'true');
  if (params.exportLogs) query.set('exportLogs', 'true');
  if (params.debug) query.set('debug', 'true');

  const qs = query.toString();
  return post<TestStartResponse>(
    `/api/perf/send${qs ? `?${qs}` : ''}`,
    params.message,
    'text/plain',
  );
}

export function subscribeTestProgress(
  testRunId: string,
  onEvent: (event: TestProgressEvent) => void,
  onComplete: () => void,
  onError: (err: Event) => void,
): () => void {
  const source = new EventSource(`/api/perf/progress/${testRunId}`);

  source.onmessage = (e) => {
    const data = JSON.parse(e.data as string) as TestProgressEvent;
    onEvent(data);
    if (data.status === 'COMPLETED' || data.status === 'TIMEOUT' || data.status === 'FAILED') {
      source.close();
      onComplete();
    }
  };

  source.onerror = (err) => {
    source.close();
    onError(err);
  };

  return () => source.close();
}

// ── Test Runs ──────────────────────────────────────────────────────

export function getTestRuns(): Promise<TestRunResponse[]> {
  return get('/api/perf/test-runs');
}

export function getTestRunSummary(id: number): Promise<TestRunDetailResponse> {
  return get(`/api/perf/test-runs/${id}`);
}

export function deleteTestRun(id: number): Promise<void> {
  return del(`/api/perf/test-runs/${id}`);
}

export function downloadTestRunUrl(id: number): string {
  return `/api/perf/test-runs/${id}/download`;
}

export function getTestRunLogs(id: number): Promise<LogEntry[]> {
  return get(`/api/perf/test-runs/${id}/logs`);
}

// ── Logging Admin ──────────────────────────────────────────────────

export function getLogLevel(loggerName?: string): Promise<LogLevelResponse> {
  const query = loggerName ? `?loggerName=${encodeURIComponent(loggerName)}` : '';
  return get(`/api/admin/logging/level${query}`);
}

export function setLogLevel(level: string, loggerName?: string): Promise<LogLevelResponse> {
  const params = new URLSearchParams({ level });
  if (loggerName) params.set('loggerName', loggerName);
  return post(`/api/admin/logging/level?${params}`);
}

// ── Kafka Admin ────────────────────────────────────────────────────

export function listTopics(): Promise<TopicInfo[]> {
  return get('/api/admin/kafka/topics/list');
}

export function getTopicInfo(topicName: string): Promise<TopicInfo> {
  return get(`/api/admin/kafka/topics?topicName=${encodeURIComponent(topicName)}`);
}

export function resizeTopic(topicName: string, partitions: number): Promise<TopicInfo> {
  const params = new URLSearchParams({
    topicName,
    partitions: String(partitions),
  });
  return post(`/api/admin/kafka/topics/resize?${params}`);
}

// ── IBM MQ Admin ───────────────────────────────────────────────────

export function listQueues(): Promise<QueueInfo[]> {
  return get('/api/admin/mq/queues/list');
}

export function getQueueInfo(queueName: string): Promise<QueueInfo> {
  return get(`/api/admin/mq/queues?queueName=${encodeURIComponent(queueName)}`);
}

export function changeQueueMaxDepth(queueName: string, maxDepth: number): Promise<QueueInfo> {
  const params = new URLSearchParams({
    queueName,
    maxDepth: String(maxDepth),
  });
  return post(`/api/admin/mq/queues/depth?${params}`);
}

// ── Kubernetes Admin ────────────────────────────────────────────────

export function listNamespaces(): Promise<NamespaceInfo[]> {
  return get('/api/admin/kubernetes/namespaces/list');
}

export function listDeployments(namespace: string): Promise<DeploymentInfo[]> {
  return get(`/api/admin/kubernetes/deployments/list?namespace=${encodeURIComponent(namespace)}`);
}

export function scaleDeployment(name: string, namespace: string, replicas: number): Promise<void> {
  const params = new URLSearchParams({ name, namespace, replicas: String(replicas) });
  return post(`/api/admin/kubernetes/deployments/scale?${params}`);
}

// ── Infra Profiles ─────────────────────────────────────────────────

export function listInfraProfiles(): Promise<InfraProfileSummary[]> {
  return get('/api/infra-profiles');
}

export function getInfraProfile(id: number): Promise<InfraProfileDetail> {
  return get(`/api/infra-profiles/${id}`);
}

export function createInfraProfile(data: InfraProfileRequest): Promise<InfraProfileDetail> {
  return post('/api/infra-profiles', data);
}

export function updateInfraProfile(id: number, data: InfraProfileRequest): Promise<InfraProfileDetail> {
  return put(`/api/infra-profiles/${id}`, data);
}

export function deleteInfraProfile(id: number): Promise<void> {
  return del(`/api/infra-profiles/${id}`);
}

export function applyInfraProfile(id: number): Promise<ApplyResult> {
  return post(`/api/infra-profiles/${id}/apply`);
}

// ── Test Cases ────────────────────────────────────────────────────

export function listTestCases(): Promise<TestCaseSummary[]> {
  return get('/api/test-cases');
}

export function getTestCase(id: number): Promise<TestCaseDetail> {
  return get(`/api/test-cases/${id}`);
}

export function createTestCase(name: string, message: string): Promise<TestCaseDetail> {
  return post('/api/test-cases', { name, message });
}

export function updateTestCase(id: number, name: string, message: string): Promise<TestCaseDetail> {
  return put(`/api/test-cases/${id}`, { name, message });
}

export function deleteTestCase(id: number): Promise<void> {
  return del(`/api/test-cases/${id}`);
}

export function uploadTestCase(name: string, file: File): Promise<TestCaseDetail> {
  const formData = new FormData();
  formData.append('name', name);
  formData.append('file', file);
  return postFormData('/api/test-cases/upload', formData);
}

// ── Header Templates ────────────────────────────────────────────────

export function listHeaderTemplates(): Promise<HeaderTemplateSummary[]> {
  return get('/api/header-templates');
}

export function getHeaderTemplate(id: number): Promise<HeaderTemplateDetail> {
  return get(`/api/header-templates/${id}`);
}

export function createHeaderTemplate(data: HeaderTemplateRequest): Promise<HeaderTemplateDetail> {
  return post('/api/header-templates', data);
}

export function updateHeaderTemplate(id: number, data: HeaderTemplateRequest): Promise<HeaderTemplateDetail> {
  return put(`/api/header-templates/${id}`, data);
}

export function deleteHeaderTemplate(id: number): Promise<void> {
  return del(`/api/header-templates/${id}`);
}

// ── Response Templates ──────────────────────────────────────────────

export function listResponseTemplates(): Promise<ResponseTemplateSummary[]> {
  return get('/api/response-templates');
}

export function getResponseTemplate(id: number): Promise<ResponseTemplateDetail> {
  return get(`/api/response-templates/${id}`);
}

export function createResponseTemplate(data: ResponseTemplateRequest): Promise<ResponseTemplateDetail> {
  return post('/api/response-templates', data);
}

export function updateResponseTemplate(id: number, data: ResponseTemplateRequest): Promise<ResponseTemplateDetail> {
  return put(`/api/response-templates/${id}`, data);
}

export function deleteResponseTemplate(id: number): Promise<void> {
  return del(`/api/response-templates/${id}`);
}

// ── Test Scenarios ─────────────────────────────────────────────────

export function listTestScenarios(): Promise<TestScenarioSummary[]> {
  return get('/api/test-scenarios');
}

export function getTestScenario(id: number): Promise<TestScenarioDetail> {
  return get(`/api/test-scenarios/${id}`);
}

export function createTestScenario(data: TestScenarioRequest): Promise<TestScenarioDetail> {
  return post('/api/test-scenarios', data);
}

export function updateTestScenario(id: number, data: TestScenarioRequest): Promise<TestScenarioDetail> {
  return put(`/api/test-scenarios/${id}`, data);
}

export function deleteTestScenario(id: number): Promise<void> {
  return del(`/api/test-scenarios/${id}`);
}
