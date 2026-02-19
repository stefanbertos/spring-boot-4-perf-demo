import MuiCard from '@mui/material/Card';
import type { CardProps as MuiCardProps } from '@mui/material/Card';
import MuiCardContent from '@mui/material/CardContent';
import type { ReactNode } from 'react';

export interface CardProps extends Omit<MuiCardProps, 'children'> {
  children: ReactNode;
  noPadding?: boolean;
}

export default function Card({ children, noPadding = false, ...props }: CardProps) {
  return (
    <MuiCard {...props}>
      {noPadding ? children : <MuiCardContent>{children}</MuiCardContent>}
    </MuiCard>
  );
}
