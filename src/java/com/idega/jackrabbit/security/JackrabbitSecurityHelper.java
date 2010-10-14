package com.idega.jackrabbit.security;

import javax.jcr.RepositoryException;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.core.accesscontrol.business.AccessController;
import com.idega.idegaweb.IWMainApplication;
import com.idega.user.data.User;

/**
 * Helper class for common methods related with security
 *
 * @author valdas
 *
 */
@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class JackrabbitSecurityHelper {

	private String superAdminId;

	public User getSuperAdmin() throws RepositoryException {
		AccessController accCtrl = IWMainApplication.getDefaultIWMainApplication().getAccessController();
		try {
			return accCtrl.getAdministratorUser();
		} catch (Exception e) {
			throw new RepositoryException(e);
		}
	}

	public String getSuperAdminId() throws RepositoryException {
		if (superAdminId == null) {
			User admin = getSuperAdmin();
			superAdminId = admin == null ? null : admin.getId();
		}
		return superAdminId;
	}

}