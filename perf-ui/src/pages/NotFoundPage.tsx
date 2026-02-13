import Button from '@mui/material/Button';
import Typography from '@mui/material/Typography';
import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  return (
    <div className="flex h-full flex-col items-center justify-center gap-4">
      <Typography variant="h1" className="text-gray-300 font-bold">
        404
      </Typography>
      <Typography variant="h5" color="text.secondary">
        Page not found
      </Typography>
      <Button component={Link} to="/" variant="contained">
        Go to Dashboard
      </Button>
    </div>
  );
}
