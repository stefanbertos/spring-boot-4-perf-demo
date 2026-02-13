import { PageHeader } from '@/components';

export default function SendTestPage() {
  return (
    <div>
      <PageHeader
        title="Send Test"
        subtitle="Configure and run a performance test"
      />
      <p className="mt-4 text-gray-500">
        Test configuration form will go here.
      </p>
    </div>
  );
}
