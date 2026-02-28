import { expect, test } from '@playwright/test';
import { mockApi } from './mocks';

test.beforeEach(async ({ page }) => { await mockApi(page); });

test('page title is Run Test', async ({ page }) => {
  await page.goto('/send');
  await expect(page.getByRole('heading', { name: 'Run Test' })).toBeVisible();
});

test('Test Scenario selector disabled when no scenarios', async ({ page }) => {
  await page.goto('/send');
  await expect(page.getByLabel('Test Scenario')).toBeDisabled();
});

test('Test Scenario selector enabled when scenarios exist', async ({ page }) => {
  await mockApi(page, { scenarios: [{ id: 1, name: 'Smoke', count: 10, updatedAt: '2024-01-01' }] });
  await page.goto('/send');
  await expect(page.getByLabel('Test Scenario')).not.toBeDisabled();
});

test('Test ID input is present', async ({ page }) => {
  await page.goto('/send');
  await expect(page.getByLabel('Test ID (optional)')).toBeVisible();
});

test('Grafana and Prometheus export checkboxes are present', async ({ page }) => {
  await page.goto('/send');
  await expect(page.getByLabel('Grafana')).toBeVisible();
  await expect(page.getByLabel('Prometheus')).toBeVisible();
});

test('Kubernetes checkbox hidden when no namespaces', async ({ page }) => {
  await page.goto('/send');
  await expect(page.getByLabel('Kubernetes')).not.toBeVisible();
});

test('Kubernetes checkbox visible when namespaces available', async ({ page }) => {
  await mockApi(page, { namespaces: [{ name: 'perf-demo', phase: 'Active' }] });
  await page.goto('/send');
  await expect(page.getByLabel('Kubernetes')).toBeVisible();
});

test('Manage button opens scenario manager dialog', async ({ page }) => {
  await page.goto('/send');
  await page.getByRole('button', { name: 'Manage' }).click();
  await expect(page.getByRole('dialog')).toBeVisible();
});
