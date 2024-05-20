package org.apache.jackrabbit.core;

import java.security.Principal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.security.AccessControlException;
import javax.security.auth.Subject;

import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
//import org.apache.jackrabbit.core.DefaultSecurityManager.WorkspaceAccessManagerImpl;
import org.apache.jackrabbit.core.config.AccessManagerConfig;
import org.apache.jackrabbit.core.config.LoginModuleConfig;
import org.apache.jackrabbit.core.config.SecurityConfig;
import org.apache.jackrabbit.core.config.SecurityManagerConfig;
import org.apache.jackrabbit.core.config.UserManagerConfig;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.config.WorkspaceSecurityConfig;
import org.apache.jackrabbit.core.security.AMContext;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.security.DefaultAccessManager;
import org.apache.jackrabbit.core.security.JackrabbitSecurityManager;
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.jackrabbit.core.security.SystemPrincipal;
import org.apache.jackrabbit.core.security.authentication.AuthContext;
import org.apache.jackrabbit.core.security.authentication.AuthContextProvider;
import org.apache.jackrabbit.core.security.authorization.AccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.AccessControlProviderFactory;
import org.apache.jackrabbit.core.security.authorization.AccessControlProviderFactoryImpl;
import org.apache.jackrabbit.core.security.authorization.WorkspaceAccessManager;
import org.apache.jackrabbit.core.security.principal.AdminPrincipal;
import org.apache.jackrabbit.core.security.principal.DefaultPrincipalProvider;
import org.apache.jackrabbit.core.security.principal.PrincipalManagerImpl;
import org.apache.jackrabbit.core.security.principal.PrincipalProvider;
import org.apache.jackrabbit.core.security.principal.PrincipalProviderRegistry;
import org.apache.jackrabbit.core.security.principal.ProviderRegistryImpl;
import org.apache.jackrabbit.core.security.user.MembershipCache;
import org.apache.jackrabbit.core.security.user.UserManagerImpl;
import org.apache.jackrabbit.core.security.user.action.AuthorizableAction;

public class IdegaDefaultSecurityManager extends DefaultSecurityManager {

	/**
     * the default logger
     */
    private static final Logger log = java.util.logging.Logger.getLogger(IdegaDefaultSecurityManager.class.getName());

    /**
     * Flag indicating if the security manager was properly initialized.
     */
    private boolean initialized;

    /**
     * the repository implementation
     */
    private RepositoryImpl repository;

    /**
     * System session.
     */
    private SystemSession systemSession;

    /**
     * System user manager. Implementation needed here for the DefaultPrincipalProvider.
     */
    private UserManager systemUserManager;

    /**
     * The user id of the administrator. The value is retrieved from
     * configuration. If the config entry is missing a default id is used (see
     * {@link SecurityConstants#ADMIN_ID}).
     */
    protected String adminId;

    /**
     * The user id of the anonymous user. The value is retrieved from
     * configuration. If the config entry is missing a default id is used (see
     * {@link SecurityConstants#ANONYMOUS_ID}).
     */
    protected String anonymousId;

    /**
     * Contains the access control providers per workspace.
     * key = name of the workspace,
     * value = {@link AccessControlProvider}
     */
    private final Map<String, AccessControlProvider> acProviders = new HashMap<>();

    /**
     * the AccessControlProviderFactory
     */
    private AccessControlProviderFactory acProviderFactory;

    /**
     * the configured WorkspaceAccessManager
     */
    private WorkspaceAccessManager workspaceAccessManager;

    /**
     * the principal provider registry
     */
    private PrincipalProviderRegistry principalProviderRegistry;

    /**
     * factory for login-context {@see Repository#login())
     */
    private AuthContextProvider authContextProvider;

