package org.alfresco.repo.node.db;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.alfresco.repo.dictionary.AspectDefinition;
import org.alfresco.repo.dictionary.ClassDefinition;
import org.alfresco.repo.dictionary.ClassRef;
import org.alfresco.repo.dictionary.DictionaryService;
import org.alfresco.repo.dictionary.PropertyDefinition;
import org.alfresco.repo.dictionary.TypeDefinition;
import org.alfresco.repo.dictionary.bootstrap.DictionaryBootstrap;
import org.alfresco.repo.domain.ChildAssoc;
import org.alfresco.repo.domain.ContainerNode;
import org.alfresco.repo.domain.Node;
import org.alfresco.repo.domain.NodeAssoc;
import org.alfresco.repo.domain.NodeKey;
import org.alfresco.repo.domain.RealNode;
import org.alfresco.repo.domain.Store;
import org.alfresco.repo.node.AbstractNodeServiceImpl;
import org.alfresco.repo.node.AssociationExistsException;
import org.alfresco.repo.node.CyclicChildRelationshipException;
import org.alfresco.repo.node.InvalidAspectException;
import org.alfresco.repo.node.InvalidNodeRefException;
import org.alfresco.repo.node.InvalidNodeTypeException;
import org.alfresco.repo.node.InvalidStoreRefException;
import org.alfresco.repo.node.PropertyException;
import org.alfresco.repo.node.StoreExistsException;
import org.alfresco.repo.ref.ChildAssocRef;
import org.alfresco.repo.ref.EntityRef;
import org.alfresco.repo.ref.NodeAssocRef;
import org.alfresco.repo.ref.NodeRef;
import org.alfresco.repo.ref.Path;
import org.alfresco.repo.ref.QName;
import org.alfresco.repo.ref.StoreRef;
import org.alfresco.repo.ref.qname.QNamePattern;
import org.alfresco.util.debug.CodeMonkey;

/**
 * Node service using database persistence layer to fulfill functionality
 * 
 * @author Derek Hulley
 */
public class DbNodeServiceImpl extends AbstractNodeServiceImpl
{
    private final DictionaryService dictionaryService;
    private final NodeDaoService nodeDaoService;
    
    public DbNodeServiceImpl(
            DictionaryService dictionaryService,
            NodeDaoService nodeDaoService)
    {
        this.dictionaryService = dictionaryService;
        this.nodeDaoService = nodeDaoService;
    }

    /**
     * @param storeRef store to search for
     * @return Returns a non-null <code>Store</code> instance
     * @throws InvalidStoreRefException if the reference is to a store that doesn't exist
     */
    private Store getStoreNotNull(StoreRef storeRef) throws InvalidStoreRefException
    {
        Store store = nodeDaoService.getStore(storeRef.getProtocol(), storeRef.getIdentifier());
        if (store == null)
        {
            throw new InvalidStoreRefException(storeRef);
        }
        return store;
    }

    /**
     * Performs a null- and type-safe check before returning the <b>container</b> node
     * @param nodeRef a reference to a container node
     * @return Returns an instance of a container node (never null)
     * @throws InvalidNodeRefException if the node referenced doesn't exist
     * @throws RuntimeException if the reference is to a node type that is incompatible with the return type
     */
    private ContainerNode getContainerNodeNotNull(NodeRef nodeRef) throws InvalidNodeRefException
    {
        Node unchecked = getNodeNotNull(nodeRef);
        if (!(unchecked instanceof ContainerNode))
        {
            throw new RuntimeException("Node must be of type " +
                    ContainerNode.class.getName() + ": " + nodeRef);
        }
        return (ContainerNode) unchecked;
    }
    
    /**
     * Performs a null- and type-safe check before returning the <b>real</b> node
     * @param nodeRef a reference to a real node
     * @return Returns an instance of a real node (never null)
     * @throws InvalidNodeRefException if the node referenced doesn't exist
     * @throws RuntimeException if the reference is to a node type that is incompatible with the return type
     */
    private RealNode getRealNodeNotNull(NodeRef nodeRef) throws InvalidNodeRefException
    {
        Node unchecked = getNodeNotNull(nodeRef);
        if (!(unchecked instanceof RealNode))
        {
            throw new RuntimeException("Node must be of type " +
                    RealNode.class.getName() + ": " + nodeRef);
        }
        return (RealNode) unchecked;
    }
    
