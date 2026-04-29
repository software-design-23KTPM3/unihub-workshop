import { Outlet } from 'react-router-dom';
import AppLogo from '../components/common/AppLogo.jsx';

export default function AuthLayout() {
  return (
    <main className="auth-layout">
      <section className="auth-layout__panel">
        <AppLogo />
        <Outlet />
      </section>
    </main>
  );
}