    //------------------------------------------< JackrabbitSecurityManager >---
    /**
     * @see JackrabbitSecurityManager#init(Repository, Session)
     */
    @Override
	public synchronized void init(Repository repository, Session systemSession) throws RepositoryException {
        if (initialized) {
            throw new IllegalStateException("already initialized");
        }
        if (!(repository instanceof RepositoryImpl)) {
            throw new RepositoryException("RepositoryImpl expected");
        }
        if (!(systemSession instanceof SystemSession)) {
            throw new RepositoryException("SystemSession expected");
        }

        this.systemSession = (SystemSession) systemSession;
        this.repository = (RepositoryImpl) repository;

        SecurityConfig config = this.repository.getConfig().getSecurityConfig();
        LoginModuleConfig loginModConf = config.getLoginModuleConfig();

        // build AuthContextProvider based on appName + optional LoginModuleConfig
        authContextProvider = new AuthContextProvider(config.getAppName(), loginModConf);
        if (authContextProvider.isLocal()) {
            log.info("init: use Repository Login-Configuration for " + config.getAppName());
        } else if (authContextProvider.isJAAS()) {
            log.info("init: use JAAS login-configuration for " + config.getAppName());
        } else {
            String msg = "Neither JAAS nor RepositoryConfig contained a valid configuration for " + config.getAppName();
            log.warning(msg);
            throw new RepositoryException(msg);
        }

        Properties[] moduleConfig = authContextProvider.getModuleConfig();

        // retrieve default-ids (admin and anonymous) from login-module-configuration.
        for (Properties props : moduleConfig) {
            if (props.containsKey(LoginModuleConfig.PARAM_ADMIN_ID)) {
                adminId = props.getProperty(LoginModuleConfig.PARAM_ADMIN_ID);
            }
            if (props.containsKey(LoginModuleConfig.PARAM_ANONYMOUS_ID)) {
                anonymousId = props.getProperty(LoginModuleConfig.PARAM_ANONYMOUS_ID);
            }
        }
        // fallback:
        if (adminId == null) {
            log.info("No adminID defined in LoginModule/JAAS config -> using default.");
            adminId = SecurityConstants.ADMIN_ID;
        }
        if (anonymousId == null) {
            log.info("No anonymousID defined in LoginModule/JAAS config -> using default.");
            anonymousId = SecurityConstants.ANONYMOUS_ID;
        }

        // create the system userManager and make sure the system-users exist.
        systemUserManager = createUserManager(this.systemSession);
        try {
        	createSystemUsers(systemUserManager, this.systemSession, adminId, anonymousId);
        } catch (Throwable t) {
        	log.log(Level.WARNING, "Error creating system users. Admin ID: " + adminId + ", anonymous ID: " + anonymousId, t);
        }

        // init default ac-provider-factory
        acProviderFactory = new AccessControlProviderFactoryImpl();
        acProviderFactory.init(this.systemSession);

        // create the workspace access manager
        SecurityManagerConfig smc = config.getSecurityManagerConfig();
        if (smc != null && smc.getWorkspaceAccessConfig() != null) {
            workspaceAccessManager =
                smc.getWorkspaceAccessConfig().newInstance(WorkspaceAccessManager.class);
        } else {
            // fallback -> the default implementation
            log.info("No WorkspaceAccessManager configured; using default.");
            workspaceAccessManager = createDefaultWorkspaceAccessManager();
        }
        workspaceAccessManager.init(this.systemSession);

        // initialize principal-provider registry
        // 1) create default
        PrincipalProvider defaultPP = createDefaultPrincipalProvider();
        // 2) create registry instance
        principalProviderRegistry = new ProviderRegistryImpl(defaultPP);
        // 3) register all configured principal providers.
        for (Properties props : moduleConfig) {
            principalProviderRegistry.registerProvider(props);
        }

        initialized = true;
    }

    /**
     * @see JackrabbitSecurityManager#dispose(String)
     */
    @Override
	public void dispose(String workspaceName) {
        checkInitialized();
        synchronized (acProviders) {
            AccessControlProvider prov = acProviders.remove(workspaceName);
            if (prov != null) {
                prov.close();
            }
        }
    }

    /**
     * @see JackrabbitSecurityManager#close()
     */
    @Override
	public void close() {
        checkInitialized();
        synchronized (acProviders) {
            for (AccessControlProvider accessControlProvider : acProviders.values()) {
                accessControlProvider.close();
            }
            acProviders.clear();
        }
    }

