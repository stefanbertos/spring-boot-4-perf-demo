export interface TestRunResponse {
  id: number;
  testId: string;
  status: string;
  messageCount: number;
  completedCount: number;
  failedCount: number;
  startTime: string;
  endTime: string | null;
  durationMs: number | null;
  tps: number | null;
  avgLatencyMs: number | null;
  minLatencyMs: number | null;
  maxLatencyMs: number | null;
}

export interface TestRunSummaryResponse {
  id: number;
  testId: string;
  status: string;
  totalMessages: number;
  sentCount: number;
  receivedCount: number;
  failedCount: number;
  timeoutCount: number;
  startTime: string;
  endTime: string | null;
  duration: string | null;
  tps: number | null;
  avgLatencyMs: number | null;
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
