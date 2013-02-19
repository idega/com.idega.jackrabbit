package com.idega.jackrabbit.webdav;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.jackrabbit.server.SessionProvider;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.WebdavRequest;
import org.apache.jackrabbit.webdav.WebdavResponse;
import org.apache.jackrabbit.webdav.jcr.JCRWebdavServerServlet;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.core.accesscontrol.business.AccessController;
import com.idega.core.accesscontrol.dao.PermissionDAO;
import com.idega.core.accesscontrol.data.bean.ICPermission;
import com.idega.core.file.util.MimeTypeUtil;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jackrabbit.repository.IdegaSessionProvider;
import com.idega.jackrabbit.security.JackrabbitSecurityHelper;
import com.idega.repository.RepositoryConstants;
import com.idega.repository.RepositoryService;
import com.idega.user.data.bean.User;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.FileUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

/**
 *
 * @author valdas
 *
 */
public class IdegaWebdavServlet extends JCRWebdavServerServlet {

	private static final long serialVersionUID = -3270277844439956826L;
	private static final Logger LOGGER = Logger.getLogger(IdegaWebdavServlet.class.getName());

	@Autowired
	private RepositoryService repository;

	@Autowired
	private JackrabbitSecurityHelper securityHelper;

	private JackrabbitSecurityHelper getSecurityHelper() {
		if (securityHelper == null)
			ELUtil.getInstance().autowire(this);
		return securityHelper;
	}

	@Override
	public RepositoryService getRepository() {
		if (repository == null)
			ELUtil.getInstance().autowire(this);
		return repository;
	}

	@Override
	protected void doGet(WebdavRequest webdavRequest, WebdavResponse webdavResponse, DavResource davResource) throws IOException, DavException {
		try {
			String path = davResource.getResourcePath();
			if (getRepository().getExistence(path)) {
				writeResponse(webdavRequest.getSession(false), webdavResponse, davResource, 0);
				webdavResponse.setStatus(DavServletResponse.SC_OK);
			} else {
				String message = "Resource '" + path + "' does not exist";
				LOGGER.warning(message);
				DavException davException = new DavException(DavServletResponse.SC_NOT_FOUND, message);
				CoreUtil.sendExceptionNotification(message, davException);
				throw davException;
			}
		} catch (DavException e) {
			throw new DavException(e.getErrorCode(), e);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error getting " + davResource.getHref(), e);
			throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, e);
		}
	}

	private void writeResponse(HttpSession session, WebdavResponse webdavResponse, DavResource davResource, int level) throws IOException, DavException {
		String name = davResource.getDisplayName();
		String path = davResource.getResourcePath();
		String prefix = RepositoryConstants.DEFAULT_WORKSPACE_ROOT_CONTENT;
		if (path.startsWith(prefix))
			path = path.replaceFirst(prefix, CoreConstants.EMPTY);
		int semicolon = path.indexOf(CoreConstants.SEMICOLON);
		if (semicolon != -1)
			path = path.substring(0, semicolon);

		boolean allowAll = path.startsWith(CoreConstants.PUBLIC_PATH) || path.startsWith(CoreConstants.CONTENT_PATH + "/themes/");
		if (!allowAll) {
			IWMainApplication iwma = IWMainApplication.getDefaultIWMainApplication();
			AccessController accessController = iwma.getAccessController();

			//	Need to resolve access rights
			User user = getSecurityHelper().getCurrentUser(session);
			if (user == null)
				throw new DavException(DavServletResponse.SC_FORBIDDEN, "User is not logged in, resource " + path + " is not accessible");

			if (!getSecurityHelper().isSuperAdmin(user)) {
				if (iwma.getSettings().getBoolean("jackrabbit_dav_check_permissions", Boolean.FALSE)) {
					LOGGER.info("Resolve access rights for resource: " + path + " and user " + user);
					Set<String> userRoles = accessController.getAllRolesForUser(user);
					if (ListUtil.isEmpty(userRoles)) {
						LOGGER.warning("User " + user + " does not have any roles!");
						throw new DavException(DavServletResponse.SC_FORBIDDEN);
					}

					PermissionDAO permissionDAO = ELUtil.getInstance().getBean(PermissionDAO.class);
					List<ICPermission> permissions = permissionDAO.findPermissions(path, new ArrayList<String>(userRoles));
					if (ListUtil.isEmpty(permissions)) {
						LOGGER.warning("User " + user + " does not have permission to read " + path);
						throw new DavException(DavServletResponse.SC_FORBIDDEN);
					}
				}
	//			AccessControlList acl = getRepository().getAccessControlList(path);
	//			List<ICPermission> permissions = acl.getPermissions();
	//			if (ListUtil.isEmpty(permissions)) {
	//				LOGGER.warning("No permissions for resource: " + path + " and user " + user);
	//			}
			}
		}

		if (isCollection(davResource, name) && canDisplay(name)) {
			//	TODO: implement
//			writer.write("<a href=\"".concat(davResource.getHref()).concat("\">").concat(name).concat("</a>\n"));
		} else {
			try {
				String contentType = MimeTypeUtil.resolveMimeTypeFromFileName(path);
				if (!StringUtil.isEmpty(contentType))
					webdavResponse.setContentType(contentType);

				OutputStream output = webdavResponse.getOutputStream();
				FileUtil.streamToOutputStream(getRepository().getInputStreamAsRoot(path), output);
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Error getting content of " + path, e);
			}
		}
	}

	private boolean canDisplay(String displayName) {
		return displayName != null && displayName.indexOf(CoreConstants.SLASH) == -1 && displayName.indexOf(CoreConstants.COLON) == -1;
	}

	private boolean isCollection(DavResource resource, String displayName) {
		return resource.isCollection() && displayName.indexOf(CoreConstants.DOT) == -1;
	}

	@Override
	protected void doPost(WebdavRequest webdavRequest, WebdavResponse webdavResponse, DavResource davResource) throws IOException, DavException {
		LOGGER.info("Using default implementation of doPost method");
		super.doPost(webdavRequest, webdavResponse, davResource);
	}

	private DavLocatorFactory locatorFactory;

	@Override
	public DavLocatorFactory getLocatorFactory() {
		if (locatorFactory == null)
			locatorFactory = new IdegaDAVLocatorFactory(CoreConstants.WEBDAV_SERVLET_URI);
		return locatorFactory;
	}

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setAttribute(SessionProvider.class.getName(), new IdegaSessionProvider());
		super.service(request, response);
	}

	@Override
	protected void doAcl(WebdavRequest request, WebdavResponse response, DavResource resource) throws DavException, IOException {
		super.doAcl(request, response, resource);
	}
}