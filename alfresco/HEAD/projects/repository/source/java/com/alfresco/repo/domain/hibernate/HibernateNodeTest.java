package org.alfresco.repo.domain.hibernate;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.alfresco.repo.dictionary.NamespaceService;
import org.alfresco.repo.dictionary.bootstrap.DictionaryBootstrap;
import org.alfresco.repo.domain.ChildAssoc;
import org.alfresco.repo.domain.ContainerNode;
import org.alfresco.repo.domain.Node;
import org.alfresco.repo.domain.NodeAssoc;
import org.alfresco.repo.domain.NodeKey;
import org.alfresco.repo.domain.RealNode;
import org.alfresco.repo.domain.Store;
import org.alfresco.repo.domain.StoreKey;
import org.alfresco.repo.ref.QName;
import org.alfresco.repo.ref.StoreRef;
import org.alfresco.util.BaseHibernateTest;
import org.alfresco.util.GUID;

/**
 * Test persistence and retrieval of Hibernate-specific implementations of the
 * {@link org.alfresco.repo.domain.Node} interface
 * 
 * @author Derek Hulley
 */
public class HibernateNodeTest extends BaseHibernateTest
{
    private Store store;
    
    public HibernateNodeTest()
    {
    }
    
    protected void onSetUpInTransaction() throws Exception
    {
        store = new StoreImpl();
		StoreKey storeKey = new StoreKey(StoreRef.PROTOCOL_WORKSPACE,
                "TestWorkspace@" + System.currentTimeMillis());
		store.setKey(storeKey);
        // persist so that it is present in the hibernate cache
        getSession().save(store);
    }
    
    protected void onTearDownInTransaction()
    {
        // force a flush to ensure that the database updates succeed
        getSession().flush();
        getSession().clear();
    }

    public void testSetUp() throws Exception
    {
        assertNotNull("Workspace not initialised", store);
    }

	public void testGetStore() throws Exception
	{
        // create a new Node
        Node node = new NodeImpl();
		NodeKey key = new NodeKey("Random Protocol", "Random Identifier", "AAA");
		node.setKey(key);
        node.setStore(store);   // not meaningful as it contradicts the key
        node.setTypeQName(DictionaryBootstrap.TYPE_QNAME_CONTAINER);
        // persist it
		try
		{
			Serializable id = getSession().save(node);
			fail("No store exists");
		}
		catch (Throwable e)
		{
			// expected
		}
		// this should not solve the problem
        node.setStore(store);
        // persist it
		try
		{
			Serializable id = getSession().save(node);
			fail("Setting store does not persist protocol and identifier attributes");
		}
		catch (Throwable e)
		{
			// expected
		}
		
		// fix the key
		key = new NodeKey(store.getKey().getProtocol(), store.getKey().getIdentifier(), "AAA");
		node.setKey(key);
		// now it should work
		Serializable id = getSession().save(node);

        // throw the reference away and get the a new one for the id
        node = (Node) getSession().load(NodeImpl.class, id);
        assertNotNull("Node not found", node);
		// check that the store has been loaded
		Store loadedStore = node.getStore();
		assertNotNull("Store not present on node", loadedStore);
		assertEquals("Incorrect store key", store, loadedStore);
	}
	
    /**
     * Check that properties can be persisted and retrieved
     */
    public void testProperties() throws Exception
    {
        // create a new Node
        Node node = new NodeImpl();
		NodeKey key = new NodeKey(store.getKey(), "AAA");
		node.setKey(key);
        node.setTypeQName(DictionaryBootstrap.TYPE_QNAME_CONTAINER);
        // give it a property map
        Map<String, Serializable> propertyMap = new HashMap<String, Serializable>(5);
        propertyMap.put("{}A", "AAA");
        node.getProperties().putAll(propertyMap);
        // persist it
        Serializable id = getSession().save(node);

        // throw the reference away and get the a new one for the id
        node = (Node) getSession().load(NodeImpl.class, id);
        assertNotNull("Node not found", node);
        // extract the Map
        propertyMap = node.getProperties();
        assertNotNull("Map not persisted", propertyMap);
        // ensure that the value is present
        assertNotNull("Property value not present in map", propertyMap.get("{}A"));
    }

    public void testSubclassing() throws Exception
    {
        // persist a subclass of Node
        Node node = new ContainerNodeImpl();
		NodeKey key = new NodeKey(store.getKey(), "AAA");
		node.setKey(key);
        node.setTypeQName(DictionaryBootstrap.TYPE_QNAME_CONTAINER);
        Serializable id = getSession().save(node);
        // get the node back
        node = (Node) getSession().get(NodeImpl.class, id);
        // check
        assertNotNull("Persisted node not found", id);
        assertTrue("Subtype not retrieved", node instanceof ContainerNode);
    }

