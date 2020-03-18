package com.idega.jackrabbit.business;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.builder.bean.AdvancedProperty;
import com.idega.idegaweb.DefaultIWBundle;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.repository.RepositoryService;
import com.idega.repository.bean.RepositoryItem;
import com.idega.repository.event.RepositoryResourceLocalizer;
import com.idega.servlet.filter.RequestResponseProvider;
import com.idega.util.CoreConstants;
import com.idega.util.IOUtil;
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

	private long lastModified = -1;

	private RepositoryResourcesManager getRepositoryResourcesManager() {
		if (resourcesManager == null) {
			ELUtil.getInstance().autowire(this);
		}

		return resourcesManager;
	}

	public JackrabbitResourceBundle() throws IOException {
		super();
	}

	@Override
	protected void initProperities() {
		setIdentifier(RESOURCE_IDENTIFIER);
		setLevel(DefaultIWBundle.isProductionEnvironment() ? MessageResourceImportanceLevel.THIRD_ORDER : MessageResourceImportanceLevel.FIRST_ORDER);
		setAutoInsert(true);
	}

	@Override
	protected Map<String, String> getLookup() {
		if (MapUtil.isEmpty(super.getLookup())) {
			Properties localizationProps = new Properties();

			String path = null;
			String content = null;
			InputStream sourceStream = null;
			try {
				path = getLocalizableFilePath();
				sourceStream = getResourceInputStream(path);

				content = StringHandler.getContentFromInputStream(sourceStream);
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Failed to read content from " + path + " cause of: ", e);
			} finally {
				IOUtil.closeInputStream(sourceStream);
			}

			if (!StringUtil.isEmpty(content)) {
				Reader reader = new StringReader(content);

				try {
					localizationProps.load(reader);
				} catch (IOException e) {
					LOGGER.log(Level.WARNING, "Failed to load properties from " + path + ", cause of: ", e);
				} finally {
					IOUtil.close(reader);
				}
			}

			@SuppressWarnings({ "unchecked", "rawtypes" })
			Map<String, String> props = new TreeMap(localizationProps);
			setLookup(props);
		}

		Map<String, String> props = super.getLookup();
		if (props == null) {
			props = new TreeMap<>();
		}
		return props;
	}

	@Override
	public void initialize(String bundleIdentifier, Locale locale, long lastModified) throws IOException {
		setLocale(locale);
		setBundleIdentifier(bundleIdentifier);
		getLookup();
		this.lastModified = lastModified;
	}

	protected InputStream getResourceInputStream(String resourcePath) {
		return getResourceInputStream(resourcePath, Boolean.TRUE);
	}

	private InputStream getResourceInputStream(String resourcePath, boolean createIfNotFound) {
		InputStream fileToCopy = null;
		try {
			RepositoryService repository = getRepositoryService();
			if (repository.getExistence(resourcePath)) {
				RepositoryItem localFile = repository.getRepositoryItemAsRootUser(resourcePath);
				if (localFile != null && localFile.getLength() == 0) {
					repository.deleteAsRootUser(resourcePath);
				}
			}

			if (createIfNotFound && !repository.getExistence(resourcePath)) {
				fileToCopy = IOUtil.getStreamFromJar(getBundleIdentifier(), getArchivePath());
				boolean empty = fileToCopy == null ?
						true :
						StringUtil.isEmpty(StringHandler.getContentFromInputStream(fileToCopy));
				if (empty) {
					fileToCopy = IOUtil.getStreamFromJar(getBundleIdentifier(), "resources/Localizable.strings");
				} else {
					fileToCopy = IOUtil.getStreamFromJar(getBundleIdentifier(), getArchivePath());
				}

				if (fileToCopy != null) {
					repository.updateFileContentsAsRoot(resourcePath, fileToCopy, createIfNotFound, (AdvancedProperty) null);
				}
			}

			if (createIfNotFound && !repository.getExistence(resourcePath)) {
				createEmptyFile(resourcePath);
			}

			return repository.getInputStreamAsRoot(resourcePath);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error getting InputStream for: " + resourcePath, e);
		} finally {
			IOUtil.close(fileToCopy);
		}
		return null;
	}

	@Override
	public void storeState() {
		getRepositoryResourcesManager().storeState(getLookup(), getLocalizableFilePath());
	}

	@Override
	public String getLocalizedString(String key) {
		if (StringUtil.isEmpty(key)) {
			return null;
		}

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

	private String getArchivePath() {
		return "resources/" + getLocale() + ".locale/Localized.strings";
	}

	private String getLocalizableFilePath() {
		String path = null;

		Integer version = null;
		boolean checkedWithoutError = false;
		while (!checkedWithoutError && (version == null || version <= 1000)) {
			path = getLocalizableFolderPath() + getLocalizableFileName(version == null ? null : String.valueOf(version));
			try {
				getRepositoryService().exists(path);
				checkedWithoutError = true;
				return path;
			} catch (Exception e) {
				path = null;
				checkedWithoutError = false;
				version = version == null ? 1 : version + 1;
			}
		}

		return path;
	}

	private String getLocalizableFolderPath() {
		StringBuffer filePath = new StringBuffer(LOCALIZATION_PATH);

		if (!StringUtil.isEmpty(getBundleIdentifier()) && !MessageResource.NO_BUNDLE.equals(getBundleIdentifier())) {
			filePath.append(getBundleIdentifier()).append(CoreConstants.SLASH);
		}

		return filePath.toString();
	}

	private String getLocalizableFileName(String version) {
		StringBuffer fileName = new StringBuffer();

		if (StringUtil.isEmpty(getBundleIdentifier()) || MessageResource.NO_BUNDLE.equals(getBundleIdentifier())) {
			fileName.append(NON_BUNDLE_LOCALIZATION_FILE_NAME);
		} else {
			fileName.append(BUNDLE_LOCALIZATION_FILE_NAME);
		}

		fileName.append(CoreConstants.UNDER).append(getLocale());
		if (version != null) {
			fileName.append(CoreConstants.UNDER).append(version);
		}
		fileName.append(NON_BUNDLE_LOCALIZATION_FILE_EXTENSION);

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
		if (!StringUtil.isEmpty(currentValue) && currentValue.equals(value)) {
			return value;
		}

		if (key != null && value != null) {
			getLookup().put(key, value);
		}

		HttpServletRequest request = null;
		RequestResponseProvider rrProvider = null;
		try {
			rrProvider = ELUtil.getInstance().getBean(RequestResponseProvider.class);
		} catch (Exception e) {}
		request = rrProvider == null ? null : rrProvider.getRequest();

		if (request == null || IWMainApplication.getDefaultIWMainApplication().getSettings().getBoolean("flush_each_localization_prop", Boolean.FALSE)) {
			storeState();
		} else {
			String path = getLocalizableFilePath();
			if (StringUtil.isEmpty(path)) {
				LOGGER.warning("Unable to set value '" + value + "' for keyword '" + key + "'");
				return value;
			}

			RepositoryResourceLocalizer localizer = null;
			Object previousLocalizations = request.getAttribute(RepositoryService.REQUEST_LOCALIZATIONS);
			if (previousLocalizations instanceof RepositoryResourceLocalizer) {
				localizer = (RepositoryResourceLocalizer) previousLocalizations;
				Map<String, Map<String, String>> allLocalizations = localizer.getLocalizations();
				Map<String, String> currentLocalizations = allLocalizations.get(path);
				currentLocalizations = currentLocalizations == null ? new ConcurrentHashMap<String, String>() : new ConcurrentHashMap<String, String>(currentLocalizations);
				MapUtil.append(currentLocalizations, getLookup());
				allLocalizations.put(path, currentLocalizations);
				localizer.setLocalizations(allLocalizations);
			} else {
				Map<String, Map<String, String>> localizations = new HashMap<String, Map<String,String>>();
				localizations.put(path, getLookup());
				localizer = new RepositoryResourceLocalizer(localizations);
			}
			request.setAttribute(RepositoryService.REQUEST_LOCALIZATIONS, localizer);
		}

		return value;
	}

	@Override
	public void setMessages(Map<String, String> values) {
		for (Object key : values.keySet()) {
			setString(String.valueOf(key), String.valueOf(values.get(key)));
		}

		storeState();
	}

	@Override
	public Set<String> getAllLocalizedKeys() {
		return getLookup().keySet();
	}

	@Override
	public void removeMessage(String key) {
		getLookup().remove(key);
		storeState();
	}

	@Override
	public long lastModified() {
		return lastModified;
	}

	@Override
	public String toString() {
		return "Repository resource: " + getBundleIdentifier() + " for " + getLocale();
	}
}