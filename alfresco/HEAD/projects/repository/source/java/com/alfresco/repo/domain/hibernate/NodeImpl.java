package org.alfresco.repo.domain.hibernate;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.alfresco.repo.domain.ChildAssoc;
import org.alfresco.repo.domain.Node;
import org.alfresco.repo.domain.NodeAssoc;
import org.alfresco.repo.domain.NodeKey;
import org.alfresco.repo.domain.Store;
import org.alfresco.repo.ref.NodeRef;
import org.alfresco.repo.ref.QName;

/**
 * Bean containing all the persistence data representing a <b>node</b>.
 * <p>
 * This implementation of the {@link org.alfresco.repo.domain.Node Node} interface is
 * Hibernate specific.
 * 
 * @author Derek Hulley
 * 
 */
public class NodeImpl implements Node
{
    private NodeKey key;
    private Store store;
    private String typeNamespaceUri;
    private String typeLocalName;
    private Set<QName> aspects;
    private Set<NodeAssoc> sourceNodeAssocs;
    private Set<ChildAssoc> parentAssocs;
    private Map<String, Serializable> properties;
    private NodeRef nodeRef;

    public NodeImpl()
    {
        aspects = new HashSet<QName>(5);
        sourceNodeAssocs = new HashSet<NodeAssoc>(3);
        parentAssocs = new HashSet<ChildAssoc>(3);
        properties = new HashMap<String, Serializable>(5);
    }
    
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        else if (obj == this)
        {
            return true;
        }
        else if (!(obj instanceof Node))
        {
            return false;
        }
        Node that = (Node) obj;
        return (this.getKey().equals(that.getKey()));
    }
    
    public int hashCode()
    {
        return getKey().hashCode();
    }

    public NodeKey getKey() {
		return key;
	}

	public void setKey(NodeKey key) {
		this.key = key;
	}
    
    public Store getStore()
    {
        return store;
    }

    public synchronized void setStore(Store store)
    {
        this.store = store;
        this.nodeRef = null;
    }

    /**
     * @see #getTypeNamespaceUri()
     * @see #getTypeLocalName()
     */
    public QName getTypeQName()
    {
        return QName.createQName(typeNamespaceUri, typeLocalName);
    }

    /**
     * @see #setTypeNamespaceUri(String)
     * @see #setTypeLocalName(String)
     */
    public void setTypeQName(QName qname)
    {
        setTypeNamespaceUri(qname.getNamespaceURI());
        setTypeLocalName(qname.getLocalName());
    }

    /**
     * For Hibernate use
     */
    private String getTypeNamespaceUri()
    {
        return typeNamespaceUri;
    }

    /**
     * For Hibernate use
     */
    private synchronized void setTypeNamespaceUri(String namespaceUri)
    {
        this.typeNamespaceUri = namespaceUri;
        this.nodeRef = null;
    }

    /**
     * For Hibernate use
     */
    private String getTypeLocalName()
    {
        return typeLocalName;
    }

    /**
     * For Hibernate use
     */
    private synchronized void setTypeLocalName(String localName)
    {
        this.typeLocalName = localName;
        this.nodeRef = null;
    }
    
    public Set<QName> getAspects()
    {
        return aspects;
    }
    
    /**
     * For Hibernate use
     */
    private void setAspects(Set<QName> aspects)
    {
        this.aspects = aspects;
    }

    public Set<NodeAssoc> getSourceNodeAssocs()
    {
        return sourceNodeAssocs;
    }

    /**
     * For Hibernate use
     */
    private void setSourceNodeAssocs(Set<NodeAssoc> sourceNodeAssocs)
    {
        this.sourceNodeAssocs = sourceNodeAssocs;
    }

    public Set<ChildAssoc> getParentAssocs()
    {
        return parentAssocs;
    }

    /**
     * For Hibernate use
     */
    private void setParentAssocs(Set<ChildAssoc> parentAssocs)
    {
        this.parentAssocs = parentAssocs;
    }

    public Map<String, Serializable> getProperties()
    {
        return properties;
    }

    /**
     * For Hibernate use
     */
    private void setProperties(Map<String, Serializable> properties)
    {
        this.properties = properties;
    }

    /**
     * Thread-safe caching of the reference is provided
     */
    public synchronized NodeRef getNodeRef()
    {
        if (nodeRef == null && key != null)
        {
            nodeRef = new NodeRef(getStore().getStoreRef(), getKey().getGuid());
        }
        return nodeRef;
    }
    
    /**
     * @see #getNodeRef()
     */
    public String toString()
    {
        return getNodeRef().toString();
    }
}
