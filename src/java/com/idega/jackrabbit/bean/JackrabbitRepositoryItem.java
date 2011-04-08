package com.idega.jackrabbit.bean;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.core.file.util.MimeTypeUtil;
import com.idega.repository.RepositoryService;
import com.idega.repository.bean.RepositoryItem;
import com.idega.user.data.bean.User;
import com.idega.util.CoreConstants;
import com.idega.util.ListUtil;
import com.idega.util.expression.ELUtil;

public class JackrabbitRepositoryItem implements RepositoryItem {

	private String path,
					mimeType;
	private User user;
	private Node node;

	private Binary binary;
	private Long size;
	private Boolean collection,
					exists;
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

}