    /**
     * Performs a null-safe get of the node
     * 
     * @param nodeRef the node to retrieve
     * @return Returns the node entity (never null)
     * @throws InvalidNodeRefException if the referenced node could not be found
     */
    private Node getNodeNotNull(NodeRef nodeRef) throws InvalidNodeRefException
    {
        String protocol = nodeRef.getStoreRef().getProtocol();
        String identifier = nodeRef.getStoreRef().getIdentifier();
        Node unchecked = nodeDaoService.getNode(protocol, identifier, nodeRef.getId());
        if (unchecked == null)
        {
            throw new InvalidNodeRefException("Node does not exist: " + nodeRef, nodeRef);
        }
        return unchecked;
    }

    public boolean exists(StoreRef storeRef)
    {
        Store store = nodeDaoService.getStore(storeRef.getProtocol(), storeRef.getIdentifier());
        boolean exists = (store != null);
        // done
        return exists;
    }
    
    public boolean exists(NodeRef nodeRef)
    {
        StoreRef storeRef = nodeRef.getStoreRef();
        Node node = nodeDaoService.getNode(storeRef.getProtocol(),
                storeRef.getIdentifier(),
                nodeRef.getId());
        boolean exists = (node != null);
        // done
        return exists;
    }
    
    /**
     * Defers to the typed service
     * @see StoreDaoService#createWorkspace(String)
     */
    public StoreRef createStore(String protocol, String identifier)
    {
        // check that the store does not already exist
        Store store = nodeDaoService.getStore(protocol, identifier);
        if (store != null)
        {
            throw new StoreExistsException("Unable to create a store that already exists",
                    new StoreRef(protocol, identifier));
        }
        // create a new one
        store = nodeDaoService.createStore(protocol, identifier);
        // get the root node
        Node rootNode = store.getRootNode();
        // assign the root aspect - this is expected of all roots, even store roots
        addAspect(rootNode.getNodeRef(),
                DictionaryBootstrap.ASPECT_ROOT,
                Collections.EMPTY_MAP);
        // done
        StoreRef storeRef = store.getStoreRef();
        return storeRef;
    }

    public NodeRef getRootNode(StoreRef storeRef) throws InvalidStoreRefException
    {
        Store store = nodeDaoService.getStore(storeRef.getProtocol(), storeRef.getIdentifier());
        // get the root
        Node node = store.getRootNode();
        NodeRef nodeRef = node.getNodeRef();
        // done
        return nodeRef;
    }

    public ChildAssocRef createNode(NodeRef parentRef,
            QName qname,
            ClassRef typeRef)
    {
        return this.createNode(parentRef, qname, typeRef, null);
    }

    public ChildAssocRef createNode(NodeRef parentRef,
            QName qname,
            ClassRef typeRef,
            Map<QName, Serializable> properties)
    {
        CodeMonkey.todo("Check that the child association is allowed"); // TODO
        if (properties == null)
        {
            properties = Collections.emptyMap();
        }
        
        // get the store that the parent belongs to
        StoreRef storeRef = parentRef.getStoreRef();
        Store store = nodeDaoService.getStore(storeRef.getProtocol(), storeRef.getIdentifier());
        if (store == null)
        {
            throw new RuntimeException("No store found for parent node: " + parentRef);
        }
        // create the node instance
        RealNode node = nodeDaoService.newRealNode(store, typeRef);
        // get the parent node
        ContainerNode parentNode = getContainerNodeNotNull(parentRef);
        // create the association
        ChildAssoc assoc = nodeDaoService.newChildAssoc(parentNode, node, true, qname);
        
        // get the mandatory aspects for the node type
        TypeDefinition nodeTypeDef = dictionaryService.getType(typeRef);
        if (nodeTypeDef == null)
        {
            throw new InvalidNodeTypeException(typeRef);
        }
        List<AspectDefinition> defaultAspectDefs = nodeTypeDef.getDefaultAspects();
        // check that property requirements are met
        checkProperties(nodeTypeDef, defaultAspectDefs, properties);
        
        // add all the aspects to the node
        Set<QName> nodeAspects = node.getAspects();
        for (AspectDefinition defaultAspectDef : defaultAspectDefs)
        {
            nodeAspects.add(defaultAspectDef.getQName());
        }
        
        // set the properties - it is a new node so only set properties if there are any
        if (properties.size() > 0)
        {
            this.setProperties(node.getNodeRef(), properties);
        }
        
        // done
        return assoc.getChildAssocRef();
    }

