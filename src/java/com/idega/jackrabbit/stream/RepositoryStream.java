package com.idega.jackrabbit.stream;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.input.AutoCloseInputStream;
import org.springframework.util.ReflectionUtils;

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

	private Method getMethod(String name) {
		try {
			return InputStream.class.getMethod(name);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public int available() throws IOException {
		Integer available = execute(true, getMethod("available"));
		return available instanceof Integer ? available : 0;
	}

	@SuppressWarnings("unchecked")
	private <T> T execute(boolean reTry, Method method) throws IOException {
		if (method == null)
			return null;

		if (in != null) {
			try {
				Object o = ReflectionUtils.invokeMethod(method, in);
				return (T) o;
			} catch (Exception e) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "Error executing method " + method + " on " + in +". Path " + path);
				IOUtil.close(in);
				in = null;
			}
		}

		if (reTry) {
			BuilderLogicWrapper builderLogic = ELUtil.getInstance().getBean(BuilderLogicWrapper.SPRING_BEAN_NAME_BUILDER_LOGIC_WRAPPER);
			builderLogic.getBuilderService(IWMainApplication.getDefaultIWApplicationContext()).clearAllCaches();
			return execute(false, method);
		}

		Session session = null;
		RepositoryService repository = ELUtil.getInstance().getBean(RepositoryService.class);
		try {
			JackrabbitSecurityHelper securityHelper = ELUtil.getInstance().getBean(JackrabbitSecurityHelper.BEAN_NAME);
			session = repository.getSession(securityHelper.getSuperAdmin());
			in = repository.getInputStream(session, path);
			if (in == null)
				throw new IOException("Can not open stream to " + path);

			Object o = ReflectionUtils.invokeMethod(method, in);
			return (T) o;
		} catch (RepositoryException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			repository.logout(session);
		}

		throw new IOException("Can not open stream to " + path);
	}

	@Override
	public int read() throws IOException {
		Integer read = execute(true, getMethod("read"));
		return read instanceof Integer ? read : 0;
	}

	@Override
	public void close() throws IOException {
		IOUtil.close(in);
	}

}