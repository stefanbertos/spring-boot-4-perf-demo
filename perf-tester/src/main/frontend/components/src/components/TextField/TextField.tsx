import MuiTextField from '@mui/material/TextField';
import type { TextFieldProps as MuiTextFieldProps } from '@mui/material/TextField';

export type TextFieldProps = MuiTextFieldProps;

export default function TextField(props: TextFieldProps) {
  return <MuiTextField {...props} />;
}