    public ClassRef getType(NodeRef nodeRef) throws InvalidNodeRefException
    {
        Node node = getNodeNotNull(nodeRef);
        ClassRef classRef = new ClassRef(node.getTypeQName());
        return  classRef;
    }
    
    /**
     * Checks that the properties required by the given classes or aspects are all present
     * in the map given.
     * <p>
     * No dependencies are fetched e.g. if a <code>TypeDefinition</code> is given, the
     * aspects for that type will not be checked.
     * 
     * @param classDef the primary class definition to check against
     * @param aspectDefs additional aspects definitions to check against - may be null
     * @param properties the properties to check
     * @throws PropertyException if the class or aspect requires a property not present
     *      amongst those provided
     */
    private void checkProperties(
            ClassDefinition classDef,
            List<AspectDefinition> aspectDefs,
            Map<QName, Serializable> properties)
            throws PropertyException
    {
        // get properties applicable to the class/aspect itself
        List<PropertyDefinition> propertyDefs = classDef.getProperties(); 
        
        // get properties of the additional aspects
        if (aspectDefs != null)
        {
            for (AspectDefinition aspectDef : aspectDefs)
            {
                List<PropertyDefinition> aspectProperties = aspectDef.getProperties();
                propertyDefs.addAll(aspectProperties);
            }
        }
        
        // check that each required property is present
        for (PropertyDefinition propertyDef : propertyDefs)
        {
            // ignore optional properties
            if (!propertyDef.isMandatory())
            {
                continue;
            }
            QName qname = propertyDef.getQName();
            // is it present?
            if (properties.get(qname) == null)
            {
                // not present
                throw new PropertyException("Mandatory property value not supplied: " + qname,
                        propertyDef.getReference());
            }
            // property has a value
        }
        // all required properties have values
        // done
    }
    
    /**
     * @param node
     * @return Returns a list of all aspects (default and optional) applied to the node
     */
    private List<AspectDefinition> getNodeAspects(Node node)
    {
        Set<QName> aspectQNames = node.getAspects();
        List<AspectDefinition> aspectDefs = new ArrayList<AspectDefinition>(aspectQNames.size());
        for (QName qname : aspectQNames)
        {
            ClassRef aspectRef = new ClassRef(qname);
            AspectDefinition aspectDef = dictionaryService.getAspect(aspectRef);
            aspectDefs.add(aspectDef);
        }
        // done
        return aspectDefs;
    }
    
    /**
     * @see #checkProperties(ClassDefinition, List<AspectDefinition>, Map<QName,Serializable>)
     * @see Node#getAspects()
     */
    public void addAspect(
            NodeRef nodeRef,
            ClassRef aspectRef,
            Map<QName, Serializable> aspectProperties)
            throws InvalidNodeRefException, InvalidAspectException, PropertyException
    {
        // get the aspect
        AspectDefinition aspectDef = dictionaryService.getAspect(aspectRef);
        if (aspectDef == null)
        {
            throw new InvalidAspectException(aspectRef);
        }
        // check that the properties supplied are adequate for the aspect
        checkProperties(aspectDef, null, aspectProperties);
        
        Node node = getNodeNotNull(nodeRef);
        // physically attach the aspect to the node
        node.getAspects().add(aspectRef.getQName());
        
        // attach the properties to the current node properties
        Map<QName, Serializable> nodeProperties = getProperties(nodeRef);
        nodeProperties.putAll(aspectProperties);
        setProperties(nodeRef, nodeProperties);
        // done
    }

