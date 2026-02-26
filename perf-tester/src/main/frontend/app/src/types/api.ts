export interface TestRunResponse {
  id: number;
  testRunId: string;
  testId: string;
  status: string;
  messageCount: number;
  completedCount: number;
  tps: number | null;
  avgLatencyMs: number | null;
  minLatencyMs: number | null;
  maxLatencyMs: number | null;
  p25LatencyMs: number | null;
  p50LatencyMs: number | null;
  p75LatencyMs: number | null;
  p90LatencyMs: number | null;
  p95LatencyMs: number | null;
  p99LatencyMs: number | null;
  timeoutCount: number;
  testType: string | null;
  thresholdStatus: string | null;
  durationMs: number | null;
  startedAt: string;
  completedAt: string | null;
  zipFilePath: string | null;
}

export interface TestRunDetailResponse {
  id: number;
  testRunId: string;
  testId: string;
  status: string;
  messageCount: number;
  completedCount: number;
  tps: number | null;
  avgLatencyMs: number | null;
  minLatencyMs: number | null;
  maxLatencyMs: number | null;
  p25LatencyMs: number | null;
  p50LatencyMs: number | null;
  p75LatencyMs: number | null;
  p90LatencyMs: number | null;
  p95LatencyMs: number | null;
  p99LatencyMs: number | null;
  timeoutCount: number;
  testType: string | null;
  thresholdStatus: string | null;
  thresholdResults: string | null;
  durationMs: number | null;
  startedAt: string;
  completedAt: string | null;
  zipFilePath: string | null;
}

export interface TestRunSnapshotResponse {
  id: number;
  testRunId: number;
  sampledAt: string;
  outboundQueueDepth: number | null;
  inboundQueueDepth: number | null;
  kafkaRequestsLag: number | null;
  kafkaResponsesLag: number | null;
}

export interface LogEntry {
  timestamp: string;
  level: string;
  message: string;
}

export interface MessageResponse {
  id: number;
  messageId: string;
  correlationId: string;
  traceId: string | null;
  status: string;
  payloadSize: number | null;
  sentAt: string;
  receivedAt: string | null;
  latencyMs: number | null;
  errorMessage: string | null;
}

export interface MetricResponse {
  id: number;
  metricName: string;
  metricType: string;
  metricValue: number;
  metricUnit: string | null;
  tags: string | null;
  collectedAt: string;
}

export interface LogLevelResponse {
  loggerName: string;
  configuredLevel: string | null;
  effectiveLevel: string;
}

export interface TopicInfo {
  topicName: string;
  partitions: number;
}

export interface QueueInfo {
  queueName: string;
  currentDepth: number;
  maxDepth: number;
}

export interface TestStartResponse {
  id: number;
  testRunId: string;
}

export interface TestProgressEvent {
  testRunId: string;
  status: 'IDLE' | 'RUNNING' | 'COMPLETED' | 'TIMEOUT' | 'FAILED' | 'EXPORTING';
  sentCount: number;
  completedCount: number;
  totalCount: number;
  progressPercent: number;
  tps: number;
  avgLatencyMs: number;
  minLatencyMs: number;
  maxLatencyMs: number;
  elapsedSeconds: number;
}

export interface TestCaseSummary {
  id: number;
  name: string;
  headerTemplateId: number | null;
  headerTemplateName: string | null;
  responseTemplateId: number | null;
  responseTemplateName: string | null;
  updatedAt: string;
}

