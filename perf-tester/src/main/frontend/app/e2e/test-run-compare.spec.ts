import { expect, test } from '@playwright/test';
import { mockApi, mockRunDetail, SAMPLE_RUNS, SAMPLE_RUN_2 } from './mocks';

test.beforeEach(async ({ page }) => {
  await mockApi(page, { testRuns: [...SAMPLE_RUNS, SAMPLE_RUN_2] });
  await mockRunDetail(page, 1);
  await mockRunDetail(page, 2);
});

test('shows Compare Test Runs heading', async ({ page }) => {
  await page.goto('/test-runs/compare?id1=1&id2=2');
  await expect(page.getByRole('heading', { name: 'Compare Test Runs' })).toBeVisible();
});

test('shows subtitle with both run IDs', async ({ page }) => {
  await page.goto('/test-runs/compare?id1=1&id2=2');
  await expect(page.getByText('Run #1 vs Run #2')).toBeVisible();
});

test('shows delta column header', async ({ page }) => {
  await page.goto('/test-runs/compare?id1=1&id2=2');
  await expect(page.getByText('Δ (A vs B)')).toBeVisible();
});

test('shows TPS values for both runs', async ({ page }) => {
  await page.goto('/test-runs/compare?id1=1&id2=2');
  await expect(page.getByText('TPS')).toBeVisible();
  await expect(page.getByText('50.0')).toBeVisible();
  await expect(page.getByText('120.0')).toBeVisible();
});

test('shows Avg Latency values for both runs', async ({ page }) => {
  await page.goto('/test-runs/compare?id1=1&id2=2');
  await expect(page.getByText('Avg Latency')).toBeVisible();
  await expect(page.getByText('20.0 ms')).toBeVisible();
  await expect(page.getByText('35.0 ms')).toBeVisible();
});

test('shows negative TPS delta when first run is slower', async ({ page }) => {
  await page.goto('/test-runs/compare?id1=1&id2=2');
  // a=50.0, b=120.0 → (50−120)/120 × 100 = −58.3%
  await expect(page.getByText('-58.3%')).toBeVisible();
});

test('shows negative Avg Latency delta when first run has lower latency', async ({ page }) => {
  await page.goto('/test-runs/compare?id1=1&id2=2');
  // a=20.0, b=35.0 → (20−35)/35 × 100 = −42.9%
  await expect(page.getByText('-42.9%')).toBeVisible();
});

test('shows P95 latency for both runs', async ({ page }) => {
  await page.goto('/test-runs/compare?id1=1&id2=2');
  await expect(page.getByText('P95')).toBeVisible();
  await expect(page.getByText('60.0 ms')).toBeVisible();
  await expect(page.getByText('90.0 ms')).toBeVisible();
});

test('shows Duration row with formatted values', async ({ page }) => {
  await page.goto('/test-runs/compare?id1=1&id2=2');
  await expect(page.getByText('Duration')).toBeVisible();
  await expect(page.getByText('2.00s')).toBeVisible();
  await expect(page.getByText('4.00s')).toBeVisible();
});

test('shows Status row', async ({ page }) => {
  await page.goto('/test-runs/compare?id1=1&id2=2');
  await expect(page.getByText('Status')).toBeVisible();
  await expect(page.getByText('COMPLETED').first()).toBeVisible();
});

test('shows Back button that navigates to /test-runs', async ({ page }) => {
  await page.goto('/test-runs/compare?id1=1&id2=2');
  await page.getByRole('button', { name: /back/i }).click();
  await expect(page).toHaveURL('/test-runs');
});

test('shows error alert when no query params provided', async ({ page }) => {
  await page.goto('/test-runs/compare');
  await expect(page.getByRole('alert')).toBeVisible();
  await expect(page.getByText('Invalid test run IDs')).toBeVisible();
});