    /**
     * @see Node#getAspects()
     */
    public void removeAspect(NodeRef nodeRef, ClassRef aspectRef)
            throws InvalidNodeRefException, InvalidAspectException
    {
        // get the aspect
        AspectDefinition aspectDef = dictionaryService.getAspect(aspectRef);
        if (aspectDef == null)
        {
            throw new InvalidAspectException(aspectRef);
        }
        QName aspectQName = aspectDef.getQName();
        // get the node
        Node node = getNodeNotNull(nodeRef);
        
        // check that the aspect may be removed
        ClassRef nodeTypeRef = new ClassRef(node.getTypeQName());
        TypeDefinition nodeTypeDef = dictionaryService.getType(nodeTypeRef);
        if (nodeTypeDef == null)
        {
            throw new InvalidNodeRefException("The node type is no longer valid: " + nodeRef, nodeRef);
        }
        List<AspectDefinition> defaultAspects = nodeTypeDef.getDefaultAspects();
        if (defaultAspects.contains(aspectDef))
        {
            throw new InvalidAspectException("The aspect is a default for the node's type and cannot be removed: " + aspectRef, aspectRef);
        }
        
        // remove the aspect, if present
        boolean removed = node.getAspects().remove(aspectQName);
        // if the aspect was present, remove the associated properties
        if (removed)
        {
            Map<String, Serializable> nodeProperties = node.getProperties();
            List<PropertyDefinition> propertyDefs = aspectDef.getProperties();
            for (PropertyDefinition propertyDef : propertyDefs)
            {
                nodeProperties.remove(propertyDef.getQName().toString());
            }
        }
        // done
        return;
    }

    /**
     * Performs a check on the set of node aspects
     * 
     * @see Node#getAspects()
     */
    public boolean hasAspect(NodeRef nodeRef, ClassRef aspectRef) throws InvalidNodeRefException, InvalidAspectException
    {
        Node node = getNodeNotNull(nodeRef);
        Set<QName> aspectQNames = node.getAspects();
        
        QName aspectQName = aspectRef.getQName();
        
        boolean hasAspect = aspectQNames.contains(aspectQName);
        // done
        return hasAspect;
    }

    /**
     * Transforms the results from {@link Node#getAspects()} into an unmodifiable set
     */
    public Set<ClassRef> getAspects(NodeRef nodeRef) throws InvalidNodeRefException
    {
        Node node = getNodeNotNull(nodeRef);
        Set<QName> aspectQNames = node.getAspects();
        Set<ClassRef> aspectRefs = new HashSet<ClassRef>(aspectQNames.size());
        // transform to an a list of ClassRef instances
        for (QName qname : aspectQNames)
        {
            ClassRef aspectRef = new ClassRef(qname);
            aspectRefs.add(aspectRef);
        }
        // done
        return Collections.unmodifiableSet(aspectRefs);
    }

    public void deleteNode(NodeRef nodeRef)
    {
		// get the store
		StoreRef storeRef = nodeRef.getStoreRef();
        // get the node
        Node node = getNodeNotNull(nodeRef);
        // delete it
        nodeDaoService.deleteNode(node);
    }

    public ChildAssocRef addChild(NodeRef parentRef, NodeRef childRef, QName qname) throws InvalidNodeRefException
    {
        CodeMonkey.todo("Check that the child association is allowed"); // TODO
        // check that both nodes belong to the same store
        if (!parentRef.getStoreRef().equals(childRef.getStoreRef()))
        {
            throw new InvalidNodeRefException("Parent and child nodes must belong to the same store: \n" +
                    "   parent: " + parentRef + "\n" +
                    "   child: " + childRef,
                    childRef);
        }
        String protocol = parentRef.getStoreRef().getProtocol();
        String identifier = parentRef.getStoreRef().getIdentifier();
        // get the parent node and ensure that it is a container node
        ContainerNode parentNode = getContainerNodeNotNull(parentRef);
        // get the child node
        Node childNode = getNodeNotNull(childRef);
        // make the association
        ChildAssoc assoc = nodeDaoService.newChildAssoc(parentNode, childNode, false, qname);
        // done
        return assoc.getChildAssocRef();
    }

    public Collection<EntityRef> removeChild(NodeRef parentRef, NodeRef childRef) throws InvalidNodeRefException
    {
        ContainerNode parentNode = getContainerNodeNotNull(parentRef);
        Node childNode = getNodeNotNull(childRef);
        NodeKey childNodeKey = childNode.getKey();
        
        // maintain a list of deleted entities
        List<EntityRef> deletedRefs = new ArrayList<EntityRef>(5);
        
        // get all the child assocs
        boolean deleteChild = false;
        Set<ChildAssoc> assocs = parentNode.getChildAssocs();
        assocs = new HashSet<ChildAssoc>(assocs);   // copy set as we will be modifying it
        for (ChildAssoc assoc : assocs)
        {
            if (!assoc.getChild().getKey().equals(childNodeKey))
            {
                continue;  // not a matching association
            }
            // is this a primary association?
            if (assoc.getIsPrimary())
            {
                deleteChild = true;
            }
            // delete the association instance
            nodeDaoService.deleteChildAssoc(assoc);
            deletedRefs.add(assoc.getChildAssocRef());    // save for return value
        }
        // must the child be deleted?
        if (deleteChild)
        {
            nodeDaoService.deleteNode(childNode);
            deletedRefs.add(childNode.getNodeRef());    // save for return value
        }
        // done
        return deletedRefs;
    }

