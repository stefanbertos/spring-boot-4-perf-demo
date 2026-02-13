import { PageHeader } from '@/components';

export default function DashboardPage() {
  return (
    <div>
      <PageHeader
        title="Dashboard"
        subtitle="Performance testing overview"
      />
      <p className="mt-4 text-gray-500">
        Dashboard content will go here.
      </p>
    </div>
  );
}
