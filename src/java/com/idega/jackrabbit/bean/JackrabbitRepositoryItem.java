package com.idega.jackrabbit.bean;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.lock.Lock;
import javax.jcr.version.Version;
import javax.jcr.version.VersionManager;

import com.idega.core.file.util.MimeTypeUtil;
import com.idega.repository.RepositoryService;
import com.idega.repository.bean.RepositoryItem;
import com.idega.repository.jcr.JCRItem;
import com.idega.user.data.bean.User;
import com.idega.util.CoreConstants;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

public class JackrabbitRepositoryItem extends JCRItem {

	private static final long serialVersionUID = -5480971201437367638L;

	private String path, mimeType;
	private User user;

	private Long size;
	private Boolean collection, exists;
	private URL url;

	public JackrabbitRepositoryItem(String path, User user) {
		super();

		this.path = path;
		this.user = user;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		try {
			return getRepositoryService().getFileContents(user, path);
		} catch (RepositoryException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public String getName() {
		try {
			return getRepositoryService().getName(path);
		} catch (RepositoryException e) {
			getLogger().log(Level.WARNING, "Error getting name for " + path, e);
		}
		return CoreConstants.EMPTY;
	}

	@Override
	public long getLength() {
		if (size == null)
			try {
				size = getRepositoryService().getLength(path);
			} catch (RepositoryException e) {
				getLogger().log(Level.WARNING, "Error getting size of " + path, e);
			}

		return size;
	}

	@Override
	public boolean delete() throws IOException {
		try {
			return getRepositoryService().delete(path, user);
		} catch (RepositoryException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public Collection<RepositoryItem> getChildResources() {
		try {
			return getRepositoryService().getChildNodes(user, path);
		} catch (RepositoryException e) {
			getLogger().log(Level.WARNING, "Error getting children of " + path, e);
		}
		return Collections.emptyList();
	}

	@Override
	public boolean isCollection() {
		if (collection == null) {
			try {
				collection = getRepositoryService().isFolder(path);
			} catch (RepositoryException e) {
				e.printStackTrace();
			}
		}
		return collection;
	}

	@Override
	public boolean exists() {
		if (exists == null) {
			try {
				exists = getRepositoryService().getExistence(path);
			} catch (RepositoryException e) {
				e.printStackTrace();
			}
		}
		return exists;
	}

	@Override
	public String getPath() {
		return path;
	}

	@Override
	public URL getHttpURL() {
		if (url == null) {
			try {
				url = new URL(path);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		return url;
	}

	@Override
	public String getMimeType() {
		if (mimeType == null && (path != null && path.contains(CoreConstants.DOT))) {
			mimeType = MimeTypeUtil.resolveMimeTypeFromFileName(path);
			if (mimeType == null) {
				getLogger().warning(getClass().getName() + ": unkown mime type!");
			}
		}
		return mimeType;
	}

	@Override
	public long getCreationDate() {
		try {
			return getRepositoryService().getCreationDate(path);
		} catch (RepositoryException e) {
			getLogger().log(Level.WARNING, "Error getting creation date for " + path, e);
		}
		return -1;
	}

	@Override
	public long getLastModified() {
		try {
			long lastModified = getRepositoryService().getLastModified(path);
			if (lastModified < 0)
				return getCreationDate();
			return lastModified;
		} catch (RepositoryException e) {}
		return getCreationDate();
	}

	@Override
	public String getParentPath() {
		try {
			return getRepositoryService().getParentPath(path);
		} catch (RepositoryException e) {
			getLogger().log(Level.WARNING, "Error getting parent path for " + path);
		}
		return null;
	}

	@Override
	public boolean isLocked() {
		try {
			return getRepositoryService().isLocked(path);
		} catch (RepositoryException e) {
			getLogger().log(Level.WARNING, "Error resolving if node at " + path + " is locked", e);
		}
		return false;
	}

	@Override
	public Lock lock(boolean isDeep, boolean isSessionScoped) {
		try {
			return getRepositoryService().lock(path, isDeep, isSessionScoped);
		} catch (RepositoryException e) {
			getLogger().log(Level.WARNING, "Error locking " + path, e);
		}
		return null;
	}

	@Override
	public void unlock() {
		try {
			getRepositoryService().unLock(path);
		} catch (RepositoryException e) {
			getLogger().log(Level.WARNING, "Error unlocking " + path, e);
		}
	}

	@Override
	public void unCheckOut() {
		try {
			VersionManager vm = getRepositoryService().getVersionManager();
			Version version = vm.getBaseVersion(getPath());
			vm.restore(version, true);
		} catch (RepositoryException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean setProperty(String name, Serializable value) {
		try {
			return getRepositoryService().setProperties(path, new com.idega.repository.bean.Property(name, value));
		} catch (RepositoryException e) {
			getLogger().log(Level.WARNING, "Error setting property (name: " + name + ", value: " + value + ") for " + path, e);
		}
		return false;
	}

	//	TODO: implement
	@Override
	public Property getProperty(String property) {
		if (StringUtil.isEmpty(property))
			return null;

		return null;
//		try {
//			return node.getProperty(property);
//		} catch (PathNotFoundException e) {
//			getLogger().warning("Property " + property + " does not exist for " + node);
//		} catch (RepositoryException e) {
//			getLogger().log(Level.WARNING, "Error getting property " + property + " for " + node, e);
//		}
//		return null;
	}

	@Override
	public RepositoryItem getParenItem() {
		try {
			String parentPath = getRepositoryService().getParentPath(path);
			getRepositoryService().getRepositoryItem(parentPath);
		} catch (RepositoryException e) {
			getLogger().log(Level.WARNING, "Error getting parent item for " + path, e);
		}
		return null;
	}

	@Override
	public Collection<RepositoryItem> getSiblingResources() {
		try {
			return getRepositoryService().getSiblingResources(path);
		} catch (RepositoryException e) {
			getLogger().log(Level.WARNING, "Error getting siblings for " + path, e);
		}
		return null;
	}

	@Override
	public boolean createNewFile() throws IOException, RepositoryException {
		if (!exists()) {
			try {
				return getRepositoryService().uploadFile(getParentPath(), getName(), getMimeType(),
						StringHandler.getStreamFromString(CoreConstants.EMPTY));
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}

	private RepositoryService getRepositoryService() {
		return ELUtil.getInstance().getBean(RepositoryService.class);
	}
}