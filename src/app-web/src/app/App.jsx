import { App as AntdApp, ConfigProvider } from 'antd';
import viVN from 'antd/locale/vi_VN';
import { RouterProvider } from 'react-router-dom';
import { AuthProvider as OidcProvider } from 'react-oidc-context';
import { router } from './router.jsx';
import { antdTheme } from '../config/theme.js';
import { AuthProvider } from '../contexts/AuthContext.jsx';
import { oidcConfig } from '../config/oidc.js';

export default function App() {
  return (
    <ConfigProvider locale={viVN} theme={antdTheme}>
      <AntdApp>
        <OidcProvider {...oidcConfig}>
          <AuthProvider>
            <RouterProvider router={router} />
          </AuthProvider>
        </OidcProvider>
      </AntdApp>
    </ConfigProvider>
  );
}
