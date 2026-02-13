import { PageHeader } from '@/components';

export default function AdminPage() {
  return (
    <div>
      <PageHeader
        title="Admin"
        subtitle="Logging, Kafka, and IBM MQ administration"
      />
      <p className="mt-4 text-gray-500">
        Admin controls will go here.
      </p>
    </div>
  );
}
