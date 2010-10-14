package com.idega.jackrabbit.webdav;

import java.io.IOException;

import javax.jcr.Repository;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.WebdavRequest;
import org.apache.jackrabbit.webdav.WebdavResponse;
import org.apache.jackrabbit.webdav.jcr.JCRWebdavServerServlet;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.repository.RepositoryService;
import com.idega.util.expression.ELUtil;

/**
 *
 * @author valdas
 *
 */
public class IdegaWebdavServlet extends JCRWebdavServerServlet {

	private static final long serialVersionUID = -3270277844439956826L;

	@Autowired
	private RepositoryService repository;

	@Override
	protected Repository getRepository() {
		if (repository == null) {
			ELUtil.getInstance().autowire(this);
		}
		return repository;
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