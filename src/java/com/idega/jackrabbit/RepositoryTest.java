package com.idega.jackrabbit;

import java.io.IOException;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;

import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.rmi.remote.RemoteRepository;
import org.apache.jackrabbit.rmi.repository.RMIRemoteRepository;
import org.apache.jackrabbit.rmi.server.ServerAdapterFactory;

public class RepositoryTest {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws NotBoundException 
	 * @throws ClassCastException 
	 * @throws RepositoryException 
	 * @throws NoSuchWorkspaceException 
	 * @throws LoginException 
	 */
	public static void main(String[] args) throws IOException, ClassCastException, NotBoundException, LoginException, NoSuchWorkspaceException, RepositoryException {
		// TODO Auto-generated method stub
		//startEmbeddedRepository();
		
		Repository repository = getRemoteRepository();
		
		Credentials credentials = new SimpleCredentials("", "".toCharArray());
		
		Session session = repository.login(credentials);
		//session.getN
		Node rootNode = session.getRootNode();
		
		NodeIterator nodeIterator = rootNode.getNodes();
		while(nodeIterator.hasNext()) {
		
			Node node = nodeIterator.nextNode();
			String nodeName = node.getName();
			//Node node = rootNode.getNode("testfolder");
			PropertyIterator iterator = node.getProperties();
			while(iterator.hasNext()) {
				Property property = iterator.nextProperty();
				String name = property.getName();
				Value value = property.getValue();
				
				System.out.println("NodeName="+nodeName+",propertyname="+name+", value="+value.getString());
			}
			//Node folderNode = rootNode.addNode("nt:folder", "testfolder");
		}
		
		Node node = rootNode.addNode("tester", "nt:folder");
		//node.setProperty("jcr:primaryType", "nt:folder");
		//node.save();
		session.save();
		
	}

	private static Repository getRemoteRepository() {
		String url = "rmi://localhost:1099/jackrabbit.repository";
		
		//ClientRepositoryFactory factory = new ClientRepositoryFactory();
		//Repository repository = factory.getRepository(url);
		
		Repository repository =
		    new RMIRemoteRepository("//localhost/jackrabbit.repository");
		return repository;
	}

	private static Repository getEmbeddedRepository() throws IOException,
			RemoteException, AccessException {
		Repository srepository = new TransientRepository();
		ServerAdapterFactory sfactory = new ServerAdapterFactory();
		RemoteRepository remote = sfactory.getRemoteRepository(srepository);
		Registry reg = LocateRegistry.createRegistry(1100);
		reg.rebind("jackrabbit", remote);
		return srepository;
	}

}
