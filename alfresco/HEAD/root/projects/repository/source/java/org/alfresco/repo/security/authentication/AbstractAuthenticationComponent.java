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
package org.alfresco.repo.security.authentication;

import org.alfresco.error.AlfrescoRuntimeException;

import net.sf.acegisecurity.Authentication;
import net.sf.acegisecurity.GrantedAuthority;
import net.sf.acegisecurity.GrantedAuthorityImpl;
import net.sf.acegisecurity.UserDetails;
import net.sf.acegisecurity.context.Context;
import net.sf.acegisecurity.context.ContextHolder;
import net.sf.acegisecurity.context.security.SecureContext;
import net.sf.acegisecurity.context.security.SecureContextImpl;
import net.sf.acegisecurity.providers.UsernamePasswordAuthenticationToken;
import net.sf.acegisecurity.providers.dao.User;

/**
 * This class abstract the support required to set up and query the Acegi
 * context for security enforcement.
 * 
 * There are some simple default method implementations to support simple
 * authentication.
 * 
 * @author Andy Hind
 */
public abstract class AbstractAuthenticationComponent implements AuthenticationComponent
{

    // Name of the system user

    private static final String SYSTEM_USER_NAME = "System";

    public AbstractAuthenticationComponent()
    {
        super();
    }

    /**
     * Explicitly set the current user to be authenticated.
     * 
     * @param userName
     *            String
     * @return Authentication
     */
    public Authentication setCurrentUser(String userName)
    {
        try
        {
            UserDetails ud = null;
            if (userName.equals(SYSTEM_USER_NAME))
            {
                GrantedAuthority[] gas = new GrantedAuthority[1];
                gas[0] = new GrantedAuthorityImpl("ROLE_SYSTEM");
                ud = new User(SYSTEM_USER_NAME, "", true, true, true, true, gas);
            }
            else
            {
                ud = getUserDetails(userName);
            }

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(ud, "", ud
                    .getAuthorities());
            auth.setDetails(ud);
            auth.setAuthenticated(true);
            return setCurrentAuthentication(auth);
        }
        catch (net.sf.acegisecurity.AuthenticationException ae)
        {
            throw new AuthenticationException(ae.getMessage(), ae);
        }
    }

    /**
     * Default implementation that makes an ACEGI object on the fly
     * 
     * @param userName
     * @return
     */
    protected UserDetails getUserDetails(String userName)
    {
        GrantedAuthority[] gas = new GrantedAuthority[1];
        gas[0] = new GrantedAuthorityImpl("ROLE_AUTHENTICATED");
        UserDetails ud = new User(userName, "", true, true, true, true, gas);
        return ud;
    }

    /**
     * Explicitly set the current authentication.
     * 
     * @param authentication
     *            Authentication
     */
    public Authentication setCurrentAuthentication(Authentication authentication)
    {
        Context context = ContextHolder.getContext();
        SecureContext sc = null;
        if ((context == null) || !(context instanceof SecureContext))
        {
            sc = new SecureContextImpl();
            ContextHolder.setContext(sc);
        }
        else
        {
            sc = (SecureContext) context;
        }
        authentication.setAuthenticated(true);
        sc.setAuthentication(authentication);
        return authentication;
    }

    /**
     * Get the current authentication context
     * 
     * @return Authentication
     * @throws AuthenticationException
     */
    public Authentication getCurrentAuthentication() throws AuthenticationException
    {
        Context context = ContextHolder.getContext();
        if ((context == null) || !(context instanceof SecureContext))
        {
            return null;
        }
        return ((SecureContext) context).getAuthentication();
    }

    /**
     * Get the current user name.
     * 
     * @return String
     * @throws AuthenticationException
     */
    public String getCurrentUserName() throws AuthenticationException
    {
        Context context = ContextHolder.getContext();
        if ((context == null) || !(context instanceof SecureContext))
        {
            return null;
        }
        return getUserName(((SecureContext) context).getAuthentication());
    }

    /**
     * Get the current user name
     * 
     * @param authentication
     *            Authentication
     * @return String
     */
    private String getUserName(Authentication authentication)
    {
        String username = authentication.getPrincipal().toString();

        if (authentication.getPrincipal() instanceof UserDetails)
        {
            username = ((UserDetails) authentication.getPrincipal()).getUsername();
        }

        return username;
    }

    /**
     * Set the system user as the current user.
     * 
     * @return Authentication
     */
    public Authentication setSystemUserAsCurrentUser()
    {
        return setCurrentUser(SYSTEM_USER_NAME);
    }

    /**
     * Get the name of the system user
     * 
     * @return String
     */
    public String getSystemUserName()
    {
        return SYSTEM_USER_NAME;
    }

    /**
     * Remove the current security information
     */
    public void clearCurrentSecurityContext()
    {
        ContextHolder.setContext(null);
    }

    /**
     * The default is not to support Authentication token base authentication
     */
    public Authentication authenticate(Authentication token) throws AuthenticationException
    {
        throw new AlfrescoRuntimeException("Authentication via token not supported");
    }

    /**
     * The should only be supported if getNTLMMode() is NTLMMode.MD4_PROVIDER.
     */
    public String getMD4HashedPassword(String userName)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Get the NTML mode - none - supports MD4 hash to integrate - or it can
     * asct as an NTLM authentication
     */
    public NTLMMode getNTLMMode()
    {
        return NTLMMode.NONE;
    }

}
