import { App as AntdApp, ConfigProvider } from 'antd';
import viVN from 'antd/locale/vi_VN';
import { RouterProvider } from 'react-router-dom';
import { router } from './router.jsx';
import { antdTheme } from '../config/theme.js';
import { AuthProvider } from '../contexts/AuthContext.jsx';

export default function App() {
  return (
    <ConfigProvider locale={viVN} theme={antdTheme}>
      <AntdApp>
        <AuthProvider>
          <RouterProvider router={router} />
        </AuthProvider>
      </AntdApp>
    </ConfigProvider>
  );
}