    public Collection<EntityRef> removeChildren(NodeRef parentRef, QName qname) throws InvalidNodeRefException
    {
        ContainerNode parentNode = getContainerNodeNotNull(parentRef);

        // maintain a list of deleted entities
        List<EntityRef> deletedRefs = new ArrayList<EntityRef>(5);
        
        // get all the child assocs
        Set<ChildAssoc> assocs = parentNode.getChildAssocs();
        assocs = new HashSet<ChildAssoc>(assocs);   // copy set as we will be modifying it
        for (ChildAssoc assoc : assocs)
        {
            if (!assoc.getQName().equals(qname))
            {
                continue;   // not a matching association
            }
            // we have a match
            nodeDaoService.deleteChildAssoc(assoc);
            deletedRefs.add(assoc.getChildAssocRef());    // save for return value
            // must we remove the child?
            if (assoc.getIsPrimary())
            {
                Node childNode = assoc.getChild();
                nodeDaoService.deleteNode(childNode);
                deletedRefs.add(childNode.getNodeRef());    // save for return value
            }
        }
        // done
        return deletedRefs;
    }

    public Map<QName, Serializable> getProperties(NodeRef nodeRef) throws InvalidNodeRefException
    {
        Node node = getNodeNotNull(nodeRef);
        Map<String, Serializable> nodeProperties = node.getProperties();
        Map<QName, Serializable> ret = new HashMap<QName, Serializable>(nodeProperties.size());
        // copy values
        for (Map.Entry entry: nodeProperties.entrySet())
        {
            String key = (String) entry.getKey();
            Serializable value = (Serializable) entry.getValue();
            QName qname = QName.createQName(key.toString());
            // copy across
            ret.put(qname, value);
        }
        return ret;
    }
    
    public Serializable getProperty(NodeRef nodeRef, QName qname) throws InvalidNodeRefException
    {
        Node node = getNodeNotNull(nodeRef);
        Map<String, Serializable> properties = node.getProperties();
        return properties.get(qname.toString());
    }

    /**
     * Ensures that all required properties are present on the node and copies the
     * property values to the <code>Node</code>.
     * <p>
     * Null-valued properties are removed.
     * 
     * @see #checkProperties(ClassDefinition, List<AspectDefinition>, Map<QName,Serializable>)
     * @see Node#getProperties()
     */
    public void setProperties(NodeRef nodeRef, Map<QName, Serializable> properties) throws InvalidNodeRefException
    {
        if (properties == null)
        {
            throw new IllegalArgumentException("Properties may not be null");
        }
        Node node = getNodeNotNull(nodeRef);

        // check that the properties fulfill all the requirements of the node type
        // and any additional aspects
        ClassRef nodeClassRef = getType(nodeRef);
        ClassDefinition nodeClassDef = dictionaryService.getClass(nodeClassRef);
        List<AspectDefinition> nodeAspectDefs = getNodeAspects(node);
        checkProperties(nodeClassDef, nodeAspectDefs, properties);  // confirms that properties are valid
        
        // copy properties onto node
        Map<String, Serializable> nodeProperties = node.getProperties();
        nodeProperties.clear();
        // copy all the values across
        for (QName qname : properties.keySet())
        {
            Serializable value = properties.get(qname);
            if (value == null)
            {
                throw new IllegalArgumentException("Property values may not be null: " + qname);
            }
            nodeProperties.put(qname.toString(), properties.get(qname));
        }
        // done
    }

    /**
     * Null values are not allowed for properties - hence no checking is done against
     * the node type definition as this is only an addition or modification of a
     * property.
     */
    public void setProperty(NodeRef nodeRef, QName qname, Serializable value) throws InvalidNodeRefException
    {
        if (value == null)
        {
            throw new IllegalArgumentException("Property values may not be null: " + qname);
        }
        Node node = getNodeNotNull(nodeRef);
        Map<String, Serializable> properties = node.getProperties();
        // Null value means remove property
        properties.put(qname.toString(), value);
        // done
    }