    /**
     * @see JackrabbitSecurityManager#getAccessManager(Session,AMContext)
     */
    @Override
	public AccessManager getAccessManager(Session session, AMContext amContext) throws RepositoryException {
        checkInitialized();
        AccessManagerConfig amConfig = repository.getConfig().getSecurityConfig().getAccessManagerConfig();
        try {
            String wspName = session.getWorkspace().getName();
            AccessControlProvider pp = getAccessControlProvider(wspName);
            AccessManager accessMgr;
            if (amConfig == null) {
                log.info("No configuration entry for AccessManager. Using org.apache.jackrabbit.core.security.DefaultAccessManager");
                accessMgr = new DefaultAccessManager();
            } else {
                accessMgr = amConfig.newInstance(AccessManager.class);
            }

            accessMgr.init(amContext, pp, workspaceAccessManager);
            return accessMgr;
        } catch (AccessDeniedException e) {
            // re-throw
            throw e;
        } catch (Exception e) {
            // wrap in RepositoryException
            String clsName = (amConfig == null) ? "-- missing access manager configuration --" : amConfig.getClassName();
            String msg = "Failed to instantiate AccessManager (" + clsName + ")";
            log.log(Level.WARNING, msg, e);
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * @see JackrabbitSecurityManager#getPrincipalManager(Session)
     */
    @Override
	public PrincipalManager getPrincipalManager(Session session) throws RepositoryException {
        checkInitialized();
        if (session instanceof SessionImpl) {
            SessionImpl sImpl = (SessionImpl) session;
            return createPrincipalManager(sImpl);
        } else {
            throw new RepositoryException("Internal error: SessionImpl expected.");
        }
    }

    /**
     * @see JackrabbitSecurityManager#getUserManager(Session)
     */
    @Override
	public UserManager getUserManager(Session session) throws RepositoryException {
        checkInitialized();
        if (session == systemSession) {
            return systemUserManager;
        } else if (session instanceof SessionImpl) {
            String workspaceName = systemSession.getWorkspace().getName();
            try {
                SessionImpl sImpl = (SessionImpl) session;
                UserManagerImpl uMgr;
                if (workspaceName.equals(sImpl.getWorkspace().getName())) {
                    uMgr = createUserManager(sImpl);
                } else {
                    SessionImpl s = (SessionImpl) sImpl.createSession(workspaceName);
                    uMgr = createUserManager(s);
                    sImpl.addListener(uMgr);
                }
                return uMgr;
            } catch (NoSuchWorkspaceException e) {
                throw new AccessControlException("Cannot build UserManager for " + session.getUserID(), e);
            }
        } else {
            throw new RepositoryException("Internal error: SessionImpl expected.");
        }
    }

    /**
     * @see JackrabbitSecurityManager#getUserID(javax.security.auth.Subject, String)
     */
    @Override
	public String getUserID(Subject subject, String workspaceName) throws RepositoryException {
        checkInitialized();

        // shortcut if the subject contains the AdminPrincipal or
        // SystemPrincipal in which cases the userID is already known.
        if (!subject.getPrincipals(AdminPrincipal.class).isEmpty()) {
            return adminId;
        } else if (!subject.getPrincipals(SystemPrincipal.class).isEmpty()) {
            // system session does not have a userId
            return null;
        }

        /* if there is a configure principal class that should be used to
           determine the UserID -> try this one. */
        Class cl = getConfig().getUserIdClass();
        if (cl != null) {
            Set<Principal> s = subject.getPrincipals(cl);
            if (!s.isEmpty()) {
                for (Principal p : s) {
                    if (!(p instanceof java.security.acl.Group)) {
                        return p.getName();
                    }
                }
                // all principals found with the given p-Class were Group principals
                log.info("Only Group principals found with class '" + cl.getName() + "' -> Not used for UserID.");
            } else {
                log.info("No principal found with class '" + cl.getName() + "'.");
            }
        }

        /*
         Fallback scenario to retrieve userID from the subject:
         Since the subject may contain multiple principals and the principal
         name may not be equals to the UserID, the id is retrieved by
         searching for the corresponding authorizable and if this doesn't
         succeed an attempt is made to obtained it from the login-credentials.
        */
        String uid = null;

        // first try to retrieve an authorizable corresponding to
        // a non-group principal. the first one present is used
        // to determine the userID.
        try {
            UserManager umgr = getSystemUserManager(workspaceName);
            for (Principal p : subject.getPrincipals()) {
                if (!(p instanceof Group)) {
                    Authorizable authorz = umgr.getAuthorizable(p);
                    if (authorz != null && !authorz.isGroup()) {
                        uid = authorz.getID();
                        break;
                    }
                }
            }
        } catch (RepositoryException e) {
            // failed to access userid via user manager -> use fallback 2.
            log.log(Level.WARNING, "Unexpected error while retrieving UserID.", e);
        }

        // 2. if no matching user is found try simple access to userID over
        // SimpleCredentials.
        if (uid == null) {
            Iterator<SimpleCredentials> creds = subject.getPublicCredentials(
                    SimpleCredentials.class).iterator();
            if (creds.hasNext()) {
                SimpleCredentials sc = creds.next();
                uid = sc.getUserID();
            }
        }

        return uid;
    }

    /**
     * Creates an AuthContext for the given {@link Credentials} and
     * {@link Subject}. The workspace name is ignored and users are
     * stored and retrieved from a specific (separate) workspace.<br>
     * This includes selection of application specific LoginModules and
     * initialization with credentials and Session to System-Workspace
     *
     * @return an {@link AuthContext} for the given Credentials, Subject
     * @throws RepositoryException in other exceptional repository states
     */
    @Override
	public AuthContext getAuthContext(Credentials creds, Subject subject, String workspaceName)
            throws RepositoryException {
        checkInitialized();
        return getAuthContextProvider().getAuthContext(creds, subject, systemSession,
                getPrincipalProviderRegistry(), adminId, anonymousId);
    }

    //----------------------------------------------------------< protected >---
    /**
     * @return The <code>SecurityManagerConfig</code> configured for the
     * repository this manager has been created for.
     */
    @Override
	protected SecurityManagerConfig getConfig() {
        return repository.getConfig().getSecurityConfig().getSecurityManagerConfig();
    }

    /**
     * @param workspaceName The name of the target workspace.
     * @return The system user manager. Since this implementation stores users
     * in a dedicated workspace the system user manager is the same for all
     * sessions irrespective of the workspace.
     * @throws javax.jcr.RepositoryException If an error occurs.
     */
    @Override
	protected UserManager getSystemUserManager(String workspaceName) throws RepositoryException {
        return systemUserManager;
    }

    /**
     * @param session The session for which to retrieve the membership cache.
     * @return The membership cache.
     * @throws RepositoryException If an error occurs.
     */
    @Override
	protected MembershipCache getMembershipCache(SessionImpl session) throws RepositoryException {
        if (session == systemSession || session instanceof SystemSession) {
            // force creation of the membership cache within the corresponding uMgr
            return null;
        } else {
            return ((UserManagerImpl) getSystemUserManager(session.getWorkspace().getName())).getMembershipCache();
        }
    }

    /**
     * Creates a {@link UserManagerImpl} for the given session. May be overridden
     * to return a custom implementation.
     *
     * @param session session
     * @return user manager
     * @throws RepositoryException if an error occurs
     */
    @Override
	protected UserManagerImpl createUserManager(SessionImpl session) throws RepositoryException {
        UserManagerConfig umc = getConfig().getUserManagerConfig();
        UserManagerImpl um;
        if (umc != null) {
            Class<?>[] paramTypes = new Class[] {
                    SessionImpl.class,
                    String.class,
                    Properties.class,
                    MembershipCache.class};
            um = (UserManagerImpl) umc.getUserManager(UserManagerImpl.class,
                    paramTypes, session, adminId, umc.getParameters(), getMembershipCache(session));
        } else {
            um = new UserManagerImpl(session, adminId, null, getMembershipCache(session));
        }

        if (umc != null && !(session instanceof SystemSession)) {
            AuthorizableAction[] actions = umc.getAuthorizableActions();
            um.setAuthorizableActions(actions);
        }
        return um;
    }

    /**
     * @param session The session used to create the principal manager.
     * @return A new instance of PrincipalManagerImpl
     * @throws javax.jcr.RepositoryException If an error occurs.
     */
    @Override
	protected PrincipalManager createPrincipalManager(SessionImpl session) throws RepositoryException {
        return new PrincipalManagerImpl(session, getPrincipalProviderRegistry().getProviders());
    }

    /**
     * @return A nwe instance of WorkspaceAccessManagerImpl to be used as
     * default workspace access manager if the configuration doesn't specify one.
     */
    @Override
	protected WorkspaceAccessManager createDefaultWorkspaceAccessManager() {
        return new WorkspaceAccessManagerImpl();
    }

    /**
     * Creates the default principal provider used to create the
     * {@link PrincipalProviderRegistry}.
     *
     * @return An new instance of <code>DefaultPrincipalProvider</code>.
     * @throws RepositoryException If an error occurs.
     */
    @Override
	protected PrincipalProvider createDefaultPrincipalProvider() throws RepositoryException {
        PrincipalProvider defaultPP = new DefaultPrincipalProvider(this.systemSession, (UserManagerImpl) systemUserManager);
        defaultPP.init(new Properties());
        return defaultPP;
    }

    /**
     * @return The PrincipalProviderRegistry created during initialization.
     */
    @Override
	protected PrincipalProviderRegistry getPrincipalProviderRegistry() {
        return principalProviderRegistry;
    }

    /**
     * @return The AuthContextProvider created during initialization.
     */
    @Override
	protected AuthContextProvider getAuthContextProvider() {
        return authContextProvider;
    }

    /**
     * Throws <code>IllegalStateException</code> if this manager hasn't been
     * initialized.
     */
    @Override
	protected void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("Not initialized");
        }
    }

