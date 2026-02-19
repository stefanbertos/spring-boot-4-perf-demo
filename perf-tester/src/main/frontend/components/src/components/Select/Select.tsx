import FormControl from '@mui/material/FormControl';
import InputLabel from '@mui/material/InputLabel';
import MuiMenuItem from '@mui/material/MenuItem';
import MuiSelect from '@mui/material/Select';
import type { SelectChangeEvent } from '@mui/material/Select';
import type { ReactNode } from 'react';

export interface SelectOption {
  value: string;
  label: string;
}

export interface SelectProps {
  label: string;
  value: string;
  options: SelectOption[];
  onChange: (event: SelectChangeEvent) => void;
  fullWidth?: boolean;
  size?: 'small' | 'medium';
  disabled?: boolean;
  required?: boolean;
  children?: ReactNode;
}

export default function Select({
  label,
  value,
  options,
  onChange,
  fullWidth = false,
  size = 'small',
  disabled = false,
  required = false,
}: SelectProps) {
  return (
    <FormControl fullWidth={fullWidth} size={size} disabled={disabled} required={required}>
      <InputLabel>{label}</InputLabel>
      <MuiSelect value={value} label={label} onChange={onChange}>
        {options.map((option) => (
          <MuiMenuItem key={option.value} value={option.value}>
            {option.label}
          </MuiMenuItem>
        ))}
      </MuiSelect>
    </FormControl>
  );
}

export type { SelectChangeEvent };
