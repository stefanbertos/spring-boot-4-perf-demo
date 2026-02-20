import Box from '@mui/material/Box';
import { PageHeader, Tabs } from 'perf-ui-components';
import type { TabItem } from 'perf-ui-components';

const IFRAME_HEIGHT = 'calc(100vh - 200px)';

function IframePanel({ src, title }: { src: string; title: string }) {
  return (
    <Box
      component="iframe"
      src={src}
      title={title}
      sx={{
        width: '100%',
        height: IFRAME_HEIGHT,
        border: 'none',
        borderRadius: 1,
      }}
    />
  );
}

const tabs: TabItem[] = [
  {
    label: 'Grafana',
    content: <IframePanel src="/grafana" title="Grafana" />,
  },
  {
    label: 'Prometheus',
    content: <IframePanel src="/prometheus" title="Prometheus" />,
  },
  {
    label: 'Kafdrop',
    content: <IframePanel src="/kafdrop" title="Kafdrop" />,
  },
  {
    label: 'Redis Commander',
    content: <IframePanel src="/redis-commander" title="Redis Commander" />,
  },
  {
    label: 'Loki',
    content: (
      <IframePanel
        src="/grafana/explore?orgId=1&left=%7B%22datasource%22:%22loki%22%7D"
        title="Loki"
      />
    ),
  },
  {
    label: 'SonarQube',
    content: <IframePanel src="/sonar" title="SonarQube" />,
  },
  {
    label: 'IBM MQ',
    content: <IframePanel src="/ibmmq/console" title="IBM MQ Console" />,
  },
];

export default function DashboardsPage() {
  return (
    <Box>
      <PageHeader title="Dashboards" subtitle="Grafana, Prometheus, Kafdrop, Redis Commander, Loki, SonarQube, and IBM MQ" />
      <Tabs tabs={tabs} />
    </Box>
  );
}
