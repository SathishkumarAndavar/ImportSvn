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

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.transaction.UserTransaction;

import junit.framework.TestCase;
import net.sf.acegisecurity.AccountExpiredException;
import net.sf.acegisecurity.Authentication;
import net.sf.acegisecurity.AuthenticationManager;
import net.sf.acegisecurity.BadCredentialsException;
import net.sf.acegisecurity.CredentialsExpiredException;
import net.sf.acegisecurity.DisabledException;
import net.sf.acegisecurity.LockedException;
import net.sf.acegisecurity.UserDetails;
import net.sf.acegisecurity.providers.UsernamePasswordAuthenticationToken;
import net.sf.acegisecurity.providers.dao.SaltSource;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.permissions.PermissionServiceSPI;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.namespace.DynamicNamespacePrefixResolver;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.ApplicationContextHelper;
import org.springframework.context.ApplicationContext;

public class AuthenticationTest extends TestCase
{
    private static ApplicationContext ctx = ApplicationContextHelper.getApplicationContext();

    private NodeService nodeService;

    private SearchService searchService;

    private NodeRef rootNodeRef;

    private NodeRef systemNodeRef;

    private NodeRef typesNodeRef;

    private NodeRef personAndyNodeRef;

    private DictionaryService dictionaryService;

    private MD4PasswordEncoder passwordEncoder;

    private MutableAuthenticationDao dao;

    private AuthenticationManager authenticationManager;

    private SaltSource saltSource;

    private TicketComponent ticketComponent;

    private AuthenticationService authenticationService;
    
    private AuthenticationService pubAuthenticationService;

    private AuthenticationComponent authenticationComponent;
    
    private PermissionServiceSPI permissionServiceSPI;

    private UserTransaction userTransaction;

    public AuthenticationTest()
    {
        super();
    }

    public AuthenticationTest(String arg0)
    {
        super(arg0);
    }

    public void setUp() throws Exception
    {

        nodeService = (NodeService) ctx.getBean("nodeService");
        searchService = (SearchService) ctx.getBean("searchService");
        dictionaryService = (DictionaryService) ctx.getBean("dictionaryService");
        passwordEncoder = (MD4PasswordEncoder) ctx.getBean("passwordEncoder");
        ticketComponent = (TicketComponent) ctx.getBean("ticketComponent");
        authenticationService = (AuthenticationService) ctx.getBean("authenticationService");
        pubAuthenticationService = (AuthenticationService) ctx.getBean("AuthenticationService");
        authenticationComponent = (AuthenticationComponent) ctx.getBean("authenticationComponent");
        permissionServiceSPI = (PermissionServiceSPI) ctx.getBean("permissionService");
        

        dao = (MutableAuthenticationDao) ctx.getBean("alfDaoImpl");
        authenticationManager = (AuthenticationManager) ctx.getBean("authenticationManager");
        saltSource = (SaltSource) ctx.getBean("saltSource");

        TransactionService transactionService = (TransactionService) ctx.getBean(ServiceRegistry.TRANSACTION_SERVICE
                .getLocalName());
        userTransaction = transactionService.getUserTransaction();
        userTransaction.begin();

        StoreRef storeRef = nodeService.createStore(StoreRef.PROTOCOL_WORKSPACE, "Test_" + System.currentTimeMillis());
        rootNodeRef = nodeService.getRootNode(storeRef);

        QName children = ContentModel.ASSOC_CHILDREN;
        QName system = QName.createQName(NamespaceService.SYSTEM_MODEL_1_0_URI, "system");
        QName container = ContentModel.TYPE_CONTAINER;
        QName types = QName.createQName(NamespaceService.SYSTEM_MODEL_1_0_URI, "people");

        systemNodeRef = nodeService.createNode(rootNodeRef, children, system, container).getChildRef();
        typesNodeRef = nodeService.createNode(systemNodeRef, children, types, container).getChildRef();
        Map<QName, Serializable> props = createPersonProperties("Andy");
        personAndyNodeRef = nodeService.createNode(typesNodeRef, children, ContentModel.TYPE_PERSON, container, props)
                .getChildRef();
        assertNotNull(personAndyNodeRef);

        deleteAndy();

    }

