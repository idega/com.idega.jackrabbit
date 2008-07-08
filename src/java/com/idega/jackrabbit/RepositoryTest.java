package com.idega.jackrabbit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.AccessException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Calendar;
import java.util.Hashtable;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.core.jndi.RegistryHelper;
import org.apache.jackrabbit.rmi.client.RemoteRepositoryException;
import org.apache.jackrabbit.rmi.remote.RemoteRepository;
import org.apache.jackrabbit.rmi.repository.RMIRemoteRepository;
import org.apache.jackrabbit.rmi.server.ServerAdapterFactory;
import org.apache.jackrabbit.value.DateValue;
import org.apache.jackrabbit.value.StringValue;

public class RepositoryTest {

	public static void main(String[] args) throws Exception{
		// TODO Auto-generated method stub
		//startEmbeddedRepository();
		
		
		RepositoryTest tester = new RepositoryTest();
		Repository repository = tester.getRemoteRepository();
		
		tester.dumpStructure(repository);
		
		
		
	}
	
	
	public void dumpStructure(Repository repository) throws Exception {
		// TODO Auto-generated method stub
		Session session=getReadonlySession(repository);
		Node root = session.getRootNode();
		dump(root,true);
	}


	private  void runCommandLineQuery(Repository repository) throws IOException, RepositoryException {
	        //Repository repository=getRepository();
	        Session session=getReadonlySession(repository);
	        Workspace workspace=session.getWorkspace();
	        QueryManager qm=workspace.getQueryManager();
	        BufferedReader reader=new BufferedReader(new InputStreamReader(System.in));
	        for(;;) {
	            System.out.print("JCRQL> ");
	            String queryString=reader.readLine();
	            if(queryString.equals("quit")) {
	                break;
	            }
	            if(queryString.length()==0 || queryString.startsWith("#")) {
	                continue;
	            }

	            int resultCounter=0;
	            try {
	                Query query=qm.createQuery(queryString, Query.XPATH);
	                QueryResult queryResult=query.execute();
	                NodeIterator nodeIterator=queryResult.getNodes();
	                while(nodeIterator.hasNext()) {
	                    Node node=nodeIterator.nextNode();
	                    dump(node);
	                    resultCounter++;
	                }
	            } catch(Exception e) {
	                e.printStackTrace();
	            }

	            System.out.println("result count: "+resultCounter);
	        }
	        logout(session);
	    }

	private void  dump(Node node) throws RepositoryException {
		dump(node,false);
	}
	
	    private void  dump(Node node,boolean children) throws RepositoryException {
	        StringBuilder sb=new StringBuilder();
	        String sep=",";
	        sb.append(node.getName());
	        sb.append("["+node.getPath());
	        PropertyIterator propIterator=node.getProperties();
	        while(propIterator.hasNext()) {
	            Property prop=propIterator.nextProperty();
	            sb.append(sep);
	            try{
	            	String propString;
	            	
	            	if(prop.getValue().getType()==PropertyType.BINARY){
	            		propString="BINARY";
	            	}
	            	else{
	            		propString=prop.getString();
	            	}
	            	sb.append("@"+prop.getName()+"=\""+propString+"\"");
	            }
	            catch(ValueFormatException e){
	            	//e.printStackTrace();
	            }
	            catch(RemoteRepositoryException re){
	            	//re.printStackTrace();
	            }
	        }
	        sb.append("]");
	        System.out.println(sb.toString());
	        if(children){
	        	NodeIterator nodeIterator = node.getNodes();
	        	while(nodeIterator.hasNext()){
	        		Node child = nodeIterator.nextNode();
	        		dump(child,true);
	        	}
	        }
	    }


