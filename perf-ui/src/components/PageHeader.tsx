import Typography from '@mui/material/Typography';

interface PageHeaderProps {
  title: string;
  subtitle?: string;
}

export default function PageHeader({ title, subtitle }: PageHeaderProps) {
  return (
    <div className="mb-6">
      <Typography variant="h4" component="h1" fontWeight="bold">
        {title}
      </Typography>
      {subtitle && (
        <Typography variant="body1" color="text.secondary" className="mt-1">
          {subtitle}
        </Typography>
      )}
    </div>
  );
}
