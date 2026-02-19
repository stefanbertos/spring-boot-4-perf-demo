import MuiLink from '@mui/material/Link';
import type { LinkProps as MuiLinkProps } from '@mui/material/Link';

export type LinkProps = MuiLinkProps;

export default function Link(props: LinkProps) {
  return <MuiLink {...props} />;
}
