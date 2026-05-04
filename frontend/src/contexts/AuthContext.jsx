import { createContext, useMemo } from 'react';
import { useAuth } from 'react-oidc-context';

export const AuthContext = createContext(null);

export function AuthProviderWrapper({ children }) {
  const auth = useAuth();

  const login = () => {
    return auth.signinRedirect();
  };

  const logout = () => {
    return auth.signoutRedirect();
  };

  const currentUser = useMemo(() => {
    if (!auth.isAuthenticated || !auth.user) return null;

    const token = auth.user.access_token;
    let roles = [];
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      roles = payload.realm_access?.roles || [];
    } catch (e) {
      console.error("Failed to parse token payload", e);
    }

    const profile = auth.user.profile;
    return {
      ...profile,
      id: profile.sub,
      studentId: profile.preferred_username, // MSSV is stored here in Keycloak
      email: profile.email,
      name: profile.name || profile.preferred_username,
      roles: roles,
      role: roles.includes('ORGANIZER') ? 'ORGANIZER' : roles.includes('STAFF') ? 'STAFF' : 'STUDENT'
    };
  }, [auth.isAuthenticated, auth.user]);

  const hasRole = (allowedRoles) => {
    if (!currentUser) return false;
    const roles = Array.isArray(allowedRoles) ? allowedRoles : [allowedRoles];
    return currentUser.roles.some(role => roles.includes(role)) || roles.includes(currentUser.role);
  };

  const value = useMemo(
    () => ({
      currentUser,
      login,
      logout,
      isAuthenticated: auth.isAuthenticated,
      hasRole,
      isLoading: auth.isLoading,
      error: auth.error
    }),
    [currentUser, auth.isAuthenticated, auth.isLoading, auth.error]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// We rename the export because App.jsx expects AuthProvider. We'll wrap the inner provider in App.jsx or here.
// Actually, let's export this as AuthProvider for now and in App.jsx we'll add the OIDC AuthProvider.
export const AuthProvider = AuthProviderWrapper;
