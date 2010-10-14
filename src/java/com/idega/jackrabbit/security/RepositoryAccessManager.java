package com.idega.jackrabbit.security;

import java.security.Principal;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.security.JackrabbitAccessControlPolicy;
import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.security.AMContext;
import org.apache.jackrabbit.core.security.DefaultAccessManager;
import org.apache.jackrabbit.core.security.authorization.AccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.core.security.authorization.WorkspaceAccessManager;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;

public class RepositoryAccessManager extends DefaultAccessManager {

	@Override
	public void init(AMContext amContext) throws AccessDeniedException, Exception {
		// TODO Auto-generated method stub
		super.init(amContext);
	}

	@Override
	public void init(AMContext amContext, AccessControlProvider acProvider, WorkspaceAccessManager wspAccessManager) throws AccessDeniedException, Exception {
		// TODO Auto-generated method stub
		super.init(amContext, acProvider, wspAccessManager);
	}

	@Override
	public void checkPermission(ItemId id, int permissions) throws AccessDeniedException, ItemNotFoundException, RepositoryException {
		// TODO Auto-generated method stub
		super.checkPermission(id, permissions);
	}

	@Override
	public void checkPermission(Path absPath, int permissions)
			throws AccessDeniedException, RepositoryException {
		// TODO Auto-generated method stub
		super.checkPermission(absPath, permissions);
	}

	@Override
	public boolean isGranted(ItemId id, int actions)
			throws ItemNotFoundException, RepositoryException {
		// TODO Auto-generated method stub
		return super.isGranted(id, actions);
	}

	@Override
	public boolean isGranted(Path absPath, int permissions)
			throws RepositoryException {
		// TODO Auto-generated method stub
		return super.isGranted(absPath, permissions);
	}

	@Override
	public boolean isGranted(Path parentPath, Name childName, int permissions)
			throws RepositoryException {
		// TODO Auto-generated method stub
		return super.isGranted(parentPath, childName, permissions);
	}

	@Override
	public boolean canRead(Path itemPath) throws RepositoryException {
		// TODO Auto-generated method stub
		return super.canRead(itemPath);
	}

	@Override
	public boolean canAccess(String workspaceName) throws RepositoryException {
		// TODO Auto-generated method stub
		return super.canAccess(workspaceName);
	}

	@Override
	public boolean hasPrivileges(String absPath, Privilege[] privileges)
			throws PathNotFoundException, RepositoryException {
		// TODO Auto-generated method stub
		return super.hasPrivileges(absPath, privileges);
	}

	@Override
	public Privilege[] getPrivileges(String absPath)
			throws PathNotFoundException, RepositoryException {
		// TODO Auto-generated method stub
		return super.getPrivileges(absPath);
	}

	@Override
	public AccessControlPolicy[] getPolicies(String absPath)
			throws PathNotFoundException, AccessDeniedException,
			RepositoryException {
		// TODO Auto-generated method stub
		return super.getPolicies(absPath);
	}

	@Override
	public AccessControlPolicy[] getEffectivePolicies(String absPath)
			throws PathNotFoundException, AccessDeniedException,
			RepositoryException {
		// TODO Auto-generated method stub
		return super.getEffectivePolicies(absPath);
	}

	@Override
	public AccessControlPolicyIterator getApplicablePolicies(String absPath)
			throws PathNotFoundException, AccessDeniedException,
			RepositoryException {
		// TODO Auto-generated method stub
		return super.getApplicablePolicies(absPath);
	}

	@Override
	public void setPolicy(String absPath, AccessControlPolicy policy)
			throws PathNotFoundException, AccessControlException,
			AccessDeniedException, RepositoryException {
		// TODO Auto-generated method stub
		super.setPolicy(absPath, policy);
	}

	@Override
	public void removePolicy(String absPath, AccessControlPolicy policy)
			throws PathNotFoundException, AccessControlException,
			AccessDeniedException, RepositoryException {
		// TODO Auto-generated method stub
		super.removePolicy(absPath, policy);
	}

	@Override
	public JackrabbitAccessControlPolicy[] getApplicablePolicies(
			Principal principal) throws AccessDeniedException,
			AccessControlException, UnsupportedRepositoryOperationException,
			RepositoryException {
		// TODO Auto-generated method stub
		return super.getApplicablePolicies(principal);
	}

	@Override
	public JackrabbitAccessControlPolicy[] getPolicies(Principal principal)
			throws AccessDeniedException, AccessControlException,
			UnsupportedRepositoryOperationException, RepositoryException {
		// TODO Auto-generated method stub
		return super.getPolicies(principal);
	}

	@Override
	public boolean hasPrivileges(String absPath, Set<Principal> principals,
			Privilege[] privileges) throws PathNotFoundException,
			RepositoryException {
		// TODO Auto-generated method stub
		return super.hasPrivileges(absPath, principals, privileges);
	}

	@Override
	public Privilege[] getPrivileges(String absPath, Set<Principal> principals)
			throws PathNotFoundException, RepositoryException {
		// TODO Auto-generated method stub
		return super.getPrivileges(absPath, principals);
	}

	@Override
	protected void checkInitialized() {
		// TODO Auto-generated method stub
		super.checkInitialized();
	}

	@Override
	protected void checkValidNodePath(String absPath)
			throws PathNotFoundException, RepositoryException {
		// TODO Auto-generated method stub
		super.checkValidNodePath(absPath);
	}

	@Override
	protected void checkPermission(String absPath, int permission)
			throws AccessDeniedException, RepositoryException {
		// TODO Auto-generated method stub
		super.checkPermission(absPath, permission);
	}

	@Override
	protected PrivilegeRegistry getPrivilegeRegistry()
			throws RepositoryException {
		// TODO Auto-generated method stub
		return super.getPrivilegeRegistry();
	}

}
