import React, { createContext, useContext, useState, ReactNode } from 'react';
import { Theme } from '@mui/material/styles';
import { lightTheme, darkTheme } from '../theme';

interface ThemeContextType {
  theme: Theme;
  isDarkMode: boolean;
  toggleTheme: () => void;
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

const THEME_STORAGE_KEY = 'mercado-solidario-theme';

function getInitialThemeMode(): boolean {
  try {
    const savedTheme = localStorage.getItem(THEME_STORAGE_KEY);
    if (savedTheme === 'dark') return true;
    if (savedTheme === 'light') return false;
  } catch (error) {
    console.warn('Não foi possível ler a preferência de tema salva.', error);
  }

  try {
    return window.matchMedia('(prefers-color-scheme: dark)').matches;
  } catch (error) {
    console.warn('Não foi possível consultar a preferência de tema do sistema.', error);
    return false;
  }
}

export const ThemeProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [isDarkMode, setIsDarkMode] = useState(getInitialThemeMode);

  const toggleTheme = () => {
    const newValue = !isDarkMode;
    setIsDarkMode(newValue);
    try {
      localStorage.setItem(THEME_STORAGE_KEY, newValue ? 'dark' : 'light');
    } catch (error) {
      console.warn('Não foi possível salvar a preferência de tema.', error);
    }
  };

  const theme = isDarkMode ? darkTheme : lightTheme;

  return (
    <ThemeContext.Provider value={{ theme, isDarkMode, toggleTheme }}>
      {children}
    </ThemeContext.Provider>
  );
};

export const useTheme = (): ThemeContextType => {
  const context = useContext(ThemeContext);
  if (context === undefined) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }
  return context;
};
