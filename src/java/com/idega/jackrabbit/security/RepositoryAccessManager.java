package com.idega.jackrabbit.security;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.security.AMContext;
import org.apache.jackrabbit.core.security.DefaultAccessManager;
import org.apache.jackrabbit.core.security.authorization.AccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.WorkspaceAccessManager;

public class RepositoryAccessManager extends DefaultAccessManager {

//	private boolean system, anonymous;

	@Override
	public void init(AMContext amContext, AccessControlProvider acProvider,
            WorkspaceAccessManager wspAccessManager) throws AccessDeniedException, Exception {
		super.init(amContext, acProvider, wspAccessManager);
	}

	@Override
	public boolean isGranted(ItemId id, int actions) throws ItemNotFoundException, RepositoryException {
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
}