#!/usr/bin/env node

/**
 * Regenerates the Helm Grafana dashboards ConfigMap from Docker Compose dashboard JSON files.
 *
 * Usage: node scripts/sync-helm-dashboards.mjs
 *
 * - Reads Docker dashboard JSONs from infrastructure/docker/grafana/dashboards/
 * - Preserves the k8s.json dashboard from the existing Helm ConfigMap (Helm-only)
 * - Removes the tempo-tracing.json dashboard
 * - For Spring Boot app dashboards, adds pod variable filtering and cAdvisor panels
 * - Escapes Grafana {{label}} templates for Helm compatibility
 */

import { readFileSync, writeFileSync } from 'fs';
import { join, resolve } from 'path';
import { fileURLToPath } from 'url';

const projectRoot = resolve(fileURLToPath(import.meta.url), '..', '..');
const dockerDashboardsDir = join(projectRoot, 'infrastructure', 'docker', 'grafana', 'dashboards');
const helmConfigMapPath = join(projectRoot, 'infrastructure', 'helm', 'grafana', 'templates', 'dashboards-configmap.yaml');

const SPRING_BOOT_DASHBOARDS = ['perf-tester', 'ibm-mq-consumer', 'kafka-consumer'];

const DASHBOARD_ORDER = [
  'perf-tester',
  'ibm-mq',
  'kafka',
  'ibm-mq-consumer',
  'kafka-consumer',
  'k8s',
  'node-exporter',
  'cadvisor',
  'kafka-exporter',
  'oracle',
];

// ---- Helpers ----

function readDockerDashboard(name) {
  var filePath = join(dockerDashboardsDir, `${name}.json`);
  return JSON.parse(readFileSync(filePath, 'utf-8'));
}

function extractK8sDashboard() {
  var existing = readFileSync(helmConfigMapPath, 'utf-8');
  var lines = existing.split('\n');

  // Find the start and end of k8s.json block
  var startIdx = -1;
  var endIdx = -1;
  for (var i = 0; i < lines.length; i++) {
    if (lines[i].match(/^\s+k8s\.json:\s*\|/)) {
      startIdx = i + 1; // content starts on next line
    } else if (startIdx > 0 && endIdx < 0 && lines[i].match(/^\s+\S+\.json:\s*\|/)) {
      endIdx = i;
      break;
    }
  }
  if (startIdx < 0) {
    throw new Error('Could not find k8s.json in existing ConfigMap');
  }
  if (endIdx < 0) {
    // k8s.json goes to end of file
    endIdx = lines.length;
  }

  // Return the raw content lines (already indented with 4 spaces)
  return lines.slice(startIdx, endIdx).join('\n');
}

/**
 * For Spring Boot dashboards: add pod=~"$pod" to every PromQL expr that has job="<service>".
 */
function addPodFilter(dashboard, serviceName) {
  var jobPattern = `job=\\"${serviceName}\\"`;
  var podFilter = `, pod=~\\"$pod\\"`;

  function processValue(val) {
    if (typeof val === 'string') {
      // Add pod filter after job="<service>" if not already present
      if (val.includes(`job="${serviceName}"`) && !val.includes('pod=~"$pod"')) {
        val = val.replace(
          new RegExp(`job="${escapeRegex(serviceName)}"`, 'g'),
          `job="${serviceName}", pod=~"$pod"`
        );
      }
      return val;
    }
    if (Array.isArray(val)) {
      return val.map(processValue);
    }
    if (val !== null && typeof val === 'object') {
      var result = {};
      for (var [k, v] of Object.entries(val)) {
        result[k] = processValue(v);
      }
      return result;
    }
    return val;
  }

  return processValue(dashboard);
}

