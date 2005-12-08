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
package org.alfresco.repo.action.executer;

import java.util.List;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.CopyService;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NoTransformerException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Transfor action executer
 * 
 * @author Roy Wetherall
 */
public class TransformActionExecuter extends ActionExecuterAbstractBase 
{
    /**
     * The logger
     */
	private static Log logger = LogFactory.getLog(TransformActionExecuter.class); 
    
    /**
     * Action constants
     */
	public static final String NAME = "transform";
	public static final String PARAM_MIME_TYPE = "mime-type";
	public static final String PARAM_DESTINATION_FOLDER = "destination-folder";
    public static final String PARAM_ASSOC_TYPE_QNAME = "assoc-type";
    public static final String PARAM_ASSOC_QNAME = "assoc-name";
	
	private DictionaryService dictionaryService;
	private NodeService nodeService;
	private ContentService contentService;
	private CopyService copyService;
    private MimetypeService mimetypeService;
    
    /**
     * Set the mime type service
     * 
     * @param mimetypeService  the mime type service
     */
    public void setMimetypeService(MimetypeService mimetypeService) 
    {
        this.mimetypeService = mimetypeService;
    }
    
    /**
     * Set the node service
     * 
     * @param nodeService  set the node service
     */
    public void setNodeService(NodeService nodeService) 
	{
		this.nodeService = nodeService;
	}
	
    /**
     * Set the dictionary service
     * 
     * @param dictionaryService  the dictionary service
     */
	public void setDictionaryService(DictionaryService dictionaryService) 
	{
		this.dictionaryService = dictionaryService;
	}
	
	/**
	 * Set the content service
	 * 
	 * @param contentService  the content service
	 */
	public void setContentService(ContentService contentService) 
	{
		this.contentService = contentService;
	}
	
	/**
	 * Set the copy service
	 * 
	 * @param copyService  the copy service
	 */
	public void setCopyService(CopyService copyService) 
	{
		this.copyService = copyService;
	}
	
	/**
	 * Add parameter definitions
	 */
	@Override
	protected void addParameterDefintions(List<ParameterDefinition> paramList) 
	{
		paramList.add(new ParameterDefinitionImpl(PARAM_MIME_TYPE, DataTypeDefinition.TEXT, true, getParamDisplayLabel(PARAM_MIME_TYPE)));
		paramList.add(new ParameterDefinitionImpl(PARAM_DESTINATION_FOLDER, DataTypeDefinition.NODE_REF, true, getParamDisplayLabel(PARAM_DESTINATION_FOLDER)));
		paramList.add(new ParameterDefinitionImpl(PARAM_ASSOC_TYPE_QNAME, DataTypeDefinition.QNAME, true, getParamDisplayLabel(PARAM_ASSOC_TYPE_QNAME)));
		paramList.add(new ParameterDefinitionImpl(PARAM_ASSOC_QNAME, DataTypeDefinition.QNAME, true, getParamDisplayLabel(PARAM_ASSOC_QNAME)));
	}

	/**
	 * @see org.alfresco.repo.action.executer.ActionExecuterAbstractBase#executeImpl(org.alfresco.service.cmr.repository.NodeRef, org.alfresco.service.cmr.repository.NodeRef)
	 */
	@Override
	protected void executeImpl(
			Action ruleAction,
			NodeRef actionedUponNodeRef) 
	{
		if (this.nodeService.exists(actionedUponNodeRef) == false)
		{
            // node doesn't exist - can't do anything
            return;
        }
		// First check that the node is a sub-type of content
		QName typeQName = this.nodeService.getType(actionedUponNodeRef);
		if (this.dictionaryService.isSubClass(typeQName, ContentModel.TYPE_CONTENT) == false)
		{
            // it is not content, so can't transform
            return;
        }
		// Get the mime type
		String mimeType = (String)ruleAction.getParameterValue(PARAM_MIME_TYPE);
		
		// Get the details of the copy destination
		NodeRef destinationParent = (NodeRef)ruleAction.getParameterValue(PARAM_DESTINATION_FOLDER);
        QName destinationAssocTypeQName = (QName)ruleAction.getParameterValue(PARAM_ASSOC_TYPE_QNAME);
        QName destinationAssocQName = (QName)ruleAction.getParameterValue(PARAM_ASSOC_QNAME);
        
		// Copy the content node
        NodeRef copyNodeRef = this.copyService.copy(
                actionedUponNodeRef, 
                destinationParent,
                destinationAssocTypeQName,
                destinationAssocQName,
                false);
		
        
        // Get the content reader
        ContentReader contentReader = this.contentService.getReader(actionedUponNodeRef, ContentModel.PROP_CONTENT);
        if (contentReader == null)
        {
            // for some reason, this action is premature
            throw new AlfrescoRuntimeException(
                    "Attempting to execute content transformation rule " +
                    "but content has not finished writing, i.e. no URL is available.");
        }
        String originalMimetype = contentReader.getMimetype();

        // get the writer and set it up
        ContentWriter contentWriter = this.contentService.getWriter(copyNodeRef, ContentModel.PROP_CONTENT, true);
        contentWriter.setMimetype(mimeType);                        // new mimetype
        contentWriter.setEncoding(contentReader.getEncoding());     // original encoding

        // Adjust the name of the copy
        String originalName = (String)nodeService.getProperty(actionedUponNodeRef, ContentModel.PROP_NAME);
        String newName = transformName(originalName, originalMimetype, mimeType);
        nodeService.setProperty(copyNodeRef, ContentModel.PROP_NAME, newName);
        String originalTitle = (String)nodeService.getProperty(actionedUponNodeRef, ContentModel.PROP_TITLE);
        if (originalTitle != null && originalTitle.length() > 0)
        {
            String newTitle = transformName(originalTitle, originalMimetype, mimeType);
            nodeService.setProperty(copyNodeRef, ContentModel.PROP_TITLE, newTitle);
        }
		
		// Try and transform the content
        try
        {
        	doTransform(ruleAction, contentReader, contentWriter);
        }
        catch(NoTransformerException e)
        {
        	if (logger.isDebugEnabled())
            {
                logger.debug("No transformer found to execute rule: \n" +
                        "   reader: " + contentReader + "\n" +
                        "   writer: " + contentWriter + "\n" +
                        "   action: " + this);
            }
            // TODO: Revisit this for alternative solutions
            nodeService.deleteNode(copyNodeRef);
        }        
	}	
	
	protected void doTransform(Action ruleAction, ContentReader contentReader, ContentWriter contentWriter)	
	{
		this.contentService.transform(contentReader, contentWriter);
	}
	
    /**
     * Transform name from original extension to new extension
     * 
     * @param original
     * @param originalMimetype
     * @param newMimetype
     * @return
     */
    private String transformName(String original, String originalMimetype, String newMimetype)
    {
        // get the current extension
        int dotIndex = original.lastIndexOf('.');
        StringBuilder sb = new StringBuilder(original.length());
        if (dotIndex > -1)
        {
            // we found it
            sb.append(original.substring(0, dotIndex));
        }
        else
        {
            // no extension
            sb.append(original);
        }
        // add the new extension
        String newExtension = mimetypeService.getExtension(newMimetype);
        sb.append('.').append(newExtension);
        // done
        return sb.toString();
    }
    
}
