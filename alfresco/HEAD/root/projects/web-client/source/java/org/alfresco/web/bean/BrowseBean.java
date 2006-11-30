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
package org.alfresco.web.bean;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.transaction.UserTransaction;

import org.alfresco.config.Config;
import org.alfresco.config.ConfigElement;
import org.alfresco.config.ConfigService;
import org.alfresco.model.ApplicationModel;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.search.LimitBy;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.app.Application;
import org.alfresco.web.app.context.IContextListener;
import org.alfresco.web.app.context.UIContextService;
import org.alfresco.web.app.servlet.DownloadContentServlet;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.repository.MapNode;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.NodePropertyResolver;
import org.alfresco.web.bean.repository.QNameNodeMap;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.bean.spaces.CreateSpaceWizard;
import org.alfresco.web.config.ViewsConfigElement;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.Utils.URLMode;
import org.alfresco.web.ui.common.component.IBreadcrumbHandler;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.common.component.UIBreadcrumb;
import org.alfresco.web.ui.common.component.UIModeList;
import org.alfresco.web.ui.common.component.UIStatusMessage;
import org.alfresco.web.ui.common.component.UIPanel.ExpandedEvent;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.alfresco.web.ui.repo.component.IRepoBreadcrumbHandler;
import org.alfresco.web.ui.repo.component.UINodeDescendants;
import org.alfresco.web.ui.repo.component.UINodePath;
import org.alfresco.web.ui.repo.component.UISimpleSearch;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

/**
 * Bean providing properties and behaviour for the main folder/document browse screen and
 * search results screens.
 * 
 * @author Kevin Roast
 */
public class BrowseBean implements IContextListener
{
   // ------------------------------------------------------------------------------
   // Construction 

   /**
    * Default Constructor
    */
   public BrowseBean()
   {
      UIContextService.getInstance(FacesContext.getCurrentInstance()).registerBean(this);
      
      initFromClientConfig();
   }
   
   
   // ------------------------------------------------------------------------------
   // Bean property getters and setters 
   
   /**
    * @param nodeService The NodeService to set.
    */
   public void setNodeService(NodeService nodeService)
   {
      this.nodeService = nodeService;
   }

   /**
    * @param searchService The Searcher to set.
    */
   public void setSearchService(SearchService searchService)
   {
      this.searchService = searchService;
   }
   
   /**
    * @param lockService The Lock Service to set.
    */
   public void setLockService(LockService lockService)
   {
      this.lockService = lockService;
   }
   
   /**
    * @param navigator The NavigationBean to set.
    */
   public void setNavigator(NavigationBean navigator)
   {
      this.navigator = navigator;
   }
   
   /**
    * @param dictionaryService The DictionaryService to set.
    */
   public void setDictionaryService(DictionaryService dictionaryService)
   {
      this.dictionaryService = dictionaryService;
   }
   
   /**
    * @param fileFolderService The FileFolderService to set.
    */
   public void setFileFolderService(FileFolderService fileFolderService)
   {
      this.fileFolderService = fileFolderService;
   }
   
   /**
    * @return Returns the browse View mode. See UIRichList
    */
   public String getBrowseViewMode()
   {
      return this.browseViewMode;
   }
   
   /**
    * @param browseViewMode      The browse View mode to set. See UIRichList.
    */
   public void setBrowseViewMode(String browseViewMode)
   {
      this.browseViewMode = browseViewMode;
   }
   
   /**
    * @return Returns true if dashboard view is available for the current node.
    */
   public boolean isDashboardView()
   {
      return this.dashboardView;
   }

   /**
    * @param dashboardView The dashboard view mode to set.
    */
   public void setDashboardView(boolean dashboardView)
   {
      this.dashboardView = dashboardView;
      if (dashboardView == true)
      {
         FacesContext fc = FacesContext.getCurrentInstance();
         fc.getApplication().getNavigationHandler().handleNavigation(fc, null, "dashboard");
      }
   }

   public int getPageSizeContent()
   {
      return this.pageSizeContent;
   }

   public void setPageSizeContent(int pageSizeContent)
   {
      this.pageSizeContent = pageSizeContent;
      this.pageSizeContentStr = Integer.toString(pageSizeContent);
   }

   public int getPageSizeSpaces()
   {
      return this.pageSizeSpaces;
   }

   public void setPageSizeSpaces(int pageSizeSpaces)
   {
      this.pageSizeSpaces = pageSizeSpaces;
      this.pageSizeSpacesStr = Integer.toString(pageSizeSpaces);
   }
   
   public String getPageSizeContentStr()
   {
      return this.pageSizeContentStr;
   }

   public void setPageSizeContentStr(String pageSizeContentStr)
   {
      this.pageSizeContentStr = pageSizeContentStr;
   }

   public String getPageSizeSpacesStr()
   {
      return this.pageSizeSpacesStr;
   }

   public void setPageSizeSpacesStr(String pageSizeSpacesStr)
   {
      this.pageSizeSpacesStr = pageSizeSpacesStr;
   }

   /**
    * @return Returns the minimum length of a valid search string.
    */
   public int getMinimumSearchLength()
   {
      return Application.getClientConfig(FacesContext.getCurrentInstance()).
            getSearchMinimum();
   }
   
   /**
    * @return Returns the panels expanded state map.
    */
   public Map<String, Boolean> getPanels()
   {
      return this.panels;
   }

   /**
    * @param panels The panels expanded state map.
    */
   public void setPanels(Map<String, Boolean> panels)
   {
      this.panels = panels;
   }
   
   /**
    * @return Returns the Space Node being used for the current browse screen action.
    */
   public Node getActionSpace()
   {
      return this.actionSpace;
   }
   
   /**
    * @param actionSpace     Set the Space Node to be used for the current browse screen action.
    */
   public void setActionSpace(Node actionSpace)
   {
      if (actionSpace != null)
      {
         for (NodeEventListener listener : getNodeEventListeners())
         {
            listener.created(actionSpace, actionSpace.getType());
         }
      }
      this.actionSpace = actionSpace;
   }
   
   /**
    * @return The document node being used for the current operation
    */
   public Node getDocument()
   {
      return this.document;
   }

   /**
    * @param document The document node to be used for the current operation
    */
   public void setDocument(Node document)
   {
      if (document != null)
      {
         for (NodeEventListener listener : getNodeEventListeners())
         {
            listener.created(document, document.getType());
         }
      }
      this.document = document;
   }

