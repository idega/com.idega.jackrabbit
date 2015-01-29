package com.idega.jackrabbit.business;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectInputStream.GetField;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.presentation.IWContext;
import com.idega.repository.RepositoryService;
import com.idega.repository.event.RepositoryResourceLocalizer;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.IOUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;
import com.idega.util.datastructures.map.MapUtil;
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

	@Autowired
	private RepositoryResourcesManager resourcesManager;

	private RepositoryResourcesManager getRepositoryResourcesManager() {
		if (resourcesManager == null)
			ELUtil.getInstance().autowire(this);
		return resourcesManager;
	}

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

		Reader reader = null;
		Properties localizationProps = new Properties();
		if (sourceStream != null) {
			try {
				String content = StringHandler.getContentFromInputStream(sourceStream);
				reader = new StringReader(content);
				localizationProps.load(reader);
			} catch (Exception e) {
				throw new IOException(e);
			} finally {
				IOUtil.close(reader);
			}
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
	public void storeState() {
		getRepositoryResourcesManager().storeState(getLookup(), getLocalizableFilePath());
	}

	@Override
	public String getLocalizedString(String key) {
		String returnObj = getLookup().get(key);
		if (returnObj != null && !"null".equals(returnObj)) {
			return returnObj;
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
		return getLocalizableFolderPath() + getLocalizableFileName();
	}

	private String getLocalizableFolderPath() {
		StringBuffer filePath = new StringBuffer(LOCALIZATION_PATH);

		if (!StringUtil.isEmpty(getBundleIdentifier()) && !MessageResource.NO_BUNDLE.equals(getBundleIdentifier()))
			filePath.append(getBundleIdentifier()).append(CoreConstants.SLASH);

		return filePath.toString();
	}

	private String getLocalizableFileName() {
		StringBuffer fileName = new StringBuffer();

		if (StringUtil.isEmpty(getBundleIdentifier()) || MessageResource.NO_BUNDLE.equals(getBundleIdentifier())) {
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
	public String getMessage(String key) {
		return getLocalizedString(key);
	}

	/**
	 * @return Message that was set or null if there was a failure setting object
	 */
	@Override
	public String setMessage(String key, String value) {
		String currentValue = getLookup().get(key);
		if (!StringUtil.isEmpty(currentValue) && currentValue.equals(value))
			return value;

		getLookup().put(key, value);

		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null || IWMainApplication.getDefaultIWMainApplication().getSettings().getBoolean("flush_each_localization_prop", Boolean.FALSE)) {
			storeState();
		} else {
			RepositoryResourceLocalizer localizer = null;
			HttpServletRequest request = iwc.getRequest();
			Object previousLocalizations = request.getAttribute(RepositoryService.REQUEST_LOCALIZATIONS);
			if (previousLocalizations instanceof RepositoryResourceLocalizer) {
				localizer = (RepositoryResourceLocalizer) previousLocalizations;
				Map<String, Map<String, String>> allLocalizations = localizer.getLocalizations();
				Map<String, String> currentLocalizations = allLocalizations.get(getLocalizableFilePath());
				MapUtil.append(currentLocalizations, getLookup());
				allLocalizations.put(getLocalizableFilePath(), currentLocalizations);
				localizer.setLocalizations(allLocalizations);
			} else {
				Map<String, Map<String, String>> localizations = new HashMap<String, Map<String,String>>();
				localizations.put(getLocalizableFilePath(), getLookup());
				localizer = new RepositoryResourceLocalizer(localizations);
			}
			request.setAttribute(RepositoryService.REQUEST_LOCALIZATIONS, localizer);
		}

		return value;
	}

	@Override
	public void setMessages(Map<String, String> values) {
		for (Object key : values.keySet())
			setString(String.valueOf(key), String.valueOf(values.get(key)));

		storeState();
	}

	@Override
	public Set<String> getAllLocalizedKeys() {
		Map<String, String> data = getLookup();

		Set<String> loadedKeys = getLookup().keySet();

		String path = LOCALIZATION_PATH + getBundleIdentifier() + "/Localizable_" + getLocale().toString() + ".strings";
		try {
			if (getRepositoryService().getExistence(path)) {
				String content = StringHandler.getContentFromInputStream(getRepositoryService().getInputStreamAsRoot(path));
				List<String> lines = StringUtil.getLinesFromString(content);
				if (!ListUtil.isEmpty(lines)) {
					List<String> newKeys = new ArrayList<String>();
					for (String line: lines) {
						if (StringUtil.isEmpty(line)) {
							continue;
						}

						String[] parts = line.split(CoreConstants.EQ);
						if (ArrayUtil.isEmpty(parts) || parts.length != 2) {
							continue;
						}

						String key = parts[0];
						if (!data.containsKey(key)) {
							String value = parts[1];
							setString(key, value);
							newKeys.add(key);
						}
					}
					if (!ListUtil.isEmpty(newKeys)) {
						LOGGER.info("Found missing keys: " + newKeys + " in " + path);
						loadedKeys = new TreeSet<String>(loadedKeys);
						loadedKeys.addAll(newKeys);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return loadedKeys;
	}

	@Override
	public void removeMessage(String key) {
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

	@Override
	public String toString() {
		return "Repository resource: " + getBundleIdentifier() + " for " + getLocale();
	}
}