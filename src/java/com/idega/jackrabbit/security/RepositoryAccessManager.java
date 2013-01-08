package com.idega.jackrabbit.security;

import java.security.Principal;
import java.util.Arrays;
import java.util.HashSet;
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

import com.idega.core.accesscontrol.business.LoginBusinessBean;
import com.idega.core.accesscontrol.business.LoginSession;
import com.idega.core.accesscontrol.jaas.IWUserPrincipal;
import com.idega.repository.access.RepositoryPrivilege;
import com.idega.user.data.bean.User;
import com.idega.util.ArrayUtil;
import com.idega.util.StringUtil;

public class RepositoryAccessManager extends DefaultAccessManager implements com.idega.repository.access.RepositoryAccessManager {

	private static final Logger LOGGER = Logger.getLogger(RepositoryAccessManager.class.getName());

//	private boolean system, anonymous;

	@Override
	public void init(AMContext amContext, AccessControlProvider acProvider,
            WorkspaceAccessManager wspAccessManager) throws AccessDeniedException, Exception {
		super.init(amContext, acProvider, wspAccessManager);
	}

	@Override
	public boolean isGranted(Path absPath, int permissions) throws RepositoryException {
		checkInitialized();

		LOGGER.warning("Not implemented! Path: " + absPath.getNormalizedPath() + ", permissions: " + permissions);
		return true;
	}

	@Override
	public boolean isGranted(ItemId id, int actions) throws ItemNotFoundException, RepositoryException {
    	/*checkInitialized();

    	//	TODO: implement
    	LOGGER.warning("Not implemented! ItemId: " + id + ", actions: " + actions);
    	return true;*/

		return super.isGranted(id, actions);

    	/*checkInitialized();

        if (system) {
            // system has always all permissions
            return true;
        } else if (anonymous) {
            // anonymous is always denied WRITE & REMOVE permissions
            if ((permissions & WRITE) == WRITE || (permissions & REMOVE) == REMOVE) {
                return false;
            }else{
            	return true;
            }
        }
        //default to false
        return false;*/
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
			if (loginSession.getUser().getId().intValue() != user.getId().intValue())
				return Boolean.FALSE;

			String loginName = LoginBusinessBean.getLoginSessionBean().getLoggedOnInfo().getLogin();
			Set<Principal> principals = new HashSet<Principal>(Arrays.asList(new IWUserPrincipal(loginName)));
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
}