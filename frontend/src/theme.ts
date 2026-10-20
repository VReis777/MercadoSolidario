import { createTheme } from '@mui/material/styles'

// Paleta da identidade visual (logo do Instagram):
// azul-marinho "Mercado", turquesa "solidário" e amarelo do círculo.
const paletteInstitucional = {
  azulProfundo: '#1B2A6B',
  azulMedio: '#2E4FA3',
  turquesa: '#19B8D2',
  amarelo: '#FFC42E',
  fundoClaro: '#FBF9F1',
  superficieBranca: '#FFFFFF',
}

const lightTheme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: paletteInstitucional.azulProfundo,
      dark: '#101C4A',
      light: paletteInstitucional.azulMedio,
      contrastText: '#ffffff',
    },
    secondary: {
      main: paletteInstitucional.turquesa,
      dark: '#0E93A8',
      light: '#5ED5E7',
      contrastText: '#1B2A6B',
    },
    background: {
      default: paletteInstitucional.fundoClaro,
      paper: paletteInstitucional.superficieBranca,
    },
    text: {
      primary: '#1B2A6B',
      secondary: '#4A5568',
    },
    divider: '#E2E8F0',
    success: {
      main: '#047857',
      light: '#10B981',
      dark: '#065F46',
    },
    warning: {
      main: '#B45309',
      light: paletteInstitucional.amarelo,
      dark: '#E0A800',
    },
    error: {
      main: '#DC2626',
      light: '#EF4444',
      dark: '#B91C1C',
    },
    action: {
      disabled: '#94A3B8',
      disabledBackground: '#E2E8F0',
    },
    info: {
      main: paletteInstitucional.azulMedio,
      light: '#3DA3C4',
      dark: '#1A5C7A',
    },
  },
  typography: {
    fontFamily: '"Nunito", "Roboto", "Helvetica", "Arial", sans-serif',
    h1: { fontWeight: 800 },
    h2: { fontWeight: 800 },
    h4: { fontWeight: 700 },
    h6: { fontWeight: 700 },
    button: { fontWeight: 700, textTransform: 'none' },
  },
  shape: {
    borderRadius: 12,
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: {
          minHeight: 44,
          paddingLeft: 20,
          paddingRight: 20,
        },
      },
    },
    MuiFab: {
      styleOverrides: {
        root: {
          minHeight: 56,
          minWidth: 56,
        },
      },
    },
  },
})

// Tema escuro mantido na paleta anterior (Azul Institucional + Turquesa).
const paletteDark = {
  azulProfundo: '#183B56',
  azulMedio: '#247BA0',
  turquesa: '#55B8A9',
}

const darkBaseTheme = createTheme({
  palette: {
    mode: 'dark',
    primary: {
      main: paletteDark.azulMedio,
      dark: paletteDark.azulProfundo,
      light: '#3A9AC0',
      contrastText: '#ffffff',
    },
    secondary: {
      main: paletteDark.turquesa,
      dark: '#3A9A8E',
      light: '#7BCFC2',
      contrastText: '#183B56',
    },
    background: {
      default: '#101C27',
      paper: '#192A38',
    },
    text: {
      primary: '#F4F7FA',
      secondary: '#B9C8D3',
    },
    divider: '#2D3E4E',
    success: {
      main: '#34D399',
      light: '#6EE7B7',
      dark: '#10B981',
    },
    warning: {
      main: '#FBBF24',
      light: '#FDE68A',
      dark: '#F59E0B',
    },
    error: {
      main: '#F87171',
      light: '#FCA5A5',
      dark: '#EF4444',
    },
    info: {
      main: paletteDark.azulMedio,
      light: '#4BBAD6',
      dark: '#1A5C7A',
    },
  },
  typography: {
    fontFamily: '"Nunito", "Roboto", "Helvetica", "Arial", sans-serif',
    h1: { fontWeight: 800 },
    h2: { fontWeight: 800 },
    h4: { fontWeight: 700 },
    h6: { fontWeight: 700 },
    button: { fontWeight: 700, textTransform: 'none' },
  },
  shape: {
    borderRadius: 12,
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: {
          minHeight: 44,
          paddingLeft: 20,
          paddingRight: 20,
        },
      },
    },
    MuiFab: {
      styleOverrides: {
        root: {
          minHeight: 56,
          minWidth: 56,
        },
      },
    },
  },
})

// Ajustes de contraste do tema escuro: navegação em azul profundo e
// textos/ícones primários sobre superfícies escuras em tom mais claro.
const darkPrimaryText = '#7CC3E0'

const darkTheme = createTheme(darkBaseTheme, {
  components: {
    MuiAppBar: {
      defaultProps: { enableColorOnDark: true },
      styleOverrides: {
        colorPrimary: {
          backgroundColor: paletteDark.azulProfundo,
          color: '#F4F7FA',
        },
      },
    },
    MuiButton: {
      styleOverrides: {
        textPrimary: { color: darkPrimaryText },
        outlinedPrimary: { color: darkPrimaryText, borderColor: darkPrimaryText },
      },
    },
    MuiIconButton: {
      styleOverrides: {
        colorPrimary: { color: darkPrimaryText },
      },
    },
    MuiLink: {
      styleOverrides: {
        root: { color: darkPrimaryText },
      },
    },
  },
})

export { lightTheme, darkTheme }
export default lightTheme
