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
package org.alfresco.service.cmr.security;

import java.util.Set;

import org.alfresco.service.Auditable;
import org.alfresco.service.PublicService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

/**
 * The public API for a permission service
 * 
 * The implementation may be changed in the application configuration
 * 
 * @author Andy Hind
 */
@PublicService
public interface PermissionService
{
    public static final String ROLE_PREFIX = "ROLE_";
    
    public static final String GROUP_PREFIX = "GROUP_";
    
    
    
    public static final String ALL_AUTHORITIES = "GROUP_EVERYONE";

    public static final String OWNER_AUTHORITY = "ROLE_OWNER";
    
    public static final String LOCK_OWNER_AUTHORITY = "ROLE_LOCK_OWNER";
    
    public static final String ADMINISTRATOR_AUTHORITY = "ROLE_ADMINISTRATOR";

    public static final String GUEST_AUTHORITY = "guest";
    
    
    
    public static final String ALL_PERMISSIONS = "All";
    
    public static final String FULL_CONTROL = "FullControl";

    public static final String READ = "Read";

    public static final String WRITE = "Write";

    public static final String DELETE = "Delete";

    public static final String ADD_CHILDREN = "AddChildren";

    public static final String READ_PROPERTIES = "ReadProperties";

    public static final String READ_CHILDREN = "ReadChildren";

    public static final String WRITE_PROPERTIES = "WriteProperties";

    public static final String DELETE_NODE = "DeleteNode";

    public static final String DELETE_CHILDREN = "DeleteChildren";

    public static final String CREATE_CHILDREN = "CreateChildren";

    public static final String LINK_CHILDREN = "LinkChildren";

    public static final String DELETE_ASSOCIATIONS = "DeleteAssociations";

    public static final String READ_ASSOCIATIONS = "ReadAssociations";

    public static final String CREATE_ASSOCIATIONS = "CreateAssociations";

    public static final String READ_PERMISSIONS = "ReadPermissions";

    public static final String CHANGE_PERMISSIONS = "ChangePermissions";

    public static final String EXECUTE = "Execute";

    public static final String READ_CONTENT = "ReadContent";

    public static final String WRITE_CONTENT = "WriteContent";

    public static final String EXECUTE_CONTENT = "ExecuteContent";

    public static final String TAKE_OWNERSHIP = "TakeOwnership";

    public static final String SET_OWNER = "SetOwner";

    public static final String COORDINATOR = "Coordinator";

    public static final String CONTRIBUTOR = "Contributor";

    public static final String EDITOR = "Editor";

    public static final String CONSUMER = "Consumer";
    
    public static final String LOCK = "Lock";
    
    public static final String UNLOCK = "Unlock";
    
    public static final String CHECK_OUT = "CheckOut";
    
    public static final String CHECK_IN = "CheckIn";
    
    public static final String CANCEL_CHECK_OUT = "CancelCheckOut";

    /**
     * Get the Owner Authority
     * 
     * @return the owner authority
     */
    @Auditable
    public String getOwnerAuthority();

    /**
     * Get the All Authorities
     * 
     * @return the All authorities
     */
    @Auditable
    public String getAllAuthorities();

    /**
     * Get the All Permission
     * 
     * @return the All permission
     */
    @Auditable
    public String getAllPermission();

    /**
     * Get all the AccessPermissions that are granted/denied to the current
     * authentication for the given node
     * 
     * @param nodeRef -
     *            the reference to the node
     * @return the set of allowed permissions
     */
    @Auditable(key = Auditable.Key.ARG_0, parameters = {"nodeRef"})
    public Set<AccessPermission> getPermissions(NodeRef nodeRef);

    /**
     * Get all the AccessPermissions that are set for anyone for the
     * given node
     * 
     * @param nodeRef -
     *            the reference to the node
     * @return the set of allowed permissions
     */
    @Auditable(key = Auditable.Key.ARG_0, parameters = {"nodeRef"})
    public Set<AccessPermission> getAllSetPermissions(NodeRef nodeRef);

    /**
     * Get the permissions that can be set for a given node
     * 
     * @param nodeRef
     * @return
     */
    @Auditable(key = Auditable.Key.ARG_0, parameters = {"nodeRef"})
    public Set<String> getSettablePermissions(NodeRef nodeRef);

    /**
     * Get the permissions that can be set for a given type
     * 
     * @param nodeRef
     * @return
     */
    @Auditable(parameters = {"type"})
    public Set<String> getSettablePermissions(QName type);

    /**
     * Check that the given authentication has a particular permission for the
     * given node. (The default behaviour is to inherit permissions)
     * 
     * @param nodeRef
     * @param permission
     * @return
     */
    @Auditable(key = Auditable.Key.ARG_0, parameters = {"nodeRef", "permission"})
    public AccessStatus hasPermission(NodeRef nodeRef, String permission);

    /**
     * Delete all the permission assigned to the node
     * 
     * @param nodeRef
     */
    @Auditable(key = Auditable.Key.ARG_0, parameters = {"nodeRef"})
    public void deletePermissions(NodeRef nodeRef);

    /**
     * Delete all permission for the given authority.
     * 
     * @param nodeRef
     * @param authority
     */
    @Auditable(key = Auditable.Key.ARG_0, parameters = {"nodeRef", "authority"})
    public void clearPermission(NodeRef nodeRef, String authority);
    
    /**
     * Find and delete a access control entry by node, authentication and permission.
     * 
     * @param nodeRef the node that the entry applies to
     * @param authority the authority recipient
     * @param permission the entry permission
     */
    @Auditable(key = Auditable.Key.ARG_0, parameters = {"nodeRef", "authority", "permission"})
    public void deletePermission(NodeRef nodeRef, String authority, String permission);

    /**
     * Set a specific permission on a node.
     * 
     * @param nodeRef
     * @param authority
     * @param permission
     * @param allow
     */
    @Auditable(key = Auditable.Key.ARG_0, parameters = {"nodeRef", "authority", "permission", "allow"})
    public void setPermission(NodeRef nodeRef, String authority, String permission, boolean allow);

    /**
     * Set the global inheritance behaviour for permissions on a node.
     * 
     * @param nodeRef
     * @param inheritParentPermissions
     */
    @Auditable(key = Auditable.Key.ARG_0, parameters = {"nodeRef", "inheritParentPermissions"})
    public void setInheritParentPermissions(NodeRef nodeRef, boolean inheritParentPermissions);
    
    /**
     * Return the global inheritance behaviour for permissions on a node.
     * 
     * @param nodeRef
     * @return inheritParentPermissions
     */
    @Auditable(key = Auditable.Key.ARG_0, parameters = {"nodeRef"})
    public boolean getInheritParentPermissions(NodeRef nodeRef);
}
