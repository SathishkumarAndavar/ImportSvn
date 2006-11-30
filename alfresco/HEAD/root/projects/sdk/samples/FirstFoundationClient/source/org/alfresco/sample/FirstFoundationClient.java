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
package org.alfresco.sample;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.transaction.TransactionUtil;
import org.alfresco.repo.transaction.TransactionUtil.TransactionWork;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.ApplicationContextHelper;
import org.springframework.context.ApplicationContext;


/**
 * Simple client example demonstrating the use of the Alfresco Foundation Java APIs.
 * 
 * The client creates a content node in the "Company Home" folder.  The content
 * may be viewed and operated on within the Alfresco Web Client.  Note: the web client
 * will need to be re-started after executing this sample to see the changes in
 * effect.
 *
 * This client demonstrates the "Embedded Repository" deployment option as described
 * in the Alfresco Respotiory Architecture docucment - 
 * http://wiki.alfresco.com/wiki/Alfresco_Repository_Architecture
 */
public class FirstFoundationClient 
{

    public static void main(String[] args)
    {
        // initialise app content 
        ApplicationContext ctx = ApplicationContextHelper.getApplicationContext();
        // get registry of services
        final ServiceRegistry serviceRegistry = (ServiceRegistry) ctx.getBean(ServiceRegistry.SERVICE_REGISTRY);
        
        // use TransactionWork to wrap service calls in a user transaction
        TransactionService transactionService = serviceRegistry.getTransactionService();
        TransactionWork<Object> exampleWork = new TransactionWork<Object>()
        {
            public Object doWork() throws Exception
            {
                doExample(serviceRegistry);
                return null;
            }
        };
        TransactionUtil.executeInUserTransaction(transactionService, exampleWork);
        System.exit(0);
    }

    
    public static void doExample(ServiceRegistry serviceRegistry) throws Exception
    {
    	//
        // authenticate
    	//
        AuthenticationService authenticationService = serviceRegistry.getAuthenticationService();
        authenticationService.authenticate("admin", "admin".toCharArray());
        
        //
        // locate the company home node
        //
        SearchService searchService = serviceRegistry.getSearchService();
        StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
        ResultSet resultSet = searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, "PATH:\"/app:company_home\"");
        NodeRef companyHome = resultSet.getNodeRef(0);

        //
        // create new content node within company home
        //
        
        // assign name
        String name = "Foundation API sample (" + System.currentTimeMillis() + ")";
        Map<QName, Serializable> contentProps = new HashMap<QName, Serializable>();
        contentProps.put(ContentModel.PROP_NAME, name);

        // create content node
        NodeService nodeService = serviceRegistry.getNodeService();
        ChildAssociationRef association = nodeService.createNode(companyHome, 
        		ContentModel.ASSOC_CONTAINS, 
        		QName.createQName(NamespaceService.CONTENT_MODEL_PREFIX, name),
                ContentModel.TYPE_CONTENT,
                contentProps);
        NodeRef content = association.getChildRef();
        
        // add titled aspect (for Web Client display)
        Map<QName, Serializable> titledProps = new HashMap<QName, Serializable>();
        titledProps.put(ContentModel.PROP_TITLE, name);
        titledProps.put(ContentModel.PROP_DESCRIPTION, name);
        nodeService.addAspect(content, ContentModel.ASPECT_TITLED, titledProps);
        
        //
        // write some content to new node
        //

        ContentService contentService = serviceRegistry.getContentService();
        ContentWriter writer = contentService.getWriter(content, ContentModel.PROP_CONTENT, true);
        writer.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
        writer.setEncoding("UTF-8");
        String text = "The quick brown fox jumps over the lazy dog";
        writer.putContent(text);
    }
}