    /**
     * Transforms {@link Node#getParentAssocs()} into an unmodifiable collection
     */
    public Collection<NodeRef> getParents(NodeRef nodeRef) throws InvalidNodeRefException
    {
        Node node = getNodeNotNull(nodeRef);
        // get the assocs pointing to it
        Set<ChildAssoc> parentAssocs = node.getParentAssocs();
        // list of results
        Collection<NodeRef> results = new ArrayList<NodeRef>(parentAssocs.size());
        for (ChildAssoc assoc : parentAssocs)
        {
            // get the parent
            Node parentNode = assoc.getParent();
            results.add(parentNode.getNodeRef());
        }
        // done
        return Collections.unmodifiableCollection(results);
    }

    /**
     * Filters out any associations if their qname is not a match to the given pattern.
     * <p>
     * Transforms {@link Node#getParentAssocs()} into an unmodifiable list
     */
    public List<ChildAssocRef> getParentAssocs(NodeRef nodeRef, QNamePattern qnamePattern)
    {
        Node node = getNodeNotNull(nodeRef);
        // get the assocs pointing to it
        Set<ChildAssoc> parentAssocs = node.getParentAssocs();
        // list of results
        List<ChildAssocRef> results = new ArrayList<ChildAssocRef>(parentAssocs.size());
        for (ChildAssoc assoc : parentAssocs)
        {
            // does the qname match the pattern?
            if (!qnamePattern.isMatch(assoc.getQName()))
            {
                // no match - ignore
                continue;
            }
            results.add(assoc.getChildAssocRef());
        }
        // done
        return Collections.unmodifiableList(results);
    }

    /**
     * Filters out any associations if their qname is not a match to the given pattern.
     * <p>
     * Transforms {@link ContainerNode#getChildAssocs()} into an unmodifiable list.
     */
    public List<ChildAssocRef> getChildAssocs(NodeRef nodeRef, QNamePattern qnamePattern)
    {
        Node nodeUnchecked = getNodeNotNull(nodeRef);
        if (!(nodeUnchecked instanceof ContainerNode))
        {
            // not being a container, we just return an empty collection
            return Collections.emptyList();
        }
        ContainerNode node = (ContainerNode) nodeUnchecked;
        // get the assocs pointing from it
        Set<ChildAssoc> childAssocs = node.getChildAssocs();
        // list of results
        List<ChildAssocRef> results = new ArrayList<ChildAssocRef>(childAssocs.size());
        for (ChildAssoc assoc : childAssocs)
        {
            // does the qname match the pattern?
            if (!qnamePattern.isMatch(assoc.getQName()))
            {
                // no match - ignore
                continue;
            }
            // get the child
            results.add(assoc.getChildAssocRef());
        }
        // done
        return Collections.unmodifiableList(results);
    }

    public ChildAssocRef getPrimaryParent(NodeRef nodeRef) throws InvalidNodeRefException
    {
        Node node = getNodeNotNull(nodeRef);
        // get the primary parent assoc
        ChildAssoc assoc = nodeDaoService.getPrimaryParentAssoc(node);

        // done - the assoc may be null for a root node
        ChildAssocRef assocRef = null;
        if (assoc == null)
        {
            assocRef = new ChildAssocRef(null, null, nodeRef);
        }
        else
        {
            assocRef = assoc.getChildAssocRef();
        }
        return assocRef;
    }

    public NodeAssocRef createAssociation(NodeRef sourceRef, NodeRef targetRef, QName qname)
            throws InvalidNodeRefException, AssociationExistsException
    {
        CodeMonkey.todo("Check that the association is allowed"); // TODO
        RealNode sourceNode = getRealNodeNotNull(sourceRef);
        Node targetNode = getNodeNotNull(targetRef);
        // see if it exists
        NodeAssoc assoc = nodeDaoService.getNodeAssoc(sourceNode, targetNode, qname);
        if (assoc != null)
        {
            throw new AssociationExistsException(sourceRef, targetRef, qname);
        }
        // we are sure that the association doesn't exist - make it
        assoc = nodeDaoService.newNodeAssoc(sourceNode, targetNode, qname);
        NodeAssocRef assocRef = assoc.getNodeAssocRef();
        // done
        return assocRef;
    }