    /**
     * @return The system session used to initialize this SecurityManager.
     */
    @Override
	protected Session getSystemSession() {
        return systemSession;
    }

    /**
     * @return The repository used to initialize this SecurityManager.
     */
    @Override
	protected Repository getRepository() {
        return repository;
    }
    //--------------------------------------------------------------------------
    /**
     * Returns the access control provider for the specified
     * <code>workspaceName</code>.
     *
     * @param workspaceName Name of the workspace.
     * @return access control provider
     * @throws NoSuchWorkspaceException If no workspace with 'workspaceName' exists.
     * @throws RepositoryException
     */
    private AccessControlProvider getAccessControlProvider(String workspaceName)
            throws NoSuchWorkspaceException, RepositoryException {
        checkInitialized();
        AccessControlProvider provider = acProviders.get(workspaceName);
        if (provider == null || !provider.isLive()) {
            // mark this workspace as 'active' so the workspace does not
            // get disposed by the workspace-janitor
            // TODO: There should be a cleaner way to do this.
            repository.markWorkspaceActive(workspaceName);

            WorkspaceSecurityConfig secConf = null;
            WorkspaceConfig conf =
                repository.getConfig().getWorkspaceConfig(workspaceName);
            if (conf != null) {
                secConf = conf.getSecurityConfig();
            }

            provider = acProviderFactory.createProvider(
                    repository.getSystemSession(workspaceName), secConf);
            synchronized (acProviders) {
                acProviders.put(workspaceName, provider);
            }
        }
        return provider;
    }

