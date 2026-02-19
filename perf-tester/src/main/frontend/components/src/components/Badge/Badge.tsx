import MuiBadge from '@mui/material/Badge';
import type { BadgeProps as MuiBadgeProps } from '@mui/material/Badge';

export type BadgeProps = MuiBadgeProps;

export default function Badge(props: BadgeProps) {
  return <MuiBadge {...props} />;
}