   /**
    * @param contentRichList The contentRichList to set.
    */
   public void setContentRichList(UIRichList browseRichList)
   {
      this.contentRichList = browseRichList;
      if (this.contentRichList != null)
      {
         this.contentRichList.setInitialSortColumn(
               this.viewsConfig.getDefaultSortColumn(PAGE_NAME_BROWSE));
         this.contentRichList.setInitialSortDescending(
               this.viewsConfig.hasDescendingSort(PAGE_NAME_BROWSE));
      }
      // special case to handle an External Access URL
      // these URLs restart the JSF lifecycle but an old UIRichList is restored from
      // the component tree - which needs clearing "late" in the lifecycle process
      if (externalForceRefresh)
      {
         this.contentRichList.setValue(null);
         externalForceRefresh = false;
      }
   }
   
   /**
    * @return Returns the contentRichList.
    */
   public UIRichList getContentRichList()
   {
      return this.contentRichList;
   }
   
   /**
    * @param spacesRichList The spacesRichList to set.
    */
   public void setSpacesRichList(UIRichList detailsRichList)
   {
      this.spacesRichList = detailsRichList;
      if (this.spacesRichList != null)
      {
         // set the initial sort column and direction
         this.spacesRichList.setInitialSortColumn(
               this.viewsConfig.getDefaultSortColumn(PAGE_NAME_BROWSE));
         this.spacesRichList.setInitialSortDescending(
               this.viewsConfig.hasDescendingSort(PAGE_NAME_BROWSE));
      }
      if (externalForceRefresh)
      {
         this.spacesRichList.setValue(null);
      }
   }
   
   /**
    * @return Returns the spacesRichList.
    */
   public UIRichList getSpacesRichList()
   {
      return this.spacesRichList;
   }
   
   /**
    * @return Returns the statusMessage component.
    */
   public UIStatusMessage getStatusMessage()
   {
      return this.statusMessage;
   }

   /**
    * @param statusMessage The statusMessage component to set.
    */
   public void setStatusMessage(UIStatusMessage statusMessage)
   {
      this.statusMessage = statusMessage;
   }
   
   /**
    * @return Returns the deleteMessage.
    */
   public String getDeleteMessage()
   {
      return this.deleteMessage;
   }

   /**
    * @param deleteMessage The deleteMessage to set.
    */
   public void setDeleteMessage(String deleteMessage)
   {
      this.deleteMessage = deleteMessage;
   }
   
   /**
    * Page accessed bean method to get the container nodes currently being browsed
    * 
    * @return List of container Node objects for the current browse location
    */
   public List<Node> getNodes()
   {
      // the references to container nodes and content nodes are transient for one use only
      // we do this so we only query/search once - as we cannot distinguish between node types
      // until after the query. The logic is a bit confusing but otherwise we would need to
      // perform the same query or search twice for every screen refresh.
      if (this.containerNodes == null)
      {
         if (this.navigator.getSearchContext() == null)
         {
            queryBrowseNodes(this.navigator.getCurrentNodeId());
         }
         else
         {
            searchBrowseNodes(this.navigator.getSearchContext());
         }
      }
      List<Node> result = this.containerNodes;
      
      // we clear the member variable during invalidateComponents()
      
      return result;
   }
   
   /**
    * Page accessed bean method to get the content nodes currently being browsed
    * 
    * @return List of content Node objects for the current browse location
    */
   public List<Node> getContent()
   {
      // see comment in getNodes() above for reasoning here
      if (this.contentNodes == null)
      {
         if (this.navigator.getSearchContext() == null)
         {
            queryBrowseNodes(this.navigator.getCurrentNodeId());
         }
         else
         {
            searchBrowseNodes(this.navigator.getSearchContext());
         }
      }
      List<Node> result = this.contentNodes;
      
      // we clear the member variable during invalidateComponents()
      
      return result;
   }
   
   /**
    * Setup the common properties required at data-binding time.
    * <p>
    * These are properties used by components on the page when iterating over the nodes.
    * The properties are available as the Node is a Map so they can be accessed directly
    * by name. Information such as download URL, size and filetype are provided etc.
    * <p>
    * We use a set of anonymous inner classes to provide the implemention for the property
    * getters. The interfaces are only called when the properties are first requested. 
    * 
    * @param node       Node to add the properties too
    */
   public void setupCommonBindingProperties(Node node)
   {
      // special properties to be used by the value binding components on the page
      node.addPropertyResolver("url", this.resolverUrl);
      node.addPropertyResolver("webdavUrl", this.resolverWebdavUrl);
      node.addPropertyResolver("cifsPath", this.resolverCifsPath);
      node.addPropertyResolver("fileType16", this.resolverFileType16);
      node.addPropertyResolver("fileType32", this.resolverFileType32);
      node.addPropertyResolver("size", this.resolverSize);
   }
   
   
   // ------------------------------------------------------------------------------
   // IContextListener implementation 
   
   /**
    * @see org.alfresco.web.app.context.IContextListener#contextUpdated()
    */
   public void contextUpdated()
   {
      invalidateComponents();
   }
   
   
   // ------------------------------------------------------------------------------
   // NodeEventListener listeners
   
   /**
    * Add a listener to those called by the BrowseBean when nodes are created
    */
   public void addNodeEventListener(NodeEventListener listener)
   {
      getNodeEventListeners().add(listener);
   }
   
   /**
    * Remove a listener from the list of those called by BrowseBean
    */
   public void removeNodeEventListener(NodeEventListener listener)
   {
      getNodeEventListeners().remove(listener);
   }
   
   
   // ------------------------------------------------------------------------------
   // Navigation action event handlers 
   
   /**
    * Change the current view mode based on user selection
    * 
    * @param event      ActionEvent
    */
   public void viewModeChanged(ActionEvent event)
   {
      UIModeList viewList = (UIModeList)event.getComponent();
      
      // get the view mode ID
      String viewMode = viewList.getValue().toString();
      
      if (VIEWMODE_DASHBOARD.equals(viewMode) == false)
      { 
         // set the page size based on the style of display
         int pageSize = this.viewsConfig.getDefaultPageSize(PAGE_NAME_BROWSE, viewMode);
         setPageSizeContent(pageSize);
         setPageSizeSpaces(pageSize);
         
         if (logger.isDebugEnabled())
            logger.debug("Browse view page size set to: " + pageSize);
         
         setDashboardView(false);
         
         // push the view mode into the lists
         setBrowseViewMode(viewMode);
         
         // setup dispatch context for custom views
         this.navigator.setupDispatchContext(this.navigator.getCurrentNode());
         
         navigateBrowseScreen();
      }
      else
      {
         // special case for Dashboard view
         setDashboardView(true);
      }
   }
   
   
   // ------------------------------------------------------------------------------
   // Helper methods
   
