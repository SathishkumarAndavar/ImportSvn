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
package org.alfresco.filesys.server;

import java.net.InetAddress;

import javax.transaction.UserTransaction;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.filesys.server.auth.ClientInfo;
import org.alfresco.filesys.server.core.SharedDevice;
import org.alfresco.filesys.server.core.SharedDeviceList;
import org.alfresco.service.transaction.TransactionService;

/**
 * Server Session Base Class
 * <p>
 * Base class for server session implementations for different protocols.
 */
public abstract class SrvSession
{

    // Network server this session is associated with

    private NetworkServer m_server;

    // Session id/slot number

    private int m_sessId;

    // Unique session id string

    private String m_uniqueId;

    // Process id

    private int m_processId = -1;

    // Session/user is logged on/validated

    private boolean m_loggedOn;

    // Client details

    private ClientInfo m_clientInfo;

    // Challenge key used for this session

    private byte[] m_challenge;

    // Debug flags for this session

    private int m_debug;
    private String m_dbgPrefix;

    // Session shutdown flag

    private boolean m_shutdown;

    // Protocol type

    private String m_protocol;

    // Remote client/host name

    private String m_remoteName;

    // Authentication token, used during logon
    
    private Object m_authToken;

    //  List of dynamic/temporary shares created for this session
    
    private SharedDeviceList m_dynamicShares;
    
    // Active transaction and read/write flag
    
    private UserTransaction m_transaction;
    private boolean m_readOnlyTrans;
    
    // Request and transaction counts
    
    protected int m_reqCount;
    protected int m_transCount;
    protected int m_transConvCount;
    
    /**
     * Class constructor
     * 
     * @param sessId int
     * @param srv NetworkServer
     * @param proto String
     * @param remName String
     */
    public SrvSession(int sessId, NetworkServer srv, String proto, String remName)
    {
        m_sessId = sessId;
        m_server = srv;

        setProtocolName(proto);
        setRemoteName(remName);
    }

    /**
     * Add a dynamic share to the list of shares created for this session
     * 
     * @param shrDev SharedDevice
     */
    public final void addDynamicShare(SharedDevice shrDev) {
        
        //  Check if the dynamic share list must be allocated
        
        if ( m_dynamicShares == null)
            m_dynamicShares = new SharedDeviceList();
            
        //  Add the new share to the list
        
        m_dynamicShares.addShare(shrDev);
    }
    
    /**
     * Return the authentication token
     * 
     * @return Object
     */
    public final Object getAuthenticationToken()
    {
        return m_authToken;
    }
    
    /**
     * Determine if the authentication token is set
     * 
     * @return boolean
     */
    public final boolean hasAuthenticationToken()
    {
        return m_authToken != null ? true : false;
    }
    
    /**
     * Return the session challenge key
     * 
     * @return byte[]
     */
    public final byte[] getChallengeKey()
    {
        return m_challenge;
    }

    /**
     * Determine if the challenge key has been set for this session
     * 
     * @return boolean
     */
    public final boolean hasChallengeKey()
    {
        return m_challenge != null ? true : false;
    }

    /**
     * Return the process id
     * 
     * @return int
     */
    public final int getProcessId()
    {
        return m_processId;
    }

    /**
     * Return the remote client network address
     * 
     * @return InetAddress
     */
    public abstract InetAddress getRemoteAddress();

    /**
     * Return the session id for this session.
     * 
     * @return int
     */
    public final int getSessionId()
    {
        return m_sessId;
    }

    /**
     * Return the server this session is associated with
     * 
     * @return NetworkServer
     */
    public final NetworkServer getServer()
    {
        return m_server;
    }

    /**
     * Check if the session has valid client information
     * 
     * @return boolean
     */
    public final boolean hasClientInformation()
    {
        return m_clientInfo != null ? true : false;
    }

    /**
     * Return the client information
     * 
     * @return ClientInfo
     */
    public final ClientInfo getClientInformation()
    {
        return m_clientInfo;
    }

    /**
     * Determine if the session has any dynamic shares
     * 
     * @return boolean
     */
    public final boolean hasDynamicShares() {
        return m_dynamicShares != null ? true : false;
    }

    /**
     * Return the list of dynamic shares created for this session
     * 
     * @return SharedDeviceList
     */
    public final SharedDeviceList getDynamicShareList() {
        return m_dynamicShares;
    }

    /**
     * Determine if the protocol type has been set
     * 
     * @return boolean
     */
    public final boolean hasProtocolName()
    {
        return m_protocol != null ? true : false;
    }

    /**
     * Return the protocol name
     * 
     * @return String
     */
    public final String getProtocolName()
    {
        return m_protocol;
    }

    /**
     * Determine if the remote client name has been set
     * 
     * @return boolean
     */
    public final boolean hasRemoteName()
    {
        return m_remoteName != null ? true : false;
    }

