import { expect, test } from '@playwright/test';
import { mockApi } from './mocks';

test.beforeEach(async ({ page }) => { await mockApi(page); });

test('shows Logging tab', async ({ page }) => {
  await page.goto('/admin');
  await expect(page.getByRole('tab', { name: 'Logging' })).toBeVisible();
});

test('shows Kafka tab', async ({ page }) => {
  await page.goto('/admin');
  await expect(page.getByRole('tab', { name: 'Kafka' })).toBeVisible();
});

test('shows IBM MQ tab', async ({ page }) => {
  await page.goto('/admin');
  await expect(page.getByRole('tab', { name: 'IBM MQ' })).toBeVisible();
});

test('shows Ping tab', async ({ page }) => {
  await page.goto('/admin');
  await expect(page.getByRole('tab', { name: 'Ping' })).toBeVisible();
});

test('Kubernetes tab absent when no namespaces', async ({ page }) => {
  await page.goto('/admin');
  await expect(page.getByRole('tab', { name: 'Kubernetes' })).not.toBeVisible();
});

test('submitting log level form shows success message', async ({ page }) => {
  // Override to ensure POST with query params is also handled
  await page.route(/\/api\/admin\/logging\/level/, (r) =>
    r.fulfill({ json: { loggerName: 'com.example', configuredLevel: null, effectiveLevel: 'INFO' } }),
  );
  await page.goto('/admin');
  await page.getByRole('button', { name: 'Set Level' }).click();
  await expect(page.getByText('Logger "com.example" set to INFO')).toBeVisible();
});

test('submitting Kafka resize form shows success message', async ({ page }) => {
  await page.route(/\/api\/admin\/kafka\/topics\/resize/, (r) =>
    r.fulfill({ json: { topicName: 'mq-requests', partitions: 6 } }),
  );
  await page.goto('/admin');
  await page.getByRole('tab', { name: 'Kafka' }).click();
  await expect(page.getByRole('button', { name: 'Resize Topic' })).toBeEnabled({ timeout: 5000 });
  await page.getByLabel('Partitions').fill('6');
  await page.getByRole('button', { name: 'Resize Topic' }).click();
  await expect(page.getByText('Topic "mq-requests" resized to 6 partitions')).toBeVisible();
});

test('submitting IBM MQ depth form shows success message', async ({ page }) => {
  await page.route(/\/api\/admin\/mq\/queues\/depth/, (r) =>
    r.fulfill({ json: { queueName: 'DEV.QUEUE.1', currentDepth: 0, maxDepth: 10000 } }),
  );
  await page.goto('/admin');
  await page.getByRole('tab', { name: 'IBM MQ' }).click();
  await expect(page.getByRole('button', { name: 'Set Max Depth' })).toBeEnabled({ timeout: 5000 });
  await page.getByLabel('Max Depth').fill('10000');
  await page.getByRole('button', { name: 'Set Max Depth' }).click();
  await expect(page.getByText('Queue "DEV.QUEUE.1" max depth set to 10000')).toBeVisible();
});