    /**
     * Make sure the system users (admin and anonymous) exist.
     *
     * @param userManager Manager to create users/groups.
     * @param session The editing session.
     * @param adminId UserID of the administrator.
     * @param anonymousId UserID of the anonymous user.
     * @throws RepositoryException If an error occurs.
     */
    static void createSystemUsers(UserManager userManager,
                                  SystemSession session,
                                  String adminId,
                                  String anonymousId) throws RepositoryException {

        Authorizable admin;
        if (adminId != null) {
            admin = userManager.getAuthorizable(adminId);
            if (admin == null) {
                userManager.createUser(adminId, adminId);
                if (!userManager.isAutoSave()) {
                    session.save();
                }
                log.info("... created admin-user with id \'" + adminId + "\' ...");
            }
        }

        if (anonymousId != null) {
            Authorizable anonymous = userManager.getAuthorizable(anonymousId);
            if (anonymous == null) {
                try {
                    userManager.createUser(anonymousId, "");
                    if (!userManager.isAutoSave()) {
                        session.save();
                    }
                    log.info("... created anonymous user with id \'" + anonymousId + "\' ...");
                } catch (RepositoryException e) {
                    // exception while creating the anonymous user.
                    // log an error but don't abort the repository start-up
                    log.log(Level.WARNING, "Failed to create anonymous user.", e);
                }
            }
        }
    }

    //------------------------------------------------------< inner classes >---
    /**
     * <code>WorkspaceAccessManager</code> that upon {@link #grants(Set principals, String)}
     * evaluates if access to the root node of a workspace with the specified
     * name is granted.
     */
    private final class WorkspaceAccessManagerImpl implements SecurityConstants, WorkspaceAccessManager {

        //-----------------------------------------< WorkspaceAccessManager >---
        /**
         * {@inheritDoc}
         */
        @Override
		public void init(Session systemSession) throws RepositoryException {
            // nothing to do here.
        }

        /**
         * {@inheritDoc}
         */
        @Override
		public void close() throws RepositoryException {
            // nothing to do here.
        }

        /**
         * {@inheritDoc}
         */
        @Override
		public boolean grants(Set<Principal> principals, String workspaceName) throws RepositoryException {
            AccessControlProvider prov = getAccessControlProvider(workspaceName);
            return prov.canAccessRoot(principals);
        }
    }

}