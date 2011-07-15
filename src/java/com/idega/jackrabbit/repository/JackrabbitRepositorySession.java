package com.idega.jackrabbit.repository;

import java.util.logging.Level;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.core.business.DefaultSpringBean;
import com.idega.repository.RepositorySession;
import com.idega.repository.access.AccessControlList;
import com.idega.user.data.bean.User;

@Service
@Scope("session")
public class JackrabbitRepositorySession extends DefaultSpringBean implements RepositorySession {

	private Session getRepositorySession() {
		User user = getCurrentUser();
		if (user == null) {
			getLogger().warning("User must be logged to work with JCR repository!");
			return null;
		}

		try {
			return getRepositoryService().getSession(user);
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error getting JCR session for user: " + user, e);
		}
		return null;
	}

	@Override
	public AccessControlList getAccessControlList(String path) throws RepositoryException {
		Session session = getRepositorySession();
		if (session == null)
			return null;

		try {
			//	FIXME: implement
			return null;
		} finally {
			session.logout();
		}
	}

	@Override
	public String getUserHomeFolder() throws RepositoryException {
		Session session = getRepositorySession();
		if (session == null)
			return null;

		try {
			return getRepositoryService().getUserHomeFolder(getCurrentUser());
		} finally {
			session.logout();
		}
	}

	@Override
	public boolean storeAccessControlList(AccessControlList acl) throws RepositoryException {
		Session session = getRepositorySession();
		if (session == null)
			return false;

		try {
			//	TODO: implement
			return false;
		} finally {
			session.logout();
		}
	}

}