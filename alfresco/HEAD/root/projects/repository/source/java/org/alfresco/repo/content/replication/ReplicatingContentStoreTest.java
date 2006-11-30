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
package org.alfresco.repo.content.replication;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.alfresco.repo.content.AbstractContentReadWriteTest;
import org.alfresco.repo.content.ContentStore;
import org.alfresco.repo.content.filestore.FileContentStore;
import org.alfresco.repo.transaction.DummyTransactionService;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.util.GUID;
import org.alfresco.util.TempFileProvider;

/**
 * Tests read and write functionality for the replicating store.
 * <p>
 * By default, replication is off for both the inbound and outbound
 * replication.  Specific tests change this.
 * 
 * @see org.alfresco.repo.content.replication.ReplicatingContentStore
 * 
 * @author Derek Hulley
 */
public class ReplicatingContentStoreTest extends AbstractContentReadWriteTest
{
    private static final String SOME_CONTENT = "The No. 1 Ladies' Detective Agency";
    
    private ReplicatingContentStore replicatingStore;
    private ContentStore primaryStore;
    private List<ContentStore> secondaryStores;
    
    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        
        File tempDir = TempFileProvider.getTempDir();
        // create a primary file store
        String storeDir = tempDir.getAbsolutePath() + File.separatorChar + GUID.generate();
        primaryStore = new FileContentStore(storeDir);
        // create some secondary file stores
        secondaryStores = new ArrayList<ContentStore>(3);
        for (int i = 0; i < 3; i++)
        {
            storeDir = tempDir.getAbsolutePath() + File.separatorChar + GUID.generate();
            ContentStore store = new FileContentStore(storeDir);
            secondaryStores.add(store);
        }
        replicatingStore = new ReplicatingContentStore();
        replicatingStore.setTransactionService(new DummyTransactionService());
        replicatingStore.setPrimaryStore(primaryStore);
        replicatingStore.setSecondaryStores(secondaryStores);
        replicatingStore.setOutbound(false);
        replicatingStore.setInbound(false);
    }

    @Override
    public ContentStore getStore()
    {
        return replicatingStore;
    }
    
    /**
     * Performs checks necessary to ensure the proper replication of content for the given
     * URL
     */
    private void checkForReplication(boolean inbound, boolean outbound, String contentUrl, String content)
    {
        if (inbound)
        {
            ContentReader reader = primaryStore.getReader(contentUrl);
            assertTrue("Content was not replicated into the primary store", reader.exists());
            assertEquals("The replicated content was incorrect", content, reader.getContentString());
        }
        if (outbound)
        {
            for (ContentStore store : secondaryStores)
            {
                ContentReader reader = store.getReader(contentUrl);
                assertTrue("Content was not replicated out to the secondary stores within a second", reader.exists());
                assertEquals("The replicated content was incorrect", content, reader.getContentString());
            }
        }
    }
    
    /**
     * Checks that the url is present in each of the stores
     * 
     * @param contentUrl
     * @param mustExist true if the content must exist, false if it must <b>not</b> exist
     */
    private void checkForUrl(String contentUrl, boolean mustExist)
    {
        // check that the URL is present for each of the stores
        for (ContentStore store : secondaryStores)
        {
            Set<String> urls = store.getUrls();
            assertTrue("URL of new content not present in store", urls.contains(contentUrl) == mustExist);
        }
    }
    
    public void testNoReplication() throws Exception
    {
        ContentWriter writer = getWriter();
        writer.putContent(SOME_CONTENT);
        
        checkForReplication(false, false, writer.getContentUrl(), SOME_CONTENT);
    }
    
    public void testOutboundReplication() throws Exception
    {
        replicatingStore.setOutbound(true);
        
        // write some content
        ContentWriter writer = getWriter();
        writer.putContent(SOME_CONTENT);
        String contentUrl = writer.getContentUrl();
        
        checkForReplication(false, true, contentUrl, SOME_CONTENT);
        
        // check for outbound deletes
        replicatingStore.delete(contentUrl);
        checkForUrl(contentUrl, false);
    }
    
    public void testAsyncOutboundReplication() throws Exception
    {
        ThreadPoolExecutor tpe = new ThreadPoolExecutor(1, 1, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
        
        replicatingStore.setOutbound(true);
        replicatingStore.setOutboundThreadPoolExecutor(tpe);
        
        // write some content
        ContentWriter writer = getWriter();
        writer.putContent(SOME_CONTENT);
        String contentUrl = writer.getContentUrl();
        
        // wait for a second
        synchronized(this)
        {
            this.wait(1000L);
        }
        
        checkForReplication(false, true, contentUrl, SOME_CONTENT);
        
        // check for outbound deletes
        replicatingStore.delete(contentUrl);
        checkForUrl(contentUrl, false);
    }
    
    public void testInboundReplication() throws Exception
    {
        replicatingStore.setInbound(false);
        
        // pick a secondary store and write some content to it
        ContentStore secondaryStore = secondaryStores.get(2);
        ContentWriter writer = secondaryStore.getWriter(null, null);
        writer.putContent(SOME_CONTENT);
        String contentUrl = writer.getContentUrl();
        
        // get a reader from the replicating store
        ContentReader reader = replicatingStore.getReader(contentUrl);
        assertTrue("Reader must have been found in secondary store", reader.exists());
        
        // set inbound replication on and repeat
        replicatingStore.setInbound(true);
        reader = replicatingStore.getReader(contentUrl);
        
        // this time, it must have been replicated to the primary store
        checkForReplication(true, false, contentUrl, SOME_CONTENT);
    }
}