   /**
    * Query a list of nodes for the specified parent node Id
    * 
    * @param parentNodeId     Id of the parent node or null for the root node
    */
   private void queryBrowseNodes(String parentNodeId)
   {
      long startTime = 0;
      if (logger.isDebugEnabled())
         startTime = System.currentTimeMillis();
      
      UserTransaction tx = null;
      try
      {
         FacesContext context = FacesContext.getCurrentInstance();
         tx = Repository.getUserTransaction(context, true);
         tx.begin();
         
         NodeRef parentRef;
         if (parentNodeId == null)
         {
            // no specific parent node specified - use the root node
            parentRef = this.nodeService.getRootNode(Repository.getStoreRef());
         }
         else
         {
            // build a NodeRef for the specified Id and our store
            parentRef = new NodeRef(Repository.getStoreRef(), parentNodeId);
         }

         List<ChildAssociationRef> childRefs = this.nodeService.getChildAssocs(parentRef, 
               ContentModel.ASSOC_CONTAINS, RegexQNamePattern.MATCH_ALL);
         this.containerNodes = new ArrayList<Node>(childRefs.size());
         this.contentNodes = new ArrayList<Node>(childRefs.size());
         for (ChildAssociationRef ref: childRefs)
         {
            // create our Node representation from the NodeRef
            NodeRef nodeRef = ref.getChildRef();
            
            if (this.nodeService.exists(nodeRef))
            {
               // find it's type so we can see if it's a node we are interested in
               QName type = this.nodeService.getType(nodeRef);
               
               // make sure the type is defined in the data dictionary
               TypeDefinition typeDef = this.dictionaryService.getType(type);
               
               if (typeDef != null)
               {
                  MapNode node = null;
                  
                  // look for Space folder node
                  if (this.dictionaryService.isSubClass(type, ContentModel.TYPE_FOLDER) == true && 
                  	 this.dictionaryService.isSubClass(type, ContentModel.TYPE_SYSTEM_FOLDER) == false)
                  {
                     // create our Node representation
                     node = new MapNode(nodeRef, this.nodeService, true);
                     node.addPropertyResolver("icon", this.resolverSpaceIcon);
                     node.addPropertyResolver("smallIcon", this.resolverSmallIcon);
                     
                     this.containerNodes.add(node);
                  }
                  // look for File content node
                  else if (this.dictionaryService.isSubClass(type, ContentModel.TYPE_CONTENT))
                  {
                     // create our Node representation
                     node = new MapNode(nodeRef, this.nodeService, true);
                     setupCommonBindingProperties(node);
                     
                     this.contentNodes.add(node);
                  }
                  // look for File Link object node
                  else if (ApplicationModel.TYPE_FILELINK.equals(type))
                  {
                     // create our File Link Node representation
                     node = new MapNode(nodeRef, this.nodeService, true);
                     node.addPropertyResolver("url", this.resolverLinkUrl);
                     node.addPropertyResolver("webdavUrl", this.resolverLinkWebdavUrl);
                     node.addPropertyResolver("cifsPath", this.resolverLinkCifsPath);
                     node.addPropertyResolver("fileType16", this.resolverFileType16);
                     node.addPropertyResolver("fileType32", this.resolverFileType32);
                     node.addPropertyResolver("size", this.resolverSize);
                     
                     this.contentNodes.add(node);
                  }
                  else if (ApplicationModel.TYPE_FOLDERLINK.equals(type))
                  {
                     // create our Folder Link Node representation
                     node = new MapNode(nodeRef, this.nodeService, true);
                     node.addPropertyResolver("icon", this.resolverSpaceIcon);
                     node.addPropertyResolver("smallIcon", this.resolverSmallIcon);
                     
                     this.containerNodes.add(node);
                  }
                  
                  // inform any listeners that a Node wrapper has been created
                  if (node != null)
                  {
                     for (NodeEventListener listener : getNodeEventListeners())
                     {
                        listener.created(node, type);
                     }
                  }
               }
               else
               {
                  if (logger.isEnabledFor(Priority.WARN))
                     logger.warn("Found invalid object in database: id = " + nodeRef + ", type = " + type);
               }
            }
         }
         
         // commit the transaction
         tx.commit();
      }
      catch (InvalidNodeRefException refErr)
      {
         Utils.addErrorMessage(MessageFormat.format(Application.getMessage(
               FacesContext.getCurrentInstance(), Repository.ERROR_NODEREF), new Object[] {refErr.getNodeRef()}), refErr );
         this.containerNodes = Collections.<Node>emptyList();
         this.contentNodes = Collections.<Node>emptyList();
         try { if (tx != null) {tx.rollback();} } catch (Exception tex) {}
      }
      catch (Throwable err)
      {
         Utils.addErrorMessage(MessageFormat.format(Application.getMessage(
               FacesContext.getCurrentInstance(), Repository.ERROR_GENERIC), err.getMessage()), err);
         this.containerNodes = Collections.<Node>emptyList();
         this.contentNodes = Collections.<Node>emptyList();
         try { if (tx != null) {tx.rollback();} } catch (Exception tex) {}
      }
      
      if (logger.isDebugEnabled())
      {
         long endTime = System.currentTimeMillis();
         logger.debug("Time to query and build map nodes: " + (endTime - startTime) + "ms");
      }
   }
   
