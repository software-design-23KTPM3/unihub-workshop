import { createContext, useCallback, useMemo, useState } from 'react';
import * as authService from '../services/authService.js';

export const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [currentUser, setCurrentUser] = useState(() => authService.getCurrentUser());

  const login = useCallback(async (email, password) => {
    const user = await authService.login(email, password);
    setCurrentUser(user);
    return user;
  }, []);

  const logout = useCallback(() => {
    authService.logout();
    setCurrentUser(null);
  }, []);

  const hasRole = useCallback(
    (allowedRoles) => {
      const roles = Array.isArray(allowedRoles) ? allowedRoles : [allowedRoles];
      return Boolean(currentUser && roles.includes(currentUser.role));
    },
    [currentUser],
  );

  const value = useMemo(
    () => ({
      currentUser,
      login,
      logout,
      isAuthenticated: Boolean(currentUser),
      hasRole,
    }),
    [currentUser, hasRole, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
