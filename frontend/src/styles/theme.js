export const palette = {
  primary: '#1769e0',
  primaryDark: '#0f2f5f',
  bgLayout: '#f5f7fb',
  surface: '#ffffff',
  border: '#e4eaf3',
  text: '#172033',
  muted: '#667085',
  success: '#16a34a',
  warning: '#d97706',
  danger: '#dc2626',
  purple: '#7c3aed',
};

export const antdTheme = {
  token: {
    colorPrimary: palette.primary,
    colorInfo: palette.primary,
    colorSuccess: palette.success,
    colorWarning: palette.warning,
    colorError: palette.danger,
    colorBgLayout: palette.bgLayout,
    colorBgContainer: palette.surface,
    colorTextBase: palette.text,
    colorBorderSecondary: palette.border,
    borderRadius: 10,
    boxShadowSecondary: '0 12px 34px rgba(15, 47, 95, 0.08)',
    fontFamily:
      "Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
  },
  components: {
    Layout: {
      headerBg: palette.surface,
      siderBg: '#102a4c',
      bodyBg: palette.bgLayout,
    },
    Menu: {
      itemSelectedBg: '#e8f2ff',
      itemSelectedColor: palette.primary,
      darkItemBg: '#102a4c',
      darkSubMenuItemBg: '#102a4c',
      darkItemSelectedBg: '#1d6fe8',
    },
    Card: {
      headerBg: palette.surface,
      borderRadiusLG: 14,
    },
    Table: {
      headerBg: '#f8fbff',
      headerColor: '#334155',
      rowHoverBg: '#f7fbff',
    },
    Tag: {
      borderRadiusSM: 999,
    },
    Button: {
      borderRadius: 10,
      controlHeightLG: 44,
    },
    Input: {
      borderRadius: 10,
    },
    Select: {
      borderRadius: 10,
    },
  },
};
