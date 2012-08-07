package com.idega.jackrabbit.webdav;

import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.jcr.DavLocatorFactoryImpl;

import com.idega.repository.RepositoryConstants;
import com.idega.util.CoreConstants;

public class IdegaDAVLocatorFactory extends DavLocatorFactoryImpl {

	public IdegaDAVLocatorFactory(String pathPrefix) {
		super(pathPrefix);
	}

	@Override
	public DavResourceLocator createResourceLocator(String prefix, String href) {
		String postfix = CoreConstants.WEBDAV_SERVLET_URI.concat(RepositoryConstants.DEFAULT_WORKSPACE_ROOT_CONTENT);
		if (!href.startsWith(postfix)) {
			href = href.replaceFirst(CoreConstants.WEBDAV_SERVLET_URI, CoreConstants.EMPTY);
			href = postfix.concat(href);
		}

		return super.createResourceLocator(prefix, href);
	}

}