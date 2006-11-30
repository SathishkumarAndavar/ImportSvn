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
package org.alfresco.repo.ownable.impl;

import java.io.Serializable;
import java.util.HashMap;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.OwnableService;
import org.alfresco.service.namespace.QName;
import org.springframework.beans.factory.InitializingBean;

/**
 * Ownership service support. Use in permissions framework as dynamic authority. 
 * 
 * @author Andy Hind
 */
public class OwnableServiceImpl implements OwnableService, InitializingBean
{
    private NodeService nodeService;
    
    private AuthenticationService authenticationService;
    
    private SimpleCache<NodeRef, String> nodeOwnerCache;

    public OwnableServiceImpl()
    {
        super();
    }

    // IOC
    
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }
    
    public void setAuthenticationService(AuthenticationService authenticationService)
    {
        this.authenticationService = authenticationService;
    }
    
    /**
     * @param ownerCache a transactionally-safe cache of node owners
     */
    public void setNodeOwnerCache(SimpleCache<NodeRef, String> ownerCache)
    {
        this.nodeOwnerCache = ownerCache;
    }

    public void afterPropertiesSet() throws Exception
    {
        if (nodeService == null)
        {
            throw new IllegalArgumentException("Property 'nodeService' has not been set");
        }
        if (authenticationService == null)
        {
            throw new IllegalArgumentException("Property 'authenticationService' has not been set");
        }
        if (nodeOwnerCache == null)
        {
            throw new IllegalArgumentException("Property 'nodeOwnerCache' has not been set");
        }
    }
    
    // OwnableService implmentation
    
    public String getOwner(NodeRef nodeRef)
    {
        String userName = nodeOwnerCache.get(nodeRef);
        
        if (userName == null)
        {
            // If ownership is not explicitly set then we fall back to the creator
            if (nodeService.hasAspect(nodeRef, ContentModel.ASPECT_OWNABLE))
            {
                userName = DefaultTypeConverter.INSTANCE.convert(String.class, nodeService.getProperty(nodeRef, ContentModel.PROP_OWNER));
            }
            else if(nodeService.hasAspect(nodeRef, ContentModel.ASPECT_AUDITABLE))
            {
                userName = DefaultTypeConverter.INSTANCE.convert(String.class, nodeService.getProperty(nodeRef, ContentModel.PROP_CREATOR));
            }
            nodeOwnerCache.put(nodeRef, userName);
        }
        
        return userName;
    }

    public void setOwner(NodeRef nodeRef, String userName)
    {
        if (!nodeService.hasAspect(nodeRef, ContentModel.ASPECT_OWNABLE))
        {
            HashMap<QName, Serializable> properties = new HashMap<QName, Serializable>(1, 1.0f);
            properties.put(ContentModel.PROP_OWNER, userName);
            nodeService.addAspect(nodeRef, ContentModel.ASPECT_OWNABLE, properties);
        }
        else
        {
            nodeService.setProperty(nodeRef, ContentModel.PROP_OWNER, userName);
        }
        nodeOwnerCache.put(nodeRef, userName);
    }

    public void takeOwnership(NodeRef nodeRef)
    {
        setOwner(nodeRef, authenticationService.getCurrentUserName());
    }

    public boolean hasOwner(NodeRef nodeRef)
    {
        return getOwner(nodeRef) != null;
    }
}
