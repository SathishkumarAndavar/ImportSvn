/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.alfresco.org/legal/license.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.repo.domain.hibernate;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.alfresco.repo.domain.ChildAssoc;
import org.alfresco.repo.domain.DbAccessControlList;
import org.alfresco.repo.domain.Node;
import org.alfresco.repo.domain.PropertyValue;
import org.alfresco.repo.domain.Store;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.EqualsHelper;

/**
 * Bean containing all the persistence data representing a <b>node</b>.
 * <p>
 * This implementation of the {@link org.alfresco.repo.domain.Node Node} interface is
 * Hibernate specific.
 * 
 * @author Derek Hulley
 */
public class NodeImpl extends LifecycleAdapter implements Node, Serializable
{
    private static final long serialVersionUID = -2101330674810283053L;

    private Long id;
    private Store store;
    private String uuid;
    private QName typeQName;
    private Set<QName> aspects;
    private Collection<ChildAssoc> parentAssocs;
    private Map<QName, PropertyValue> properties;
    private DbAccessControlList accessControlList;
    
    private transient ReadLock refReadLock;
    private transient WriteLock refWriteLock;
    private transient NodeRef nodeRef;

    public NodeImpl()
    {
        aspects = new HashSet<QName>(5);
        parentAssocs = new HashSet<ChildAssoc>(5);
        properties = new HashMap<QName, PropertyValue>(5);
        
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        refReadLock = lock.readLock();
        refWriteLock = lock.writeLock();
    }

    /**
     * Thread-safe caching of the reference is provided
     */
    public NodeRef getNodeRef()
    {
        // first check if it is available
        refReadLock.lock();
        try
        {
            if (nodeRef != null)
            {
                return nodeRef;
            }
        }
        finally
        {
            refReadLock.unlock();
        }
        // get write lock
        refWriteLock.lock();
        try
        {
            // double check
            if (nodeRef == null )
            {
                nodeRef = new NodeRef(getStore().getStoreRef(), getUuid());
            }
            return nodeRef;
        }
        finally
        {
            refWriteLock.unlock();
        }
    }
    
    /**
     * @see #getNodeRef()
     */
    public String toString()
    {
        return getNodeRef().toString();
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
        if (EqualsHelper.nullSafeEquals(id, that.getId()))
        {
            return true;
        }
        else
        {
            return (this.getNodeRef().equals(that.getNodeRef()));
        }
    }
    
    public int hashCode()
    {
        return getUuid().hashCode();
    }

//    @Override
//    public boolean onDelete(Session session) throws CallbackException
//    {
//        // check if there is an access control list
//        DbAccessControlList acl = getAccessControlList();
//        if (acl != null)
//        {
//            session.delete(acl);
//        }
//        return NO_VETO;
//    }
//

    public Long getId()
    {
        return id;
    }

    /**
     * For Hibernate use
     */
    @SuppressWarnings("unused")
    private void setId(Long id)
    {
        this.id = id;
    }

    public Store getStore()
    {
        return store;
    }

    public void setStore(Store store)
    {
        refWriteLock.lock();
        try
        {
            this.store = store;
            this.nodeRef = null;
        }
        finally
        {
            refWriteLock.unlock();
        }
    }

    public String getUuid()
    {
        return uuid;
    }

    public void setUuid(String uuid)
    {
        refWriteLock.lock();
        try
        {
            this.uuid = uuid;
            this.nodeRef = null;
        }
        finally
        {
            refWriteLock.unlock();
        }
    }

    public QName getTypeQName()
    {
        return typeQName;
    }

    public void setTypeQName(QName typeQName)
    {
        this.typeQName = typeQName;
    }

    public Set<QName> getAspects()
    {
        return aspects;
    }
    
    /**
     * For Hibernate use
     */
    @SuppressWarnings("unused")
    private void setAspects(Set<QName> aspects)
    {
        this.aspects = aspects;
    }

    public Collection<ChildAssoc> getParentAssocs()
    {
        return parentAssocs;
    }

    /**
     * For Hibernate use
     */
    @SuppressWarnings("unused")
    private void setParentAssocs(Collection<ChildAssoc> parentAssocs)
    {
        this.parentAssocs = parentAssocs;
    }

    public Map<QName, PropertyValue> getProperties()
    {
        return properties;
    }

    /**
     * For Hibernate use
     */
    @SuppressWarnings("unused")
    private void setProperties(Map<QName, PropertyValue> properties)
    {
        this.properties = properties;
    }

    public DbAccessControlList getAccessControlList()
    {
        return accessControlList;
    }

    public void setAccessControlList(DbAccessControlList accessControlList)
    {
        this.accessControlList = accessControlList;
    }
}
