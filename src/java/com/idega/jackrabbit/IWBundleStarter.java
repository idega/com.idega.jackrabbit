package com.idega.jackrabbit;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWBundleStartable;
import com.idega.repository.RepositoryService;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.IOUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

/**
 * Bundle, responsible for instantiating repository and default workspace
 *
 * @author valdas
 *
 */
public class IWBundleStarter implements IWBundleStartable {

	private static final Logger LOGGER = Logger.getLogger(IWBundleStarter.class.getName());
	private static final String CONFIG_FILE = "WEB-INF/repository.xml";

	@Autowired
	private RepositoryService repository;

	@Override
	public void start(IWBundle starterBundle) {
		try {
			initializeRepository(starterBundle);
		} catch (Exception e) {
			String message = "Error loading configuration file: " + CONFIG_FILE;
			LOGGER.log(Level.SEVERE, message, e);
			CoreUtil.sendExceptionNotification(message, e);

			throw new RuntimeException(message, e);
		}
	}

	private void initializeRepository(IWBundle bundle) throws Exception {
		InputStream stream = getConfig();
		if (stream == null) {
			stream = getConfig(bundle);
		}

		//	TODO: use real folder ("store") after implementation is finished
		getRepository().initializeRepository(stream, "jcr_store");
	}

	private InputStream getConfig() {
		return IOUtil.getStreamFromJar(JackrabbitConstants.IW_BUNDLE_IDENTIFIER, CONFIG_FILE);
	}

	private InputStream getConfig(IWBundle bundle) throws IOException {
		String realPath = bundle.getRealPath();
		if (StringUtil.isEmpty(realPath)) {
			LOGGER.warning("Real path of bundle " + getClass().getName() + " is undefined!");
			return null;
		}

		if (realPath.endsWith("/resources")) {
			realPath = realPath.replace("resources", CoreConstants.EMPTY);
		}

		String configFile = realPath.concat(CONFIG_FILE);
		File file = new File(configFile);
		if (!file.exists()) {
			LOGGER.severe("File " + configFile + " does not exist!");
			return null;
		}
		return new FileInputStream(file);
	}

	private RepositoryService getRepository() {
		if (repository == null) {
			ELUtil.getInstance().autowire(this);
		}
		return repository;
	}

	@Override
	public void stop(IWBundle starterBundle) {
	}

}