    public void removeAssociation(NodeRef sourceRef, NodeRef targetRef, QName qname)
            throws InvalidNodeRefException
    {
        RealNode sourceNode = getRealNodeNotNull(sourceRef);
        Node targetNode = getNodeNotNull(targetRef);
        // get the association
        NodeAssoc assoc = nodeDaoService.getNodeAssoc(sourceNode, targetNode, qname);
        // delete it
        nodeDaoService.deleteNodeAssoc(assoc);
    }

    /**
     * Transforms {@link NodeDaoService#getNodeAssocTargets(RealNode, String)} into
     * an unmodifiable collection.
     */
    public List<NodeAssocRef> getTargetAssocs(NodeRef sourceRef, QNamePattern qnamePattern)
            throws InvalidNodeRefException
    {
        RealNode sourceNode = getRealNodeNotNull(sourceRef);
        // get all assocs to target
        Set<NodeAssoc> assocs = sourceNode.getTargetNodeAssocs();
        List<NodeAssocRef> nodeAssocRefs = new ArrayList<NodeAssocRef>(assocs.size());
        for (NodeAssoc assoc : assocs)
        {
            // check qname pattern
            if (!qnamePattern.isMatch(assoc.getQName()))
            {
                continue;   // the assoc name doesn't match the pattern given 
            }
            nodeAssocRefs.add(assoc.getNodeAssocRef());
        }
        // done
        return Collections.unmodifiableList(nodeAssocRefs);
    }

    /**
     * Transforms {@link NodeDaoService#getNodeAssocSources(Node, String)} into
     * an unmodifiable collection.
     */
    public List<NodeAssocRef> getSourceAssocs(NodeRef targetRef, QNamePattern qnamePattern)
            throws InvalidNodeRefException
    {
        RealNode sourceNode = getRealNodeNotNull(targetRef);
        // get all assocs to source
        Set<NodeAssoc> assocs = sourceNode.getSourceNodeAssocs();
        List<NodeAssocRef> nodeAssocRefs = new ArrayList<NodeAssocRef>(assocs.size());
        for (NodeAssoc assoc : assocs)
        {
            // check qname pattern
            if (!qnamePattern.isMatch(assoc.getQName()))
            {
                continue;   // the assoc name doesn't match the pattern given 
            }
            nodeAssocRefs.add(assoc.getNodeAssocRef());
        }
        // done
        return Collections.unmodifiableList(nodeAssocRefs);
    }
    
