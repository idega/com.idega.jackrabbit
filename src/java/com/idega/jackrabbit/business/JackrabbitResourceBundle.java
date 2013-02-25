package com.idega.jackrabbit.business;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectInputStream.GetField;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.idegaweb.IWResourceBundle;
import com.idega.repository.RepositoryService;
import com.idega.util.CoreConstants;
import com.idega.util.IOUtil;
import com.idega.util.SortedProperties;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;
import com.idega.util.messages.MessageResource;
import com.idega.util.messages.MessageResourceImportanceLevel;

/**
 *
 *
 * @author <a href="anton@idega.com">Anton Makarov</a>
 * @version Revision: 1.0
 *
 * Last modified: Oct 15, 2008 by Author: Anton
 *
 */
@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class JackrabbitResourceBundle extends IWResourceBundle implements MessageResource, Serializable {

	private static final long serialVersionUID = -4849846267697372361L;

	private static final Logger LOGGER = Logger.getLogger(JackrabbitResourceBundle.class.getName());

	private static final String LOCALIZATION_PATH = CoreConstants.CONTENT_PATH + "/bundles/",
								NON_BUNDLE_LOCALIZATION_FILE_NAME = "Localizable_no_bundle",
								BUNDLE_LOCALIZATION_FILE_NAME = "Localizable",
								NON_BUNDLE_LOCALIZATION_FILE_EXTENSION = ".strings",
								RESOURCE_IDENTIFIER = "repository_resource";

	public JackrabbitResourceBundle() throws IOException {
		super();
	}

	@Override
	protected void initProperities() {
		setIdentifier(RESOURCE_IDENTIFIER);
		setLevel(MessageResourceImportanceLevel.FIRST_ORDER);
		setAutoInsert(true);
	}

	@Override
	public void initialize(String bundleIdentifier, Locale locale) throws IOException {
		setLocale(locale);
		setBundleIdentifier(bundleIdentifier);

		InputStream sourceStream = getResourceInputStream(getLocalizableFilePath());

		Properties localizationProps = new Properties();
		if (sourceStream != null) {
			localizationProps.load(sourceStream);
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		Map<String, String> props = new TreeMap(localizationProps);
		setLookup(props);

		IOUtil.closeInputStream(sourceStream);
	}

	protected InputStream getResourceInputStream(String resourcePath) {
		return getResourceInputStream(resourcePath, Boolean.TRUE);
	}

	private InputStream getResourceInputStream(String resourcePath, boolean createIfNotFound) {
		try {
			RepositoryService repository = getRepositoryService();
			if (createIfNotFound && !repository.getExistence(resourcePath))
				createEmptyFile(resourcePath);

			return repository.getInputStreamAsRoot(resourcePath);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error getting InputStream for: " + resourcePath, e);
		}
		return null;
	}

	@Override
	public synchronized void storeState() {
		Properties props = new SortedProperties();
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

		if (getLookup() != null) {
			for (Iterator<String> iter = getLookup().keySet().iterator(); iter.hasNext();) {
				String key = iter.next();
				if (key != null) {
					Object value = getLookup().get(key);
					if (value != null) {
						props.put(key, value);
					}
				}
			}

			try {
				props.store(byteStream, CoreConstants.EMPTY);
			} catch (IOException e) {
				LOGGER.log(Level.WARNING, "Can't store properties to ByteArrayOutputStream", e);
			}
		}

		InputStream stream = getResourceInputStream(getLocalizableFilePath());
		if (stream == null) {
			LOGGER.warning("Can't save localization file '" + getLocalizableFilePath() + "' to repository - unable to create empty file!");
			return;
		} else {
			IOUtil.close(stream);
		}

		InputStream tmp = null;
		try {
			tmp = new ByteArrayInputStream(byteStream.toByteArray());
			getRepositoryService().updateFileContentsAsRoot(getLocalizableFilePath(), tmp, true);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Can't save localization file '" + getLocalizableFilePath() + "' to repository", e);
			throw new RuntimeException(e);
		} finally {
			IOUtil.close(byteStream);
			IOUtil.close(tmp);
		}
	}

	@Override
	public String getLocalizedString(String key) {
		Object returnObj = getLookup().get(key);
		if (returnObj != null && !"null".equals(returnObj)) {
			return String.valueOf(returnObj);
		} else {
			return null;
		}
	}

	@Override
	public void setString(String key, String value) {
		getLookup().put(key, value);
	}

	/**
	 * @return <code>true</code> - if the value presents in repository bundle. <code>false</code> - in other case
	 *
	 */
	@Override
	protected boolean checkBundleLocalizedString(String key, String value) {
		return !StringUtil.isEmpty((String) handleGetObject(key));
	}

	private String getLocalizableFilePath() {
		return getLocalisableFolderPath() + getLocalisableFileName();
	}

	private String getLocalisableFolderPath() {
		StringBuffer filePath = new StringBuffer(LOCALIZATION_PATH);

		if(!StringUtil.isEmpty(getBundleIdentifier()) && !MessageResource.NO_BUNDLE.equals(getBundleIdentifier())) {
			filePath.append(getBundleIdentifier()).append(CoreConstants.SLASH);
		}

		return filePath.toString();
	}

	private String getLocalisableFileName() {
		StringBuffer fileName = new StringBuffer();

		if(StringUtil.isEmpty(getBundleIdentifier()) || MessageResource.NO_BUNDLE.equals(getBundleIdentifier())) {
			fileName.append(NON_BUNDLE_LOCALIZATION_FILE_NAME)
					.append(CoreConstants.UNDER).append(getLocale())
					.append(NON_BUNDLE_LOCALIZATION_FILE_EXTENSION);
		} else {
			fileName.append(BUNDLE_LOCALIZATION_FILE_NAME)
					.append(CoreConstants.UNDER).append(getLocale())
					.append(NON_BUNDLE_LOCALIZATION_FILE_EXTENSION);
		}

		return fileName.toString();
	}

	private RepositoryService getRepositoryService() {
		return ELUtil.getInstance().getBean(RepositoryService.BEAN_NAME);
	}

	private boolean createEmptyFile(String path) {
		try {
			getRepositoryService().updateFileContentsAsRoot(path, StringHandler.getStreamFromString(CoreConstants.EMPTY), true);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	/**
	 * @param key - message key
	 * @return object that was found in resource or set to it, null - if there are no values with specified key
	 */
	@Override
	public Object getMessage(Object key) {
		return getLocalizedString(String.valueOf(key));
	}

	/**
	 * @return object that was set or null if there was a failure setting object
	 */
	@Override
	public Object setMessage(Object key, Object value) {
		getLookup().put(String.valueOf(key), String.valueOf(value));
		storeState();
		return value;
	}

	@Override
	public void setMessages(Map<Object, Object> values) {
		for (Object key : values.keySet())
			setString(String.valueOf(key), String.valueOf(values.get(key)));

		storeState();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<String> getAllLocalizedKeys() {
		return getLookup().keySet();
	}

	@Override
	public void removeMessage(Object key) {
		getLookup().remove(key);
		storeState();
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeBoolean(isAutoInsert());
		out.writeObject(getBundleIdentifier());
		out.writeObject(getLocale());
		out.writeObject(getLookup());
		out.writeObject(getLevel());
		return;
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		try {
			GetField fields = in.readFields();
			fields.get("autoInsert", Boolean.TRUE);
			fields.get("bundleIdentifier", MessageResource.NO_BUNDLE);
			fields.get("locale", Locale.ENGLISH);
			fields.get("lookup", Collections.emptyMap());
			fields.get("usagePriorityLevel", MessageResourceImportanceLevel.FIRST_ORDER);
		} catch (IllegalArgumentException e) {
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error reading objects from the stream: " + in, e);
		}
	}
}