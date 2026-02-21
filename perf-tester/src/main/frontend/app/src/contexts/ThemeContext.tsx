import { themeBlue, themeDefault, themeRed } from 'perf-ui-components';
import type { AppTheme } from 'perf-ui-components';
import { createContext, useContext, useEffect, useState } from 'react';
import type { ReactNode } from 'react';

export type ThemeName = 'default' | 'blue' | 'red';

const THEMES: Record<ThemeName, AppTheme> = {
  default: themeDefault,
  blue: themeBlue,
  red: themeRed,
};

function applyNavColors(navColors: { bg: string; active: string; hover: string }) {
  document.documentElement.style.setProperty('--nav-bg', navColors.bg);
  document.documentElement.style.setProperty('--nav-active', navColors.active);
  document.documentElement.style.setProperty('--nav-hover', navColors.hover);
}

interface ThemeContextValue {
  themeName: ThemeName;
  activeTheme: AppTheme;
  setThemeName: (name: ThemeName) => void;
}

const ThemeContext = createContext<ThemeContextValue | null>(null);

export function ThemeContextProvider({ children }: { children: ReactNode }) {
  const [themeName, setThemeNameState] = useState<ThemeName>(() => {
    const saved = localStorage.getItem('ui-theme') as ThemeName | null;
    const initial: ThemeName = saved && saved in THEMES ? saved : 'default';
    // Apply synchronously to avoid a flash of the wrong sidebar colour on load
    applyNavColors(THEMES[initial].navColors);
    return initial;
  });

  const activeTheme = THEMES[themeName];

  const setThemeName = (name: ThemeName) => {
    setThemeNameState(name);
    localStorage.setItem('ui-theme', name);
  };

  // Keep CSS vars in sync whenever the theme changes
  useEffect(() => {
    applyNavColors(activeTheme.navColors);
  }, [activeTheme]);

  return (
    <ThemeContext.Provider value={{ themeName, activeTheme, setThemeName }}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useThemeContext() {
  const ctx = useContext(ThemeContext);
  if (!ctx) {
    throw new Error('useThemeContext must be used inside ThemeContextProvider');
  }
  return ctx;
}