    private void deleteAndy()
    {
        RepositoryAuthenticationDao dao = new RepositoryAuthenticationDao();
        dao.setNodeService(nodeService);
        dao.setSearchService(searchService);
        dao.setDictionaryService(dictionaryService);
        dao.setNamespaceService(getNamespacePrefixReolsver(""));
        dao.setPasswordEncoder(passwordEncoder);
        
        if(dao.getUserOrNull("andy") != null)
        {
            dao.deleteUser("andy");
        }
    }

    @Override
    protected void tearDown() throws Exception
    {
        userTransaction.rollback();
        super.tearDown();
    }

    private Map<QName, Serializable> createPersonProperties(String userName)
    {
        HashMap<QName, Serializable> properties = new HashMap<QName, Serializable>();
        properties.put(ContentModel.PROP_USERNAME, "Andy");
        return properties;
    }

    public void testCreateAndyUserAndOtherCRUD() throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        RepositoryAuthenticationDao dao = new RepositoryAuthenticationDao();
        dao.setNodeService(nodeService);
        dao.setSearchService(searchService);
        dao.setDictionaryService(dictionaryService);
        dao.setNamespaceService(getNamespacePrefixReolsver(""));
        dao.setPasswordEncoder(passwordEncoder);

        dao.createUser("Andy", "cabbage".toCharArray());
        assertNotNull(dao.getUserOrNull("Andy"));

        byte[] decodedHash = passwordEncoder.decodeHash(dao.getMD4HashedPassword("Andy"));
        byte[] testHash = MessageDigest.getInstance("MD4").digest("cabbage".getBytes("UnicodeLittleUnmarked"));
        assertEquals(new String(decodedHash), new String(testHash));

        UserDetails AndyDetails = (UserDetails) dao.loadUserByUsername("Andy");
        assertNotNull(AndyDetails);
        assertEquals(dao.getUserNamesAreCaseSensitive() ? "Andy" : "andy", AndyDetails.getUsername());
        // assertNotNull(dao.getSalt(AndyDetails));
        assertTrue(AndyDetails.isAccountNonExpired());
        assertTrue(AndyDetails.isAccountNonLocked());
        assertTrue(AndyDetails.isCredentialsNonExpired());
        assertTrue(AndyDetails.isEnabled());
        assertNotSame("cabbage", AndyDetails.getPassword());
        assertEquals(AndyDetails.getPassword(), passwordEncoder.encodePassword("cabbage", saltSource
                .getSalt(AndyDetails)));
        assertEquals(1, AndyDetails.getAuthorities().length);

        // Object oldSalt = dao.getSalt(AndyDetails);
        dao.updateUser("Andy", "carrot".toCharArray());
        UserDetails newDetails = (UserDetails) dao.loadUserByUsername("Andy");
        assertNotNull(newDetails);
        assertEquals(dao.getUserNamesAreCaseSensitive() ? "Andy" : "andy", newDetails.getUsername());
        // assertNotNull(dao.getSalt(newDetails));
        assertTrue(newDetails.isAccountNonExpired());
        assertTrue(newDetails.isAccountNonLocked());
        assertTrue(newDetails.isCredentialsNonExpired());
        assertTrue(newDetails.isEnabled());
        assertNotSame("carrot", newDetails.getPassword());
        assertEquals(1, newDetails.getAuthorities().length);

        assertNotSame(AndyDetails.getPassword(), newDetails.getPassword());
        // assertNotSame(oldSalt, dao.getSalt(newDetails));

        dao.deleteUser("Andy");
        assertNull(dao.getUserOrNull("Andy"));

