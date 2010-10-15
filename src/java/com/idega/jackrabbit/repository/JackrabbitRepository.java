package com.idega.jackrabbit.repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;

import javax.jcr.Binary;
import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.value.ValueFactoryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.builder.bean.AdvancedProperty;
import com.idega.core.accesscontrol.business.LoginDBHandler;
import com.idega.core.accesscontrol.data.LoginTable;
import com.idega.core.business.DefaultSpringBean;
import com.idega.core.file.util.MimeTypeUtil;
import com.idega.idegaweb.IWMainApplicationShutdownEvent;
import com.idega.jackrabbit.JackrabbitConstants;
import com.idega.jackrabbit.security.JackrabbitSecurityHelper;
import com.idega.repository.RepositoryService;
import com.idega.repository.authentication.AuthenticationBusiness;
import com.idega.repository.bean.RepositoryItemVersionInfo;
import com.idega.user.data.User;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.IOUtil;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;

/**
 * Implementation of {@link RepositoryService}
 *
 * @author valdas
 *
 */

@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class JackrabbitRepository extends DefaultSpringBean implements RepositoryService, org.apache.jackrabbit.api.JackrabbitRepository, ApplicationListener {

	private Repository repository;

	@Autowired
	private JackrabbitSecurityHelper securityHelper;
	@Autowired
	private AuthenticationBusiness authenticationBusiness;

	@Override
	public void initializeRepository(InputStream configStream, String repositoryName) throws Exception {
		try {
			repository = RepositoryImpl.create(RepositoryConfig.create(configStream, repositoryName));
		} finally {
			IOUtil.close(configStream);
		}
	}

	@Override
	public String[] getDescriptorKeys() {
		return repository.getDescriptorKeys();
	}
	@Override
	public boolean isStandardDescriptor(String key) {
		return repository.isStandardDescriptor(key);
	}
	@Override
	public boolean isSingleValueDescriptor(String key) {
		return repository.isSingleValueDescriptor(key);
	}
	@Override
	public Value getDescriptorValue(String key) {
		return repository.getDescriptorValue(key);
	}
	@Override
	public Value[] getDescriptorValues(String key) {
		return repository.getDescriptorValues(key);
	}
	@Override
	public String getDescriptor(String key) {
		return repository.getDescriptor(key);
	}
	@Override
	public Session login(Credentials credentials, String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException {
		return repository.login(credentials, workspaceName);
	}
	@Override
	public Session login(Credentials credentials) throws LoginException, RepositoryException {
		return repository.login(credentials);
	}
	@Override
	public Session login(String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException {
		return repository.login(workspaceName);
	}
	@Override
	public Session login() throws LoginException, RepositoryException {
		return repository.login();
	}

	@Override
	public void shutdown() {
		if (repository instanceof org.apache.jackrabbit.api.JackrabbitRepository) {
			((org.apache.jackrabbit.api.JackrabbitRepository) repository).shutdown();
		}
	}
	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof IWMainApplicationShutdownEvent) {
			this.shutdown();
		}
	}

	@Override
	public boolean uploadFileAndCreateFoldersFromStringAsRoot(String parentPath, String fileName, String fileContentString,	String contentType)
		throws RepositoryException {

		try {
			return uploadFile(parentPath, fileName, StringHandler.getStreamFromString(fileContentString), contentType, null) != null;
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error uploading file: " + fileContentString + "\n to: " + parentPath + fileName, e);
		}
		return false;
	}
	@Override
	public boolean uploadXMLFileAndCreateFoldersFromStringAsRoot(String parentPath, String fileName, String fileContentString) throws RepositoryException {
		return uploadFileAndCreateFoldersFromStringAsRoot(parentPath, fileName, fileContentString, MimeTypeUtil.MIME_TYPE_XML);
	}
	@Override
	public boolean uploadFileAndCreateFoldersFromStringAsRoot(String parentPath, String fileName, InputStream stream, String contentType) throws RepositoryException {
		return uploadFile(parentPath, fileName, stream, contentType, null) != null;
	}
	@Override
	public boolean uploadFile(String uploadPath, String fileName, String contentType, InputStream fileInputStream) throws RepositoryException {
		return uploadFile(uploadPath, fileName, fileInputStream, contentType, getUser()) != null;
	}
	private Node uploadFile(String parentPath, String fileName, InputStream content, String mimeType, User user, AdvancedProperty... properties)
		throws RepositoryException {

		if (parentPath == null) {
			getLogger().warning("Parent path is not defined!");
			return null;
		}
		if (StringUtil.isEmpty(fileName)) {
			getLogger().warning("File name is not defined!");
			return null;
		}
		if (content == null) {
			getLogger().warning("Input stream is invalid!");
			return null;
		}
		if (!parentPath.endsWith(CoreConstants.SLASH)) {
			parentPath = parentPath.concat(CoreConstants.SLASH);
		}

		Binary binary = null;
		Session session = null;
		try {
			session = getSession(user);
			VersionManager versionManager = session.getWorkspace().getVersionManager();

			Node root = session.getRootNode();
			Node folder = getFolderNode(root, parentPath);
			Node file = getFileNode(folder, fileName, versionManager);
			Node resource = getResourceNode(file);
			if (resource == null) {
				resource = file.addNode(JcrConstants.JCR_CONTENT, JcrConstants.NT_RESOURCE);
			}

			mimeType = StringUtil.isEmpty(mimeType) ? MimeTypeUtil.resolveMimeTypeFromFileName(fileName) : mimeType;
			mimeType = StringUtil.isEmpty(mimeType) ? MimeTypeUtil.MIME_TYPE_APPLICATION : mimeType;
			resource.setProperty(JcrConstants.JCR_MIMETYPE, mimeType);
			resource.setProperty(JcrConstants.JCR_ENCODING, CoreConstants.ENCODING_UTF8);
			binary = ValueFactoryImpl.getInstance().createBinary(content);
			resource.setProperty(JcrConstants.JCR_DATA, binary);
			Calendar lastModified = Calendar.getInstance();
			lastModified.setTimeInMillis(System.currentTimeMillis());
			resource.setProperty(JcrConstants.JCR_LASTMODIFIED, lastModified);

			session.save();

			versionManager.checkin(file.getPath());
			return file;
		} finally {
			if (binary != null) {
				binary.dispose();
			}
			IOUtil.close(content);

			logout(session);
		}
	}

	@Override
	public List<RepositoryItemVersionInfo> getVersions(String parentPath, String fileName) throws RepositoryException {
		List<RepositoryItemVersionInfo> itemsInfo = new ArrayList<RepositoryItemVersionInfo>();

		Session session = null;
		try {
			session = getSession(getUser());
			Node root = session.getRootNode();
			Node folder = getFolderNode(root, parentPath);
			Node file = getFileNode(folder, fileName, null);

			VersionManager vm = session.getWorkspace().getVersionManager();
			VersionHistory vh = vm.getVersionHistory(file.getPath());
			for (VersionIterator vi = vh.getAllVersions(); vi.hasNext();) {
				Version version = vi.nextVersion();

				for (NodeIterator nodeIter = version.getNodes(); nodeIter.hasNext();) {
					Node n = nodeIter.nextNode();
					Binary binary = getBinary(n);
					if (binary != null) {
						RepositoryItemVersionInfo info = new RepositoryItemVersionInfo();
						info.setCreated(version.getCreated().getTime());
						info.setId(version.getIdentifier());
						info.setVersion(getVersion(version.toString()));
						itemsInfo.add(info);
					}
				}
			}

			return itemsInfo;
		} finally {
			logout(session);
		}
	}

	private Double getVersion(String nodePath) {
		Double version = 1.0;
		if (StringUtil.isEmpty(nodePath)) {
			return version;
		}
		if (nodePath.indexOf(CoreConstants.SLASH) == -1) {
			return version;
		}

		String versionId = null;
		try {
			versionId = nodePath.substring(nodePath.lastIndexOf(CoreConstants.SLASH) + 1);
			return Double.valueOf(versionId);
		} catch (IndexOutOfBoundsException e) {
			getLogger().warning("Error while trying to get version from " + nodePath);
		} catch (NumberFormatException e) {
			getLogger().warning("Error while converting " + versionId + " to Double");
		}

		return version;
	}

	private Node getFolderNode(Node parent, String nodeName) throws RepositoryException {
		return getNode(parent, nodeName, true, JcrConstants.NT_FOLDER, null);
	}
	private Node getFileNode(Node parent, String nodeName, VersionManager versionManager) throws RepositoryException {
		return getNode(parent, nodeName, true, JcrConstants.NT_FILE, versionManager);
	}
	private Node getResourceNode(Node parent) throws RepositoryException {
		return getNode(parent, JcrConstants.JCR_CONTENT, false, JcrConstants.NT_RESOURCE, null);
	}
	private Node getNode(Node parent, String nodeName, String type) throws RepositoryException {
		return getNode(parent, nodeName, true, type, null);
	}
	private Node getNode(Node parent, String nodeName, boolean createIfNotFound, String type, VersionManager versionManager) throws RepositoryException {
		if (nodeName.startsWith(CoreConstants.SLASH)) {
			nodeName = nodeName.substring(1);
		}
		if (nodeName.endsWith(CoreConstants.SLASH)) {
			nodeName = nodeName.substring(0, nodeName.length() - 1);
		}

		Node node = null;
		boolean created = false;
		try {
			node = parent.getNode(nodeName);
		} catch (PathNotFoundException e) {
			created = true;
		}

		if (node == null && createIfNotFound) {
			boolean noType = StringUtil.isEmpty(type);
			if (noType) {
				getLogger().warning("No type for node is provided");
			}

			String[] pathParts = nodeName.split(CoreConstants.SLASH);
			for (int i = 0; i < pathParts.length; i++) {
				String name = pathParts[i];
				node = getNode(parent, name, false, type, versionManager);
				if (node == null) {
					node = noType ? parent.addNode(name) : parent.addNode(name, type);
					setDefaultProperties(node);
				}

				if (node == null) {
					throw new RepositoryException("Node " + name + " can not be added to " + parent);
				}

				parent = node;
			}
		}

		if (!created && node != null && versionManager != null) {
			versionManager.checkout(node.getPath());
		}

		return node;
	}

	private void setDefaultProperties(Node node) throws RepositoryException {
		List<String> defaultMixins = Arrays.asList(
				JcrConstants.MIX_VERSIONABLE,
				JcrConstants.MIX_REFERENCEABLE
		);
		for (String mixin: defaultMixins) {
			node.addMixin(mixin);
		}

	}

	private Session getSessionAsRootUser() throws RepositoryException {
		return getSession(null);
	}
	private Session getSession(User user) throws RepositoryException {
		if (user == null) {
			try {
				user = securityHelper.getSuperAdmin();
			} catch (Exception e) {
				getLogger().severe("Administrator user can not be resolved!");
				throw new RepositoryException(e);
			}
		}

		if (user == null) {
			throw new RepositoryException("User can not be identified!");
		}

		LoginTable loginTable = LoginDBHandler.getUserLogin(user);
		Credentials credentials = getCredentials(Integer.valueOf(user.getId()), loginTable.getUserPasswordInClearText());

		return repository.login(credentials);
	}

	private void logout(Session session) {
		if (session != null) {
			session.logout();
		}
	}

	@Override
	public InputStream getInputStream(String path) throws IOException, RepositoryException {
		return getInputStream(path, getUser());
	}
	@Override
	public InputStream getInputStreamAsRoot(String path) throws IOException, RepositoryException {
		return getInputStream(path, null);
	}
	private InputStream getInputStream(String path, User user) throws IOException, RepositoryException {
		if (StringUtil.isEmpty(path)) {
			getLogger().warning("Path to resource is not defined!");
			return null;
		}

		Session session = null;
		try {
			session = getSession(user);

			Node root = session.getRootNode();
			Node file = getNode(root, path, false, null, null);
			if (file == null) {
				getLogger().warning("Resource does not exist: " + path);
				return null;
			}

			return getInputStream(getBinary(file));
		} finally {
			logout(session);
		}
	}

	private Binary getBinary(Node file) throws RepositoryException {
		if (file == null) {
			return null;
		}

		Node resource = getResourceNode(file);
		Property prop = resource == null ? null : resource.getProperty(JcrConstants.JCR_DATA);
		return prop == null ? null : prop.getBinary();
	}

	private InputStream getInputStream(Binary data) throws IOException, RepositoryException {
		if (data == null) {
			getLogger().warning("Value is not set for resource!");
			return null;
		}

		return data.getStream();
	}

	private User getUser() throws RepositoryException {
		User user = getCurrentUser();
		if (user == null) {
			throw new RepositoryException("User must be logged in!");
		}
		return user;
	}

	@Override
	public boolean deleteAsRootUser(String path) throws RepositoryException {
		return delete(path, null);
	}
	@Override
	public boolean delete(String path) throws RepositoryException {
		return delete(path, getUser());
	}
	private boolean delete(String path, User user) throws RepositoryException {
		if (StringUtil.isEmpty(path)) {
			getLogger().warning("Resource is not defined!");
			return false;
		}

		Session session = null;
		try {
			session = getSession(user);

			Node root = session.getRootNode();
			Node fileOrFolder = getNode(root, path, false, null, null);
			if (fileOrFolder == null) {
				getLogger().warning("Resource does not exist: " + path);
				return false;
			}

			fileOrFolder.remove();
			session.save();
			return true;
		} finally {
			logout(session);
		}
	}

	@Override
	public boolean createFolder(String path) throws RepositoryException {
		return createFolder(path, getUser());
	}
	@Override
	public boolean createFolderAsRoot(String path) throws RepositoryException {
		return createFolder(path, null);
	}
	private boolean createFolder(String path, User user) throws RepositoryException {
		if (StringUtil.isEmpty(path)) {
			getLogger().warning("Resource path is not defined!");
			return false;
		}

		Session session = null;
		try {
			session = getSession(user);

			Node root = session.getRootNode();
			Node folder = getNode(root, path, JcrConstants.NT_FOLDER);

			session.save();

			return folder != null;
		} finally {
			logout(session);
		}
	}

	@Override
	public AccessControlPolicy[] applyAccessControl(String absPath, AccessControlPolicy[] policies) throws RepositoryException {
		if (StringUtil.isEmpty(absPath) || ArrayUtil.isEmpty(policies)) {
			getLogger().warning("Path (" + absPath + ") or the policies (" + policies + ") are invalid");
			return null;
		}


		Session session = null;
		try {
			session = getSessionAsRootUser();

			//	TODO: finish up!
			/*AccessControlManager acm = session.getAccessControlManager();
			PrincipalManager pm = null;
			if (session instanceof SessionImpl) {
				pm = ((SessionImpl) session).getPrincipalManager();
			}
			for (AccessControlPolicyIterator policiesIter = acm.getApplicablePolicies(absPath); policiesIter.hasNext();) {
				AccessControlPolicy acp = policiesIter.nextAccessControlPolicy();
				if (!(acp instanceof AbstractACLTemplate)) {
					continue;
				}

				AbstractACLTemplate acl = (AbstractACLTemplate) acp;
				for (AccessControlPolicy policy: policies) {
					if (policy instanceof IWAccessControlPolicy) {
						IWAccessControlPolicy pol = (IWAccessControlPolicy) policy;
						Map<String, List<String>> prinicpalsAndPolicies = pol.getPolicies();
						for (Iterator<String> roleOrIdIter = prinicpalsAndPolicies.keySet().iterator(); roleOrIdIter.hasNext();) {
							final String roleOrId = roleOrIdIter.next();
							List<String> privilegies = prinicpalsAndPolicies.get(roleOrId);

							Principal p = new Principal() {
								@Override
								public String getName() {
									return roleOrId;
								}
							};

							int index = 0;
							Privilege[] privs = new Privilege[privilegies.size()];
							for (String privilege: privilegies) {
								privs[index] = acm.privilegeFromName(privilege);
								index++;
							}
							acl.addAccessControlEntry(p, privs);
						}
					}
				}

				acm.setPolicy(absPath, acl);
			}

			session.save();*/
		} finally {
			logout(session);
		}

		return null;
	}

	private boolean isValidPath(String absolutePath) {
		if (StringUtil.isEmpty(absolutePath) || absolutePath.indexOf(CoreConstants.SLASH) == -1) {
			getLogger().warning("Invalid absolute path: " + absolutePath);
			return false;
		}
		return true;
	}

	private String getParentPath(String absolutePath) {
		return isValidPath(absolutePath) ? absolutePath.substring(0, absolutePath.lastIndexOf(CoreConstants.SLASH)) : null;
	}

	private String getNodeName(String absolutePath) {
		return isValidPath(absolutePath) ? absolutePath.substring(absolutePath.lastIndexOf(CoreConstants.SLASH)) : null;
	}

	@Override
	public Node updateFileContents(String absolutePath, InputStream fileContents, boolean createFile, AdvancedProperty... properties) throws RepositoryException {
		return uploadFile(getParentPath(absolutePath), getNodeName(absolutePath), fileContents, null, getUser());
	}
	@Override
	public Node updateFileContents(String absolutePath, InputStream fileContents, AdvancedProperty... properties) throws RepositoryException {
		return updateFileContents(absolutePath, fileContents, Boolean.TRUE, properties);
	}

	@Override
	public InputStream getFileContents(Node fileNode) throws IOException, RepositoryException {
		Session session = null;
		try {
			session = getSession(getUser());
			return getInputStream(getBinary(fileNode));
		} finally {
			logout(session);
		}
	}

	@Override
	public Node getNode(String absolutePath) throws RepositoryException {
		return getNode(absolutePath, false, true);
	}
	private Node getNode(String absolutePath, boolean createIfNotFound, boolean useVersionManager) throws RepositoryException {
		if (!isValidPath(absolutePath)) {
			return null;
		}

		Session session = null;
		try {
			session = getSession(getUser());
			Node root = session.getRootNode();
			return getNode(root, absolutePath, createIfNotFound, null, useVersionManager ? session.getWorkspace().getVersionManager() : null);
		} finally {
			logout(session);
		}
	}

	@Override
	public boolean setProperties(Node node, AdvancedProperty... properties)	throws RepositoryException {
		Session session = null;
		try {
			session = getSession(getUser());
			return setProperties(session, node, properties);
		} finally {
			logout(session);
		}
	}

	private boolean setProperties(Session session, Node node, AdvancedProperty... properties) throws RepositoryException {
		if (node == null || ArrayUtil.isEmpty(properties)) {
			return false;
		}

		for (AdvancedProperty property: properties) {
			node.setProperty(property.getId(), property.getValue());
		}
		if (session != null) {
			session.save();
		}

		return true;
	}

	@Override
	public String getRepositoryConstantFolderType() {
		return JcrConstants.NT_FOLDER;
	}

	@Override
	public String getWebdavServerURI() {
		String appContext = getApplication().getApplicationContextURI();
		if (appContext.endsWith(CoreConstants.SLASH)) {
			appContext = appContext.substring(0, appContext.lastIndexOf(CoreConstants.SLASH));
		}
		return appContext.concat("/repository");//CoreConstants.WEBDAV_SERVLET_URI;	//	TODO use a constant
	}

	@Override
	public Credentials getCredentials(String userName, String password) {
		return getCredentials(userName, password, new AdvancedProperty[] {});
	}
	public Credentials getCredentials(int userId, String password) {
		return getCredentials(String.valueOf(userId), password, new AdvancedProperty(JackrabbitConstants.REAL_USER_ID_USED, Boolean.TRUE.toString()));
	}

	private Credentials getCredentials(String userNameOrId, String password, AdvancedProperty... attributes) {
		if (StringUtil.isEmpty(userNameOrId)) {
			getLogger().warning("User name or ID is not defined!");
			return null;
		}

		SimpleCredentials credentials = new SimpleCredentials(userNameOrId, StringUtil.isEmpty(password) ? CoreConstants.EMPTY.toCharArray() : password.toCharArray());
		if (!ArrayUtil.isEmpty(attributes)) {
			for (AdvancedProperty attribute: attributes) {
				credentials.setAttribute(attribute.getId(), attribute.getValue());
			}
		}

		return credentials;
	}

	@Override
	public boolean generateUserFolders(String loginName) throws RepositoryException {
		if (StringUtil.isEmpty(loginName)) {
			getLogger().warning("User name is not defined");
			return false;
		}

		String userPath = authenticationBusiness.getUserPath(loginName);
		Node userNode = getNode(userPath, true, false);
		if (userNode == null) {
			throw new RepositoryException("Node at " + userPath + " can not be created!");
		}
		//	TODO: set access rights for userNode

//		if (!getExistence(userPath)) {
//			WebdavResource user = getWebdavResourceAuthenticatedAsRoot(userPath);
//			user.mkcolMethod();
//			user.close();
//		}

		String userHomeFolder = getUserHomeFolderPath(loginName);
		Node userHomeNode = getNode(userHomeFolder, true, false);
		if (userHomeNode == null) {
			throw new RepositoryException("Node at " + userHomeFolder + " can not be created!");
		}
		//	TODO: set access rights for userHomeNode

//		WebdavResource rootFolder = getWebdavResourceAuthenticatedAsRoot();
//		String userFolderPath = getURI(getUserHomeFolderPath(loginName));
//		rootFolder.mkcolMethod(userFolderPath);
//		rootFolder.mkcolMethod(userFolderPath + FOLDER_NAME_DROPBOX);
//		rootFolder.mkcolMethod(userFolderPath + FOLDER_NAME_PUBLIC);
//		rootFolder.close();

		try {
			updateUserFolderPrivileges(loginName);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	private String getUserHomeFolderPath(String loginName) {
		return JackrabbitConstants.PATH_USERS_HOME_FOLDERS + CoreConstants.SLASH + loginName;
	}

	private void updateUserFolderPrivileges(String loginName) throws IOException, IOException {
		//	TODO: implement!

//		String userFolderPath = getURI(getUserHomeFolderPath(loginName));
//
//		AuthenticationBusiness aBusiness = getAuthenticationBusiness();
//		String userPrincipal = aBusiness.getUserURI(loginName);
//
//		// user folder
//		AccessControlList userFolderList = getAccessControlList(userFolderPath);
//		// should be 'all' for the user himself
//		List<AccessControlEntry> userFolderUserACEs = userFolderList.getAccessControlEntriesForUsers();
//		AccessControlEntry usersPositiveAce = null;
//		AccessControlEntry usersNegativeAce = null;
//		boolean madeChangesToUserFolderList = false;
//		// Find the ace
//		for (Iterator<AccessControlEntry> iter = userFolderUserACEs.iterator(); iter.hasNext();) {
//			AccessControlEntry ace = iter.next();
//			if (ace.getPrincipal().equals(userPrincipal) && !ace.isInherited()) {
//				if (ace.isNegative()) {
//					usersNegativeAce = ace;
//				} else {
//					usersPositiveAce = ace;
//				}
//			}
//		}
//		if (usersPositiveAce == null) {
//			usersPositiveAce = new AccessControlEntry(userPrincipal, false, false, false, null, AccessControlEntry.PRINCIPAL_TYPE_USER);
//			userFolderList.add(usersPositiveAce);
//		}
//
//		if (!usersPositiveAce.containsPrivilege(IWSlideConstants.PRIVILEGE_ALL)) {
//			if (usersNegativeAce != null && usersNegativeAce.containsPrivilege(IWSlideConstants.PRIVILEGE_ALL)) {
//				// do nothing becuse this is not ment to reset permissions but
//				// to set them in the first
//				// first place and update for legacy reasons. If Administrator
//				// has closed someones user folder
//				// for some reason, this is not supposed to reset that.
//			} else {
//				usersPositiveAce.addPrivilege(IWSlideConstants.PRIVILEGE_ALL);
//				madeChangesToUserFolderList = true;
//
//				// temporary at least:
//				usersPositiveAce.setInherited(false);
//				usersPositiveAce.setInheritedFrom(null);
//				// temporary ends
//			}
//		}
//		if (madeChangesToUserFolderList) {
//			storeAccessControlList(userFolderList);
//		}
//
//		// dropbox
//		updateUsersDropboxPrivileges(userFolderPath);
//
//		// public folder
//		updateUsersPublicFolderPrivileges(userFolderPath);
	}

	@Override
	public boolean getExistence(String absolutePath) throws RepositoryException {
		if (!isValidPath(absolutePath)) {
			return false;
		}

		Session session = null;
		try {
			session = getSessionAsRootUser();
			Node root = session.getRootNode();
			Node node = getNode(root, absolutePath, false, null, null);
			return node != null;
		} finally {
			logout(session);
		}
	}
}