   /**
    * Search for a list of nodes using the specific search context
    * 
    * @param searchContext    To use to perform the search
    */
   private void searchBrowseNodes(SearchContext searchContext)
   {
      long startTime = 0;
      if (logger.isDebugEnabled())
         startTime = System.currentTimeMillis();
      
      // get the searcher object to build the query
      String query = searchContext.buildQuery(getMinimumSearchLength());
      if (query == null)
      {
         // failed to build a valid query, the user probably did not enter the
         // minimum text required to construct a valid search
         Utils.addErrorMessage(MessageFormat.format(Application.getMessage(FacesContext.getCurrentInstance(), MSG_SEARCH_MINIMUM),
               new Object[] {getMinimumSearchLength()}));
         this.containerNodes = Collections.<Node>emptyList();
         this.contentNodes = Collections.<Node>emptyList();
         return;
      }
      
      // perform the search against the repo
      UserTransaction tx = null;
      ResultSet results = null;
      try
      {
         tx = Repository.getUserTransaction(FacesContext.getCurrentInstance(), true);
         tx.begin();
         
         // Limit search to the first 100 matches
         SearchParameters sp = new SearchParameters();
         sp.setLanguage(SearchService.LANGUAGE_LUCENE);
         sp.setQuery(query);
         sp.addStore(Repository.getStoreRef());
         
         int searchLimit = Application.getClientConfig(FacesContext.getCurrentInstance()).getSearchMaxResults();
         if(searchLimit > 0)
         {
            sp.setLimitBy(LimitBy.FINAL_SIZE);
            sp.setLimit(searchLimit);
         }
         
         results = this.searchService.query(sp);
         if (logger.isDebugEnabled())
            logger.debug("Search results returned: " + results.length());
         
         // create a list of items from the results
         this.containerNodes = new ArrayList<Node>(results.length());
         this.contentNodes = new ArrayList<Node>(results.length());
         if (results.length() != 0)
         {
            for (ResultSetRow row: results)
            {
               NodeRef nodeRef = row.getNodeRef();
               
               if (this.nodeService.exists(nodeRef))
               {
                  // find it's type so we can see if it's a node we are interested in
                  QName type = this.nodeService.getType(nodeRef);
                  
                  // make sure the type is defined in the data dictionary
                  TypeDefinition typeDef = this.dictionaryService.getType(type);
               
                  if (typeDef != null)
                  {
                     MapNode node = null;
                     
                     // look for Space or File nodes
                     if (this.dictionaryService.isSubClass(type, ContentModel.TYPE_FOLDER) && 
                         this.dictionaryService.isSubClass(type, ContentModel.TYPE_SYSTEM_FOLDER) == false)
                     {
                        // create our Node representation
                        node = new MapNode(nodeRef, this.nodeService, false);
                        
                        node.addPropertyResolver("path", this.resolverPath);
                        node.addPropertyResolver("displayPath", this.resolverDisplayPath);
                        node.addPropertyResolver("icon", this.resolverSpaceIcon);
                        node.addPropertyResolver("smallIcon", this.resolverSmallIcon);
                        
                        this.containerNodes.add(node);
                     }
                     else if (this.dictionaryService.isSubClass(type, ContentModel.TYPE_CONTENT))
                     {
                        // create our Node representation
                        node = new MapNode(nodeRef, this.nodeService, false);
                        
                        setupCommonBindingProperties(node);
                        
                        node.addPropertyResolver("path", this.resolverPath);
                        node.addPropertyResolver("displayPath", this.resolverDisplayPath);
                        
                        this.contentNodes.add(node);
                     }
                     // look for File Link object node
                     else if (ApplicationModel.TYPE_FILELINK.equals(type))
                     {
                        // create our File Link Node representation
                        node = new MapNode(nodeRef, this.nodeService, false);
                        node.addPropertyResolver("url", this.resolverLinkUrl);
                        node.addPropertyResolver("webdavUrl", this.resolverLinkWebdavUrl);
                        node.addPropertyResolver("cifsPath", this.resolverLinkCifsPath);
                        node.addPropertyResolver("fileType16", this.resolverFileType16);
                        node.addPropertyResolver("fileType32", this.resolverFileType32);
                        node.addPropertyResolver("size", this.resolverSize);
                        node.addPropertyResolver("path", this.resolverPath);
                        node.addPropertyResolver("displayPath", this.resolverDisplayPath);
                        
                        this.contentNodes.add(node);
                     }
                     else if (ApplicationModel.TYPE_FOLDERLINK.equals(type))
                     {
                        // create our Folder Link Node representation
                        node = new MapNode(nodeRef, this.nodeService, false);
                        node.addPropertyResolver("icon", this.resolverSpaceIcon);
                        node.addPropertyResolver("smallIcon", this.resolverSmallIcon);
                        node.addPropertyResolver("path", this.resolverPath);
                        node.addPropertyResolver("displayPath", this.resolverDisplayPath);
                        
                        this.containerNodes.add(node);
                     }
                     
                     // inform any listeners that a Node wrapper has been created
                     if (node != null)
                     {
                        for (NodeEventListener listener : getNodeEventListeners())
                        {
                           listener.created(node, type);
                        }
                     }
                  }
                  else
                  {
                     if (logger.isEnabledFor(Priority.WARN))
                        logger.warn("Found invalid object in database: id = " + nodeRef + ", type = " + type);
                  }
               }
               else
               {
                  if (logger.isEnabledFor(Priority.WARN))
                     logger.warn("Missing object returned from search indexes: id = " + nodeRef + " search query: " + query);
               }
            }
         }
         
         // commit the transaction
         tx.commit();
      }
      catch (InvalidNodeRefException refErr)
      {
         Utils.addErrorMessage(MessageFormat.format(Application.getMessage(
               FacesContext.getCurrentInstance(), Repository.ERROR_NODEREF), new Object[] {refErr.getNodeRef()}), refErr );
         this.containerNodes = Collections.<Node>emptyList();
         this.contentNodes = Collections.<Node>emptyList();
         try { if (tx != null) {tx.rollback();} } catch (Exception tex) {}
      }
      catch (Throwable err)
      {
         logger.info("Search failed for: " + query);
         Utils.addErrorMessage(MessageFormat.format(Application.getMessage(
               FacesContext.getCurrentInstance(), Repository.ERROR_SEARCH), new Object[] {err.getMessage()}), err );
         this.containerNodes = Collections.<Node>emptyList();
         this.contentNodes = Collections.<Node>emptyList();
         try { if (tx != null) {tx.rollback();} } catch (Exception tex) {}
      }
      finally
      {
         if (results != null)
         {
            results.close();
         }
      }
      
      if (logger.isDebugEnabled())
      {
         long endTime = System.currentTimeMillis();
         logger.debug("Time to query and build map nodes: " + (endTime - startTime) + "ms");
      }
   }
   
   
   // ------------------------------------------------------------------------------
   // Property Resolvers

   public NodePropertyResolver resolverDownload = new NodePropertyResolver() {
      public Object get(Node node) {
         return DownloadContentServlet.generateDownloadURL(node.getNodeRef(), node.getName());
      }
   };
   
   public NodePropertyResolver resolverUrl = new NodePropertyResolver() {
      public Object get(Node node) {
         return DownloadContentServlet.generateBrowserURL(node.getNodeRef(), node.getName());
      }
   };
   
   public NodePropertyResolver resolverWebdavUrl = new NodePropertyResolver() {
      public Object get(Node node) {
         return Utils.generateURL(FacesContext.getCurrentInstance(), node, URLMode.WEBDAV); 
      }   
   };
   
   public NodePropertyResolver resolverCifsPath = new NodePropertyResolver() {
      public Object get(Node node) {
         return Utils.generateURL(FacesContext.getCurrentInstance(), node, URLMode.CIFS);
      }
   };
   
