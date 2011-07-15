package com.idega.jackrabbit.bean;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.jcr.AccessDeniedException;
import javax.jcr.Binary;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionManager;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.value.StringValue;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.core.file.util.MimeTypeUtil;
import com.idega.repository.RepositoryService;
import com.idega.repository.bean.RepositoryItem;
import com.idega.repository.jcr.JCRItem;
import com.idega.user.data.bean.User;
import com.idega.util.CoreConstants;
import com.idega.util.ListUtil;
import com.idega.util.StringHandler;
import com.idega.util.expression.ELUtil;

public class JackrabbitRepositoryItem extends JCRItem {

	private String path, mimeType;
	private User user;
	private Node node;

	private Binary binary;
	private Long size;
	private Boolean collection, exists;
	private URL url;

	@Autowired
	private RepositoryService repository;

	public JackrabbitRepositoryItem(String path, User user, Node node) {
		super();
		ELUtil.getInstance().autowire(this);

		this.path = path;
		this.user = user;
		this.node = node;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		try {
			return binary == null ?
					repository.getFileContents(user, node) :
					binary.getStream();
		} catch (RepositoryException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public String getName() {
		try {
			return node.getName();
		} catch (RepositoryException e) {
			e.printStackTrace();
		}
		return CoreConstants.EMPTY;
	}

	@Override
	public long getLength() {
		if (size == null) {
			Binary binary = getBinary();
			try {
				size = binary == null ? 0 : binary.getSize();
			} catch (RepositoryException e) {
				e.printStackTrace();
			}
		}
		return size;
	}

	@Override
	public boolean delete() throws IOException {
		try {
			return repository.delete(path, user);
		} catch (RepositoryException e) {
			e.printStackTrace();
		}
		return false;
	}

	private Binary getBinary() {
		if (binary == null) {
			try {
				binary = repository.getBinary(node);
			} catch (RepositoryException e) {
				e.printStackTrace();
			}
		}
		return binary;
	}

	@Override
	public Collection<RepositoryItem> getChildResources() {
		Collection<RepositoryItem> items = new ArrayList<RepositoryItem>();

		Collection<Node> nodes = null;
		try {
			nodes = repository.getChildNodes(user, node);
		} catch (RepositoryException e) {
			e.printStackTrace();
		}
		if (ListUtil.isEmpty(nodes)) {
			return items;
		}

		for (Node node: nodes) {
			try {
				items.add(new JackrabbitRepositoryItem(node.getPath(), user, node));
			} catch (RepositoryException e) {
				e.printStackTrace();
			}
		}

		return items;
	}

	@Override
	public boolean isCollection() {
		if (collection == null) {
			try {
				collection = repository.isCollection(node);
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
				exists = repository.getExistence(path);
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
				System.out.println(getClass().getName() + ": unkown mime type!");
			}
		}
		return mimeType;
	}

	@Override
	public long getCreationDate() {
		try {
			Property property = node.getProperty(JcrConstants.JCR_CREATED);
			return property == null ? 0 : property.getDate().getTime().getTime();
		} catch (PathNotFoundException e) {
			e.printStackTrace();
		} catch (RepositoryException e) {
			e.printStackTrace();
		}
		return 0;
	}

	@Override
	public long getLastModified() {
		try {
			Property property = node.getProperty(JcrConstants.JCR_LASTMODIFIED);
			return property == null ? getCreationDate() : property.getDate().getTime().getTime();
		} catch (Exception e) {
			return getCreationDate();
		}
	}

	@Override
	public String getParentPath() {
		try {
			Node parent = node.getParent();
			return parent == null ? null : parent.getPath();
		} catch (RepositoryException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean isLocked() {
		try {
			return node.isLocked();
		} catch (RepositoryException e) {
			e.printStackTrace();
		}
		return false;
	}

	@SuppressWarnings("deprecation")
	@Override
	public Lock lock(boolean isDeep, boolean isSessionScoped) {
		try {
			return node.lock(isDeep, isSessionScoped);
		} catch (AccessDeniedException e) {
			e.printStackTrace();
		} catch (UnsupportedRepositoryOperationException e) {
			e.printStackTrace();
		} catch (LockException e) {
			e.printStackTrace();
		} catch (InvalidItemStateException e) {
			e.printStackTrace();
		} catch (RepositoryException e) {
			e.printStackTrace();
		}
		return null;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void unlock() {
		try {
			node.unlock();
		} catch (AccessDeniedException e) {
			e.printStackTrace();
		} catch (UnsupportedRepositoryOperationException e) {
			e.printStackTrace();
		} catch (LockException e) {
			e.printStackTrace();
		} catch (InvalidItemStateException e) {
			e.printStackTrace();
		} catch (RepositoryException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void unCheckOut() {
		try {
			VersionManager vm = repository.getVersionManager();
			Version version = vm.getBaseVersion(getPath());
			vm.restore(version, true);
		} catch (RepositoryException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Property setProperty(String name, Serializable value) {
		Value propValue = null;
		if (value instanceof String)
			propValue = new StringValue((String) value);

		try {
			if (propValue != null)
				return node.setProperty(name, propValue);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public RepositoryItem getParenItem() {
		if (node == null)
			return null;

		try {
			Node parent = node.getParent();
			return new JackrabbitRepositoryItem(parent.getPath(), user, parent);
		} catch (RepositoryException e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public Collection<RepositoryItem> getSiblingResources() {
		if (node == null)
			return Collections.emptyList();

		try {
			Node parent = node.getParent();
			if (parent == null)
				return Collections.emptyList();

			Collection<RepositoryItem> siblings = new ArrayList<RepositoryItem>();
			for (NodeIterator nodeIterator = parent.getNodes(); nodeIterator.hasNext();) {
				Node child = nodeIterator.nextNode();
				if (child.equals(node))
					continue;

				if (node.hasProperty(JcrConstants.NT_FILE) || node.hasProperty(JcrConstants.NT_FOLDER))
					siblings.add(new JackrabbitRepositoryItem(child.getPath(), user, child));
			}
			return siblings;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return Collections.emptyList();
	}

	@Override
	public boolean createNewFile() throws IOException, RepositoryException {
		if (!exists()) {
			try {
				return repository.uploadFile(getParentPath(), getName(), getMimeType(), StringHandler.getStreamFromString(CoreConstants.EMPTY));
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}

}