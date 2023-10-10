package com.idega.jackrabbit.security;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.security.AMContext;
import org.apache.jackrabbit.core.security.DefaultAccessManager;
import org.apache.jackrabbit.core.security.authorization.AccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.WorkspaceAccessManager;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.idega.core.accesscontrol.business.AccessController;
import com.idega.core.accesscontrol.business.LoginBusinessBean;
import com.idega.core.accesscontrol.business.LoginSession;
import com.idega.core.accesscontrol.dao.PermissionDAO;
import com.idega.core.accesscontrol.data.bean.ICPermission;
import com.idega.core.accesscontrol.jaas.IWUserPrincipal;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWMainApplicationSettings;
import com.idega.presentation.IWContext;
import com.idega.repository.access.RepositoryPrivilege;
import com.idega.user.data.bean.User;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.datastructures.map.MapUtil;
import com.idega.util.expression.ELUtil;

@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class RepositoryAccessManager extends DefaultAccessManager implements com.idega.repository.access.RepositoryAccessManager {

	private static final Logger LOGGER = Logger.getLogger(RepositoryAccessManager.class.getName());

	@Autowired
	private JackrabbitSecurityHelper securityHelper;

	private JackrabbitSecurityHelper getSecurityHelper() {
		if (securityHelper == null) {
			ELUtil.getInstance().autowire(this);
		}
		return securityHelper;
	}

	@Override
	public void init(AMContext amContext, AccessControlProvider acProvider, WorkspaceAccessManager wspAccessManager) throws AccessDeniedException, Exception {
		super.init(amContext, acProvider, wspAccessManager);
	}

	@Override
	public boolean isGranted(Path absPath, int permissions) throws RepositoryException {
		return true;
	}

	@Override
	public boolean isGranted(ItemId id, int actions) throws ItemNotFoundException, RepositoryException {
		return super.isGranted(id, actions);
    }

	@Override
	public boolean hasPermission(User user, String path, String privilegeName) {
		return hasPermission(user, path, new RepositoryPrivilege(privilegeName));
	}

	@Override
	public boolean hasPermission(User user, String path, Privilege privilege) {
		if (user == null || StringUtil.isEmpty(path) || privilege == null)
			return Boolean.FALSE;

		try {
			LoginSession loginSession = LoginBusinessBean.getLoginSessionBean();
			if (loginSession.getUserEntity().getId().intValue() != user.getId().intValue())
				return Boolean.FALSE;

			String loginName = LoginBusinessBean.getLoginSessionBean().getLoggedOnInfo().getLogin();
			Set<Principal> principals = new HashSet<>(Arrays.asList(new IWUserPrincipal(loginName)));
			Privilege[] privileges = getPrivileges(path, principals);
			if (ArrayUtil.isEmpty(privileges))
				return Boolean.FALSE;

			for (Privilege privilegeByPrincipal: privileges) {
				if (privilegeByPrincipal.getName().equals(privilege.getName()))
					return Boolean.TRUE;
			}
		} catch (PathNotFoundException e) {
			e.printStackTrace();
		} catch (RepositoryException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return Boolean.FALSE;
	}

	@Override
	public boolean hasPermission(IWContext iwc, String path) throws Exception {
		if (iwc == null || StringUtil.isEmpty(path)) {
			throw new DavException(DavServletResponse.SC_FORBIDDEN, "Invalid parameters, resource " + path + " is not accessible");
		}

		String themes = CoreConstants.CONTENT_PATH + "/themes/";
		boolean allowAll =	path.startsWith(CoreConstants.WEBDAV_SERVLET_URI.concat(CoreConstants.PUBLIC_PATH)) || path.startsWith(CoreConstants.PUBLIC_PATH) ||
							path.startsWith(CoreConstants.WEBDAV_SERVLET_URI.concat(themes)) || path.startsWith(themes) ||
							path.startsWith(CoreConstants.WEBDAV_SERVLET_URI.concat("/undefined/"));

		IWMainApplication iwma = iwc.getIWMainApplication();
		IWMainApplicationSettings settings = iwma.getSettings();
		if (!allowAll) {
			String accessGrantedProp = settings.getProperty("repository.granted");
			if (!StringUtil.isEmpty(accessGrantedProp)) {
				List<String> accessGrantedToPaths = StringUtil.getValuesFromString(accessGrantedProp, CoreConstants.COMMA);
				if (!ListUtil.isEmpty(accessGrantedToPaths)) {
					for (Iterator<String> iter = accessGrantedToPaths.iterator(); (iter.hasNext() && !allowAll);) {
						allowAll = path.startsWith(CoreConstants.WEBDAV_SERVLET_URI.concat(iter.next()));
					}
				}
			}
		}

		if (!allowAll) {
			AccessController accessController = iwma.getAccessController();

			//	Need to resolve access rights
			User user = getSecurityHelper().getCurrentUser(iwc.getRequest().getSession(false));
			if (user == null) {
				user = iwc == null || !iwc.isLoggedOn() ? null : iwc.getLoggedInUser();
			}
			if (user == null) {
				throw new DavException(DavServletResponse.SC_FORBIDDEN, "User is not logged in, resource " + path + " is not accessible");
			}

			if (!getSecurityHelper().isSuperAdmin(user)) {
				if (settings.getBoolean("jackrabbit_dav_check_permissions", Boolean.FALSE)) {
					LOGGER.info("Resolve access rights for resource: " + path + " and user " + user);
					Set<String> userRoles = accessController.getAllRolesForUser(user);
					if (ListUtil.isEmpty(userRoles)) {
						LOGGER.warning("User " + user + " does not have any roles!");
						throw new DavException(DavServletResponse.SC_FORBIDDEN);
					}

					PermissionDAO permissionDAO = ELUtil.getInstance().getBean(PermissionDAO.class);
					List<ICPermission> permissions = permissionDAO.findPermissions(path, new ArrayList<>(userRoles));
					if (ListUtil.isEmpty(permissions)) {
						LOGGER.warning("User " + user + " does not have permission to read " + path);
						throw new DavException(DavServletResponse.SC_FORBIDDEN);
					}
				}

				WebApplicationContext appContext = WebApplicationContextUtils.getWebApplicationContext(iwc.getServletContext());
				Map<String, RepositoryItemAccessManager> managers = appContext.getBeansOfType(RepositoryItemAccessManager.class);
				if (!MapUtil.isEmpty(managers)) {
					boolean allowed = false;
					for (Iterator<RepositoryItemAccessManager> iter = managers.values().iterator(); (iter.hasNext() && !allowed);) {
						allowed = iter.next().hasPermission(iwc, path, user);
					}
					return allowed;
				}
			}
		}

		return true;
	}

}