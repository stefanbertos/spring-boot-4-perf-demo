import MuiMenu from '@mui/material/Menu';
import type { MenuProps as MuiMenuProps } from '@mui/material/Menu';
import MuiMenuItem from '@mui/material/MenuItem';
import type { MenuItemProps as MuiMenuItemProps } from '@mui/material/MenuItem';

export type MenuProps = MuiMenuProps;
export type MenuItemProps = MuiMenuItemProps;

export function Menu(props: MenuProps) {
  return <MuiMenu {...props} />;
}

export function MenuItem(props: MenuItemProps) {
  return <MuiMenuItem {...props} />;
}
