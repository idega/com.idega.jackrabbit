package com.idega.jackrabbit.repository;

import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.jackrabbit.server.SessionProvider;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.jackrabbit.security.JackrabbitSecurityHelper;
import com.idega.repository.RepositoryService;
import com.idega.user.data.bean.User;
import com.idega.util.expression.ELUtil;

public class IdegaSessionProvider implements SessionProvider {

	@Autowired
	private JackrabbitSecurityHelper securityHelper;

	private JackrabbitSecurityHelper getJackrabbitSecurityHelper() {
		if (securityHelper == null)
			ELUtil.getInstance().autowire(this);
		return securityHelper;
	}

	private RepositoryService getRepositoryService() {
		return ELUtil.getInstance().getBean(RepositoryService.class);
	}

	@Override
	public Session getSession(HttpServletRequest request, Repository rep, String workspace) throws LoginException, ServletException, RepositoryException {
		User user = getJackrabbitSecurityHelper().getSuperAdmin();	//	TODO: resolve when to use super admin and regular user
		return getRepositoryService().getSession(user);
	}

	@Override
	public void releaseSession(Session session) {
		if (session != null && session.isLive())
			session.logout();
	}

}