   public NodePropertyResolver resolverLinkDownload = new NodePropertyResolver() {
      public Object get(Node node) {
         NodeRef destRef = (NodeRef)node.getProperties().get(ContentModel.PROP_LINK_DESTINATION);
         if (nodeService.exists(destRef) == true)
         {
            String destName = Repository.getNameForNode(nodeService, destRef);
            return DownloadContentServlet.generateDownloadURL(node.getNodeRef(), destName);
         }
         else
         {
            // TODO: link object is missing - navigate to a page with appropriate message
            return "#";
         }
      }
   };
   
   public NodePropertyResolver resolverLinkUrl = new NodePropertyResolver() {
      public Object get(Node node) {
         NodeRef destRef = (NodeRef)node.getProperties().get(ContentModel.PROP_LINK_DESTINATION);
         if (nodeService.exists(destRef) == true)
         {
            String destName = Repository.getNameForNode(nodeService, destRef);
            return DownloadContentServlet.generateBrowserURL(destRef, destName);
         }
         else
         {
            // TODO: link object is missing - navigate to a page with appropriate message
            return "#";
         }
      }
   };
   
   public NodePropertyResolver resolverLinkWebdavUrl = new NodePropertyResolver() {
      public Object get(Node node) {
         NodeRef destRef = (NodeRef)node.getProperties().get(ContentModel.PROP_LINK_DESTINATION);
         if (nodeService.exists(destRef) == true)
         {
            return Utils.generateURL(FacesContext.getCurrentInstance(), new Node(destRef), URLMode.WEBDAV);
         }
         else
         {
            // TODO: link object is missing - navigate to a page with appropriate message
            return "#";
         }
      }   
   };
   
   public NodePropertyResolver resolverLinkCifsPath = new NodePropertyResolver() {
      public Object get(Node node) {
         NodeRef destRef = (NodeRef)node.getProperties().get(ContentModel.PROP_LINK_DESTINATION);
         if (nodeService.exists(destRef) == true)
         {
            return Utils.generateURL(FacesContext.getCurrentInstance(), new Node(destRef), URLMode.CIFS);
         }
         else
         {
            // TODO: link object is missing - navigate to a page with appropriate message
            return "#";
         }
      }
   };
   
   public NodePropertyResolver resolverFileType16 = new NodePropertyResolver() {
      public Object get(Node node) {
         return Utils.getFileTypeImage(node.getName(), true);
      }
   };
   
   public NodePropertyResolver resolverFileType32 = new NodePropertyResolver() {
      public Object get(Node node) {
         return Utils.getFileTypeImage(node.getName(), false);
      }
   };
   
   public NodePropertyResolver resolverPath = new NodePropertyResolver() {
      public Object get(Node node) {
         return nodeService.getPath(node.getNodeRef());
      }
   };
   
   public NodePropertyResolver resolverDisplayPath = new NodePropertyResolver() {
      public Object get(Node node) {
         // TODO: replace this with a method that shows the full display name - not QNames?
         return Repository.getDisplayPath( (Path)node.getProperties().get("path") );
      }
   };
   
   public NodePropertyResolver resolverSpaceIcon = new NodePropertyResolver() {
      public Object get(Node node) {
         QNameNodeMap props = (QNameNodeMap)node.getProperties();
         String icon = (String)props.getRaw("app:icon");
         return (icon != null ? icon : CreateSpaceWizard.DEFAULT_SPACE_ICON_NAME);
      }
   };
   
   public NodePropertyResolver resolverSmallIcon = new NodePropertyResolver() {
      public Object get(Node node) {
         QNameNodeMap props = (QNameNodeMap)node.getProperties();
         String icon = (String)props.getRaw("app:icon");
         return (icon != null ? icon + "-16" : SPACE_SMALL_DEFAULT);
      }
   };
   
   public NodePropertyResolver resolverMimetype = new NodePropertyResolver() {
      public Object get(Node node) {
         ContentData content = (ContentData)node.getProperties().get(ContentModel.PROP_CONTENT);
         return (content != null ? content.getMimetype() : null);
      }
   };
   
   public NodePropertyResolver resolverSize = new NodePropertyResolver() {
      public Object get(Node node) {
         ContentData content = (ContentData)node.getProperties().get(ContentModel.PROP_CONTENT);
         return (content != null ? new Long(content.getSize()) : 0L);
      }
   };
   
   
   // ------------------------------------------------------------------------------
   // Navigation action event handlers
   
   /**
    * Action called from the Simple Search component.
    * Sets up the SearchContext object with the values from the simple search menu.
    */
   public void search(ActionEvent event)
   {
      // setup the search text string on the top-level navigation handler
      UISimpleSearch search = (UISimpleSearch)event.getComponent();
      this.navigator.setSearchContext(search.getSearchContext());
      
      navigateBrowseScreen();
   }
   
   /**
    * Action called to Close the search dialog by returning to the last view node Id
    */
   public void closeSearch(ActionEvent event)
   {
      // set the current node Id ready for page refresh
      this.navigator.setCurrentNodeId( this.navigator.getCurrentNodeId() );
   }
   
   /**
    * Update page size based on user selection
    */
   public void updateSpacesPageSize(ActionEvent event)
   {
      try
      {
         int size = Integer.parseInt(this.pageSizeSpacesStr);
         if (size >= 0)
         {
            this.pageSizeSpaces = size;
         }
         else
         {
            // reset to known value if this occurs
            this.pageSizeSpacesStr = Integer.toString(this.pageSizeSpaces);
         }
      }
      catch (NumberFormatException err)
      {
         // reset to known value if this occurs
         this.pageSizeSpacesStr = Integer.toString(this.pageSizeSpaces);
      }
   }
   
   /**
    * Update page size based on user selection
    */
   public void updateContentPageSize(ActionEvent event)
   {
      try
      {
         int size = Integer.parseInt(this.pageSizeContentStr);
         if (size >= 0)
         {
            this.pageSizeContent = size;
         }
         else
         {
            // reset to known value if this occurs
            this.pageSizeContentStr = Integer.toString(this.pageSizeContent);
         }
      }
      catch (NumberFormatException err)
      {
         // reset to known value if this occurs
         this.pageSizeContentStr = Integer.toString(this.pageSizeContent);
      }
   }
   
   /**
    * Action called when a folder space is clicked.
    * Navigate into the space.
    */
   public void clickSpace(ActionEvent event)
   {
      UIActionLink link = (UIActionLink)event.getComponent();
      Map<String, String> params = link.getParameterMap();
      String id = params.get("id");
      if (id != null && id.length() != 0)
      {
         try
         {
            NodeRef ref = new NodeRef(Repository.getStoreRef(), id);
            
            // handle special folder link node case
            if (ApplicationModel.TYPE_FOLDERLINK.equals(this.nodeService.getType(ref)))
            {
               ref = (NodeRef)this.nodeService.getProperty(ref, ContentModel.PROP_LINK_DESTINATION);
            }
            
            clickSpace(ref);
         }
         catch (InvalidNodeRefException refErr)
         {
            Utils.addErrorMessage(MessageFormat.format(Application.getMessage(
               FacesContext.getCurrentInstance(), Repository.ERROR_NODEREF), new Object[] {id}) );
         }
      }
   }
   
