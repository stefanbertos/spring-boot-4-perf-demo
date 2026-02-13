import { useParams } from 'react-router-dom';
import { PageHeader } from '@/components';

export default function TestRunDetailPage() {
  const { id } = useParams<{ id: string }>();

  return (
    <div>
      <PageHeader
        title={`Test Run #${id}`}
        subtitle="Summary, messages, and metrics"
      />
      <p className="mt-4 text-gray-500">
        Test run detail will go here.
      </p>
    </div>
  );
}