        MessageDigest digester;
        try
        {
            digester = MessageDigest.getInstance("MD4");
            System.out.println("Digester from " + digester.getProvider());
        }
        catch (NoSuchAlgorithmException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.out.println("No digester");
        }

    }

    public void testAuthentication()
    {
        dao.createUser("GUEST", "".toCharArray());

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("GUEST", "");
        token.setAuthenticated(false);

        Authentication result = authenticationManager.authenticate(token);
        assertNotNull(result);

        dao.createUser("Andy", "squash".toCharArray());

        token = new UsernamePasswordAuthenticationToken("Andy", "squash");
        token.setAuthenticated(false);

        result = authenticationManager.authenticate(token);
        assertNotNull(result);

        dao.setEnabled("Andy", false);
        try
        {
            result = authenticationManager.authenticate(token);
            assertNotNull(result);
            assertNotNull(null);
        }
        catch (DisabledException e)
        {
            // Expected
        }

        dao.setEnabled("Andy", true);
        result = authenticationManager.authenticate(token);
        assertNotNull(result);

        dao.setLocked("Andy", true);
        try
        {
            result = authenticationManager.authenticate(token);
            assertNotNull(result);
            assertNotNull(null);
        }
        catch (LockedException e)
        {
            // Expected
        }

        dao.setLocked("Andy", false);
        result = authenticationManager.authenticate(token);
        assertNotNull(result);

        dao.setAccountExpires("Andy", true);
        dao.setCredentialsExpire("Andy", true);
        result = authenticationManager.authenticate(token);
        assertNotNull(result);

        dao.setAccountExpiryDate("Andy", null);
        dao.setCredentialsExpiryDate("Andy", null);
        result = authenticationManager.authenticate(token);
        assertNotNull(result);

        dao.setAccountExpiryDate("Andy", new Date(new Date().getTime() + 10000));
        dao.setCredentialsExpiryDate("Andy", new Date(new Date().getTime() + 10000));
        result = authenticationManager.authenticate(token);
        assertNotNull(result);

        dao.setAccountExpiryDate("Andy", new Date(new Date().getTime() - 10000));
        try
        {
            result = authenticationManager.authenticate(token);
            assertNotNull(result);
            assertNotNull(null);
        }
        catch (AccountExpiredException e)
        {
            // Expected
        }
        dao.setAccountExpiryDate("Andy", new Date(new Date().getTime() + 10000));
        result = authenticationManager.authenticate(token);
        assertNotNull(result);

        dao.setCredentialsExpiryDate("Andy", new Date(new Date().getTime() - 10000));
        try
        {
            result = authenticationManager.authenticate(token);
            assertNotNull(result);
            assertNotNull(null);
        }
        catch (CredentialsExpiredException e)
        {
            // Expected
        }
        dao.setCredentialsExpiryDate("Andy", new Date(new Date().getTime() + 10000));
        result = authenticationManager.authenticate(token);
        assertNotNull(result);

        dao.deleteUser("Andy");
        // assertNull(dao.getUserOrNull("Andy"));
    }

    public void testAuthenticationFailure()
    {
        dao.createUser("Andy", "squash".toCharArray());

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("Andy", "turnip");
        token.setAuthenticated(false);

        try
        {
            Authentication result = authenticationManager.authenticate(token);
            assertNotNull(result);
            assertNotNull(null);
        }
        catch (BadCredentialsException e)
        {
            // Expected
        }
        dao.deleteUser("Andy");
        // assertNull(dao.getUserOrNull("Andy"));
    }

    public void testTicket()
    {
        dao.createUser("Andy", "ticket".toCharArray());

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("Andy", "ticket");
        token.setAuthenticated(false);

        Authentication result = authenticationManager.authenticate(token);
        result.setAuthenticated(true);

        String ticket = ticketComponent.getTicket(getUserName(result));
        String user = ticketComponent.validateTicket(ticket);

        user = null;
        try
        {
            user = ticketComponent.validateTicket("INVALID");
            assertNotNull(null);
        }
        catch (AuthenticationException e)
        {
            assertNull(user);
        }

        ticketComponent.invalidateTicketById(ticket);
        try
        {
            user = ticketComponent.validateTicket(ticket);
            assertNotNull(null);
        }
        catch (AuthenticationException e)
        {

        }

        dao.deleteUser("Andy");
        // assertNull(dao.getUserOrNull("Andy"));

    }

    public void testTicketRepeat()
    {
        InMemoryTicketComponentImpl tc = new InMemoryTicketComponentImpl();
        tc.setOneOff(false);
        tc.setTicketsExpire(false);
        tc.setValidDuration("P0D");

        dao.createUser("Andy", "ticket".toCharArray());

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("Andy", "ticket");
        token.setAuthenticated(false);

        Authentication result = authenticationManager.authenticate(token);
        result.setAuthenticated(true);

        String ticket = tc.getTicket(getUserName(result));
        tc.validateTicket(ticket);
        tc.validateTicket(ticket);

        dao.deleteUser("Andy");
        // assertNull(dao.getUserOrNull("Andy"));
    }

    public void testTicketOneOff()
    {
        InMemoryTicketComponentImpl tc = new InMemoryTicketComponentImpl();
        tc.setOneOff(true);
        tc.setTicketsExpire(false);
        tc.setValidDuration("P0D");

        dao.createUser("Andy", "ticket".toCharArray());

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("Andy", "ticket");
        token.setAuthenticated(false);

        Authentication result = authenticationManager.authenticate(token);
        result.setAuthenticated(true);

        String ticket = tc.getTicket(getUserName(result));
        tc.validateTicket(ticket);
        try
        {
            tc.validateTicket(ticket);
            assertNotNull(null);
        }
        catch (AuthenticationException e)
        {

        }

        dao.deleteUser("Andy");
        // assertNull(dao.getUserOrNull("Andy"));
    }

    public void testTicketExpires()
    {
        InMemoryTicketComponentImpl tc = new InMemoryTicketComponentImpl();
        tc.setOneOff(false);
        tc.setTicketsExpire(true);
        tc.setValidDuration("P5S");

        dao.createUser("Andy", "ticket".toCharArray());

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("Andy", "ticket");
        token.setAuthenticated(false);

        Authentication result = authenticationManager.authenticate(token);
        result.setAuthenticated(true);

        String ticket = tc.getTicket(getUserName(result));
        tc.validateTicket(ticket);
        tc.validateTicket(ticket);
        tc.validateTicket(ticket);
        synchronized (this)
        {
            try
            {
                wait(10000);
            }
            catch (InterruptedException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try
        {
            tc.validateTicket(ticket);
            assertNotNull(null);
        }
        catch (AuthenticationException e)
        {

        }

        dao.deleteUser("Andy");
        // assertNull(dao.getUserOrNull("Andy"));
    }

    public void testTicketDoesNotExpire()
    {
        InMemoryTicketComponentImpl tc = new InMemoryTicketComponentImpl();
        tc.setOneOff(false);
        tc.setTicketsExpire(true);
        tc.setValidDuration("P1D");

        dao.createUser("Andy", "ticket".toCharArray());

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("Andy", "ticket");
        token.setAuthenticated(false);

        Authentication result = authenticationManager.authenticate(token);
        result.setAuthenticated(true);

        String ticket = tc.getTicket(getUserName(result));
        tc.validateTicket(ticket);
        tc.validateTicket(ticket);
        tc.validateTicket(ticket);
        synchronized (this)
        {
            try
            {
                wait(10000);
            }
            catch (InterruptedException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        tc.validateTicket(ticket);

        dao.deleteUser("Andy");
        // assertNull(dao.getUserOrNull("Andy"));

    }

    public void testAuthenticationService()
    {
        authenticationService.createAuthentication("GUEST", "".toCharArray());
        authenticationService.authenticate("GUEST", "".toCharArray());

        // create an authentication object e.g. the user
        authenticationService.createAuthentication("Andy", "auth1".toCharArray());

        // authenticate with this user details
        authenticationService.authenticate("Andy", "auth1".toCharArray());

        // assert the user is authenticated
        assertEquals(dao.getUserNamesAreCaseSensitive() ? "Andy" : "andy", authenticationService.getCurrentUserName());
        // delete the user authentication object

        authenticationService.clearCurrentSecurityContext();
        authenticationService.deleteAuthentication("Andy");

        // create a new authentication user object
        authenticationService.createAuthentication("Andy", "auth2".toCharArray());
        // change the password
        authenticationService.setAuthentication("Andy", "auth3".toCharArray());
        // authenticate again to assert password changed
        authenticationService.authenticate("Andy", "auth3".toCharArray());

        try
        {
            authenticationService.authenticate("Andy", "auth1".toCharArray());
            assertNotNull(null);
        }
        catch (AuthenticationException e)
        {

        }
        try
        {
            authenticationService.authenticate("Andy", "auth2".toCharArray());
            assertNotNull(null);
        }
        catch (AuthenticationException e)
        {

        }

        // get the ticket that represents the current user authentication
        // instance
        String ticket = authenticationService.getCurrentTicket();
        // validate our ticket is still valid
        authenticationService.validate(ticket);

        // destroy the ticket instance
        authenticationService.invalidateTicket(ticket);
        try
        {
            authenticationService.validate(ticket);
            assertNotNull(null);
        }
        catch (AuthenticationException e)
        {

        }

        // clear any context and check we are no longer authenticated
        authenticationService.clearCurrentSecurityContext();
        assertNull(authenticationService.getCurrentUserName());

        dao.deleteUser("Andy");
        // assertNull(dao.getUserOrNull("Andy"));
    }

    
    public void testPubAuthenticationService()
    {
        pubAuthenticationService.createAuthentication("GUEST", "".toCharArray());
        pubAuthenticationService.authenticate("GUEST", "".toCharArray());

        // create an authentication object e.g. the user
        pubAuthenticationService.createAuthentication("Andy", "auth1".toCharArray());

        // authenticate with this user details
        pubAuthenticationService.authenticate("Andy", "auth1".toCharArray());

        // assert the user is authenticated
        assertEquals(dao.getUserNamesAreCaseSensitive() ? "Andy" : "andy", authenticationService.getCurrentUserName());
        // delete the user authentication object

        pubAuthenticationService.clearCurrentSecurityContext();
        pubAuthenticationService.deleteAuthentication("Andy");

        // create a new authentication user object
        pubAuthenticationService.createAuthentication("Andy", "auth2".toCharArray());
        // change the password
        pubAuthenticationService.setAuthentication("Andy", "auth3".toCharArray());
        // authenticate again to assert password changed
        pubAuthenticationService.authenticate("Andy", "auth3".toCharArray());

        try
        {
            pubAuthenticationService.authenticate("Andy", "auth1".toCharArray());
            assertNotNull(null);
        }
        catch (AuthenticationException e)
        {

        }
        try
        {
            pubAuthenticationService.authenticate("Andy", "auth2".toCharArray());
            assertNotNull(null);
        }
        catch (AuthenticationException e)
        {

        }

        // get the ticket that represents the current user authentication
        // instance
        String ticket = pubAuthenticationService.getCurrentTicket();
        // validate our ticket is still valid
        pubAuthenticationService.validate(ticket);

        // destroy the ticket instance
        pubAuthenticationService.invalidateTicket(ticket);
        try
        {
            pubAuthenticationService.validate(ticket);
            assertNotNull(null);
        }
        catch (AuthenticationException e)
        {

        }

        // clear any context and check we are no longer authenticated
        pubAuthenticationService.clearCurrentSecurityContext();
        assertNull(pubAuthenticationService.getCurrentUserName());

        dao.deleteUser("Andy");
        // assertNull(dao.getUserOrNull("Andy"));
    }

    
    public void testPassThroughLogin()
    {
        authenticationService.createAuthentication("Andy", "auth1".toCharArray());

        authenticationComponent.setCurrentUser("Andy");
        assertEquals(dao.getUserNamesAreCaseSensitive() ? "Andy" : "andy", authenticationService.getCurrentUserName());

        //authenticationService.deleteAuthentication("andy");
    }

    private String getUserName(Authentication authentication)
    {
        String username = authentication.getPrincipal().toString();

        if (authentication.getPrincipal() instanceof UserDetails)
        {
            username = ((UserDetails) authentication.getPrincipal()).getUsername();
        }
        return username;
    }

    private NamespacePrefixResolver getNamespacePrefixReolsver(String defaultURI)
    {
        DynamicNamespacePrefixResolver nspr = new DynamicNamespacePrefixResolver(null);
        nspr.registerNamespace(NamespaceService.SYSTEM_MODEL_PREFIX, NamespaceService.SYSTEM_MODEL_1_0_URI);
        nspr.registerNamespace(NamespaceService.CONTENT_MODEL_PREFIX, NamespaceService.CONTENT_MODEL_1_0_URI);
        nspr.registerNamespace(ContentModel.USER_MODEL_PREFIX, ContentModel.USER_MODEL_URI);
        nspr.registerNamespace("namespace", "namespace");
        nspr.registerNamespace(NamespaceService.DEFAULT_PREFIX, defaultURI);
        return nspr;
    }
}