   /**
    * Action called when a folder space is clicked.
    *
    * @param nodeRef The node being clicked
    */
   public void clickSpace(NodeRef nodeRef)
   {
      // refresh UI based on node selection
      updateUILocation(nodeRef);
   }
   
   /**
    * Handler called when a path element is clicked - navigate to the appropriate Space
    */
   public void clickSpacePath(ActionEvent event)
   {
      UINodePath.PathElementEvent pathEvent = (UINodePath.PathElementEvent)event;
      NodeRef ref = pathEvent.NodeReference;
      try
      {
         // refresh UI based on node selection
         this.updateUILocation(ref);
      }
      catch (InvalidNodeRefException refErr)
      {
         Utils.addErrorMessage(MessageFormat.format(Application.getMessage(
               FacesContext.getCurrentInstance(), Repository.ERROR_NODEREF), new Object[] {ref.getId()}) );
      }
   }
   
   /**
    * Action called when a folders direct descendant (in the 'list' browse mode) is clicked.
    * Navigate into the the descendant space.
    */
   public void clickDescendantSpace(ActionEvent event)
   {
      UINodeDescendants.NodeSelectedEvent nodeEvent = (UINodeDescendants.NodeSelectedEvent)event;
      NodeRef nodeRef = nodeEvent.NodeReference;
      if (nodeRef == null)
      {
         throw new IllegalStateException("NodeRef returned from UINodeDescendants.NodeSelectedEvent cannot be null!");
      }
      
      if (logger.isDebugEnabled())
         logger.debug("Selected noderef Id: " + nodeRef.getId());
      
      try
      {
         // user can either select a descendant of a node display on the page which means we
         // must add the it's parent and itself to the breadcrumb
         ChildAssociationRef parentAssocRef = nodeService.getPrimaryParent(nodeRef);
         
         if (logger.isDebugEnabled())
         {
            logger.debug("Selected item getPrimaryParent().getChildRef() noderef Id:  " + parentAssocRef.getChildRef().getId());
            logger.debug("Selected item getPrimaryParent().getParentRef() noderef Id: " + parentAssocRef.getParentRef().getId());
            logger.debug("Current value getNavigator().getCurrentNodeId() noderef Id: " + this.navigator.getCurrentNodeId());
         }
         
         if (nodeEvent.IsParent == false)
         {
            // a descendant of the displayed node was selected
            // first refresh based on the parent and add to the breadcrumb
            updateUILocation(parentAssocRef.getParentRef());
            
            // now add our selected node
            updateUILocation(nodeRef);
         }
         else
         {
            // else the parent ellipses i.e. the displayed node was selected
            updateUILocation(nodeRef);
         }
      }
      catch (InvalidNodeRefException refErr)
      {
         Utils.addErrorMessage(MessageFormat.format(Application.getMessage(
               FacesContext.getCurrentInstance(), Repository.ERROR_NODEREF), new Object[] {nodeRef.getId()}) );
      }
   }
   
   /**
    * Action event called by all Browse actions that need to setup a Space context
    * before an action page/wizard is called. The context will be a Node in setActionSpace() which
    * can be retrieved on the action page from BrowseBean.getActionSpace().
    * 
    * @param event   ActionEvent
    */
   public void setupSpaceAction(ActionEvent event)
   {
      UIActionLink link = (UIActionLink)event.getComponent();
      Map<String, String> params = link.getParameterMap();
      String id = params.get("id");
      setupSpaceAction(id, true);
   }
   
   /**
    * Public helper to setup action pages with Space context
    * 
    * @param id     of the Space node to setup context for
    */
   public void setupSpaceAction(String id, boolean invalidate)
   {
      if (id != null && id.length() != 0)
      {
         if (logger.isDebugEnabled())
            logger.debug("Setup for action, setting current space to: " + id);
         
         try
         {
            // create the node ref, then our node representation
            NodeRef ref = new NodeRef(Repository.getStoreRef(), id);
            Node node = new Node(ref);
            
            // resolve icon in-case one has not been set
            node.addPropertyResolver("icon", this.resolverSpaceIcon);
            
            // prepare a node for the action context
            setActionSpace(node);
            
            // setup the dispatch context in case it is required
            this.navigator.setupDispatchContext(node);
         }
         catch (InvalidNodeRefException refErr)
         {
            Utils.addErrorMessage(MessageFormat.format(Application.getMessage(
               FacesContext.getCurrentInstance(), Repository.ERROR_NODEREF), new Object[] {id}) );
         }
      }
      else
      {
         setActionSpace(null);
      }
      
      // clear the UI state in preparation for finishing the next action
      if (invalidate == true)
      {
         // use the context service to notify all registered beans
         UIContextService.getInstance(FacesContext.getCurrentInstance()).notifyBeans();
      }
   }
   
   /**
    * Acrtion event called by Delete Space actions. We setup the action space as normal, then prepare
    * any special case message string to be shown to the user if they are trying to delete specific spaces. 
    */
   public void setupDeleteAction(ActionEvent event)
   {
      String message = null;
      
      setupSpaceAction(event);
      
      Node node = getActionSpace();
      if (node != null)
      {
         NodeRef companyRootRef = new NodeRef(Repository.getStoreRef(), Application.getCompanyRootId());
         if (node.getNodeRef().equals(companyRootRef))
         {
            message = Application.getMessage(FacesContext.getCurrentInstance(), MSG_DELETE_COMPANYROOT);
         }
      }
      
      setDeleteMessage(message);
   }
   
   /**
    * Action event called by all actions that need to setup a Content Document context on the 
    * BrowseBean before an action page/wizard is called. The context will be a Node in
    * setDocument() which can be retrieved on the action page from BrowseBean.getDocument().
    */
   public void setupContentAction(ActionEvent event)
   {
      UIActionLink link = (UIActionLink)event.getComponent();
      Map<String, String> params = link.getParameterMap();
      setupContentAction(params.get("id"), true);
   }
   
