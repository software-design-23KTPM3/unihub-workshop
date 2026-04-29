export const oidcConfig = {
  authority: "http://localhost:8080/realms/unihub",
  client_id: "unihub-client",
  redirect_uri: window.location.origin + window.location.pathname,
  post_logout_redirect_uri: window.location.origin + window.location.pathname,
  response_type: "code",
  scope: "openid profile email",
  onSigninCallback: (_user) => {
    window.history.replaceState(
      {},
      document.title,
      window.location.pathname
    );
  }
};
