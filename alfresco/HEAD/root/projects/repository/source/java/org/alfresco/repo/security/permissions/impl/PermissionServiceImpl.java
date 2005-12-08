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
package org.alfresco.repo.security.permissions.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.acegisecurity.Authentication;
import net.sf.acegisecurity.GrantedAuthority;
import net.sf.acegisecurity.providers.dao.User;

import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.security.permissions.DynamicAuthority;
import org.alfresco.repo.security.permissions.NodePermissionEntry;
import org.alfresco.repo.security.permissions.PermissionEntry;
import org.alfresco.repo.security.permissions.PermissionReference;
import org.alfresco.repo.security.permissions.PermissionServiceSPI;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.EqualsHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * The Alfresco implementation of a permissions service against our APIs for the
 * permissions model and permissions persistence.
 * 
 * 
 * @author andyh
 */
public class PermissionServiceImpl implements PermissionServiceSPI, InitializingBean
{
    
    static SimplePermissionReference OLD_ALL_PERMISSIONS_REFERENCE = new SimplePermissionReference(QName.createQName(
            NamespaceService.SECURITY_MODEL_1_0_URI, PermissionService.ALL_PERMISSIONS), PermissionService.ALL_PERMISSIONS);
    

    private static Log log = LogFactory.getLog(PermissionServiceImpl.class);

    /*
     * Access to the model
     */
    private ModelDAO modelDAO;

    /*
     * Access to permissions
     */
    private PermissionsDAO permissionsDAO;

    /*
     * Access to the node service
     */
    private NodeService nodeService;

    /*
     * Access to the data dictionary
     */
    private DictionaryService dictionaryService;

    /*
     * Access to the authentication component
     */

    private AuthenticationComponent authenticationComponent;

    private AuthorityService authorityService;
    
    /*
     * Dynamic authorities providers
     */

    private List<DynamicAuthority> dynamicAuthorities;

    /*
     * Standard spring construction.
     */
    public PermissionServiceImpl()
    {
        super();
    }

    //
    // Inversion of control
    //

    public void setDictionaryService(DictionaryService dictionaryService)
    {
        this.dictionaryService = dictionaryService;
    }

    public void setModelDAO(ModelDAO modelDAO)
    {
        this.modelDAO = modelDAO;
    }

    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    public void setPermissionsDAO(PermissionsDAO permissionsDAO)
    {
        this.permissionsDAO = permissionsDAO;
    }

    public void setAuthenticationComponent(AuthenticationComponent authenticationComponent)
    {
        this.authenticationComponent = authenticationComponent;
    }
    
    public void setAuthorityService(AuthorityService authorityService)
    {
        this.authorityService = authorityService;
    }

    public void setDynamicAuthorities(List<DynamicAuthority> dynamicAuthorities)
    {
        this.dynamicAuthorities = dynamicAuthorities;
    }

    public void afterPropertiesSet() throws Exception
    {
        if (dictionaryService == null)
        {
            throw new IllegalArgumentException("There must be a dictionary service");
        }
        if (modelDAO == null)
        {
            throw new IllegalArgumentException("There must be a permission model service");
        }
        if (nodeService == null)
        {
            throw new IllegalArgumentException("There must be a node service");
        }
        if (permissionsDAO == null)
        {
            throw new IllegalArgumentException("There must be a permission dao");
        }
        if (authenticationComponent == null)
        {
            throw new IllegalArgumentException("There must be an authentication component");
        }
        if(authorityService == null)
        {
            throw new IllegalArgumentException("There must be an authority service"); 
        }

    }

    //
    // Permissions Service
    //

    public String getOwnerAuthority()
    {
        return OWNER_AUTHORITY;
    }

    public String getAllAuthorities()
    {
        return ALL_AUTHORITIES;
    }

    public String getAllPermission()
    {
        return ALL_PERMISSIONS;
    }

    public Set<AccessPermission> getPermissions(NodeRef nodeRef)
    {
        return getAllPermissionsImpl(nodeRef, true, true);
    }

    public Set<AccessPermission> getAllSetPermissions(NodeRef nodeRef)
    {
        HashSet<AccessPermission> accessPermissions = new HashSet<AccessPermission>();
        NodePermissionEntry nodePremissionEntry = getSetPermissions(nodeRef);
        for (PermissionEntry pe : nodePremissionEntry.getPermissionEntries())
        {
            accessPermissions.add(new AccessPermissionImpl(getPermission(pe.getPermissionReference()), pe
                    .getAccessStatus(), pe.getAuthority()));
        }
        return accessPermissions;
    }

