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
package org.alfresco.repo.dictionary;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.TransactionListener;
import org.alfresco.service.cmr.dictionary.ModelDefinition;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.GUID;

/**
 * Dictionary model type behaviour.
 * 
 * @author Roy Wetherall
 */
public class DictionaryModelType
{
    /** Key to the pending models */
    private static final String KEY_PENDING_MODELS = "dictionaryModelType.pendingModels";

    /** The dictionary DAO */
    private DictionaryDAO dictionaryDAO;
    
    /** The namespace DAO */
    private NamespaceDAO namespaceDAO;
    
    /** The node service */
    private NodeService nodeService;
    
    /** The content service */
    private ContentService contentService;
    
    /** The policy component */
    private PolicyComponent policyComponent;
    
    /** Transaction listener */
    private DictionaryModelTypeTransactionListener transactionListener;
        
    /**
     * Set the dictionary DAO
     * 
     * @param dictionaryDAO     the dictionary DAO
     */
    public void setDictionaryDAO(DictionaryDAO dictionaryDAO)
    {
        this.dictionaryDAO = dictionaryDAO;
    }
    
    /**
     * Set the namespace DOA
     * 
     * @param namespaceDAO      the namespace DAO
     */
    public void setNamespaceDAO(NamespaceDAO namespaceDAO)
    {
        this.namespaceDAO = namespaceDAO;
    }
    
    /**
     * Set the node service
     * 
     * @param nodeService       the node service
     */
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }
    
    /**
     * Set the content service
     * 
     * @param contentService    the content service
     */
    public void setContentService(ContentService contentService)
    {
        this.contentService = contentService;
    }
    
    /**
     * Set the policy component
     * 
     * @param policyComponent   the policy component
     */
    public void setPolicyComponent(PolicyComponent policyComponent)
    {
        this.policyComponent = policyComponent;
    }
    
    /**
     * The initialise method     
     */
    public void init()
    {
        // Register interest in the onContentUpdate policy for the dictionary model type
        policyComponent.bindClassBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "onContentUpdate"), 
                ContentModel.TYPE_DICTIONARY_MODEL, 
                new JavaBehaviour(this, "onContentUpdate"));
        
        // Create the transaction listener
        this.transactionListener = new DictionaryModelTypeTransactionListener(this.nodeService, this.contentService);
    }
    
    /**
     * On content update behaviour implementation
     * 
     * @param nodeRef   the node reference whose content has been updated
     */
    @SuppressWarnings("unchecked")
    public void onContentUpdate(NodeRef nodeRef)
    {
        Set<NodeRef> pendingModelUpdates = (Set<NodeRef>)AlfrescoTransactionSupport.getResource(KEY_PENDING_MODELS);
        if (pendingModelUpdates == null)
        {
            pendingModelUpdates = new HashSet<NodeRef>();
            AlfrescoTransactionSupport.bindResource(KEY_PENDING_MODELS, pendingModelUpdates);
        }
        pendingModelUpdates.add(nodeRef);
        
        AlfrescoTransactionSupport.bindListener(this.transactionListener);
    }
    
    // TODO need to listen for a change in the modelActive attribute and update appropriatly
    
    // TODO need to listen for node deletion and act accordingly
    
    /**
     * Dictionary model type transaction listener class.
     */
    public class DictionaryModelTypeTransactionListener implements TransactionListener
    {
        /**
         * Id used in equals and hash
         */
        private String id = GUID.generate();
        
        private NodeService nodeService;
        private ContentService contentService;
        
        public DictionaryModelTypeTransactionListener(NodeService nodeService, ContentService contentService)
        {
            this.nodeService = nodeService;
            this.contentService = contentService;
        }
        
        /**
         * @see org.alfresco.repo.transaction.TransactionListener#flush()
         */
        public void flush()
        {
        }
        
        /**
         * @see org.alfresco.repo.transaction.TransactionListener#beforeCommit(boolean)
         */
        @SuppressWarnings("unchecked")
        public void beforeCommit(boolean readOnly)
        { 
            Set<NodeRef> pendingModels = (Set<NodeRef>)AlfrescoTransactionSupport.getResource(KEY_PENDING_MODELS);
            
            if (pendingModels != null)
            {
                for (NodeRef nodeRef : pendingModels)
                {
                    // Find out whether the model is active (by default it is)
                    boolean isActive = true;
                    Boolean value = (Boolean)nodeService.getProperty(nodeRef, ContentModel.PROP_MODEL_ACTIVE);
                    if (value != null)
                    {
                        isActive = value.booleanValue();
                    }
                    
                    // Ignore if the node is a working copy or if its inactive
                    if (nodeService.hasAspect(nodeRef, ContentModel.ASPECT_WORKING_COPY) == false &&
                        isActive == true)
                    {
                        // 1. Compile the model and update the details on the node            
                        // 2. Re-put the model
                        
                        ContentReader contentReader = this.contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
                        if (contentReader != null)
                        {
                            // Create a model from the current content
                            M2Model m2Model = M2Model.createModel(contentReader.getContentInputStream());                
                            // TODO what do we do if we don't have a model??
                            
                            // Try and compile the model
                            ModelDefinition modelDefintion = m2Model.compile(dictionaryDAO, namespaceDAO).getModelDefinition();
                            // TODO what do we do if the model does not compile
                            
                            // Update the meta data for the model
                            Map<QName, Serializable> props = this.nodeService.getProperties(nodeRef);
                            props.put(ContentModel.PROP_MODEL_NAME, modelDefintion.getName());
                            props.put(ContentModel.PROP_MODEL_DESCRIPTION, modelDefintion.getDescription());
                            props.put(ContentModel.PROP_MODEL_AUTHOR, modelDefintion.getAuthor());
                            props.put(ContentModel.PROP_MODEL_PUBLISHED_DATE, modelDefintion.getPublishedDate());
                            props.put(ContentModel.PROP_MODEL_VERSION, modelDefintion.getVersion());
                            this.nodeService.setProperties(nodeRef, props);
                            
                            // TODO how do we get the dependancies for this model ??
                            
                            // Put the model
                            dictionaryDAO.putModel(m2Model);
                        }
                    }
                }
            }
        }

        /**
         * @see org.alfresco.repo.transaction.TransactionListener#beforeCompletion()
         */
        public void beforeCompletion()
        {
        }

        /**
         * @see org.alfresco.repo.transaction.TransactionListener#afterCommit()
         */
        public void afterCommit()
        {
        }
        
        /**
         * @see org.alfresco.repo.transaction.TransactionListener#afterRollback()
         */
        public void afterRollback()
        {
        }
        
        /**
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
            {
                return true;
            }
            if (obj instanceof DictionaryModelTypeTransactionListener)
            {
                DictionaryModelTypeTransactionListener that = (DictionaryModelTypeTransactionListener) obj;
                return (this.id.equals(that.id));
            }
            else
            {
                return false;
            }
        }
    }
}
