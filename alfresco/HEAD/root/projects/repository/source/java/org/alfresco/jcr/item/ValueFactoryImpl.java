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
package org.alfresco.jcr.item;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;

import org.alfresco.jcr.session.SessionImpl;
import org.alfresco.jcr.util.JCRProxyFactory;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;

/**
 * Alfresco implementation of JCR Value Factory
 * 
 * @author David Caruana
 *
 */
public class ValueFactoryImpl implements ValueFactory
{
    private SessionImpl session;
    private ValueFactory proxy = null;

    /**
     * Construct
     * 
     * @param session
     */
    public ValueFactoryImpl(SessionImpl session)
    {
        this.session = session;
    }
    
    /**
     * Get proxied JCR Value Factory
     * 
     * @return
     */
    public ValueFactory getProxy()
    {
        if (proxy == null)
        {
            proxy = (ValueFactory)JCRProxyFactory.create(this, ValueFactory.class, session);
        }
        return proxy;
    }

    /* (non-Javadoc)
     * @see javax.jcr.ValueFactory#createValue(java.lang.String, int)
     */
    public Value createValue(String value, int type) throws ValueFormatException
    {
        Value createdValue = null;
        
        try
        {
            switch(type)
            {
                case PropertyType.STRING:
                    createdValue = createValue(session.getTypeConverter().stringValue(value));
                    break;
                case PropertyType.LONG:
                    createdValue = createValue(session.getTypeConverter().longValue(value));
                    break;
                case PropertyType.DOUBLE:
                    createdValue = createValue(session.getTypeConverter().doubleValue(value));
                    break;
                case PropertyType.BOOLEAN:
                    createdValue = createValue(session.getTypeConverter().booleanValue(value));
                    break;
                case PropertyType.DATE:
                    createdValue = new ValueImpl(session, PropertyType.DATE, session.getTypeConverter().convert(Date.class, value));
                    break;
                case PropertyType.BINARY:
                    createdValue = createValue(session.getTypeConverter().streamValue(value));
                    break;
                case PropertyType.REFERENCE:
                    createdValue = new ValueImpl(session, PropertyType.REFERENCE, session.getTypeConverter().referenceValue(value));
                    break;
                case PropertyType.NAME:
                    QName name = session.getTypeConverter().convert(QName.class, value);
                    createdValue = new ValueImpl(session, PropertyType.NAME, name);
                    break;
                case PropertyType.PATH:
                    Path path = session.getTypeConverter().convert(Path.class, value);
                    createdValue = new ValueImpl(session, PropertyType.PATH, path);
                    break;
                default:
                    throw new ValueFormatException("Cannot create value of type " + type);
            }
        }
        catch(RepositoryException e)
        {
            // Should this method also throw repository exception
            throw new ValueFormatException("Failed to create value " + value + " of type " + type, e);
        }
        return createdValue;
    }
        
    /* (non-Javadoc)
     * @see javax.jcr.ValueFactory#createValue(java.lang.String)
     */
    public Value createValue(String value)
    {
        return new ValueImpl(session, PropertyType.STRING, value);
    }

    /* (non-Javadoc)
     * @see javax.jcr.ValueFactory#createValue(long)
     */
    public Value createValue(long value)
    {
        return new ValueImpl(session, PropertyType.LONG, value);
    }

    /* (non-Javadoc)
     * @see javax.jcr.ValueFactory#createValue(double)
     */
    public Value createValue(double value)
    {
        return new ValueImpl(session, PropertyType.DOUBLE, value);
    }

    /* (non-Javadoc)
     * @see javax.jcr.ValueFactory#createValue(boolean)
     */
    public Value createValue(boolean value)
    {
        return new ValueImpl(session, PropertyType.BOOLEAN, value);
    }

    /* (non-Javadoc)
     * @see javax.jcr.ValueFactory#createValue(java.util.Calendar)
     */
    public Value createValue(Calendar value)
    {
        return new ValueImpl(session, PropertyType.DATE, value.getTime());
    }

    /* (non-Javadoc)
     * @see javax.jcr.ValueFactory#createValue(java.io.InputStream)
     */
    public Value createValue(InputStream value)
    {
        return new ValueImpl(session, PropertyType.BINARY, value);
    }

    /* (non-Javadoc)
     * @see javax.jcr.ValueFactory#createValue(javax.jcr.Node)
     */
    public Value createValue(Node value) throws RepositoryException
    {
        if (value == null)
        {
            throw new RepositoryException("Node value must not be null");
        }
        
        // TODO: refer to ContentModel Constants
        Property protocol = value.getProperty("sys:store-protocol");
        Property identifier = value.getProperty("sys:store-identifier");
        Property uuid = value.getProperty("sys:node-uuid");

        // construct a node reference
        NodeRef ref = new NodeRef(new StoreRef(protocol.getString(), identifier.getString()), uuid.getString());
        return new ValueImpl(session, PropertyType.REFERENCE, ref);
    }
    
}
