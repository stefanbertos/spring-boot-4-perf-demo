import MuiChip from '@mui/material/Chip';
import type { ChipProps as MuiChipProps } from '@mui/material/Chip';

export type ChipProps = MuiChipProps;

export default function Chip(props: ChipProps) {
  return <MuiChip {...props} />;
}
