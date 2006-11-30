/*
 * Copyright (C) 2005-2006 Alfresco, Inc.
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
package org.alfresco.repo.node.index;

import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import net.sf.acegisecurity.Authentication;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.domain.Transaction;
import org.alfresco.repo.node.db.NodeDaoService;
import org.alfresco.repo.search.Indexer;
import org.alfresco.repo.search.impl.lucene.LuceneQueryParser;
import org.alfresco.repo.search.impl.lucene.fts.FullTextSearchIndexer;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.TransactionComponent;
import org.alfresco.repo.transaction.TransactionUtil;
import org.alfresco.repo.transaction.TransactionUtil.TransactionWork;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.NodeRef.Status;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.util.PropertyCheck;
import org.alfresco.util.VmShutdownListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Abstract helper for reindexing.
 * 
 * @see #reindexImpl()
 * @see #getIndexerWriteLock()
 * @see #isShuttingDown()
 * 
 * @author Derek Hulley
 */
public abstract class AbstractReindexComponent implements IndexRecovery
{
    private static Log logger = LogFactory.getLog(AbstractReindexComponent.class);
    
    /** kept to notify the thread that it should quit */
    private static VmShutdownListener vmShutdownListener = new VmShutdownListener("MissingContentReindexComponent");
    
    private AuthenticationComponent authenticationComponent;
    /** provides transactions to atomically index each missed transaction */
    protected TransactionComponent transactionService;
    /** the component to index the node hierarchy */
    protected Indexer indexer;
    /** the FTS indexer that we will prompt to pick up on any un-indexed text */
    protected FullTextSearchIndexer ftsIndexer;
    /** the component providing searches of the indexed nodes */
    protected SearchService searcher;
    /** the component giving direct access to <b>store</b> instances */
    protected NodeService nodeService;
    /** the component giving direct access to <b>transaction</b> instances */
    protected NodeDaoService nodeDaoService;
    
    private boolean shutdown;
    private final WriteLock indexerWriteLock;
    
    public AbstractReindexComponent()
    {
        shutdown = false;
        ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        indexerWriteLock = readWriteLock.writeLock();
    }
    
    /**
     * Convenience method to get a common write lock.  This can be used to avoid
     * concurrent access to the work methods.
     */
    protected WriteLock getIndexerWriteLock()
    {
        return indexerWriteLock;
    }
    
    /**
     * Programmatically notify a reindex thread to terminate
     * 
     * @param shutdown true to shutdown, false to reset
     */
    public void setShutdown(boolean shutdown)
    {
        this.shutdown = shutdown;
    }
    
    /**
     * 
     * @return Returns true if the VM shutdown hook has been triggered, or the instance
     *      was programmatically {@link #shutdown shut down}
     */
    protected boolean isShuttingDown()
    {
        return shutdown || vmShutdownListener.isVmShuttingDown();
    }

    /**
     * @param authenticationComponent ensures that reindexing operates as system user
     */
    public void setAuthenticationComponent(AuthenticationComponent authenticationComponent)
    {
        this.authenticationComponent = authenticationComponent;
    }

    /**
     * Set the low-level transaction component to use
     * 
     * @param transactionComponent provide transactions to index each missed transaction
     */
    public void setTransactionComponent(TransactionComponent transactionComponent)
    {
        this.transactionService = transactionComponent;
    }

    /**
     * @param indexer the indexer that will be index
     */
    public void setIndexer(Indexer indexer)
    {
        this.indexer = indexer;
    }
    
    /**
     * @param ftsIndexer the FTS background indexer
     */
    public void setFtsIndexer(FullTextSearchIndexer ftsIndexer)
    {
        this.ftsIndexer = ftsIndexer;
    }

    /**
     * @param searcher component providing index searches
     */
    public void setSearcher(SearchService searcher)
    {
        this.searcher = searcher;
    }

    /**
     * @param nodeService provides information about nodes for indexing
     */
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    /**
     * @param nodeDaoService provides access to transaction-related queries
     */
    public void setNodeDaoService(NodeDaoService nodeDaoService)
    {
        this.nodeDaoService = nodeDaoService;
    }

