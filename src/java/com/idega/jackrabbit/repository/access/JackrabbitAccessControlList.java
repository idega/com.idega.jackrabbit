package com.idega.jackrabbit.repository.access;

import java.security.Principal;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.Privilege;

import com.idega.repository.access.AccessControlList;

public class JackrabbitAccessControlList implements org.apache.jackrabbit.api.security.JackrabbitAccessControlList, AccessControlList {

	@Override
	public String getPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AccessControlEntry[] getAccessControlEntries()
			throws RepositoryException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean addAccessControlEntry(Principal principal,
			Privilege[] privileges) throws AccessControlException,
			RepositoryException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void removeAccessControlEntry(AccessControlEntry ace)
			throws AccessControlException, RepositoryException {
		// TODO Auto-generated method stub

	}

	@Override
	public String[] getRestrictionNames() throws RepositoryException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getRestrictionType(String restrictionName)
			throws RepositoryException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean addEntry(Principal principal, Privilege[] privileges,
			boolean isAllow) throws AccessControlException, RepositoryException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean addEntry(Principal principal, Privilege[] privileges,
			boolean isAllow, Map<String, Value> restrictions)
			throws AccessControlException, RepositoryException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void orderBefore(AccessControlEntry srcEntry,
			AccessControlEntry destEntry) throws AccessControlException,
			UnsupportedRepositoryOperationException, RepositoryException {
		// TODO Auto-generated method stub

	}

	@Override
	public String getResourcePath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void add(com.idega.repository.access.AccessControlEntry ace) {
		// TODO Auto-generated method stub

	}

}