package com.idega.jackrabbit.business;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.core.business.DefaultSpringBean;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.RepositoryStartedEvent;
import com.idega.repository.event.RepositoryResourceLocalizer;
import com.idega.util.CoreConstants;
import com.idega.util.IOUtil;
import com.idega.util.ListUtil;
import com.idega.util.SortedProperties;
import com.idega.util.StringUtil;
import com.idega.util.datastructures.map.MapUtil;
import com.idega.util.messages.MessageResource;
import com.idega.util.messages.MessageResourceFactory;

@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class RepositoryResourcesManager extends DefaultSpringBean implements ApplicationListener<ApplicationEvent> {

	@Autowired
	private MessageResourceFactory messageResourceFactory;

	public synchronized void storeState(Map<String, String> localizations, String localizableFilePath) {
		Properties props = new SortedProperties();
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

		if (localizations != null) {
			for (Iterator<String> iter = localizations.keySet().iterator(); iter.hasNext();) {
				String key = iter.next();
				if (key != null) {
					Object value = localizations.get(key);
					if (value != null) {
						props.put(key, value);
					}
				}
			}

			try {
				props.store(byteStream, CoreConstants.EMPTY);
			} catch (IOException e) {
				getLogger().log(Level.WARNING, "Can't store properties to ByteArrayOutputStream", e);
			}
		}

		InputStream tmp = null;
		try {
			tmp = new ByteArrayInputStream(byteStream.toByteArray());
			getRepositoryService().updateFileContentsAsRoot(localizableFilePath, tmp, true);
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Can't save localization file '" + localizableFilePath + "' to repository", e);
			throw new RuntimeException(e);
		} finally {
			IOUtil.close(byteStream);
			IOUtil.close(tmp);
		}
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof RepositoryStartedEvent)
			doLoadResources();
		else if (event instanceof RepositoryResourceLocalizer) {
			RepositoryResourceLocalizer localizer = (RepositoryResourceLocalizer) event;
			Map<String, Map<String, String>> localizations = localizer.getLocalizations();
			for (String path: localizations.keySet())
				storeState(localizations.get(path), path);
		}
	}

	private void doLoadResources() {
		Map<String, IWBundle> bundles = getApplication().getLoadedBundles();
		if (MapUtil.isEmpty(bundles)) {
			getLogger().warning("No bundles are loaded");
			return;
		}

		for (IWBundle bundle: bundles.values()) {
			List<Locale> locales = bundle.getEnabledLocales();
			if (ListUtil.isEmpty(locales))
				continue;

			for (Locale locale: locales) {
				List<MessageResource> messages = messageResourceFactory.getResourceListByBundleAndLocale(bundle.getBundleIdentifier(), locale);
				if (ListUtil.isEmpty(messages))
					continue;

				JackrabbitResourceBundle repositoryBundle = null;
				List<String> repositoryResourceKeys = null;
				Map<String, String> localizationsFromOtherResources = new HashMap<String, String>();
				for (MessageResource messageResource: messages) {
					if (!(messageResource instanceof JackrabbitResourceBundle)) {
						Set<String> keys = messageResource.getAllLocalizedKeys();
						if (ListUtil.isEmpty(keys))
							continue;

						for (String key: keys) {
							if (!localizationsFromOtherResources.containsKey(key)) {
								String localized = messageResource.getMessage(key);
								if (!StringUtil.isEmpty(localized))
									localizationsFromOtherResources.put(key, localized);
							}
						}
					} else {
						repositoryBundle = (JackrabbitResourceBundle) messageResource;
						try {
							repositoryBundle.initialize(bundle.getBundleIdentifier(), locale);
						} catch (IOException e) {
							getLogger().warning("Error initializing " + bundle.getBundleIdentifier() + " for locale " + locale);
							continue;
						}
						repositoryResourceKeys = new ArrayList<String>(repositoryBundle.getAllLocalizedKeys());
					}
				}

				//	Making sure we are not going to over write existing localizations
				if (!ListUtil.isEmpty(repositoryResourceKeys))
					localizationsFromOtherResources.keySet().removeAll(repositoryResourceKeys);

				if (!MapUtil.isEmpty(localizationsFromOtherResources))
					repositoryBundle.setMessages(localizationsFromOtherResources);
			}
		}
	}

}