    private Set<AccessPermission> getAllPermissionsImpl(NodeRef nodeRef, boolean includeTrue, boolean includeFalse)
    {
        String userName = authenticationComponent.getCurrentUserName();
        HashSet<AccessPermission> accessPermissions = new HashSet<AccessPermission>();
        for (PermissionReference pr : getSettablePermissionReferences(nodeRef))
        {
            if (hasPermission(nodeRef, pr) == AccessStatus.ALLOWED)
            {
                accessPermissions.add(new AccessPermissionImpl(getPermission(pr), AccessStatus.ALLOWED, userName));
            }
            else
            {
                if (includeFalse)
                {
                    accessPermissions.add(new AccessPermissionImpl(getPermission(pr), AccessStatus.DENIED, userName));
                }
            }
        }
        return accessPermissions;
    }

    private class AccessPermissionImpl implements AccessPermission
    {
        private String permission;

        private AccessStatus accessStatus;

        private String authority;

        private AuthorityType authorityType;

        AccessPermissionImpl(String permission, AccessStatus accessStatus, String authority)
        {
            this.permission = permission;
            this.accessStatus = accessStatus;
            this.authority = authority;
            this.authorityType = AuthorityType.getAuthorityType(authority);
        }

        public String getPermission()
        {
            return permission;
        }

        public AccessStatus getAccessStatus()
        {
            return accessStatus;
        }

        public String getAuthority()
        {
            return authority;
        }

        public AuthorityType getAuthorityType()
        {
            return authorityType;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(o instanceof AccessPermissionImpl))
            {
                return false;
            }
            AccessPermissionImpl other = (AccessPermissionImpl) o;
            return this.getPermission().equals(other.getPermission())
                    && (this.getAccessStatus() == other.getAccessStatus() && (this.getAccessStatus().equals(other
                            .getAccessStatus())));
        }

