package com.idega.jackrabbit;

import java.security.Principal;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;
import javax.security.auth.Subject;

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.security.AMContext;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.security.authorization.AccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.WorkspaceAccessManager;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdegaWebSimpleAccessManager implements AccessManager {


    private static Logger log = LoggerFactory.getLogger(IdegaWebSimpleAccessManager.class);

    /**
     * Subject whose access rights this AccessManager should reflect
     */
    protected Subject subject;

    /**
     * hierarchy manager used for ACL-based access control model
     */
    protected HierarchyManager hierMgr;

    private boolean initialized;

    protected boolean system;
    protected boolean anonymous;

    /**
     * Empty constructor
     */
    public IdegaWebSimpleAccessManager() {
        initialized = false;
        anonymous = false;
        system = false;
    }

    //--------------------------------------------------------< AccessManager >
    /**
     * {@inheritDoc}
     */
    public void init(AMContext context, AccessControlProvider arg1, WorkspaceAccessManager arg2) throws AccessDeniedException, Exception {
    	if (initialized) {
            throw new IllegalStateException("already initialized");
        }

        subject = context.getSubject();
        hierMgr = context.getHierarchyManager();
        Set<Principal> ps = subject.getPrincipals();
        
        /*
        Properties rolemaps = new Properties();
        String rolemaploc = context.getHomeDir() + "/rolemappings.properties";
        FileInputStream rolefs = new FileInputStream(rolemaploc);
        rolemaps.load(rolefs);
        rolefs.close();
        log.info("Load jbossgroup role mappings from " + rolemaploc);
        
        for (Principal p : ps){
//        	log.warn(p.getName());
        	if (p.getName().equalsIgnoreCase("Roles")){
//        		log.warn("listing roles:");
//        		log.warn(p.getClass().toString());
        		org.jboss.security.SimpleGroup sg = (org.jboss.security.SimpleGroup)p;
        		Enumeration<org.jboss.security.SimplePrincipal> em = sg.members();
        		while (em.hasMoreElements()) {
        			org.jboss.security.SimplePrincipal myp = em.nextElement();
        			String role = rolemaps.getProperty(myp.getName());
        			
        			if (role != null && role.equalsIgnoreCase("full")){
        				system = true;
        			}else if (role != null && role.equalsIgnoreCase("read")){
        				anonymous = true;
        			}
				}
        	}
        }
        
        */


        // @todo check permission to access given workspace based on principals
        initialized = true;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void close() throws Exception {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        initialized = false;
    }

    /**
     * {@inheritDoc}
     */
    public void checkPermission(ItemId id, int permissions)
            throws AccessDeniedException, ItemNotFoundException,
            RepositoryException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        if (system) {
            // system has always all permissions
            return;
        } else if (anonymous) {
            // anonymous is always denied WRITE & REMOVE permissions
            if ((permissions & WRITE) == WRITE
                    || (permissions & REMOVE) == REMOVE) {
                throw new AccessDeniedException();
            }
        }else{
        	//no permissions
            throw new AccessDeniedException();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isGranted(ItemId id, int permissions)
            throws ItemNotFoundException, RepositoryException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        if (system) {
            // system has always all permissions
            return true;
        } else if (anonymous) {
            // anonymous is always denied WRITE & REMOVE premissions
            if ((permissions & WRITE) == WRITE
                    || (permissions & REMOVE) == REMOVE) {
                return false;
            }else{
            	return true;
            }
        }
        //default to false
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean canAccess(String workspaceName)
            throws NoSuchWorkspaceException, RepositoryException {
    	
    	if (system || anonymous) return true;
    	
    	return false;
    }

	public boolean canRead(Path arg0) throws RepositoryException {
		// TODO Auto-generated method stub
		return false;
	}


	public boolean isGranted(Path arg0, int arg1) throws RepositoryException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isGranted(Path arg0, Name arg1, int arg2) throws RepositoryException {
		// TODO Auto-generated method stub
		return false;
	}

	public void init(AMContext context) throws AccessDeniedException, Exception {
		init(context,null,null);
		
	}
}
