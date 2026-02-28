import { expect, test } from '@playwright/test';
import { mockApi, SAMPLE_RUNS } from './mocks';

test('page title is Test Runs', async ({ page }) => {
  await mockApi(page, { testRuns: [] });
  await page.goto('/test-runs');
  await expect(page.getByRole('heading', { name: 'Test Runs' })).toBeVisible();
});

test('shows empty state when no runs', async ({ page }) => {
  await mockApi(page, { testRuns: [] });
  await page.goto('/test-runs');
  await expect(page.getByText(/no test runs/i)).toBeVisible();
});

test('shows run data in table', async ({ page }) => {
  await mockApi(page, { testRuns: SAMPLE_RUNS });
  await page.goto('/test-runs');
  await expect(page.getByText('smoke-test')).toBeVisible();
  await expect(page.getByText('COMPLETED')).toBeVisible();
  await expect(page.getByText('SMOKE')).toBeVisible();
});

test('Compare button disabled with no selections', async ({ page }) => {
  await mockApi(page, { testRuns: SAMPLE_RUNS });
  await page.goto('/test-runs');
  await expect(page.getByRole('button', { name: /compare/i })).toBeDisabled();
});