        @Override
        public int hashCode()
        {
            return ((authority.hashCode() * 37) + permission.hashCode()) * 37 + accessStatus.hashCode();
        }
    }

    public Set<String> getSettablePermissions(NodeRef nodeRef)
    {
        Set<PermissionReference> settable = getSettablePermissionReferences(nodeRef);
        Set<String> strings = new HashSet<String>(settable.size());
        for (PermissionReference pr : settable)
        {
            strings.add(getPermission(pr));
        }
        return strings;
    }

    public Set<String> getSettablePermissions(QName type)
    {
        Set<PermissionReference> settable = getSettablePermissionReferences(type);
        Set<String> strings = new HashSet<String>(settable.size());
        for (PermissionReference pr : settable)
        {
            strings.add(getPermission(pr));
        }
        return strings;
    }

    public NodePermissionEntry getSetPermissions(NodeRef nodeRef)
    {
        return permissionsDAO.getPermissions(nodeRef);
    }

    public AccessStatus hasPermission(NodeRef nodeRef, PermissionReference perm)
    {
        // If the node ref is null there is no sensible test to do - and there
        // must be no permissions
        // - so we allow it

        if (nodeRef == null)
        {
            return AccessStatus.ALLOWED;
        }

        // If the permission is null we deny

        if (perm == null)
        {
            return AccessStatus.DENIED;
        }

        // Allow permissions for nodes that do not exist
        if (!nodeService.exists(nodeRef))
        {
            return AccessStatus.ALLOWED;
        }

        // If the node does not support the given permission there is no point
        // doing the test
        Set<PermissionReference> available = modelDAO.getAllPermissions(nodeRef);
        available.add(getAllPermissionReference());
        available.add(OLD_ALL_PERMISSIONS_REFERENCE);

        if (!(available.contains(perm)))
        {
            return AccessStatus.DENIED;
        }

        //
        // TODO: Dynamic permissions via evaluators
        //

        /*
         * Does the current authentication have the supplied permission on the
         * given node.
         */

        // Get the current authentications
        Authentication auth = authenticationComponent.getCurrentAuthentication();

        // Get the available authorisations
        Set<String> authorisations = getAuthorisations(auth, nodeRef);

        QName typeQname = nodeService.getType(nodeRef);
        Set<QName> aspectQNames = nodeService.getAspects(nodeRef);

        NodeTest nt = new NodeTest(perm.equals(OLD_ALL_PERMISSIONS_REFERENCE) ? getAllPermissionReference() : perm, typeQname, aspectQNames);
        boolean result = nt.evaluate(authorisations, nodeRef);
        if (log.isDebugEnabled())
        {
            log.debug("Permission <"
                    + perm + "> is " + (result ? "allowed" : "denied") + " for "
                    + authenticationComponent.getCurrentUserName() + " on node " + nodeService.getPath(nodeRef));
        }
        return result ? AccessStatus.ALLOWED : AccessStatus.DENIED;

    }

    /**
     * Get the authorisations for the currently authenticated user
     * 
     * @param auth
     * @return
     */
    private Set<String> getAuthorisations(Authentication auth, NodeRef nodeRef)
    {
        HashSet<String> auths = new HashSet<String>();
        // No authenticated user then no permissions
        if (auth == null)
        {
            return auths;
        }
        // TODO: Refactor and use the authentication service for this.
        User user = (User) auth.getPrincipal();
        auths.add(user.getUsername());
        auths.add(getAllAuthorities());
        for (GrantedAuthority authority : auth.getAuthorities())
        {
            auths.add(authority.getAuthority());
        }
        if (dynamicAuthorities != null)
        {
            for (DynamicAuthority da : dynamicAuthorities)
            {
                if (da.hasAuthority(nodeRef, user.getUsername()))
                {
                    auths.add(da.getAuthority());
                }
            }
        }
        auths.addAll(authorityService.getAuthorities());
        return auths;
    }

    public NodePermissionEntry explainPermission(NodeRef nodeRef, PermissionReference perm)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void deletePermissions(NodeRef nodeRef)
    {
        permissionsDAO.deletePermissions(nodeRef);
    }

    public void deletePermissions(NodePermissionEntry nodePermissionEntry)
    {
        permissionsDAO.deletePermissions(nodePermissionEntry);
    }

    public void deletePermission(PermissionEntry permissionEntry)
    {
        permissionsDAO.deletePermissions(permissionEntry);
    }

    public void deletePermission(NodeRef nodeRef, String authority, PermissionReference perm, boolean allow)
    {
        permissionsDAO.deletePermissions(nodeRef, authority, perm, allow);
    }

    public void clearPermission(NodeRef nodeRef, String authority)
    {
        permissionsDAO.clearPermission(nodeRef, authority);
    }

    public void setPermission(NodeRef nodeRef, String authority, PermissionReference perm, boolean allow)
    {
        permissionsDAO.setPermission(nodeRef, authority, perm, allow);
    }

    public void setPermission(PermissionEntry permissionEntry)
    {
        permissionsDAO.setPermission(permissionEntry);
    }

    public void setPermission(NodePermissionEntry nodePermissionEntry)
    {
        permissionsDAO.setPermission(nodePermissionEntry);
    }

    public void setInheritParentPermissions(NodeRef nodeRef, boolean inheritParentPermissions)
    {
        permissionsDAO.setInheritParentPermissions(nodeRef, inheritParentPermissions);
    }
    
    /**
     * @see org.alfresco.service.cmr.security.PermissionService#getInheritParentPermissions(org.alfresco.service.cmr.repository.NodeRef)
     */
    public boolean getInheritParentPermissions(NodeRef nodeRef)
    {
        return permissionsDAO.getInheritParentPermissions(nodeRef);
    }

    //
    // SUPPORT CLASSES
    //

    /**
     * Support class to test the permission on a node.
     * 
     * @author Andy Hind
     */
    private class NodeTest
    {
        /*
         * The required permission.
         */
        PermissionReference required;

        /*
         * Granters of the permission
         */
        Set<PermissionReference> granters;

        /*
         * The additional permissions required at the node level.
         */
        Set<PermissionReference> nodeRequirements = new HashSet<PermissionReference>();

        /*
         * The additional permissions required on the parent.
         */
        Set<PermissionReference> parentRequirements = new HashSet<PermissionReference>();

        /*
         * The permissions required on all children .
         */
        Set<PermissionReference> childrenRequirements = new HashSet<PermissionReference>();

        /*
         * The type name of the node.
         */
        QName typeQName;

        /*
         * The aspects set on the node.
         */
        Set<QName> aspectQNames;

        /*
         * Constructor just gets the additional requirements
         */
        NodeTest(PermissionReference required, QName typeQName, Set<QName> aspectQNames)
        {
            this.required = required;
            this.typeQName = typeQName;
            this.aspectQNames = aspectQNames;

            // Set the required node permissions
            nodeRequirements = modelDAO.getRequiredPermissions(required, typeQName, aspectQNames,
                    RequiredPermission.On.NODE);

            parentRequirements = modelDAO.getRequiredPermissions(required, typeQName, aspectQNames,
                    RequiredPermission.On.PARENT);

            childrenRequirements = modelDAO.getRequiredPermissions(required, typeQName, aspectQNames,
                    RequiredPermission.On.CHILDREN);

            // Find all the permissions that grant the allowed permission
            // All permissions are treated specially.
            granters = modelDAO.getGrantingPermissions(required);
            granters.add(getAllPermissionReference());
            granters.add(OLD_ALL_PERMISSIONS_REFERENCE);
        }

        /**
         * External hook point
         * 
         * @param authorisations
         * @param nodeRef
         * @return
         */
        boolean evaluate(Set<String> authorisations, NodeRef nodeRef)
        {
            Set<Pair<String, PermissionReference>> denied = new HashSet<Pair<String, PermissionReference>>();
            return evaluate(authorisations, nodeRef, denied, null);
        }

        /**
         * Internal hook point for recursion
         * 
         * @param authorisations
         * @param nodeRef
         * @param denied
         * @param recursiveIn
         * @return
         */
        boolean evaluate(Set<String> authorisations, NodeRef nodeRef, Set<Pair<String, PermissionReference>> denied,
                MutableBoolean recursiveIn)
        {
            // Do we defer our required test to a parent (yes if not null)
            MutableBoolean recursiveOut = null;

            Set<Pair<String, PermissionReference>> locallyDenied = new HashSet<Pair<String, PermissionReference>>();
            locallyDenied.addAll(denied);
            locallyDenied.addAll(getDenied(nodeRef));

            // Start out true and "and" all other results
            boolean success = true;

            // Check the required permissions but not for sets they rely on
            // their underlying permissions
            if (required.equals(getPermissionReference(ALL_PERMISSIONS)) || modelDAO.checkPermission(required))
            {
                if (parentRequirements.contains(required))
                {
                    if (checkGlobalPermissions(authorisations) || checkRequired(authorisations, nodeRef, locallyDenied))
                    {
                        // No need to do the recursive test as it has been found
                        recursiveOut = null;
                        if (recursiveIn != null)
                        {
                            recursiveIn.setValue(true);
                        }
                    }
                    else
                    {
                        // Much cheaper to do this as we go then check all the
                        // stack values for each parent
                        recursiveOut = new MutableBoolean(false);
                    }
                }
                else
                {
                    // We have to do the test as no parent will help us out
                    success &= hasSinglePermission(authorisations, nodeRef);
                }
                if (!success)
                {
                    return false;
                }
            }

            // Check the other permissions required on the node
            for (PermissionReference pr : nodeRequirements)
            {
                // Build a new test
                NodeTest nt = new NodeTest(pr, typeQName, aspectQNames);
                success &= nt.evaluate(authorisations, nodeRef, locallyDenied, null);
                if (!success)
                {
                    return false;
                }
            }

            // Check the permission required of the parent

            if (success)
            {
                ChildAssociationRef car = nodeService.getPrimaryParent(nodeRef);
                if (car.getParentRef() != null)
                {

                    NodePermissionEntry nodePermissions = permissionsDAO.getPermissions(car.getChildRef());
                    if ((nodePermissions == null) || (nodePermissions.inheritPermissions()))
                    {

                        locallyDenied.addAll(getDenied(car.getParentRef()));
                        for (PermissionReference pr : parentRequirements)
                        {
                            if (pr.equals(required))
                            {
                                // Recursive permission
                                success &= this.evaluate(authorisations, car.getParentRef(), locallyDenied,
                                        recursiveOut);
                                if ((recursiveOut != null) && recursiveOut.getValue())
                                {
                                    if (recursiveIn != null)
                                    {
                                        recursiveIn.setValue(true);
                                    }
                                }
                            }
                            else
                            {
                                NodeTest nt = new NodeTest(pr, typeQName, aspectQNames);
                                success &= nt.evaluate(authorisations, car.getParentRef(), locallyDenied, null);
                            }

                            if (!success)
                            {
                                return false;
                            }
                        }
                    }
                }
            }

            if ((recursiveOut != null) && (!recursiveOut.getValue()))
            {
                // The required authentication was not resolved in recursion
                return false;
            }

            // Check permissions required of children
            if (childrenRequirements.size() > 0)
            {
                List<ChildAssociationRef> childAssocRefs = nodeService.getChildAssocs(nodeRef);
                for (PermissionReference pr : childrenRequirements)
                {
                    for (ChildAssociationRef child : childAssocRefs)
                    {
                        success &= (hasPermission(child.getChildRef(), pr) == AccessStatus.ALLOWED);
                        if (!success)
                        {
                            return false;
                        }
                    }
                }
            }

            return success;
        }

        public boolean hasSinglePermission(Set<String> authorisations, NodeRef nodeRef)
        {
            // Check global permission

            if (checkGlobalPermissions(authorisations))
            {
                return true;
            }

            Set<Pair<String, PermissionReference>> denied = new HashSet<Pair<String, PermissionReference>>();

            // Keep track of permission that are denied

            // Permissions are only evaluated up the primary parent chain
            // TODO: Do not ignore non primary permissions
            ChildAssociationRef car = nodeService.getPrimaryParent(nodeRef);
            // Work up the parent chain evaluating permissions.
            while (car != null)
            {
                // Add any denied permission to the denied list - these can not
                // then
                // be used to given authentication.
                // A -> B -> C
                // If B denies all permissions to any - allowing all permissions
                // to
                // andy at node A has no effect

                denied.addAll(getDenied(car.getChildRef()));

                // If the current node allows the permission we are done
                // The test includes any parent or ancestor requirements
                if (checkRequired(authorisations, car.getChildRef(), denied))
                {
                    return true;
                }

                // Build the next element of the evaluation chain
                if (car.getParentRef() != null)
                {
                    NodePermissionEntry nodePermissions = permissionsDAO.getPermissions(car.getChildRef());
                    if ((nodePermissions == null) || (nodePermissions.inheritPermissions()))
                    {
                        car = nodeService.getPrimaryParent(car.getParentRef());
                    }
                    else
                    {
                        car = null;
                    }
                }
                else
                {
                    car = null;
                }

            }

            // TODO: Support meta data permissions on the root node?

            return false;

        }

        /**
         * Check if we have a global permission
         * 
         * @param authorisations
         * @return
         */
        private boolean checkGlobalPermissions(Set<String> authorisations)
        {
            for (PermissionEntry pe : modelDAO.getGlobalPermissionEntries())
            {
                if (isGranted(pe, authorisations, null))
                {
                    return true;
                }
            }
            return false;
        }

        /**
         * Get the list of permissions denied for this node.
         * 
         * @param nodeRef
         * @return
         */
        Set<Pair<String, PermissionReference>> getDenied(NodeRef nodeRef)
        {
            Set<Pair<String, PermissionReference>> deniedSet = new HashSet<Pair<String, PermissionReference>>();

            // Loop over all denied permissions
            NodePermissionEntry nodeEntry = permissionsDAO.getPermissions(nodeRef);
            if (nodeEntry != null)
            {
                for (PermissionEntry pe : nodeEntry.getPermissionEntries())
                {
                    if (pe.isDenied())
                    {
                        // All the sets that grant this permission must be
                        // denied
                        // Note that granters includes the orginal permission
                        Set<PermissionReference> granters = modelDAO
                                .getGrantingPermissions(pe.getPermissionReference());
                        for (PermissionReference granter : granters)
                        {
                            deniedSet.add(new Pair<String, PermissionReference>(pe.getAuthority(), granter));
                        }

                        // All the things granted by this permission must be
                        // denied
                        Set<PermissionReference> grantees = modelDAO.getGranteePermissions(pe.getPermissionReference());
                        for (PermissionReference grantee : grantees)
                        {
                            deniedSet.add(new Pair<String, PermissionReference>(pe.getAuthority(), grantee));
                        }

                        // All permission excludes all permissions available for
                        // the node.
                        if (pe.getPermissionReference().equals(getAllPermissionReference()) || pe.getPermissionReference().equals(OLD_ALL_PERMISSIONS_REFERENCE))
                        {
                            for (PermissionReference deny : modelDAO.getAllPermissions(nodeRef))
                            {
                                deniedSet.add(new Pair<String, PermissionReference>(pe.getAuthority(), deny));
                            }
                        }
                    }
                }
            }
            return deniedSet;
        }

        /**
         * Check that a given authentication is available on a node
         * 
         * @param authorisations
         * @param nodeRef
         * @param denied
         * @return
         */
        boolean checkRequired(Set<String> authorisations, NodeRef nodeRef, Set<Pair<String, PermissionReference>> denied)
        {
            NodePermissionEntry nodeEntry = permissionsDAO.getPermissions(nodeRef);

            // No permissions set - short cut to deny
            if (nodeEntry == null)
            {
                return false;
            }

            // Check if each permission allows - the first wins.
            // We could have other voting style mechanisms here
            for (PermissionEntry pe : nodeEntry.getPermissionEntries())
            {
                if (isGranted(pe, authorisations, denied))
                {
                    return true;
                }
            }
            return false;
        }

        /**
         * Is a permission granted
         * 
         * @param pe -
         *            the permissions entry to consider
         * @param granters -
         *            the set of granters
         * @param authorisations -
         *            the set of authorities
         * @param denied -
         *            the set of denied permissions/authority pais
         * @return
         */
        private boolean isGranted(PermissionEntry pe, Set<String> authorisations,
                Set<Pair<String, PermissionReference>> denied)
        {
            // If the permission entry denies then we just deny
            if (pe.isDenied())
            {
                return false;
            }

            // The permission is allowed but we deny it as it is in the denied
            // set
            if (denied != null)
            {
                Pair<String, PermissionReference> specific = new Pair<String, PermissionReference>(pe.getAuthority(),
                        required);
                if (denied.contains(specific))
                {
                    return false;
                }
            }

            // If the permission has a match in both the authorities and
            // granters list it is allowed
            // It applies to the current user and it is granted
            if (authorisations.contains(pe.getAuthority()) && granters.contains(pe.getPermissionReference()))
            {
                {
                    return true;
                }
            }

            // Default deny
            return false;

        }
    }

    /**
     * Helper class to store a pair of objects which may be null
     * 
     * @author Andy Hind
     */
    private static class Pair<A, B>
    {
        A a;

        B b;

        Pair(A a, B b)
        {
            this.a = a;
            this.b = b;
        }

        A getA()
        {
            return a;
        }

        B getB()
        {
            return b;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(this instanceof Pair))
            {
                return false;
            }
            Pair other = (Pair) o;
            return EqualsHelper.nullSafeEquals(this.getA(), other.getA())
                    && EqualsHelper.nullSafeEquals(this.getB(), other.getB());
        }

        @Override
        public int hashCode()
        {
            return (((a == null) ? 0 : a.hashCode()) * 37) + ((b == null) ? 0 : b.hashCode());
        }

    }

    private static class MutableBoolean
    {
        private boolean value;

        MutableBoolean(boolean value)
        {
            this.value = value;
        }

        void setValue(boolean value)
        {
            this.value = value;
        }

        boolean getValue()
        {
            return value;
        }
    }

    public PermissionReference getPermissionReference(QName qname, String permissionName)
    {
        return modelDAO.getPermissionReference(qname, permissionName);
    }

    public PermissionReference getAllPermissionReference()
    {
        return getPermissionReference(ALL_PERMISSIONS);
    }
    
    

    public String getPermission(PermissionReference permissionReference)
    {
        if (modelDAO.isUnique(permissionReference))
        {
            return permissionReference.getName();
        }
        else
        {
            return permissionReference.toString();
        }
    }

    public PermissionReference getPermissionReference(String permissionName)
    {
        return modelDAO.getPermissionReference(null, permissionName);
    }

    public Set<PermissionReference> getSettablePermissionReferences(QName type)
    {
        return modelDAO.getExposedPermissions(type);
    }

    public Set<PermissionReference> getSettablePermissionReferences(NodeRef nodeRef)
    {
        return modelDAO.getExposedPermissions(nodeRef);
    }

    public void deletePermission(NodeRef nodeRef, String authority, String perm, boolean allow)
    {
        deletePermission(nodeRef, authority, getPermissionReference(perm), allow);
    }

    public AccessStatus hasPermission(NodeRef nodeRef, String perm)
    {
        return hasPermission(nodeRef, getPermissionReference(perm));
    }

    public void setPermission(NodeRef nodeRef, String authority, String perm, boolean allow)
    {
        setPermission(nodeRef, authority, getPermissionReference(perm), allow);
    }

    public void deletePermissions(String recipient)
    {
        permissionsDAO.deleteAllPermissionsForAuthority(recipient);
    }
}
