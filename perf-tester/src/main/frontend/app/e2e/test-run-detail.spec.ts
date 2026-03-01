import { expect, test } from '@playwright/test';
import { mockApi, mockRunDetail, SAMPLE_RUNS } from './mocks';

test.beforeEach(async ({ page }) => {
  await mockApi(page, { testRuns: SAMPLE_RUNS });
  await mockRunDetail(page, 1);
});

test('shows Test Run #1 heading', async ({ page }) => {
  await page.goto('/test-runs/1');
  await expect(page.getByRole('heading', { name: /test run #1/i })).toBeVisible();
});

test('shows COMPLETED status chip', async ({ page }) => {
  await page.goto('/test-runs/1');
  await expect(page.getByText('COMPLETED').first()).toBeVisible();
});

test('shows SMOKE test type chip', async ({ page }) => {
  await page.goto('/test-runs/1');
  await expect(page.getByText('SMOKE')).toBeVisible();
});

test('shows TPS in Summary tab', async ({ page }) => {
  await page.goto('/test-runs/1');
  await expect(page.getByText('TPS')).toBeVisible();
  await expect(page.getByText('50.0')).toBeVisible();
});

test('shows Avg Latency in Summary tab', async ({ page }) => {
  await page.goto('/test-runs/1');
  await expect(page.getByText('Avg Latency')).toBeVisible();
  await expect(page.getByText('20.0 ms')).toBeVisible();
});

test('shows P95 latency', async ({ page }) => {
  await mockApi(page, { testRuns: SAMPLE_RUNS });
  await mockRunDetail(page, 1, { p95LatencyMs: 60.0 });
  await page.goto('/test-runs/1');
  await expect(page.getByText('P95')).toBeVisible();
  await expect(page.getByText('60.0 ms')).toBeVisible();
});

test('shows Summary, Logs and Monitoring tabs', async ({ page }) => {
  await page.goto('/test-runs/1');
  await expect(page.getByRole('tab', { name: 'Summary' })).toBeVisible();
  await expect(page.getByRole('tab', { name: 'Logs' })).toBeVisible();
  await expect(page.getByRole('tab', { name: 'Monitoring' })).toBeVisible();
});

test('Logs tab shows empty message when no logs', async ({ page }) => {
  await page.goto('/test-runs/1');
  await page.getByRole('tab', { name: 'Logs' }).click();
  await expect(page.getByText('No logs available.')).toBeVisible();
});

test('Monitoring tab shows empty message when no snapshots', async ({ page }) => {
  await page.goto('/test-runs/1');
  await page.getByRole('tab', { name: 'Monitoring' }).click();
  await expect(page.getByText('No monitoring snapshots available.')).toBeVisible();
});

test('shows SLA Thresholds section when thresholdResults present', async ({ page }) => {
  const results = JSON.stringify([
    { metric: 'AVG_LATENCY', operator: 'LT', threshold: 100, actual: 20.0, passed: true },
  ]);
  await mockApi(page, { testRuns: SAMPLE_RUNS });
  await mockRunDetail(page, 1, { thresholdResults: results });
  await page.goto('/test-runs/1');
  await expect(page.getByText('SLA Thresholds')).toBeVisible();
});

test('shows passed threshold chip in green', async ({ page }) => {
  const results = JSON.stringify([
    { metric: 'AVG_LATENCY', operator: 'LT', threshold: 100, actual: 20.0, passed: true },
  ]);
  await mockApi(page, { testRuns: SAMPLE_RUNS });
  await mockRunDetail(page, 1, { thresholdResults: results });
  await page.goto('/test-runs/1');
  await expect(page.getByText(/AVG_LATENCY LT 100/)).toBeVisible();
});

test('shows Download ZIP button when zipFilePath is set', async ({ page }) => {
  await mockApi(page, { testRuns: SAMPLE_RUNS });
  await mockRunDetail(page, 1, { zipFilePath: '/exports/run-1.zip' });
  await page.goto('/test-runs/1');
  await expect(page.getByRole('link', { name: /download zip/i })).toBeVisible();
});

test('hides Download ZIP button when zipFilePath is null', async ({ page }) => {
  await page.goto('/test-runs/1');
  await expect(page.getByRole('link', { name: /download zip/i })).not.toBeVisible();
});

test('shows Back to Test Runs button', async ({ page }) => {
  await page.goto('/test-runs/1');
  await expect(page.getByRole('link', { name: /back to test runs/i })).toBeVisible();
});

test('Back to Test Runs navigates to /test-runs', async ({ page }) => {
  await page.goto('/test-runs/1');
  await page.getByRole('link', { name: /back to test runs/i }).click();
  await expect(page).toHaveURL('/test-runs');
});

test('shows error when test run API returns 404', async ({ page }) => {
  await page.route('/api/perf/test-runs/99', r => r.fulfill({ status: 404, json: { message: 'Not found' } }));
  await page.route('/api/perf/test-runs/99/logs',      r => r.fulfill({ json: [] }));
  await page.route('/api/perf/test-runs/99/snapshots', r => r.fulfill({ json: [] }));
  await page.goto('/test-runs/99');
  await expect(page.getByRole('alert')).toBeVisible();
});

test('shows logs when logs are returned', async ({ page }) => {
  await mockApi(page, { testRuns: SAMPLE_RUNS });
  await page.route('/api/perf/test-runs/1', r => r.fulfill({ json: { ...SAMPLE_RUNS[0], thresholdResults: null } }));
  await page.route('/api/perf/test-runs/1/logs', r => r.fulfill({ json: [
    { timestamp: '2024-01-01T10:00:01Z', level: 'INFO', message: 'Test started' },
  ] }));
  await page.route('/api/perf/test-runs/1/snapshots', r => r.fulfill({ json: [] }));
  await page.goto('/test-runs/1');
  await page.getByRole('tab', { name: 'Logs' }).click();
  await expect(page.getByText('Test started')).toBeVisible();
});

test('shows snapshot table when snapshots are returned', async ({ page }) => {
  await mockApi(page, { testRuns: SAMPLE_RUNS });
  await page.route('/api/perf/test-runs/1', r => r.fulfill({ json: { ...SAMPLE_RUNS[0], thresholdResults: null } }));
  await page.route('/api/perf/test-runs/1/logs', r => r.fulfill({ json: [] }));
  await page.route('/api/perf/test-runs/1/snapshots', r => r.fulfill({ json: [
    { id: 1, sampledAt: '2024-01-01T10:00:01Z', outboundQueueDepth: 5, inboundQueueDepth: 0,
      kafkaRequestsLag: 2, kafkaResponsesLag: 0 },
  ] }));
  await page.goto('/test-runs/1');
  await page.getByRole('tab', { name: 'Monitoring' }).click();
  await expect(page.getByText('Outbound Q')).toBeVisible();
  await expect(page.getByText('5')).toBeVisible();
});
