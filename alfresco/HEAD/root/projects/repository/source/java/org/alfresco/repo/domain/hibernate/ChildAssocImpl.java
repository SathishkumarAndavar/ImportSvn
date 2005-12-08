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

import org.alfresco.repo.domain.ChildAssoc;
import org.alfresco.repo.domain.Node;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.EqualsHelper;

/**
 * @author Derek Hulley
 */
public class ChildAssocImpl implements ChildAssoc
{
    private Long id;
    private Node parent;
    private Node child;
    private QName typeQName;
    private QName qName;
    private boolean isPrimary;
    private int index;
    private transient ChildAssociationRef childAssocRef;
    
    public ChildAssocImpl()
    {
        setIndex(Integer.MAX_VALUE);              // comes last
    }

    public void buildAssociation(Node parentNode, Node childNode)
    {
        // add the forward associations
        this.setParent(parentNode);
        this.setChild(childNode);
        // add the inverse associations
        parentNode.getChildAssocs().add(this);
        childNode.getParentAssocs().add(this);
    }
    
    public void removeAssociation()
    {
        // maintain inverse assoc from parent node to this instance
        this.getParent().getChildAssocs().remove(this);
        // maintain inverse assoc from child node to this instance
        this.getChild().getParentAssocs().remove(this);
    }
    
    public synchronized ChildAssociationRef getChildAssocRef()
    {
        if (childAssocRef == null)
        {
            childAssocRef = new ChildAssociationRef(
                    this.typeQName,
                    getParent().getNodeRef(),
                    this.qName,
                    getChild().getNodeRef(),
                    this.isPrimary,
                    -1);
        }
        return childAssocRef;
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer(32);
        sb.append("ChildAssoc")
          .append("[ parent=").append(parent)
          .append(", child=").append(child)
          .append(", name=").append(getQname())
          .append(", isPrimary=").append(isPrimary)
          .append("]");
        return sb.toString();
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
        else if (!(obj instanceof ChildAssoc))
        {
            return false;
        }
        ChildAssoc that = (ChildAssoc) obj;
        return (this.getIsPrimary() == that.getIsPrimary()
                && EqualsHelper.nullSafeEquals(this.getTypeQName(), that.getTypeQName())
                && EqualsHelper.nullSafeEquals(this.getQname(), that.getQname())
                && EqualsHelper.nullSafeEquals(this.getParent(), that.getParent())
                && EqualsHelper.nullSafeEquals(this.getChild(), that.getChild()));
    }
    
    public int hashCode()
    {
        return (qName == null ? 0 : qName.hashCode());
    }

    /**
     * Orders the child associations by ID.  A smaller ID has a higher priority.
     * This may change once we introduce a changeable index against which to order.
     */
    public int compareTo(ChildAssoc another)
    {
        if (this == another)
        {
            return 0;
        }
        
        int thisIndex = this.getIndex();
        int anotherIndex = another.getIndex();
        
        Long thisId = this.getId();
        Long anotherId = another.getId();

        if (thisId == null)                     // this ID has not been set, make this instance greater
        {
            return -1; 
        }
        else if (anotherId == null)             // other ID has not been set, make this instance lesser
        {
            return 1;
        }
        else if (thisIndex == anotherIndex)     // use the explicit index
        {
            return thisId.compareTo(anotherId);
        }
        else                                    // fallback on order of creation 
        {
            return (thisIndex > anotherIndex) ? 1 : -1;     // a lower index, make this instance lesser
        }
    }

    public Long getId()
    {
        return id;
    }

    /**
     * For Hibernate use
     */
    private void setId(Long id)
    {
        this.id = id;
    }

    public Node getParent()
    {
        return parent;
    }

    /**
     * For Hibernate use
     */
    private void setParent(Node parentNode)
    {
        this.parent = parentNode;
    }

    public Node getChild()
    {
        return child;
    }

    /**
     * For Hibernate use
     */
    private void setChild(Node node)
    {
        child = node;
    }
    
    public QName getTypeQName()
    {
        return typeQName;
    }
    
    public void setTypeQName(QName typeQName)
    {
        this.typeQName = typeQName;
    }
    
    public QName getQname()
    {
        return qName;
    }

    public void setQname(QName qname)
    {
        this.qName = qname;
    }

    public boolean getIsPrimary()
    {
        return isPrimary;
    }

    public void setIsPrimary(boolean isPrimary)
    {
        this.isPrimary = isPrimary;
    }

    public int getIndex()
    {
        return index;
    }

    public void setIndex(int index)
    {
        this.index = index;
    }
}
