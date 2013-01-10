package com.idega.jackrabbit.stream;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jcr.Binary;
import javax.jcr.RepositoryException;

import org.apache.commons.io.input.AutoCloseInputStream;

import com.idega.builder.business.BuilderLogicWrapper;
import com.idega.idegaweb.IWMainApplication;
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
		try {
			return in.read();
		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Error reading from " + path, e);
			IOUtil.close(in);
			in = null;
		}

		if (reTry) {
			BuilderLogicWrapper builderLogic = ELUtil.getInstance().getBean(BuilderLogicWrapper.SPRING_BEAN_NAME_BUILDER_LOGIC_WRAPPER);
			builderLogic.getBuilderService(IWMainApplication.getDefaultIWApplicationContext()).clearAllCaches();
			return read(false);
		}

		RepositoryService repository = ELUtil.getInstance().getBean(RepositoryService.class);
		Binary data = null;
		try {
			data = repository.getBinaryAsRoot(path);
		} catch (RepositoryException e) {
			e.printStackTrace();
		}
		if (data == null)
			throw new IOException("Data can not be resolved for " + path);

		try {
			in = data.getStream();
			return in.read();
		} catch (RepositoryException e) {
			e.printStackTrace();
		}

		throw new IOException("Unable to open stream to " + path);
	}

	@Override
	public void close() throws IOException {
		if (in == null)
			return;

		super.close();
	}

}