    /**
     * Recursive method used to build up paths from a given node to the root.
     * <p>
     * Whilst walking up the hierarchy to the root, some nodes may have a <b>root</b> aspect.
     * Everytime one of these is encountered, a new path is farmed off, but the method
     * continues to walk up the hierarchy.
     * 
     * @param currentNode the node to start from, i.e. the child node to work upwards from
     * @param currentPath the path from the current node to the descendent that we started from
     * @param completedPaths paths that have reached the root are added to this collection
     * @param assocStack the parent-child relationships traversed whilst building the path.
     *      Used to detected cyclic relationships.
     * @param primaryOnly true if only the primary parent association must be traversed.
     *      If this is true, then the only root is the top level node having no parents.
     * @throws CyclicChildRelationshipException
     */
    private void prependPaths(final Node currentNode,
            final Path currentPath,
            Collection<Path> completedPaths,
            Stack<ChildAssoc> assocStack,
            boolean primaryOnly)
        throws CyclicChildRelationshipException
    {
        NodeRef currentNodeRef = currentNode.getNodeRef();
        // get the parent associations of the given node
        Set<ChildAssoc> parentAssocs = currentNode.getParentAssocs();
        // does the node have parents
        boolean hasParents = parentAssocs.size() > 0;
        // does the current node have a root aspect?
        boolean isRoot = hasAspect(currentNodeRef, DictionaryBootstrap.ASPECT_ROOT);
        
        // look for a root.  If we only want the primary root, then ignore all but the
        // top-level root.
        if (isRoot && !(primaryOnly && hasParents))  // exclude primary search with parents present
        {
            // create a one-sided assoc ref for the root node and prepend to the stack
            ChildAssocRef assocRef = new ChildAssocRef(null, null, getRootNode(currentNode.getNodeRef().getStoreRef()));
            // create a path to save and add the 'root' assoc
            Path pathToSave = new Path();
            Path.ChildAssocElement first = null;
            for(Path.Element element: currentPath)
            {
                if(first == null)
                {
                    first = (Path.ChildAssocElement)element;
                }
                else
                {
                    pathToSave.append(element);
                }
            }
            if(first != null)
            {
               ChildAssocRef updateAssocRef = new ChildAssocRef(getRootNode(currentNode.getNodeRef().getStoreRef()), first.getRef().getQName(), first.getRef().getChildRef());
               Path.Element newFirst =  new Path.ChildAssocElement(updateAssocRef);
               pathToSave.prepend(newFirst);
            }
            
            Path.Element element = new Path.ChildAssocElement(assocRef);
            pathToSave.prepend(element);
            // append the current path elements onto the 'root' assoc
            //pathToSave.append(currentPath);
            completedPaths.add(pathToSave);
        }

        if (parentAssocs.size() == 0 && !isRoot)
        {
            throw new RuntimeException("Node without parents does not have root aspect: " +
                    currentNodeRef);
        }
        // walk up each parent association
        for (ChildAssoc assoc : parentAssocs)
        {
            // does the association already exist in the stack
            if (assocStack.contains(assoc))
            {
                // the association was present already
                throw new CyclicChildRelationshipException(
                        "Cyclic parent-child relationship detected: \n" +
                        "   current node: " + currentNode + "\n" +
                        "   current path: " + currentPath + "\n" +
                        "   next assoc: " + assoc,
                        assoc);
            }
            // do we consider only primary assocs?
            if (primaryOnly && !assoc.getIsPrimary())
            {
                continue;
            }
            // build a path element
            NodeRef parentRef = assoc.getParent().getNodeRef();
            QName qname = assoc.getQName();
            NodeRef childRef = assoc.getChild().getNodeRef();
            boolean isPrimary = assoc.getIsPrimary();
            ChildAssocRef assocRef = new ChildAssocRef(parentRef, qname, childRef, isPrimary, -1);
            CodeMonkey.issue("Is ordering relevant here?");  // TODO: consider ordering
            Path.Element element = new Path.ChildAssocElement(assocRef);
            // create a new path that builds on the current path
            Path path = new Path();
            path.append(currentPath);
            // prepend element
            path.prepend(element);
            // get parent node
            Node parentNode = assoc.getParent();
            
            // push the assoc stack, recurse and pop
            assocStack.push(assoc);
            prependPaths(parentNode, path, completedPaths, assocStack, primaryOnly);
            assocStack.pop();
        }
        // done
    }

    /**
     * @see #getPaths(NodeRef, boolean)
     * @see #prependPaths(Node, Path, Collection<Path>, Stack<ChildAssoc>, boolean)
     */
    public Path getPath(NodeRef nodeRef) throws InvalidNodeRefException
    {
        Collection<Path> paths = getPaths(nodeRef, true);   // checks primary path count
        if (paths.size() == 1)
        {
            for (Path path : paths)
            {
                return path;   // we know there is only one
            }
        }
        throw new RuntimeException("Primary path count not checked");  // checked by getPaths()
    }

    /**
     * When searching for <code>primaryOnly == true</code>, checks that there is exactly
     * one path.
     * @see #prependPaths(Node, Path, Collection<Path>, Stack<ChildAssoc>, boolean)
     */
    public Collection<Path> getPaths(NodeRef nodeRef, boolean primaryOnly) throws InvalidNodeRefException
    {
        // get the starting node
        Node node = getNodeNotNull(nodeRef);
        // create storage for the paths - only need 1 bucket if we are looking for the primary path
        Collection<Path> paths = new ArrayList<Path>(primaryOnly ? 1 : 10);
        // create an emtpy current path to start from
        Path currentPath = new Path();
        // create storage for touched associations
        Stack<ChildAssoc> assocStack = new Stack<ChildAssoc>();
        // call recursive method to sort it out
        prependPaths(node, currentPath, paths, assocStack, primaryOnly);
        
        // check that for the primary only case we have exactly one path
        if (primaryOnly && paths.size() != 1)
        {
            throw new RuntimeException("Node has " + paths.size() + " primary paths: " + nodeRef);
        }
        
        // done
        return Collections.unmodifiableCollection(paths);
    }
}
