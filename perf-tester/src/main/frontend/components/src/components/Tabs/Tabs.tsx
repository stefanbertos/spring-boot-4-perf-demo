import Box from '@mui/material/Box';
import MuiTab from '@mui/material/Tab';
import MuiTabs from '@mui/material/Tabs';
import type { ReactNode, SyntheticEvent } from 'react';
import { useState } from 'react';

export interface TabItem {
  label: string;
  content: ReactNode;
  disabled?: boolean;
}

export interface TabsProps {
  tabs: TabItem[];
  defaultIndex?: number;
}

export default function Tabs({ tabs, defaultIndex = 0 }: TabsProps) {
  const [value, setValue] = useState(defaultIndex);

  const handleChange = (_: SyntheticEvent, newValue: number) => {
    setValue(newValue);
  };

  return (
    <Box>
      <MuiTabs value={value} onChange={handleChange} sx={{ borderBottom: 1, borderColor: 'divider' }}>
        {tabs.map((tab, index) => (
          <MuiTab key={index} label={tab.label} disabled={tab.disabled} />
        ))}
      </MuiTabs>
      {tabs.map((tab, index) => (
        <Box key={index} role="tabpanel" hidden={value !== index} sx={{ pt: 3 }}>
          {value === index && tab.content}
        </Box>
      ))}
    </Box>
  );
}
