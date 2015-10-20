package com.idega.jackrabbit.security;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import org.apache.jackrabbit.api.security.user.AuthorizableExistsException;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.jackrabbit.core.security.authentication.DefaultLoginModule;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.core.accesscontrol.business.LoginDBHandler;
import com.idega.core.accesscontrol.business.StandardRoles;
import com.idega.core.accesscontrol.data.LoginTable;
import com.idega.jackrabbit.JackrabbitConstants;
import com.idega.presentation.IWContext;
import com.idega.util.CoreUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

/**
 * Class, responsible for handling logins into repository.
 *
 * @author valdas
 *
 */
public class RepositoryLoginModule extends DefaultLoginModule {

	@Autowired
	private JackrabbitSecurityHelper securityHelper;

	private UserManager userManager;

	private Map<String, String> ids = new HashMap<String, String>();

	JackrabbitSecurityHelper getSecurityHelper() {
		if (securityHelper == null) {
			ELUtil.getInstance().autowire(this);
		}
		return securityHelper;
	}

	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
		if (adminId == null || SecurityConstants.ADMIN_ID.equals(adminId)) {
			try {
				adminId = getSecurityHelper().getSuperAdminId();
			} catch (RepositoryException e) {
				e.printStackTrace();
			}
		}

		super.initialize(subject, callbackHandler, sharedState, options);
	}

	@Override
	protected void doInit(CallbackHandler callbackHandler, Session session,	@SuppressWarnings("rawtypes") Map options) throws LoginException {
		super.doInit(callbackHandler, session, options);

		try {
            userManager = ((SessionImpl) session).getUserManager();
        } catch (RepositoryException e) {
            throw new LoginException("Unable to initialize LoginModule: " + e.getMessage());
        }
	}

	private boolean createLogin(String user, String password) {
		try {
			return userManager.createUser(user, password) != null;
		} catch (AuthorizableExistsException e) {
			e.printStackTrace();
			return true;
		} catch (RepositoryException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean login() throws LoginException {
		Credentials credentials = getCredentials();
		if (!(credentials instanceof SimpleCredentials)) {
			return super.login();
		}

		Principal principal = getPrincipal(credentials);
		if (principal == null) {
			SimpleCredentials cred = (SimpleCredentials) credentials;
			if (!createLogin(getUserID(credentials), String.valueOf(cred.getPassword()))) {
				return false;
			}
		}

		return super.login();
	}

	@Override
	protected boolean authenticate(Principal principal, Credentials credentials) throws FailedLoginException, RepositoryException {
		String userId = getUserID(credentials);
		if (StringUtil.isEmpty(userId)) {
			return super.authenticate(principal, credentials);
		}

		if (userId.equals(getAdminId())) {
			return true;	//	Administrator user has all rights
		}

		if (credentials instanceof SimpleCredentials) {
			credentials = new SimpleCredentials(userId, ((SimpleCredentials) credentials).getPassword());
		}

		try {
			IWContext iwc = CoreUtil.getIWContext();
			if (iwc != null && iwc.isLoggedOn()) {
				if (iwc.hasRole(StandardRoles.ROLE_KEY_ADMIN) || iwc.hasRole(StandardRoles.ROLE_KEY_EDITOR) || iwc.hasRole(StandardRoles.ROLE_KEY_AUTHOR)) {
					return true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return super.authenticate(principal, credentials);
	}

	@Override
	protected String getUserID(Credentials credentials) {
		if (!(credentials instanceof SimpleCredentials)) {
			return super.getUserID(credentials);
		}

		SimpleCredentials cred = (SimpleCredentials) credentials;
		String id = cred.getUserID();
		if (id == null) {
			return super.getUserID(credentials);
		}

		String key = id + String.valueOf(cred.getPassword());
		String userId = ids.get(key);
		if (userId != null) {
			return userId;
		}

		boolean realId = false;
		Object realIdAttr = cred.getAttribute(JackrabbitConstants.REAL_USER_ID_USED);
		if (realIdAttr instanceof String) {
			realId = Boolean.valueOf((String) realIdAttr);
		}

		if (!realId) {
			try {
				LoginTable loginTable = LoginDBHandler.getUserLoginByUserName(id);
				userId = loginTable == null ? null : String.valueOf(loginTable.getUserId());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (userId == null) {
			userId = super.getUserID(credentials);
		}

		if (userId != null) {
			ids.put(key, userId);
		}

		return userId;
	}

}