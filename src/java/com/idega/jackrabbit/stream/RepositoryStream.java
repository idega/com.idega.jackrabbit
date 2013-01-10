package com.idega.jackrabbit.stream;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.input.AutoCloseInputStream;

import com.idega.builder.business.BuilderLogicWrapper;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jackrabbit.security.JackrabbitSecurityHelper;
import com.idega.repository.RepositoryService;
import com.idega.util.IOUtil;
import com.idega.util.expression.ELUtil;

public class RepositoryStream extends AutoCloseInputStream {

	private String path;

	public RepositoryStream(String path, InputStream stream) throws FileNotFoundException {
		super(stream);

		this.path = path;
	}

	@Override
	public int read() throws IOException {
		return read(true);
	}

	private int read(boolean reTry) throws IOException {
		if (in != null) {
			try {
				return in.read();
			} catch (Exception e) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "Error reading from " + path, e);
				IOUtil.close(in);
				in = null;
			}
		}

		if (reTry) {
			BuilderLogicWrapper builderLogic = ELUtil.getInstance().getBean(BuilderLogicWrapper.SPRING_BEAN_NAME_BUILDER_LOGIC_WRAPPER);
			builderLogic.getBuilderService(IWMainApplication.getDefaultIWApplicationContext()).clearAllCaches();
			return read(false);
		}

		Session session = null;
		RepositoryService repository = ELUtil.getInstance().getBean(RepositoryService.class);
		try {
			JackrabbitSecurityHelper securityHelper = ELUtil.getInstance().getBean(JackrabbitSecurityHelper.BEAN_NAME);
			session = repository.getSession(securityHelper.getSuperAdmin());
			in = repository.getInputStream(session, path);
			if (in == null)
				throw new IOException("Can not open stream to " + path);

			return in.read();
		} catch (RepositoryException e) {
			e.printStackTrace();
		} finally {
			repository.logout(session);
		}

		throw new IOException("Can not open stream to " + path);
	}

	@Override
	public void close() throws IOException {
		IOUtil.close(in);
	}

}