package com.idega.jackrabbit.authorization;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;

import org.apache.jackrabbit.core.config.LoginModuleConfig;
import org.apache.jackrabbit.core.security.authentication.LocalAuthContext;

public class AuthorizationContext extends LocalAuthContext {

	protected AuthorizationContext(LoginModuleConfig config, CallbackHandler cbHandler, Subject subject) {
		super(config, cbHandler, subject);
	}

	@Override
	public void login() throws LoginException {
		super.login();
	}

}
