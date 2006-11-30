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
package org.alfresco.util;

import java.io.Serializable;
import java.util.HashMap;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;

/**
 * Utility class containing some useful methods to help when writing tets that require authenticated users
 * 
 * @author Roy Wetherall
 */
public abstract class TestWithUserUtils extends BaseSpringTest
{
    /**
     * Create a new user, including the corresponding person node.
     * 
     * @param userName                  the user name
     * @param password                  the password
     * @param rootNodeRef               the root node reference
     * @param nodeService               the node service
     * @param authenticationService     the authentication service
     */
    public static void createUser(
            String userName, 
            String password, 
            NodeRef rootNodeRef,
            NodeService nodeService,
            AuthenticationService authenticationService)
    {
        // ignore if the user's authentication already exists
        if (authenticationService.authenticationExists(userName))
        {
            // ignore
            return;
        }
        QName children = ContentModel.ASSOC_CHILDREN;
        QName system = QName.createQName(NamespaceService.SYSTEM_MODEL_1_0_URI, "system");
        QName container = ContentModel.TYPE_CONTAINER;
        QName types = QName.createQName(NamespaceService.SYSTEM_MODEL_1_0_URI, "people");
        
        NodeRef systemNodeRef = nodeService.createNode(rootNodeRef, children, system, container).getChildRef();
        NodeRef typesNodeRef = nodeService.createNode(systemNodeRef, children, types, container).getChildRef();
        
        HashMap<QName, Serializable> properties = new HashMap<QName, Serializable>();
        properties.put(ContentModel.PROP_USERNAME, userName);
        nodeService.createNode(typesNodeRef, children, ContentModel.TYPE_PERSON, container, properties);
        
        // Create the  users

        authenticationService.createAuthentication(userName, password.toCharArray()); 
    }

    /**
     * Autneticate the user with the specified password
     * 
     * @param userName                  the user name
     * @param password                  the password
     * @param rootNodeRef               the root node reference
     * @param authenticationService     the authentication service
     */
    public static void authenticateUser(
            String userName, 
            String password,
            NodeRef rootNodeRef,
            AuthenticationService authenticationService)
    {
        authenticationService.authenticate(userName, password.toCharArray());
    }
    
    /**
     * Authenticate as the given user.  If the user does not exist, then authenticate as the system user
     * and create the authentication first.
     */
    public static void authenticateUser(
            String userName,
            String password,
            AuthenticationService authenticationService,
            AuthenticationComponent authenticationComponent)
    {
        // go system
        try
        {
            authenticationComponent.setSystemUserAsCurrentUser();
            if (!authenticationService.authenticationExists(userName))
            {
                authenticationService.createAuthentication(userName, password.toCharArray());
            }
        }
        finally
        {
            authenticationComponent.clearCurrentSecurityContext();
        }
        authenticationService.authenticate(userName, password.toCharArray());
    }
    
    /**
     * Get the current user node reference
     * 
     * @param authenticationService     the authentication service
     * @return                          the currenlty authenticated user's node reference
     */
    public static String getCurrentUser(AuthenticationService authenticationService)
    {
        String un = authenticationService.getCurrentUserName();
        if (un != null)
        {
            return un;
        }
        else
        {
            throw new RuntimeException("The current user could not be retrieved.");
        }
        
    }

    public static void deleteUser(String user_name, String pwd, NodeRef ref, NodeService service, AuthenticationService service2)
    {
        service2.deleteAuthentication(user_name);
    }

}
