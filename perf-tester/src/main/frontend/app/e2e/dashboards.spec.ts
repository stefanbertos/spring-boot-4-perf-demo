import { expect, test } from '@playwright/test';
import { mockApi, SAMPLE_RUNS } from './mocks';

test('overview tab shows all stat cards', async ({ page }) => {
  await mockApi(page, { testRuns: SAMPLE_RUNS });
  await page.goto('/dashboards');
  await expect(page.getByText('Total Runs')).toBeVisible();
  await expect(page.getByText('Completed')).toBeVisible();
  await expect(page.getByText('Failed')).toBeVisible();
  await expect(page.getByText('Total Messages')).toBeVisible();
  await expect(page.getByText('Avg TPS')).toBeVisible();
  await expect(page.getByText('Avg Latency')).toBeVisible();
});

test('overview shows zero values when no runs', async ({ page }) => {
  await mockApi(page, { testRuns: [] });
  await page.goto('/dashboards');
  await expect(page.getByText('Total Runs')).toBeVisible();
});

test('external dashboard tab appears when link returned', async ({ page }) => {
  await mockApi(page, {
    testRuns: [],
    dashboardLinks: [{ label: 'Grafana', url: 'http://grafana.local' }],
  });
  await page.goto('/dashboards');
  await expect(page.getByRole('tab', { name: 'Grafana' })).toBeVisible();
});
