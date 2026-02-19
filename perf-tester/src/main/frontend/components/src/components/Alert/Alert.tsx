import MuiAlert from '@mui/material/Alert';
import type { AlertProps as MuiAlertProps } from '@mui/material/Alert';

export type AlertProps = MuiAlertProps;

export default function Alert(props: AlertProps) {
  return <MuiAlert {...props} />;
}