   /**
    * Public helper to setup action pages with content context
    * 
    * @param id     of the content node to setup context for
    */
   public void setupContentAction(String id, boolean invalidate)
   {
      if (id != null && id.length() != 0)
      {
         if (logger.isDebugEnabled())
            logger.debug("Setup for action, setting current document to: " + id);
         
         try
         {
            // create the node ref, then our node representation
            NodeRef ref = new NodeRef(Repository.getStoreRef(), id);
            Node node = new Node(ref);
            
            // store the URL to for downloading the content
            if (ApplicationModel.TYPE_FILELINK.equals(node.getType()))
            {
               node.addPropertyResolver("url", this.resolverLinkDownload);
            }
            else
            {
               node.addPropertyResolver("url", this.resolverDownload);
            }
            node.addPropertyResolver("fileType32", this.resolverFileType32);
            node.addPropertyResolver("mimetype", this.resolverMimetype);
            node.addPropertyResolver("size", this.resolverSize);
            
            for (NodeEventListener listener : getNodeEventListeners())
            {
               listener.created(node, node.getType());
            }
            
            // get hold of the DocumentDetailsBean and reset it
            DocumentDetailsBean docDetails = (DocumentDetailsBean)FacesContext.getCurrentInstance().
               getExternalContext().getSessionMap().get("DocumentDetailsBean");
            if (docDetails != null)
            {
               docDetails.reset();
            }
            
            // remember the document
            setDocument(node);
            
            // setup the dispatch context in case it is required
            this.navigator.setupDispatchContext(node);
         }
         catch (InvalidNodeRefException refErr)
         {
            Utils.addErrorMessage(MessageFormat.format(Application.getMessage(
               FacesContext.getCurrentInstance(), Repository.ERROR_NODEREF), new Object[] {id}) );
         }
      }
      else
      {
         setDocument(null);
      }
      
      // clear the UI state in preparation for finishing the next action
      if (invalidate == true)
      {
         // use the context service to notify all registered beans
         UIContextService.getInstance(FacesContext.getCurrentInstance()).notifyBeans();
      }
   }
   
   /**
    * Removes the given node from the breadcrumb i.e. following a delete
    * 
    * @param node The space to remove from the breadcrumb
    */
   public void removeSpaceFromBreadcrumb(Node node)
   {
      List<IBreadcrumbHandler> location = navigator.getLocation();
      IBreadcrumbHandler handler = location.get(location.size() - 1);
      if (handler instanceof BrowseBreadcrumbHandler)
      {
         // see if the current breadcrumb location is our node 
         if ( ((BrowseBreadcrumbHandler)handler).getNodeRef().equals(node.getNodeRef()) == true )
         {
            location.remove(location.size() - 1);
            
            // now work out which node to set the list to refresh against
            if (location.size() != 0)
            {
               handler = location.get(location.size() - 1);
               if (handler instanceof BrowseBreadcrumbHandler)
               {
                  // change the current node Id
                  navigator.setCurrentNodeId(((BrowseBreadcrumbHandler)handler).getNodeRef().getId());
               }
               else
               {
                  // TODO: shouldn't do this - but for now the user home dir is the root!
                  navigator.setCurrentNodeId(Application.getCurrentUser(FacesContext.getCurrentInstance()).getHomeSpaceId());
               }
            }
         }
      }
   }
   
   /**
    * Support for refresh of lists via special case for an External Access URL.
    * these URLs restart the JSF lifecycle but an old UIRichList is restored from
    * the component tree - which needs clearing "late" in the lifecycle process.
    */
   public void externalAccessRefresh()
   {
      this.externalForceRefresh = true;
   }
   
   /**
    * Save the state of the panel that was expanded/collapsed
    */
   public void expandPanel(ActionEvent event)
   {
      if (event instanceof ExpandedEvent)
      {
         String id = event.getComponent().getId();
         this.panels.put(id, ((ExpandedEvent)event).State);
      }
   }
   
   
   // ------------------------------------------------------------------------------
   // Private helpers
   
   /**
    * Initialise default values from client configuration
    */
   private void initFromClientConfig()
   {
      ConfigService config = Application.getConfigService(FacesContext.getCurrentInstance());
      
      this.viewsConfig = (ViewsConfigElement)config.getConfig("Views").
            getConfigElement(ViewsConfigElement.CONFIG_ELEMENT_ID);
      
      this.browseViewMode = this.viewsConfig.getDefaultView(PAGE_NAME_BROWSE);
      int pageSize = this.viewsConfig.getDefaultPageSize(PAGE_NAME_BROWSE, this.browseViewMode);
      setPageSizeContent(pageSize);
      setPageSizeSpaces(pageSize);
   }
   
   /**
    * @return the Set of NodeEventListeners registered against this bean
    */
   private Set<NodeEventListener> getNodeEventListeners()
   {
      if (this.nodeEventListeners == null)
      {
         this.nodeEventListeners = new HashSet<NodeEventListener>();
         
         FacesContext fc = FacesContext.getCurrentInstance();
         
         Config listenerConfig = Application.getConfigService(fc).getConfig("Node Event Listeners");
         if (listenerConfig != null)
         {
            ConfigElement listenerElement = listenerConfig.getConfigElement("node-event-listeners");
            if (listenerElement != null)
            {
               for (ConfigElement child : listenerElement.getChildren())
               {
                  if (child.getName().equals("listener"))
                  {
                     // retrieved the JSF Managed Bean identified in the config
                     String listenerName = child.getValue().trim();
                     Object bean = FacesHelper.getManagedBean(fc, listenerName);
                     if (bean instanceof NodeEventListener)
                     {
                        addNodeEventListener((NodeEventListener)bean);
                     }
                  }
               }
            }
         }
      }
      return this.nodeEventListeners;
   }
   
   /**
    * Refresh the UI after a Space selection change. Adds the selected space to the breadcrumb
    * location path and also updates the list components in the UI.
    * 
    * @param ref     NodeRef of the selected space
    */
   public void updateUILocation(NodeRef ref)
   {
      // get the current breadcrumb location and append a new handler to it
      // our handler know the ID of the selected node and the display label for it
      List<IBreadcrumbHandler> location = this.navigator.getLocation();
      if (location.size() != 0)
      {
         // attempt to find the ID - if it's already in the breadcrumb then we
         // navigate directly to that node - rather than add duplication to the breadcrumb path
         boolean foundNode = false;
         for (int i=0; i<location.size(); i++)
         {
            IBreadcrumbHandler element = location.get(i);
            if (element instanceof IRepoBreadcrumbHandler)
            {
               NodeRef nodeRef = ((IRepoBreadcrumbHandler)element).getNodeRef();
               if (ref.equals(nodeRef) == true)
               {
                  // TODO: we should be able to do this - but the UIBreadcrumb component modifies
                  //       it's own internal value when clicked - then uses that from then on!
                  //       the other ops are using the same List object and modding it directly.
                  //List<IBreadcrumbHandler> newLocation = new ArrayList<IBreadcrumbHandler>(i+1);
                  //newLocation.addAll(location.subList(0, i + 1));
                  //this.navigator.setLocation(newLocation);
                  // TODO: but instead for now we do this:
                  int count = location.size();
                  for (int n=i+1; n<count; n++)
                  {
                     location.remove(i+1);
                  }
                  
                  foundNode = true;
                  break;
               }
            }
         }
         
         // add new node to the end of the existing breadcrumb
         if (foundNode == false)
         {
            String name = Repository.getNameForNode(this.nodeService, ref);
            location.add(new BrowseBreadcrumbHandler(ref, name));
         }
      }
      else
      {
         // special case to add first item to the location
         String name = Repository.getNameForNode(this.nodeService, ref);
         location.add(new BrowseBreadcrumbHandler(ref, name));
      }
      
      // set the current node Id ready for page refresh
      this.navigator.setCurrentNodeId(ref.getId());
      
      // set up the dispatch context for the navigation handler
      this.navigator.setupDispatchContext(new Node(ref));
      
      navigateBrowseScreen();
   }
   
