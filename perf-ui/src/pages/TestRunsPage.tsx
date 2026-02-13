import { PageHeader } from '@/components';

export default function TestRunsPage() {
  return (
    <div>
      <PageHeader
        title="Test Runs"
        subtitle="Recent performance test results"
      />
      <p className="mt-4 text-gray-500">
        Test run list will go here.
      </p>
    </div>
  );
}