	private  void simpleNodeTest(Repository repository)
			throws LoginException, RepositoryException, ValueFormatException,
			ItemExistsException, PathNotFoundException,
			NoSuchNodeTypeException, LockException, VersionException,
			ConstraintViolationException, AccessDeniedException,
			InvalidItemStateException {
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
	
	/**
	 * See http://www.artima.com/lejava/articles/contentrepository3.html
	 * @throws Exception 
	 */
	public  void contentTest(Repository r) throws Exception{

		//Create a new repository session, after authenticating 
		Session session = 
		   r.login(new SimpleCredentials("userid", "".toCharArray()), null);

		//Obtain the root node 
		Node rn = session.getRootNode();

		//Create and add a new blog node. The node's type will be "blog". 
		Node n = rn.addNode("blog");
		n.setProperty("blogtitle",  
		  new StringValue("Chasing Jackrabbit article"));
		n.setProperty("blogauthor", new StringValue("Joe Blogger"));
		n.setProperty("blogdate", new DateValue(Calendar.getInstance()));
		n.setProperty("blogtext", 
		    new StringValue("JCR is an interesting API to lo learn."));
		session.save();
		
		QueryManager qm = contentTestPart2(session);
		
		n = contentTestPart3(session, rn);
		
		contentTestPart4(session, qm);
		
		contentTestPart5(n);
		
	}

	private  Repository getJNDIRepository() throws NamingException,
			RepositoryException {
		//A repository config file. 
		String configFile = "repotest/repository.xml";
		//Filesystem for Jackrabbit repository 
		String repHomeDir = "repotest";

		//Register the repository in JNDI
		Hashtable env = new Hashtable();
		env.put(Context.INITIAL_CONTEXT_FACTORY,
		   
		"org.apache.jackrabbit.core.jndi.provider.DummyInitialContextFactory");
		env.put(Context.PROVIDER_URL, "localhost");
		InitialContext ctx = new InitialContext(env);
		RegistryHelper.registerRepository(ctx, 
		       "repo", 
		        configFile, 
		        repHomeDir, 
		        true);

		//Obtain the repository through a JNDI lookup 
		Repository r = (Repository)ctx.lookup("repo");
		return r;
	}

	 void contentTestPart5(Node n)
			throws UnsupportedRepositoryOperationException,
			RepositoryException, ValueFormatException, PathNotFoundException {
		//Obtain the blog node's version history
		VersionHistory history = n.getVersionHistory();
		VersionIterator ito = history.getAllVersions();

		while (ito.hasNext()) {

		   Version v = ito.nextVersion();

		   //The version node will have a "frozen" child node that contains 
		   //the corresponding version's node data
		   NodeIterator it = v.getNodes("jcr:frozenNode");

		   if (it.hasNext()) {

		     Node no = it.nextNode();
		     System.out.println("Version saved on " + 
		        v.getCreated().getTime().toString() + 
		        " has the following message: " + 
		        no.getProperty("blogtext").getString());
		   }
		}
	}

	 QueryManager contentTestPart2(Session session)
			throws RepositoryException, InvalidQueryException,
			PathNotFoundException, ValueFormatException {
		//Part 2 - Query Blog
		
		Workspace ws = session.getWorkspace();
		QueryManager qm = ws.getQueryManager();

		//Specify a query using the XPATH query language
		Query q = 
		   qm.createQuery("//blog[@blogauthor = 'Joe Blogger']", Query.XPATH);
		QueryResult res = q.execute();

		//Obtain a node iterator 
		NodeIterator it = res.getNodes();

		while (it.hasNext()) {

		   Node n = it.nextNode();
		   Property prop = n.getProperty("blogtitle");
		   System.out.println("Found blog entry with title: " 
		      + prop.getString());
		}
		return qm;
	}

	 Node contentTestPart3(Session session, Node rn)
			throws ItemExistsException, PathNotFoundException,
			VersionException, ConstraintViolationException, LockException,
			RepositoryException, NoSuchNodeTypeException, ValueFormatException,
			AccessDeniedException, InvalidItemStateException {
		//Part 3 - Versioned Blog

		
		Node n = rn.addNode("versionedblog");

		n.addMixin("mix:versionable");
		n.setProperty("blogtitle",  new StringValue("Versioned rabbit") );
		n.setProperty("blogauthor", new StringValue("Joe Blogger"));
		n.setProperty("blogdate", new DateValue(Calendar.getInstance()));
		n.setProperty("blogtext", 
		   new StringValue("JCR is an interesting API to lo learn."));

		session.save();
		return n;
	}

	 void contentTestPart4(Session session, QueryManager qm)
			throws InvalidQueryException, RepositoryException,
			UnsupportedRepositoryOperationException, LockException,
			ValueFormatException, VersionException,
			ConstraintViolationException, AccessDeniedException,
			ItemExistsException, InvalidItemStateException,
			NoSuchNodeTypeException {
		//Part 4 - Query versioned blog
		
		Query q = 
			   qm.createQuery("//versionedblog[@blogauthor = 'Joe Blogger' and @blogtitle = 'Versioned rabbit']", Query.XPATH);

			QueryResult res = q.execute();

			NodeIterator it = res.getNodes();

			if (it.hasNext()) {

			   Node n = it.nextNode();

			   //Check out the current node 
			   n.checkout();

			   //Set a new property value for blogtext 
			   n.setProperty("blogtext", new StringValue("Edited blog text"));

			   //Save the new version. 
			   session.save();

			   //Check the new vesion in, resulting in a new item being added 
			   //the the node's version history 
			   n.checkin();
			}
	}

	private  Repository getRemoteRepository() {
		String url = "rmi://localhost:1099/jackrabbit.repository";
		
		//ClientRepositoryFactory factory = new ClientRepositoryFactory();
		//Repository repository = factory.getRepository(url);
		
		Repository repository =
		    new RMIRemoteRepository("//localhost/jackrabbit.repository");
		return repository;
	}

	private  Repository getEmbeddedRepository() throws IOException,
			RemoteException, AccessException {
		Repository srepository = new TransientRepository();
		ServerAdapterFactory sfactory = new ServerAdapterFactory();
		RemoteRepository remote = sfactory.getRemoteRepository(srepository);
		Registry reg = LocateRegistry.createRegistry(1100);
		reg.rebind("jackrabbit", remote);
		return srepository;
	}
	
	
    public Repository getRepository() throws IOException {
        return new TransientRepository();
    }

    public Session getReadonlySession(Repository repository) throws RepositoryException {
        return repository.login();
    }

    public Session getSession(Repository repository) throws RepositoryException {
        return repository.login(new SimpleCredentials("username", "password".toCharArray()));
    }

    public void logout(Session session) {
        session.logout();
    }

}
