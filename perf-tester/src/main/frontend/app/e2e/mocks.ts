import type { Page } from '@playwright/test';

export const SAMPLE_RUNS = [{
  id: 1, testRunId: 'run-1', testId: 'smoke-test', status: 'COMPLETED',
  messageCount: 100, completedCount: 100, tps: 50.0, avgLatencyMs: 20.0,
  minLatencyMs: 5.0, maxLatencyMs: 80.0,
  p25LatencyMs: null, p50LatencyMs: null, p75LatencyMs: null,
  p90LatencyMs: null, p95LatencyMs: 60.0, p99LatencyMs: null,
  timeoutCount: 0, testType: 'SMOKE', thresholdStatus: null,
  durationMs: 2000, startedAt: '2024-01-01T10:00:00Z',
  completedAt: '2024-01-01T10:00:02Z', zipFilePath: null,
}];

export const DEFAULT_PREFS = {
  exportGrafana: false, exportPrometheus: false, exportKubernetes: false,
  exportLogs: false, exportDatabase: false, debug: false,
};

export async function mockApi(page: Page, overrides: {
  testRuns?: object[];
  scenarios?: object[];
  dashboardLinks?: object[];
  namespaces?: object[];
} = {}) {
  const { testRuns = [], scenarios = [], dashboardLinks = [], namespaces = [] } = overrides;
  await page.route('/api/perf/test-runs',                     r => r.fulfill({ json: testRuns }));
  await page.route('/api/dashboards',                         r => r.fulfill({ json: dashboardLinks }));
  await page.route('/api/test-scenarios',                     r => r.fulfill({ json: scenarios }));
  await page.route('/api/run-test/preferences',               r => r.fulfill({ json: DEFAULT_PREFS }));
  await page.route('/api/admin/kubernetes/namespaces/list',   r => r.fulfill({ json: namespaces }));
  await page.route('/api/admin/loki/services',                r => r.fulfill({ json: [] }));
  await page.route('/api/admin/db-queries',                   r => r.fulfill({ json: [] }));
  await page.route('/api/admin/logging/level',                r => r.fulfill({ json: {
    loggerName: 'com.example', configuredLevel: null, effectiveLevel: 'INFO' } }));
  await page.route('/api/admin/kafka/topics/list',            r => r.fulfill({ json: [
    { topicName: 'mq-requests', partitions: 3 }] }));
  await page.route('/api/admin/mq/queues/list',               r => r.fulfill({ json: [
    { queueName: 'DEV.QUEUE.1', currentDepth: 0, maxDepth: 5000 }] }));
  await page.route('/api/admin/healthcheck',                  r => r.fulfill({ json: [] }));
}
