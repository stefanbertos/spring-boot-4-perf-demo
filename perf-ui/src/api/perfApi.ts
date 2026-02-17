import { del, get, post, postFormData, put } from './client';
import type {
  LogLevelResponse,
  MessageResponse,
  MetricResponse,
  QueueInfo,
  TestCaseDetail,
  TestCaseSummary,
  TestRunResponse,
  TestRunSummaryResponse,
  TopicInfo,
} from '@/types/api';

// ── Performance Tests ──────────────────────────────────────────────

export function sendTest(params: {
  message: string;
  count?: number;
  timeoutSeconds?: number;
  delayMs?: number;
  testId?: string;
  exportStatistics?: boolean;
  debug?: boolean;
}): Promise<void> {
  const query = new URLSearchParams();
  if (params.count != null) query.set('count', String(params.count));
  if (params.timeoutSeconds != null) query.set('timeoutSeconds', String(params.timeoutSeconds));
  if (params.delayMs != null) query.set('delayMs', String(params.delayMs));
  if (params.testId) query.set('testId', params.testId);
  if (params.exportStatistics) query.set('exportStatistics', 'true');
  if (params.debug) query.set('debug', 'true');

  const qs = query.toString();
  return post(`/api/perf/send${qs ? `?${qs}` : ''}`, params.message, 'text/plain');
}

// ── Test Runs ──────────────────────────────────────────────────────

export function getTestRuns(): Promise<TestRunResponse[]> {
  return get('/api/perf/test-runs');
}

export function getTestRunSummary(id: number): Promise<TestRunSummaryResponse> {
  return get(`/api/perf/test-runs/${id}`);
}

export function getTestRunMessages(id: number): Promise<MessageResponse[]> {
  return get(`/api/perf/test-runs/${id}/messages`);
}

export function getTestRunMetrics(id: number): Promise<MetricResponse[]> {
  return get(`/api/perf/test-runs/${id}/metrics`);
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
