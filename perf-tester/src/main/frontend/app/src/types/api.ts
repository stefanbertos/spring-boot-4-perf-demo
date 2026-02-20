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
  durationMs: number | null;
  startedAt: string;
  completedAt: string | null;
  zipFilePath: string | null;
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
  updatedAt: string;
}

export interface TestCaseDetail {
  id: number;
  name: string;
  message: string;
  createdAt: string;
  updatedAt: string;
}
