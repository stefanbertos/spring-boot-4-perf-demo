import MuiIconButton from '@mui/material/IconButton';
import type { IconButtonProps as MuiIconButtonProps } from '@mui/material/IconButton';

export type IconButtonProps = MuiIconButtonProps;

export default function IconButton(props: IconButtonProps) {
  return <MuiIconButton {...props} />;
}
