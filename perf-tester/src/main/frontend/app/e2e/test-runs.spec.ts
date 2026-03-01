import { expect, test } from '@playwright/test';
import { mockApi, mockRunDetail, SAMPLE_RUNS, SAMPLE_RUN_2 } from './mocks';

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

test('Delete Selected button disabled with no selections', async ({ page }) => {
  await mockApi(page, { testRuns: SAMPLE_RUNS });
  await page.goto('/test-runs');
  await expect(page.getByRole('button', { name: /delete selected/i })).toBeDisabled();
});

test('Delete Selected button enabled after selecting a run', async ({ page }) => {
  await mockApi(page, { testRuns: SAMPLE_RUNS });
  await page.goto('/test-runs');
  const checkbox = page.getByRole('checkbox').first();
  await checkbox.click();
  await expect(page.getByRole('button', { name: /delete selected/i })).toBeEnabled();
});

test('clicking a row navigates to the detail page', async ({ page }) => {
  await mockApi(page, { testRuns: SAMPLE_RUNS });
  await mockRunDetail(page, 1);
  await page.goto('/test-runs');
  await page.getByText('smoke-test').click();
  await expect(page).toHaveURL('/test-runs/1');
});

test('selecting a run checks its checkbox', async ({ page }) => {
  await mockApi(page, { testRuns: SAMPLE_RUNS });
  await page.goto('/test-runs');
  const checkbox = page.getByRole('checkbox').first();
  await checkbox.click();
  await expect(checkbox).toBeChecked();
});

test('Compare button enabled after 2 runs are selected', async ({ page }) => {
  await mockApi(page, { testRuns: [...SAMPLE_RUNS, SAMPLE_RUN_2] });
  await page.goto('/test-runs');
  const checkboxes = page.getByRole('checkbox');
  await checkboxes.nth(0).click();
  await checkboxes.nth(1).click();
  await expect(page.getByRole('button', { name: /compare/i })).toBeEnabled();
});

test('Compare Selected navigates to the compare page', async ({ page }) => {
  await mockApi(page, { testRuns: [...SAMPLE_RUNS, SAMPLE_RUN_2] });
  await mockRunDetail(page, 1);
  await mockRunDetail(page, 2);
  await page.goto('/test-runs');
  const checkboxes = page.getByRole('checkbox');
  await checkboxes.nth(0).click();
  await checkboxes.nth(1).click();
  await page.getByRole('button', { name: /compare/i }).click();
  await expect(page).toHaveURL(/\/test-runs\/compare\?id/);
});

test('delete icon opens confirmation dialog', async ({ page }) => {
  await mockApi(page, { testRuns: SAMPLE_RUNS });
  await page.goto('/test-runs');
  await page.getByRole('button', { name: 'Delete test run' }).click();
  await expect(page.getByRole('dialog')).toBeVisible();
  await expect(page.getByText('Delete Test Run')).toBeVisible();
});

test('Cancel button closes the confirmation dialog', async ({ page }) => {
  await mockApi(page, { testRuns: SAMPLE_RUNS });
  await page.goto('/test-runs');
  await page.getByRole('button', { name: 'Delete test run' }).click();
  await page.getByRole('button', { name: 'Cancel' }).click();
  await expect(page.getByRole('dialog')).not.toBeVisible();
});

test('confirming delete calls the delete API', async ({ page }) => {
  await mockApi(page, { testRuns: SAMPLE_RUNS });
  await page.route('**/api/perf/test-runs/1', async (route) => {
    if (route.request().method() === 'DELETE') {
      await route.fulfill({ status: 204 });
    } else {
      await route.continue();
    }
  });
  await page.goto('/test-runs');
  await page.getByRole('button', { name: 'Delete test run' }).click();
  const deletePromise = page.waitForRequest(
    (req) => req.url().includes('/api/perf/test-runs/1') && req.method() === 'DELETE',
  );
  await page.getByRole('button', { name: 'Delete', exact: true }).click();
  const deleteReq = await deletePromise;
  expect(deleteReq.method()).toBe('DELETE');
});