    /**
     * Check that aspect qnames can be added and removed from a node and that they
     * are persisted correctly 
     */
    public void testAspects() throws Exception
    {
        // make a real node
        Node node = new RealNodeImpl();
        NodeKey nodeKey = new NodeKey(store.getKey(), GUID.generate());
        node.setKey(nodeKey);
        node.setStore(store);
        node.setTypeQName(DictionaryBootstrap.TYPE_QNAME_BASE);
        
        // add some aspects
        QName aspect1 = QName.createQName(NamespaceService.alfresco_TEST_URI, "1");
        QName aspect2 = QName.createQName(NamespaceService.alfresco_TEST_URI, "2");
        QName aspect3 = QName.createQName(NamespaceService.alfresco_TEST_URI, "3");
        QName aspect4 = QName.createQName(NamespaceService.alfresco_TEST_URI, "4");
        Set<QName> aspects = node.getAspects();
        aspects.add(aspect1);
        aspects.add(aspect2);
        aspects.add(aspect3);
        aspects.add(aspect4);
        assertFalse("Set did not eliminate duplicate aspect qname", aspects.add(aspect4));
        
        // persist
        Serializable id = getSession().save(node);
        
        // flush and clear
        flushAndClear();
        
        // get node and check aspects
        node = (Node) getSession().get(NodeImpl.class, id);
        assertNotNull("Node not persisted", node);
        aspects = node.getAspects();
        assertEquals("Not all aspects persisted", 4, aspects.size());
    }
    
    public void testNodeAssoc() throws Exception
    {
        // make a real node
        RealNode sourceNode = new RealNodeImpl();
        NodeKey sourceKey = new NodeKey(store.getKey(), GUID.generate());
        sourceNode.setKey(sourceKey);
        sourceNode.setStore(store);
        sourceNode.setTypeQName(DictionaryBootstrap.TYPE_QNAME_BASE);
        Serializable realNodeKey = getSession().save(sourceNode);
        
        // make a container node
        ContainerNode targetNode = new ContainerNodeImpl();
        NodeKey targetKey = new NodeKey(store.getKey(), GUID.generate());
        targetNode.setKey(targetKey);
        targetNode.setStore(store);
        targetNode.setTypeQName(DictionaryBootstrap.TYPE_QNAME_CONTAINER);
        Serializable containerNodeKey = getSession().save(targetNode);
        
        // create an association between them
        NodeAssoc assoc = new NodeAssocImpl();
        assoc.setQName(QName.createQName("next"));
        assoc.buildAssociation(sourceNode, targetNode);
        getSession().save(assoc);
        
        // make another association between the same two nodes
        assoc = new NodeAssocImpl();
        assoc.setQName(QName.createQName("helper"));
        assoc.buildAssociation(sourceNode, targetNode);
        getSession().save(assoc);
        
        // flush and clear the session
        getSession().flush();
        getSession().clear();
        
        // reload the source
        sourceNode = (RealNode) getSession().get(RealNodeImpl.class, sourceKey);
        assertNotNull("Source node not found", sourceNode);
        // check that the associations are present
        assertEquals("Expected exactly 2 target assocs", 2, sourceNode.getTargetNodeAssocs().size());
        
        // reload the target
        targetNode = (ContainerNode) getSession().get(NodeImpl.class, targetKey);
        assertNotNull("Target node not found", targetNode);
        // check that the associations are present
        assertEquals("Expected exactly 2 source assocs", 2, targetNode.getSourceNodeAssocs().size());
    }

    public void testChildAssoc() throws Exception
    {
        // make a content node
        Node contentNode = new RealNodeImpl();
		NodeKey key = new NodeKey(store.getKey(), GUID.generate());
		contentNode.setKey(key);
        contentNode.setStore(store);
        contentNode.setTypeQName(DictionaryBootstrap.TYPE_QNAME_CONTENT);
        Serializable contentNodeKey = getSession().save(contentNode);

        // make a container node
        ContainerNode containerNode = new ContainerNodeImpl();
		key = new NodeKey(store.getKey(), GUID.generate());
		containerNode.setKey(key);
        containerNode.setStore(store);
        containerNode.setTypeQName(DictionaryBootstrap.TYPE_QNAME_CONTAINER);
        Serializable containerNodeKey = getSession().save(containerNode);
        // create an association to the content
        ChildAssoc assoc1 = new ChildAssocImpl();
        assoc1.setIsPrimary(true);
        assoc1.setQName(QName.createQName(null, "number1"));
        assoc1.buildAssociation(containerNode, contentNode);
        getSession().save(assoc1);

        // make another association between the same two parent and child nodes
        ChildAssoc assoc2 = new ChildAssocImpl();
        assoc2.setIsPrimary(true);
        assoc2.setQName(QName.createQName(null, "number2"));
        assoc2.buildAssociation(containerNode, contentNode);
        getSession().save(assoc2);

//        flushAndClear();

        // reload the container
        containerNode = (ContainerNode) getSession().get(ContainerNodeImpl.class, containerNodeKey);
        assertNotNull("Node not found", containerNode);
        // check
        assertEquals("Expected exactly 2 children", 2, containerNode.getChildAssocs().size());
        for (Iterator iterator = containerNode.getChildAssocs().iterator(); iterator.hasNext(); /**/)
        {
            ChildAssoc assoc = (ChildAssoc) iterator.next();
            // the node id must be known
            assertNotNull("Node not populated on assoc", assoc.getChild());
            assertEquals("Node key on child assoc is incorrect", contentNodeKey,
                    assoc.getChild().getKey());
        }

        // check that we can traverse the association from the child
        Set<ChildAssoc> parentAssocs = contentNode.getParentAssocs();
        assertEquals("Expected exactly 2 parent assocs", 2, parentAssocs.size());
        parentAssocs = new HashSet<ChildAssoc>(parentAssocs);
        for (ChildAssoc assoc : parentAssocs)
        {
            // maintain inverse assoc sets
            assoc.removeAssociation();
            // remove the assoc
            getSession().delete(assoc);
        }
        
        // check that the child now has zero parents
        parentAssocs = contentNode.getParentAssocs();
        assertEquals("Expected exactly 0 parent assocs", 0, parentAssocs.size());
    }
}