export interface TestCaseDetail {
  id: number;
  name: string;
  message: string;
  headerTemplateId: number | null;
  headerTemplateName: string | null;
  responseTemplateId: number | null;
  responseTemplateName: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface NamespaceInfo {
  name: string;
  phase: string;
}

export interface DeploymentInfo {
  name: string;
  namespace: string;
  desiredReplicas: number;
  readyReplicas: number;
}

export interface InfraProfileSummary {
  id: number;
  name: string;
  updatedAt: string;
}

export interface InfraProfileDetail {
  id: number;
  name: string;
  logLevels: Record<string, string>;
  kafkaTopics: Record<string, number>;
  kubernetesReplicas: Record<string, number>;
  ibmMqQueues: Record<string, number>;
  createdAt: string;
  updatedAt: string;
}

export interface InfraProfileRequest {
  name: string;
  logLevels: Record<string, string>;
  kafkaTopics: Record<string, number>;
  kubernetesReplicas: Record<string, number>;
  ibmMqQueues: Record<string, number>;
}

export interface ApplyResult {
  applied: string[];
  errors: string[];
}

export interface TestScenarioSummary {
  id: number;
  name: string;
  count: number;
  updatedAt: string;
}

export interface ScenarioEntryDto {
  id: number;
  testCaseId: number;
  testCaseName: string;
  percentage: number;
  displayOrder: number;
}

export interface ScenarioEntryRequest {
  testCaseId: number;
  percentage: number;
  displayOrder: number;
}

export interface ThresholdDef {
  metric: string;
  operator: string;
  value: number;
}

export interface ThresholdResult {
  metric: string;
  operator: string;
  threshold: number;
  actual: number;
  passed: boolean;
}

export interface ThinkTimeConfig {
  distribution: 'CONSTANT' | 'UNIFORM' | 'GAUSSIAN';
  minMs: number;
  maxMs: number;
  meanMs: number;
  stdDevMs: number;
}

export interface TestScenarioDetail {
  id: number;
  name: string;
  count: number;
  entries: ScenarioEntryDto[];
  scheduledEnabled: boolean;
  scheduledTime: string | null;
  warmupCount: number;
  testType: string | null;
  infraProfileId: number | null;
  thinkTime: ThinkTimeConfig | null;
  thresholds: ThresholdDef[];
  createdAt: string;
  updatedAt: string;
}

export interface TestScenarioRequest {
  name: string;
  count: number;
  entries: ScenarioEntryRequest[];
  scheduledEnabled: boolean;
  scheduledTime: string | null;
  warmupCount: number;
  testType: string | null;
  infraProfileId: number | null;
  thinkTime: ThinkTimeConfig | null;
  thresholds: ThresholdDef[];
}

export interface HeaderTemplateField {
  name: string;
  size: number;
  value: string;
  type: string | null;
  paddingChar: string | null;
  uuidPrefix: string | null;
  uuidSeparator: string | null;
  correlationKey: boolean;
}

export interface HeaderTemplateSummary {
  id: number;
  name: string;
  fieldCount: number;
  updatedAt: string;
}

export interface HeaderTemplateDetail {
  id: number;
  name: string;
  fields: HeaderTemplateField[];
  createdAt: string;
  updatedAt: string;
}

export interface HeaderTemplateRequest {
  name: string;
  fields: HeaderTemplateField[];
}

export interface ResponseTemplateField {
  name: string;
  size: number;
  value: string | null;
  type: string | null;
  paddingChar: string | null;
}

export interface ResponseTemplateSummary {
  id: number;
  name: string;
  fieldCount: number;
  updatedAt: string;
}

export interface ResponseTemplateDetail {
  id: number;
  name: string;
  fields: ResponseTemplateField[];
  createdAt: string;
  updatedAt: string;
}

export interface ResponseTemplateRequest {
  name: string;
  fields: ResponseTemplateField[];
}

export interface HealthCheckConfig {
  service: string;
  host: string;
  port: number;
  enabled: boolean;
  connectionTimeoutMs: number;
  intervalMs: number;
}

export interface HealthCheckConfigRequest {
  host: string;
  port: number;
  enabled: boolean;
  connectionTimeoutMs: number;
  intervalMs: number;
}

export interface DbExportQuery {
  id: number;
  name: string;
  sqlQuery: string;
  displayOrder: number;
}

export interface DbExportQueryRequest {
  name: string;
  sqlQuery: string;
  displayOrder: number;
}

export interface DashboardLink {
  label: string;
  url: string;
}

export interface RunTestPreferences {
  exportGrafana: boolean;
  exportPrometheus: boolean;
  exportKubernetes: boolean;
  exportLogs: boolean;
  exportDatabase: boolean;
  debug: boolean;
}