function escapeRegex(str) {
  return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

/**
 * Replace "templating": {"list": []} with pod variable template for Spring Boot dashboards.
 */
function addPodTemplating(dashboard, serviceName) {
  dashboard.templating = {
    list: [
      {
        allValue: `${serviceName}-.*`,
        current: { selected: true, text: 'All', value: '$__all' },
        datasource: { type: 'prometheus', uid: 'prometheus' },
        definition: `label_values(up{job="${serviceName}"}, pod)`,
        includeAll: true,
        label: 'Pod',
        multi: true,
        name: 'pod',
        query: `label_values(up{job="${serviceName}"}, pod)`,
        refresh: 2,
        sort: 5,
        type: 'query',
      },
    ],
  };
  return dashboard;
}

/**
 * Add cAdvisor row and panels at the end of the panels array.
 */
function addCadvisorPanels(dashboard) {
  var panels = dashboard.panels;

  // Compute Y from the last panel
  var lastPanel = panels[panels.length - 1];
  var y = (lastPanel.gridPos.y || 0) + (lastPanel.gridPos.h || 1);

  panels.push(
    {
      collapsed: false,
      gridPos: { h: 1, w: 24, x: 0, y: y },
      panels: [],
      title: 'Container Resources (cAdvisor)',
      type: 'row',
    },
    {
      title: 'Container CPU Usage',
      type: 'timeseries',
      gridPos: { h: 8, w: 12, x: 0, y: y + 1 },
      datasource: { type: 'prometheus', uid: 'prometheus' },
      targets: [
        {
          expr: 'rate(container_cpu_usage_seconds_total{pod=~"$pod", container!="", container!="POD"}[$__rate_interval])',
          legendFormat: '{{pod}}',
        },
      ],
      fieldConfig: { defaults: { unit: 'percentunit', custom: { fillOpacity: 20 } }, overrides: [] },
    },
    {
      title: 'Container Memory Usage',
      type: 'timeseries',
      gridPos: { h: 8, w: 12, x: 12, y: y + 1 },
      datasource: { type: 'prometheus', uid: 'prometheus' },
      targets: [
        {
          expr: 'container_memory_working_set_bytes{pod=~"$pod", container!="", container!="POD"}',
          legendFormat: '{{pod}} working set',
        },
        {
          expr: 'kube_pod_container_resource_limits{pod=~"$pod", resource="memory"}',
          legendFormat: '{{pod}} limit',
        },
      ],
      fieldConfig: { defaults: { unit: 'bytes', custom: { fillOpacity: 20 } }, overrides: [] },
    },
    {
      title: 'Container Network I/O',
      type: 'timeseries',
      gridPos: { h: 8, w: 12, x: 0, y: y + 9 },
      datasource: { type: 'prometheus', uid: 'prometheus' },
      targets: [
        {
          expr: 'sum(rate(container_network_receive_bytes_total{pod=~"$pod"}[$__rate_interval])) by (pod)',
          legendFormat: '{{pod}} rx',
        },
        {
          expr: 'sum(rate(container_network_transmit_bytes_total{pod=~"$pod"}[$__rate_interval])) by (pod)',
          legendFormat: '{{pod}} tx',
        },
      ],
      fieldConfig: { defaults: { unit: 'Bps', custom: { fillOpacity: 20 } }, overrides: [] },
    },
    {
      title: 'Container Filesystem I/O',
      type: 'timeseries',
      gridPos: { h: 8, w: 12, x: 12, y: y + 9 },
      datasource: { type: 'prometheus', uid: 'prometheus' },
      targets: [
        {
          expr: 'sum(rate(container_fs_reads_bytes_total{pod=~"$pod", container!=""}[$__rate_interval])) by (pod)',
          legendFormat: '{{pod}} read',
        },
        {
          expr: 'sum(rate(container_fs_writes_bytes_total{pod=~"$pod", container!=""}[$__rate_interval])) by (pod)',
          legendFormat: '{{pod}} write',
        },
      ],
      fieldConfig: { defaults: { unit: 'Bps', custom: { fillOpacity: 20 } }, overrides: [] },
    }
  );

  return dashboard;
}

/**
 * Escape Grafana {{label}} patterns for Helm templates.
 * {{label}} -> {{ "{{label}}" }}
 *
 * But do NOT escape Helm template syntax like {{ include ... }} or {{- include ... }}.
 * Those only appear in the YAML header, not inside JSON dashboard content.
 */
function escapeHelmMustache(jsonStr) {
  // Match {{...}} that are Grafana legend labels (inside JSON strings).
  // Replace {{word}} with {{ "{{word}}" }}
  return jsonStr.replace(/\{\{([^{}]+)\}\}/g, '{{ "{{$1}}" }}');
}

/**
 * Convert a dashboard JSON object to a 2-space indented JSON string,
 * then indent every line by 4 spaces for YAML block scalar.
 */
function formatDashboardForYaml(dashboard) {
  var json = JSON.stringify(dashboard, null, 2);
  // Indent each line by 4 spaces
  return json
    .split('\n')
    .map((line) => `    ${line}`)
    .join('\n');
}

/**
 * Format raw k8s.json content (already indented with 4 spaces from existing file).
 * Re-apply Helm escaping to be safe.
 */
function formatK8sForYaml(rawContent) {
  // The k8s content is already indented and Helm-escaped from the existing file.
  // Just return it as-is, trimming trailing whitespace.
  return rawContent.replace(/\s+$/, '');
}

// ---- Main ----

console.log('Syncing Helm Grafana dashboards ConfigMap...');

// 1. Extract k8s.json from existing ConfigMap
console.log('  Extracting k8s.json from existing ConfigMap...');
var k8sRawContent = extractK8sDashboard();

// 2. Read and process Docker dashboards
var dashboardYamlBlocks = {};

for (var name of DASHBOARD_ORDER) {
  if (name === 'k8s') {
    // Preserved from existing file
    dashboardYamlBlocks[name] = formatK8sForYaml(k8sRawContent);
    console.log(`  Preserved: k8s.json (from existing ConfigMap)`);
    continue;
  }

  console.log(`  Processing: ${name}.json`);
  var dashboard = readDockerDashboard(name);

  // Apply Spring Boot transformations
  if (SPRING_BOOT_DASHBOARDS.includes(name)) {
    dashboard = addPodFilter(dashboard, name);
    dashboard = addPodTemplating(dashboard, name);
    dashboard = addCadvisorPanels(dashboard);
    console.log(`    -> Added pod filter, templating, and cAdvisor panels`);
  }

  // Format as JSON and apply Helm escaping
  var jsonStr = JSON.stringify(dashboard, null, 2);
  var escaped = escapeHelmMustache(jsonStr);

  // Indent by 4 spaces for YAML block scalar
  var indented = escaped
    .split('\n')
    .map((line) => `    ${line}`)
    .join('\n');

  dashboardYamlBlocks[name] = indented;
}

// 3. Assemble the ConfigMap
var yamlHeader = `apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ include "grafana.fullname" . }}-dashboards
  labels:
    {{- include "grafana.labels" . | nindent 4 }}
data:`;

var yamlParts = [yamlHeader];

for (var name of DASHBOARD_ORDER) {
  yamlParts.push(`  ${name}.json: |`);
  yamlParts.push(dashboardYamlBlocks[name]);
}

var output = yamlParts.join('\n') + '\n';

// 4. Write the output
writeFileSync(helmConfigMapPath, output, 'utf-8');
console.log(`\nWrote: ${helmConfigMapPath}`);
console.log(`Total dashboards: ${DASHBOARD_ORDER.length} (tempo-tracing removed, k8s preserved)`);
