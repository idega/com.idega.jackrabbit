package com.idega.jackrabbit.webdav;

import java.io.IOException;
import java.io.Writer;
import java.util.logging.Logger;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Repository;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.WebdavRequest;
import org.apache.jackrabbit.webdav.WebdavResponse;
import org.apache.jackrabbit.webdav.jcr.JCRWebdavServerServlet;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.core.file.util.MimeTypeUtil;
import com.idega.repository.RepositoryService;
import com.idega.util.CoreConstants;
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

	@Override
	public Repository getRepository() {
		if (repository == null) {
			ELUtil.getInstance().autowire(this);
		}
		return repository;
	}

	@Override
	protected void doGet(WebdavRequest webdavRequest, WebdavResponse webdavResponse, DavResource davResource) throws IOException, DavException {
		try {
			if (davResource.exists()) {
				webdavResponse.setContentType(MimeTypeUtil.MIME_TYPE_HTML.concat(";charset=".concat(CoreConstants.ENCODING_UTF8.toLowerCase())));

				writeResponse(webdavResponse, davResource, 0);

				webdavResponse.setStatus(DavServletResponse.SC_OK);
			} else {
				throw new DavException(DavServletResponse.SC_NOT_FOUND, new ItemNotFoundException("No node at " + davResource.getResourcePath()));
			}
		} catch (Exception e) {
			LOGGER.warning(e.getMessage());
			throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, e);
		}
	}

	private void writeResponse(WebdavResponse webdavResponse, DavResource davResource, int level) throws IOException {
		String name = davResource.getDisplayName();
		Writer writer = webdavResponse.getWriter();

		if (canDisplay(name)) {
			writer.write("<a href=\"".concat(davResource.getHref()).concat("\">").concat(name).concat("</a>\n"));
		}

		if (level > 0) {
			return;
		}

		if (isCollection(davResource, name)) {
			for (DavResourceIterator davResIter = davResource.getMembers(); davResIter.hasNext();) {
				DavResource resource = davResIter.nextResource();
				writeResponse(webdavResponse, resource, 1);
			}
		} else {
			String fileType = MimeTypeUtil.resolveMimeTypeFromFileName(name);

			webdavResponse.setContentType(fileType.concat("charset=".concat(CoreConstants.ENCODING_UTF8.toLowerCase())));

			writer.write(name);
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
		super.doPost(webdavRequest, webdavResponse, davResource);
	}

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		super.service(request, response);
	}

	@Override
	protected void doAcl(WebdavRequest request, WebdavResponse response, DavResource resource) throws DavException, IOException {
		super.doAcl(request, response, resource);
	}
}