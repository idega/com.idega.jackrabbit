package com.idega.jackrabbit.webdav;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jcr.RepositoryException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
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
import com.idega.util.FileUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

/**
 *
 * @author valdas
 *
 */
public class IdegaWebdavServlet extends JCRWebdavServerServlet implements Filter {

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
			doServeRequest(davResource.getResourcePath(), webdavRequest, webdavResponse);
		} catch (DavException e) {
			throw new DavException(e.getErrorCode(), e);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error getting " + davResource.getHref(), e);
			throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, e);
		}
	}

	private void doServeRequest(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
		long start = System.currentTimeMillis();
		try {
			int semiColonIndex = path.lastIndexOf(CoreConstants.SEMICOLON);
			if (semiColonIndex > 0) {
				path = path.substring(0, semiColonIndex);
			}

			if (getRepository().getExistence(path)) {
				writeResponse(request.getSession(false), response, path);
				response.setStatus(DavServletResponse.SC_OK);
			} else {
				String message = "Resource '" + StringHandler.replace(path, RepositoryConstants.DEFAULT_WORKSPACE_ROOT_CONTENT, CoreConstants.EMPTY) + "' does not exist";
				LOGGER.warning(message);
				DavException davException = new DavException(DavServletResponse.SC_NOT_FOUND, message);
				throw davException;
			}
		} finally {
			long duration = System.currentTimeMillis() - start;
			if (duration >= 50 && IWMainApplication.getDefaultIWMainApplication().getSettings().getBoolean("jcr.log_request_duration", false)) {
				LOGGER.info("It took " + duration + " ms to serve " + StringHandler.replace(path, RepositoryConstants.DEFAULT_WORKSPACE_ROOT_CONTENT, CoreConstants.EMPTY));
			}
		}
	}

	private void writeResponse(HttpSession session, HttpServletResponse response, String path) throws IOException, DavException {
		String prefix = RepositoryConstants.DEFAULT_WORKSPACE_ROOT_CONTENT;
		if (path.startsWith(prefix)) {
			path = path.replaceFirst(prefix, CoreConstants.EMPTY);
		}
		int semicolon = path.indexOf(CoreConstants.SEMICOLON);
		if (semicolon != -1) {
			path = path.substring(0, semicolon);
		}

		String themes = CoreConstants.CONTENT_PATH + "/themes/";
		boolean allowAll =	path.startsWith(CoreConstants.WEBDAV_SERVLET_URI.concat(CoreConstants.PUBLIC_PATH)) || path.startsWith(CoreConstants.PUBLIC_PATH) ||
							path.startsWith(CoreConstants.WEBDAV_SERVLET_URI.concat(themes)) || path.startsWith(themes);

		if (!allowAll) {
			IWMainApplication iwma = IWMainApplication.getDefaultIWMainApplication();
			AccessController accessController = iwma.getAccessController();

			//	Need to resolve access rights
			User user = getSecurityHelper().getCurrentUser(session);
			if (user == null) {
				throw new DavException(DavServletResponse.SC_FORBIDDEN, "User is not logged in, resource " + path + " is not accessible");
			}

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

		boolean folder = false;
		try {
			folder = getRepository().isFolder(path);
		} catch (RepositoryException e) {}

		String name = null;
		try {
			if (path.indexOf(CoreConstants.SLASH) != -1) {
				name = path.substring(path.lastIndexOf(CoreConstants.SLASH) + 1);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (folder && canDisplay(name)) {
			LOGGER.warning("Request to " + path + " can not be served: it's either folder or can't display this resource");
		} else {
			try {
				String contentType = MimeTypeUtil.resolveMimeTypeFromFileName(path);
				if (!StringUtil.isEmpty(contentType)) {
					response.setContentType(contentType);
				}

				OutputStream output = response.getOutputStream();
				FileUtil.streamToOutputStream(getRepository().getInputStreamAsRoot(path), output);
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Error getting content of " + path, e);
			}
		}
	}

	private boolean canDisplay(String displayName) {
		return displayName != null && displayName.indexOf(CoreConstants.SLASH) == -1 && displayName.indexOf(CoreConstants.COLON) == -1;
	}

	@Override
	protected void doPost(WebdavRequest webdavRequest, WebdavResponse webdavResponse, DavResource davResource) throws IOException, DavException {
		super.doPost(webdavRequest, webdavResponse, davResource);
	}

	private DavLocatorFactory locatorFactory;

	@Override
	public DavLocatorFactory getLocatorFactory() {
		if (locatorFactory == null) {
			locatorFactory = new IdegaDAVLocatorFactory(CoreConstants.WEBDAV_SERVLET_URI);
		}
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

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		String path = null;
		try {
			HttpServletRequest httpRequest = (HttpServletRequest) request;
			HttpServletResponse httpResponse = (HttpServletResponse) response;
			path = httpRequest.getRequestURI();
			doServeRequest(path, httpRequest, httpResponse);
			return;
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error while serving request: " + path, e);
		}

		chain.doFilter(request, response);
	}
}