package com.idega.jackrabbit.security;

import java.util.logging.Logger;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.DefaultSecurityManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.user.UserManagerImpl;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.util.expression.ELUtil;

/**
 *
 * @author valdas
 *
 */
public class RepositorySecurityManager extends DefaultSecurityManager {

	@Autowired
	private JackrabbitSecurityHelper securityHelper;

	private UserManager userManager;

	public RepositorySecurityManager() {
		super();
	}

	JackrabbitSecurityHelper getSecurityHelper() {
		if (securityHelper == null) {
			ELUtil.getInstance().autowire(this);
		}
		return securityHelper;
	}

	@Override
	public UserManager getUserManager(Session session) throws RepositoryException {
		if (userManager == null) {
			userManager = new UserManagerImpl((SessionImpl) session, getSecurityHelper().getSuperAdminId());
		}
		return userManager;
	}

	@Override
	public void init(Repository repository, Session systemSession) throws RepositoryException {
		adminId = getSecurityHelper().getSuperAdminId();
		Logger.getLogger(getClass().getName()).info("Admin ID: " + adminId);
		super.init(repository, systemSession);
	}
}