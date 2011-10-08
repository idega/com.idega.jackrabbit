package com.idega.jackrabbit.repository.access;

import java.security.Principal;

import javax.jcr.RepositoryException;
import javax.jcr.ValueFactory;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.core.security.authorization.AccessControlEntryImpl;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;

import com.idega.repository.access.AccessControlEntry;

public class JackrabbitAccessControlEntry extends AccessControlEntryImpl implements AccessControlEntry {

	private javax.jcr.security.AccessControlEntry ace;

	private boolean negative, protectedAce, inherited;

	private int principalType;

	private String inheritedFrom;

	public JackrabbitAccessControlEntry(javax.jcr.security.AccessControlEntry ace) throws AccessControlException, RepositoryException {
		this(ace.getPrincipal(), ace.getPrivileges());

		this.ace = ace;
	}

	public JackrabbitAccessControlEntry(Principal principal, boolean negative, boolean protectedAce, boolean inherited, String inheritedFrom, int principalType)
		throws AccessControlException, RepositoryException {

		this(principal, null);

		this.negative = negative;
		this.protectedAce = protectedAce;
		this.inherited = inherited;
		this.inheritedFrom = inheritedFrom;
		this.principalType = principalType;
	}

	public JackrabbitAccessControlEntry(Principal principal, Privilege[] privileges) throws AccessControlException, RepositoryException {
		super(principal, privileges);
	}

	@Override
	protected NameResolver getResolver() {
		// TODO implement!
		return null;
	}

	@Override
	protected ValueFactory getValueFactory() {
		// TODO implement!
		return null;
	}

	@Override
	public int getPrincipalType() {
		return principalType;
	}

	@Override
	public boolean isNegative() {
		return negative;
	}

	@Override
	public void setInherited(boolean inherited) {
		this.inherited = inherited;
	}

	@Override
	public boolean isInherited() {
		return inherited;
	}

	@Override
	public void setInheritedFrom(String inheritedFrom) {
		this.inheritedFrom = inheritedFrom;
	}

	@Override
	public String getInheritedFrom() {
		return inheritedFrom;
	}

	@Override
	public void addPrivilege(Privilege privilege) {
		// TODO Auto-generated method stub

	}

	@Override
	public void clearPrivileges() {
		// TODO Auto-generated method stub

	}

}