   /**
    * Invalidate list component state after an action which changes the UI context
    */
   private void invalidateComponents()
   {
      if (logger.isDebugEnabled())
         logger.debug("Invalidating browse components...");
      
      // clear the value for the list components - will cause re-bind to it's data and refresh
      if (this.contentRichList != null)
      {
         this.contentRichList.setValue(null);
         if (this.navigator.getSearchContext() != null)
         {
            // clear the sorting mode so the search results are displayed in default 'score' order
            this.contentRichList.clearSort();
         }
      }
      if (this.spacesRichList != null)
      {
         this.spacesRichList.setValue(null);
         if (this.navigator.getSearchContext() != null)
         {
            // clear the sorting mode so the search results are displayed in default 'score' order
            this.spacesRichList.clearSort();
         }
      }
      
      // clear the storage of the last set of nodes
      this.containerNodes = null;
      this.contentNodes = null;
   }
   
   /**
    * @return whether the current View ID is the "browse" screen
    */
   private boolean isViewCurrent()
   {
      return (FacesContext.getCurrentInstance().getViewRoot().getViewId().equals(BROWSE_VIEW_ID));
   }
   
   /**
    * Perform navigation to the browse screen if it is not already the current View
    */
   private void navigateBrowseScreen()
   {
      String outcome = null;
      
      if (isViewCurrent() == false)
      {
         outcome = "browse";
      }
      
      FacesContext fc = FacesContext.getCurrentInstance();
      fc.getApplication().getNavigationHandler().handleNavigation(fc, null, outcome);
   }
   
   
   // ------------------------------------------------------------------------------
   // Inner classes
   
   /**
    * Class to handle breadcrumb interaction for Browse pages
    */
   private class BrowseBreadcrumbHandler implements IRepoBreadcrumbHandler
   {
      private static final long serialVersionUID = 3833183653173016630L;
      
      /**
       * Constructor
       * 
       * @param NodeRef    The NodeRef for this browse navigation element
       * @param label      Element label
       */
      public BrowseBreadcrumbHandler(NodeRef nodeRef, String label)
      {
         this.label = label;
         this.nodeRef = nodeRef;
      }
      
      /**
       * @see java.lang.Object#toString()
       */
      public String toString()
      {
         return this.label;
      }

      /**
       * @see org.alfresco.web.ui.common.component.IBreadcrumbHandler#navigationOutcome(org.alfresco.web.ui.common.component.UIBreadcrumb)
       */
      @SuppressWarnings("unchecked")
      public String navigationOutcome(UIBreadcrumb breadcrumb)
      {
         // All browse breadcrumb element relate to a Node Id - when selected we
         // set the current node id
         navigator.setCurrentNodeId(this.nodeRef.getId());
         navigator.setLocation( (List)breadcrumb.getValue() );
         
         // setup the dispatch context
         navigator.setupDispatchContext(new Node(this.nodeRef));
         
         // return to browse page if required
         return (isViewCurrent() ? null : "browse"); 
      }
      
      public NodeRef getNodeRef()
      {
         return this.nodeRef;
      }
      
      private NodeRef nodeRef;
      private String label;
   }

   
   // ------------------------------------------------------------------------------
   // Private data
   
   /** Browse screen view ID */
   public static final String BROWSE_VIEW_ID = "/jsp/browse/browse.jsp";
   
   /** Small icon default name */
   public static final String SPACE_SMALL_DEFAULT = "space_small";
   
   private static final String VIEWMODE_DASHBOARD = "dashboard";
   private static final String PAGE_NAME_BROWSE = "browse";
   
   /** I18N messages */
   private static final String MSG_DELETE_COMPANYROOT = "delete_companyroot_confirm";
   private static final String MSG_SEARCH_MINIMUM     = "search_minimum";
   
   private static Logger logger = Logger.getLogger(BrowseBean.class);
   
   /** The NodeService to be used by the bean */
   protected NodeService nodeService;
   
   /** The SearchService to be used by the bean */
   protected SearchService searchService;
   
   /** The LockService to be used by the bean */
   protected LockService lockService;
   
   /** The NavigationBean bean reference */
   protected NavigationBean navigator;
   
   /** The DictionaryService bean reference */
   protected DictionaryService dictionaryService;
   
   /** The file folder service */
   protected FileFolderService fileFolderService;

   /** Views configuration object */
   protected ViewsConfigElement viewsConfig = null;
   
   /** Listeners for Node events */
   protected Set<NodeEventListener> nodeEventListeners = null;
   
   /** Collapsable Panel state */
   private Map<String, Boolean> panels = new HashMap<String, Boolean>(4, 1.0f);
   
   /** Component references */
   protected UIRichList spacesRichList;
   protected UIRichList contentRichList;
   private UIStatusMessage statusMessage;
   
   /** Transient lists of container and content nodes for display */
   protected List<Node> containerNodes = null;
   protected List<Node> contentNodes = null;
   
   /** The current space and it's properties - if any */
   protected Node actionSpace;
   
   /** The current document */
   protected Node document;
   
   /** Special message to display when user deleting certain folders e.g. Company Home */
   private String deleteMessage;
   
   /** The current browse view mode - set to a well known IRichListRenderer identifier */
   private String browseViewMode;
   
   /** The current browse view page sizes */
   private int pageSizeSpaces;
   private int pageSizeContent;
   private String pageSizeSpacesStr;
   private String pageSizeContentStr;
   
   /** True if current space has a dashboard (template) view available */
   private boolean dashboardView;
   
   private boolean externalForceRefresh = false;
}
