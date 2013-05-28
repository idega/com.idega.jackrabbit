package com.idega.jackrabbit.security;

import java.util.logging.Level;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.core.accesscontrol.business.AccessController;
import com.idega.core.accesscontrol.business.LoginBusinessBean;
import com.idega.core.accesscontrol.business.LoginSession;
import com.idega.core.business.DefaultSpringBean;
import com.idega.idegaweb.IWMainApplication;
import com.idega.user.data.bean.User;
import com.idega.util.expression.ELUtil;

/**
 * Helper class for common methods related with security
 *
 * @author valdas
 *
 */
@Service(JackrabbitSecurityHelper.BEAN_NAME)
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class JackrabbitSecurityHelper extends DefaultSpringBean {

	public static final String BEAN_NAME = "jackrabbitSecurityHelper";

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
			superAdminId = admin == null ? null : String.valueOf(admin.getId());
		}
		return superAdminId;
	}

	public boolean isLoggedOn(HttpSession session) {
		if (session == null)
			return false;

		try {
			LoginBusinessBean loginBusiness = LoginBusinessBean.getLoginBusinessBean(session);
			return loginBusiness.isLoggedOn(session);
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error while resolving if user is logged into session " + session.getId(), e);
		}

		return false;
	}

	public User getCurrentUser(HttpSession session) {
		if (!isLoggedOn(session))
			return null;

		try {
			LoginSession loginSession = ELUtil.getInstance().getBean(LoginSession.class);
			return loginSession.getUserEntity();
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error getting current user from session " + session.getId(), e);
		}

		return null;
	}

	public boolean isSuperAdmin(User user) {
		if (user == null)
			return false;

		try {
			return getSuperAdminId().equals(String.valueOf(user.getId()));
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error while resolving if user (" + user + ", ID: " + user.getId() + ") is super admin", e);
		}

		return false;
	}

}