    /**
     * Perform the actual work.  This method will be called as the system user
     * and within an existing transaction.  This thread will only ever be accessed
     * by a single thread per instance.
     *
     */
    protected abstract void reindexImpl();
    
    /**
     * If this object is currently busy, then it just nothing
     */
    public final void reindex()
    {
        PropertyCheck.mandatory(this, "authenticationComponent", this.authenticationComponent);
        PropertyCheck.mandatory(this, "ftsIndexer", this.ftsIndexer);
        PropertyCheck.mandatory(this, "indexer", this.indexer);
        PropertyCheck.mandatory(this, "searcher", this.searcher);
        PropertyCheck.mandatory(this, "nodeService", this.nodeService);
        PropertyCheck.mandatory(this, "nodeDaoService", this.nodeDaoService);
        PropertyCheck.mandatory(this, "transactionComponent", this.transactionService);
        
        if (indexerWriteLock.tryLock())
        {
            Authentication auth = null;
            try
            {
                auth = AuthenticationUtil.getCurrentAuthentication();
                // authenticate as the system user
                authenticationComponent.setSystemUserAsCurrentUser();
                TransactionWork<Object> reindexWork = new TransactionWork<Object>()
                {
                    public Object doWork() throws Exception
                    {
                        reindexImpl();
                        return null;
                    }
                };
                TransactionUtil.executeInUserTransaction(transactionService, reindexWork);
            }
            finally
            {
                try { indexerWriteLock.unlock(); } catch (Throwable e) {}
                if (auth != null)
                {
                    authenticationComponent.setCurrentAuthentication(auth);
                }
            }
            // done
            if (logger.isDebugEnabled())
            {
                logger.debug("Reindex work completed: " + this);
            }
        }
        else
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Bypassed reindex work - already busy: " + this);
            }
        }
    }
    
    /**
     * Gets the last indexed transaction working back from the provided index.
     * This method can be used to hunt for a starting point for indexing of
     * transactions not yet in the index.
     */
    protected long getLastIndexedTxn(long lastTxnId)
    {
        // get the last transaction
        long lastFoundTxnId = lastTxnId + 10L;
        boolean found = false;
        while (!found && lastFoundTxnId >= 0)
        {
            // reduce the transaction ID
            lastFoundTxnId = lastFoundTxnId - 10L;
            // break out as soon as we find a transaction that is in the index
            found = isTxnIdPresentInIndex(lastFoundTxnId);
            if (found)
            {
                break;
            }
        }
        // done
        if (logger.isDebugEnabled())
        {
            logger.debug("Found last index txn before " + lastTxnId + ": " + lastFoundTxnId);
        }
        return lastFoundTxnId;
    }
    
    protected boolean isTxnIdPresentInIndex(long txnId)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Checking for transaction in index: " + txnId);
        }
        
        Transaction txn = nodeDaoService.getTxnById(txnId);
        if (txn == null)
        {
            return true;
        }

        // count the changes in the transaction
        int updateCount = nodeDaoService.getTxnUpdateCount(txnId);
        int deleteCount = nodeDaoService.getTxnDeleteCount(txnId);
        if (logger.isDebugEnabled())
        {
            logger.debug("Transaction has " + updateCount + " updates and " + deleteCount + " deletes: " + txnId);
        }
        
        // get the stores
        boolean found = false;
        List<StoreRef> storeRefs = nodeService.getStores();
        for (StoreRef storeRef : storeRefs)
        {
            boolean inStore = isTxnIdPresentInIndex(storeRef, txn, updateCount, deleteCount);
            if (inStore)
            {
                // found in a particular store
                found = true;
                break;
            }
        }
        // done
        if (logger.isDebugEnabled())
        {
            logger.debug("Transaction " + txnId + " was " + (found ? "found" : "not found") + " in indexes.");
        }
        return found;
    }
    
    /**
     * @return Returns true if the given transaction is indexed in the in the 
     */
    private boolean isTxnIdPresentInIndex(StoreRef storeRef, Transaction txn, int updateCount, int deleteCount)
    {
        long txnId = txn.getId();
        String changeTxnId = txn.getChangeTxnId();
        // do the most update check, which is most common
        if (updateCount > 0)
        {
            ResultSet results = null;
            try
            {
                SearchParameters sp = new SearchParameters();
                sp.addStore(storeRef);
                // search for it in the index, sorting with youngest first, fetching only 1
                sp.setLanguage(SearchService.LANGUAGE_LUCENE);
                sp.setQuery("TX:" + LuceneQueryParser.escape(changeTxnId));
                sp.setLimit(1);
                
                results = searcher.query(sp);
                
                if (results.length() > 0)
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Index has results for txn (OK): " + txnId);
                    }
                    return true;        // there were updates/creates and results for the txn were found
                }
                else
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Index has no results for txn (Index out of date): " + txnId);
                    }
                    return false;
                }
            }
            finally
            {
                if (results != null) { results.close(); }
            }
        }
        // there have been deletes, so we have to ensure that none of the nodes deleted are present in the index
        // get all node refs for the transaction
        List<NodeRef> nodeRefs = nodeDaoService.getTxnChangesForStore(storeRef, txnId);
        for (NodeRef nodeRef : nodeRefs)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Searching for node in index: \n" +
                        "   node: " + nodeRef + "\n" +
                        "   txn: " + txnId);
            }
            // we know that these are all deletions
            ResultSet results = null;
            try
            {
                SearchParameters sp = new SearchParameters();
                sp.addStore(storeRef);
                // search for it in the index, sorting with youngest first, fetching only 1
                sp.setLanguage(SearchService.LANGUAGE_LUCENE);
                sp.setQuery("ID:" + LuceneQueryParser.escape(nodeRef.toString()));
                sp.setLimit(1);
                
                results = searcher.query(sp);
                
                if (results.length() == 0)
                {
                    // no results, as expected
                    if (logger.isDebugEnabled())
                    {
                        logger.debug(" --> Node not found (OK)");
                    }
                    continue;
                }
                else
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug(" --> Node found (Index out of date)");
                    }
                    return false;
                }
            }
            finally
            {
                if (results != null) { results.close(); }
            }
        }
        
        // all tests passed
        if (logger.isDebugEnabled())
        {
            logger.debug("Index is in synch with transaction: " + txnId);
        }
        return true;
    }
    
    /**
     * Perform a full reindexing of the given transaction in the context of a completely
     * new transaction.
     * 
     * @param txnId the transaction identifier
     */
    protected void reindexTransaction(final long txnId)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Reindexing transaction: " + txnId);
        }
        
        TransactionWork<Object> reindexWork = new TransactionWork<Object>()
        {
            public Object doWork() throws Exception
            {
                // get the node references pertinent to the transaction
                List<NodeRef> nodeRefs = nodeDaoService.getTxnChanges(txnId);
                // reindex each node
                for (NodeRef nodeRef : nodeRefs)
                {
                    Status nodeStatus = nodeService.getNodeStatus(nodeRef);
                    if (nodeStatus == null)
                    {
                        // it's not there any more
                        continue;
                    }
                    if (nodeStatus.isDeleted())                                 // node deleted
                    {
                        // only the child node ref is relevant
                        ChildAssociationRef assocRef = new ChildAssociationRef(
                                ContentModel.ASSOC_CHILDREN,
                                null,
                                null,
                                nodeRef);
                      indexer.deleteNode(assocRef);
                    }
                    else                                                        // node created
                    {
                        // get the primary assoc for the node
                        ChildAssociationRef primaryAssocRef = nodeService.getPrimaryParent(nodeRef);
                        // reindex
                        indexer.createNode(primaryAssocRef);
                    }
                }
                // done
                return null;
            }
        };
        TransactionUtil.executeInNonPropagatingUserTransaction(transactionService, reindexWork, true);
        // done
    }
}