package com.idega.jackrabbit.test;

import java.io.InputStream;

import javax.jcr.RepositoryException;

import org.directwebremoting.annotations.Param;
import org.directwebremoting.annotations.RemoteMethod;
import org.directwebremoting.annotations.RemoteProxy;
import org.directwebremoting.spring.SpringCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.core.business.DefaultSpringBean;
import com.idega.dwr.business.DWRAnnotationPersistance;
import com.idega.repository.RepositoryService;
import com.idega.util.StringHandler;

@Service(RepositoryTests.BEAN_IDENTIFIER)
@Scope(BeanDefinition.SCOPE_SINGLETON)
@RemoteProxy(creator=SpringCreator.class, creatorParams={
	@Param(name="beanName", value=RepositoryTests.BEAN_IDENTIFIER),
	@Param(name="javascript", value=RepositoryTests.DWR_OBJECT)
}, name=RepositoryTests.DWR_OBJECT)
public class RepositoryTests extends DefaultSpringBean implements DWRAnnotationPersistance {

	public static final String BEAN_IDENTIFIER = "repositoryTestsExecutor";
	public static final String DWR_OBJECT = "RepositoryTestsExecutor";

	@Autowired
	private RepositoryService repository;

	@RemoteMethod
	public boolean upload(String path, String fileName, String fileContent, boolean rootUser) {
		try {
			return rootUser ?	repository.uploadFileAndCreateFoldersFromStringAsRoot(path, fileName, fileContent, null) :
								repository.uploadFile(path, fileName, null, StringHandler.getStreamFromString(fileContent));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@RemoteMethod
	public String getContent(String path, boolean rootUser) {
		try {
			InputStream stream = rootUser ? repository.getInputStreamAsRoot(path) : repository.getInputStream(path);
			return StringHandler.getContentFromInputStream(stream);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@RemoteMethod
	public boolean deleteNode(String path, boolean rootUser) {
		try {
			return rootUser ? repository.deleteAsRootUser(path) : repository.delete(path);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@RemoteMethod
	public String getVersionsInfo(String parentPath, String fileName) {
		try {
			return repository.getVersions(parentPath, fileName).toString();
		} catch (RepositoryException e) {
			e.printStackTrace();
		}
		return null;
	}
}