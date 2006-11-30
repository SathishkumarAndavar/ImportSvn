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
package org.alfresco.repo.security.permissions.impl.acegi;

import java.util.BitSet;
import java.util.List;
import java.util.ListIterator;

import org.alfresco.repo.search.ResultSetRowIterator;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetMetaData;
import org.alfresco.service.cmr.search.ResultSetRow;

public class FilteringResultSet extends ACLEntryAfterInvocationProvider implements ResultSet
{
    private ResultSet unfiltered;

    private BitSet inclusionMask;
    
    private ResultSetMetaData resultSetMetaData;

    FilteringResultSet(ResultSet unfiltered)
    {
        super();
        this.unfiltered = unfiltered;
        inclusionMask = new BitSet(unfiltered.length());
    }

    /* package */ResultSet getUnFilteredResultSet()
    {
        return unfiltered;
    }

    /* package */void setIncluded(int i, boolean excluded)
    {
        inclusionMask.set(i, excluded);
    }

    /* package */boolean getIncluded(int i)
    {
        return inclusionMask.get(i);
    }

    public Path[] getPropertyPaths()
    {
        return unfiltered.getPropertyPaths();
    }

    public int length()
    {
        return inclusionMask.cardinality();
    }

    private int translateIndex(int n)
    {
        if (n > length())
        {
            throw new IndexOutOfBoundsException();
        }
        int count = -1;
        for (int i = 0, l = unfiltered.length(); i < l; i++)
        {
            if (inclusionMask.get(i))
            {
                count++;
            }
            if (count == n)
            {
                return i;
            }

        }
        throw new IndexOutOfBoundsException();
    }

    public NodeRef getNodeRef(int n)
    {
        return unfiltered.getNodeRef(translateIndex(n));
    }

    public float getScore(int n)
    {
        return unfiltered.getScore(translateIndex(n));
    }

    public void close()
    {
        unfiltered.close();
    }

    public ResultSetRow getRow(int i)
    {
        return unfiltered.getRow(translateIndex(i));
    }

    public List<NodeRef> getNodeRefs()
    {
        List<NodeRef> answer = unfiltered.getNodeRefs();
        for (int i = unfiltered.length() - 1; i >= 0; i--)
        {
            if (!inclusionMask.get(i))
            {
                answer.remove(i);
            }
        }
        return answer;
    }

    public List<ChildAssociationRef> getChildAssocRefs()
    {
        List<ChildAssociationRef> answer = unfiltered.getChildAssocRefs();
        for (int i = unfiltered.length() - 1; i >= 0; i--)
        {
            if (!inclusionMask.get(i))
            {
                answer.remove(i);
            }
        }
        return answer;
    }

    public ChildAssociationRef getChildAssocRef(int n)
    {
        return unfiltered.getChildAssocRef(translateIndex(n));
    }

    public ListIterator<ResultSetRow> iterator()
    {
        return new FilteringIterator();
    }

    class FilteringIterator implements ResultSetRowIterator
    {
        // -1 at the start
        int underlyingPosition = -1;

        public boolean hasNext()
        {
            return inclusionMask.nextSetBit(underlyingPosition + 1) != -1;
        }

        public ResultSetRow next()
        {
            underlyingPosition = inclusionMask.nextSetBit(underlyingPosition + 1);
            if( underlyingPosition == -1)
            {
                throw new IllegalStateException();
            }
            return unfiltered.getRow(underlyingPosition);
        }

        public boolean hasPrevious()
        {
            if (underlyingPosition <= 0)
            {
                return false;
            }
            else
            {
                for (int i = underlyingPosition - 1; i >= 0; i--)
                {
                    if (inclusionMask.get(i))
                    {
                        return true;
                    }
                }
            }
            return false;
        }

        public ResultSetRow previous()
        {
            if (underlyingPosition <= 0)
            {
                throw new IllegalStateException();
            }
            for (int i = underlyingPosition - 1; i >= 0; i--)
            {
                if (inclusionMask.get(i))
                {
                    underlyingPosition = i;
                    return unfiltered.getRow(underlyingPosition);
                }
            }
            throw new IllegalStateException();
        }

        public int nextIndex()
        {
            return inclusionMask.nextSetBit(underlyingPosition+1);
        }

        public int previousIndex()
        {
            if (underlyingPosition <= 0)
            {
                return -1;
            }
            for (int i = underlyingPosition - 1; i >= 0; i--)
            {
                if (inclusionMask.get(i))
                {
                    return i;
                }
            }
            return -1;
        }

        /*
         * Mutation is not supported
         */

        public void remove()
        {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public void set(ResultSetRow o)
        {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public void add(ResultSetRow o)
        {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

    }

    public ResultSetMetaData getResultSetMetaData()
    {
        return resultSetMetaData;
    }

    public void setResultSetMetaData(ResultSetMetaData resultSetMetaData)
    {
        this.resultSetMetaData = resultSetMetaData;
    }

    
}