    /**
     * Return the remote client name
     * 
     * @return String
     */
    public final String getRemoteName()
    {
        return m_remoteName;
    }

    /**
     * Determine if the session is logged on/validated
     * 
     * @return boolean
     */
    public final boolean isLoggedOn()
    {
        return m_loggedOn;
    }

    /**
     * Determine if the session has been shut down
     * 
     * @return boolean
     */
    public final boolean isShutdown()
    {
        return m_shutdown;
    }

    /**
     * Return the unique session id
     * 
     * @return String
     */
    public final String getUniqueId()
    {
        return m_uniqueId;
    }

    /**
     * Determine if the specified debug flag is enabled.
     * 
     * @return boolean
     * @param dbg int
     */
    public final boolean hasDebug(int dbgFlag)
    {
        if ((m_debug & dbgFlag) != 0)
            return true;
        return false;
    }

    /**
     * Set the authentication token
     * 
     * @param authToken Object
     */
    public final void setAuthenticationToken(Object authToken)
    {
        m_authToken = authToken;
    }
    
    /**
     * Set the client information
     * 
     * @param client ClientInfo
     */
    public final void setClientInformation(ClientInfo client)
    {
        m_clientInfo = client;
    }

    /**
     * Set the session challenge key
     * 
     * @param key byte[]
     */
    public final void setChallengeKey(byte[] key)
    {
        m_challenge = key;
    }

    /**
     * Set the debug output interface.
     * 
     * @param flgs int
     */
    public final void setDebug(int flgs)
    {
        m_debug = flgs;
    }

    /**
     * Set the debug output prefix for this session
     * 
     * @param prefix String
     */
    public final void setDebugPrefix(String prefix)
    {
        m_dbgPrefix = prefix;
    }

    /**
     * Set the logged on/validated status for the session
     * 
     * @param loggedOn boolean
     */
    public final void setLoggedOn(boolean loggedOn)
    {
        m_loggedOn = loggedOn;
    }

    /**
     * Set the process id
     * 
     * @param id int
     */
    public final void setProcessId(int id)
    {
        m_processId = id;
    }

    /**
     * Set the protocol name
     * 
     * @param name String
     */
    public final void setProtocolName(String name)
    {
        m_protocol = name;
    }

    /**
     * Set the remote client name
     * 
     * @param name String
     */
    public final void setRemoteName(String name)
    {
        m_remoteName = name;
    }

    /**
     * Set the session id for this session.
     * 
     * @param id int
     */
    public final void setSessionId(int id)
    {
        m_sessId = id;
    }

    /**
     * Set the unique session id
     * 
     * @param unid String
     */
    public final void setUniqueId(String unid)
    {
        m_uniqueId = unid;
    }

    /**
     * Set the shutdown flag
     * 
     * @param flg boolean
     */
    protected final void setShutdown(boolean flg)
    {
        m_shutdown = flg;
    }

    /**
     * Close the network session
     */
    public void closeSession()
    {
        //  Release any dynamic shares owned by this session
        
        if ( hasDynamicShares()) {
            
            //  Close the dynamic shares
            
            getServer().getShareMapper().deleteShares(this);
        }
    }
    
    /**
     * Create and start a transaction, if not already active
     * 
     * @param transService TransactionService
     * @param readOnly boolean
     * @return boolean
     * @exception AlfrescoRuntimeException
     */
    public final boolean beginTransaction(TransactionService transService, boolean readOnly)
        throws AlfrescoRuntimeException
    {
        boolean created = false;
        
        // If there is an active transaction check that it is the required type
        
        if ( m_transaction != null)
        {
            // Check if the transaction is a write transaction, if write has been requested
            
            if ( readOnly == false && m_readOnlyTrans == true)
            {
                // Commit the read-only transaction
                
                try
                {
                    m_transaction.commit();
                    m_transConvCount++;
                }
                catch ( Exception ex)
                {
                    throw new AlfrescoRuntimeException("Failed to commit read-only transaction, " + ex.getMessage());
                }
                finally
                {
                    // Clear the active transaction

                    m_transaction = null;
                }
            }
        }
        
        // Create the transaction
        
        if ( m_transaction == null)
        {
            try
            {
                m_transaction = transService.getUserTransaction(readOnly);
                m_transaction.begin();
                
                created = true;
                
                m_readOnlyTrans = readOnly;
                
                m_transCount++;
            }
            catch (Exception ex)
            {
                throw new AlfrescoRuntimeException("Failed to create transaction, " + ex.getMessage());
            }
        }
        
        return created;
    }
    
    /**
     * Determine if the session has an active transaction
     * 
     * @return boolean
     */
    public final boolean hasUserTransaction()
    {
        return m_transaction != null ? true : false;
    }
    
    /**
     * Get the active transaction and clear the stored transaction
     * 
     *  @return UserTransaction
     */
    public final UserTransaction getUserTransaction()
    {
        UserTransaction trans = m_transaction;
        m_transaction = null;
        return trans;
    }
}
