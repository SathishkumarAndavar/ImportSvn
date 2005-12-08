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

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.ActionEvent;
import javax.transaction.UserTransaction;

import org.alfresco.model.ContentModel;
import org.alfresco.model.ForumModel;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.QueryParameterDefinition;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.Application;
import org.alfresco.web.app.context.IContextListener;
import org.alfresco.web.app.context.UIContextService;
import org.alfresco.web.bean.repository.MapNode;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.NodePropertyResolver;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.config.ClientConfigElement;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.component.UIModeList;
import org.alfresco.web.ui.common.component.data.UIColumn;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.alfresco.web.ui.common.renderer.data.IRichListRenderer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Bean providing properties and behaviour for the forums screens.
 * 
 * @author gavinc
 */
public class ForumsBean implements IContextListener
{
   private static Log logger = LogFactory.getLog(ForumsBean.class);
   private static final String PAGE_NAME_FORUMS = "forums";
   private static final String PAGE_NAME_FORUM = "forum";
   private static final String PAGE_NAME_TOPIC = "topic"; 
   
   /** The NodeService to be used by the bean */
   private NodeService nodeService;
   
   /** The ContentService to be used by the bean */
   private ContentService contentService;
   
   /** The DictionaryService bean reference */
   private DictionaryService dictionaryService;
   
   /** The SearchService bean reference. */
   private SearchService searchService;
   
   /** The NamespaceService bean reference. */
   private NamespaceService namespaceService;
   
   /** The browse bean */
   private BrowseBean browseBean;
   
   /** The NavigationBean bean reference */
   private NavigationBean navigator;
   
   /** Client configuration object */
   private ClientConfigElement clientConfig = null;
   
   /** Component references */
   private UIRichList forumsRichList;
   private UIRichList forumRichList;
   private UIRichList topicRichList;

   /** Node lists */
   private List<Node> forums;
   private List<Node> topics;
   private List<Node> posts;
   
   /** The current forums view mode - set to a well known IRichListRenderer identifier */
   private String forumsViewMode;
   
   /** The current forums view page size */
   private int forumsPageSize;
   
   /** The current forum view mode - set to a well known IRichListRenderer identifier */
   private String forumViewMode;
   
   /** The current forum view page size */
   private int forumPageSize;
   
   /** The current topic view mode - set to a well known IRichListRenderer identifier */
   private String topicViewMode;
   
   /** The current topic view page size */
   private int topicPageSize;
   
   // ------------------------------------------------------------------------------
   // Construction 

   /**
    * Default Constructor
    */
   public ForumsBean()
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
    * Sets the content service to use
    * 
    * @param contentService The ContentService
    */
   public void setContentService(ContentService contentService)
   {
      this.contentService = contentService;
   }

   /**
    * @param dictionaryService The DictionaryService to set.
    */
   public void setDictionaryService(DictionaryService dictionaryService)
   {
      this.dictionaryService = dictionaryService;
   }
   
   /**
    * @param searchService The SearchService to set.
    */
   public void setSearchService(SearchService searchService)
   {
      this.searchService = searchService;
   }

   /**
    * @param namespaceService The NamespaceService to set.
    */
   public void setNamespaceService(NamespaceService namespaceService)
   {
      this.namespaceService = namespaceService;
   }

   /**
    * Sets the BrowseBean instance to use to retrieve the current document
    * 
    * @param browseBean BrowseBean instance
    */
   public void setBrowseBean(BrowseBean browseBean)
   {
      this.browseBean = browseBean;
   }
   
   /**
    * @param navigator The NavigationBean to set.
    */
   public void setNavigator(NavigationBean navigator)
   {
      this.navigator = navigator;
   }
   
   /**
    * @param forumsRichList The forumsRichList to set.
    */
   public void setForumsRichList(UIRichList forumsRichList)
   {
      this.forumsRichList = forumsRichList;
      if (this.forumsRichList != null)
      {
         // set the initial sort column and direction
         this.forumsRichList.setInitialSortColumn(
               clientConfig.getDefaultSortColumn(PAGE_NAME_FORUMS));
         this.forumsRichList.setInitialSortDescending(
               clientConfig.hasDescendingSort(PAGE_NAME_FORUMS));
      }
   }
   
   /**
    * @return Returns the forumsRichList.
    */
   public UIRichList getForumsRichList()
   {
      return this.forumsRichList;
   }
   
   /**
    * @return Returns the forums View mode. See UIRichList
    */
   public String getForumsViewMode()
   {
      return this.forumsViewMode;
   }
   
   /**
    * @param forumsViewMode      The forums View mode to set. See UIRichList.
    */
   public void setForumsViewMode(String forumsViewMode)
   {
      this.forumsViewMode = forumsViewMode;
   }
   
   /**
    * @return Returns the forumsPageSize.
    */
   public int getForumsPageSize()
   {
      return this.forumsPageSize;
   }
   
   /**
    * @param forumsPageSize The forumsPageSize to set.
    */
   public void setForumsPageSize(int forumsPageSize)
   {
      this.forumsPageSize = forumsPageSize;
   }
   
   /**
    * @param topicRichList The topicRichList to set.
    */
   public void setTopicRichList(UIRichList topicRichList)
   {
      this.topicRichList = topicRichList;
      
      if (this.topicRichList != null)
      {
         // set the initial sort column and direction
         this.topicRichList.setInitialSortColumn(
               clientConfig.getDefaultSortColumn(PAGE_NAME_TOPIC));
         this.topicRichList.setInitialSortDescending(
               clientConfig.hasDescendingSort(PAGE_NAME_TOPIC));
      }
   }
   
   /**
    * @return Returns the topicRichList.
    */
   public UIRichList getTopicRichList()
   {
      return this.topicRichList;
   }
   
   /**
    * @return Returns the topics View mode. See UIRichList
    */
   public String getTopicViewMode()
   {
      return this.topicViewMode;
   }
   
   /**
    * @param topicViewMode      The topic View mode to set. See UIRichList.
    */
   public void setTopicViewMode(String topicViewMode)
   {
      this.topicViewMode = topicViewMode;
   }
   
   /**
    * @return Returns the topicsPageSize.
    */
   public int getTopicPageSize()
   {
      return this.topicPageSize;
   }
   
   /**
    * @param topicPageSize The topicPageSize to set.
    */
   public void setTopicPageSize(int topicPageSize)
   {
      this.topicPageSize = topicPageSize;
   }
   
   /**
    * @param forumRichList The forumRichList to set.
    */
   public void setForumRichList(UIRichList forumRichList)
   {
      this.forumRichList = forumRichList;
      
      if (this.forumRichList != null)
      {
         // set the initial sort column and direction
         this.forumRichList.setInitialSortColumn(
               clientConfig.getDefaultSortColumn(PAGE_NAME_FORUM));
         this.forumRichList.setInitialSortDescending(
               clientConfig.hasDescendingSort(PAGE_NAME_FORUM));
      }
   }
   
   /**
    * @return Returns the forumRichList.
    */
   public UIRichList getForumRichList()
   {
      return this.forumRichList;
   }
   
   /**
    * @return Returns the forum View mode. See UIRichList
    */
   public String getForumViewMode()
   {
      return this.forumViewMode;
   }
   
   /**
    * @param forumViewMode      The forum View mode to set. See UIRichList.
    */
   public void setForumViewMode(String forumViewMode)
   {
      this.forumViewMode = forumViewMode;
   }
   
   /**
    * @return Returns the forumPageSize.
    */
   public int getForumPageSize()
   {
      return this.forumPageSize;
   }
   
   /**
    * @param forumPageSize The forumPageSize to set.
    */
   public void setForumPageSize(int forumPageSize)
   {
      this.forumPageSize = forumPageSize;
   }
   
   public List<Node> getForums()
   {
      if (this.forums == null)
      {
         getNodes();
      }
      
      return this.forums;
   }
   
   public List<Node> getTopics()
   {
      if (this.topics == null)
      {
         getNodes();
      }
      
      return this.topics;
   }
   
   public List<Node> getPosts()
   {
      if (this.posts == null)
      {
         getNodes();
      }
      
      return this.posts;
   }
   
   private void getNodes()
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
         
         // get the current space from NavigationBean
         String parentNodeId = this.navigator.getCurrentNodeId();
         
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
         
         // TODO: can we improve the Get here with an API call for children of a specific type?
         List<ChildAssociationRef> childRefs = this.nodeService.getChildAssocs(parentRef);
         this.forums = new ArrayList<Node>(childRefs.size());
         this.topics = new ArrayList<Node>(childRefs.size());
         this.posts = new ArrayList<Node>(childRefs.size());
         
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
                  // extract forums, forum, topic and post types
                  
                  if (this.dictionaryService.isSubClass(type, ContentModel.TYPE_SYSTEM_FOLDER) == false)
                  {
                     if (this.dictionaryService.isSubClass(type, ForumModel.TYPE_FORUMS) || 
                         this.dictionaryService.isSubClass(type, ForumModel.TYPE_FORUM)) 
                     {
                        // create our Node representation
                        MapNode node = new MapNode(nodeRef, this.nodeService, true);
                        node.addPropertyResolver("icon", this.browseBean.resolverSpaceIcon);
                        
                        this.forums.add(node);
                     }
                     if (this.dictionaryService.isSubClass(type, ForumModel.TYPE_TOPIC)) 
                     {
                        // create our Node representation
                        MapNode node = new MapNode(nodeRef, this.nodeService, true);
                        node.addPropertyResolver("icon", this.browseBean.resolverSpaceIcon);
                        node.addPropertyResolver("replies", this.resolverReplies);
                        
                        this.topics.add(node);
                     }
                     else if (this.dictionaryService.isSubClass(type, ForumModel.TYPE_POST))
                     {
                        // create our Node representation
                        MapNode node = new MapNode(nodeRef, this.nodeService, true);
                        
                        this.browseBean.setupDataBindingProperties(node);
                        node.addPropertyResolver("message", this.resolverContent);
                        node.addPropertyResolver("replyTo", this.resolverReplyTo);
                        
                        this.posts.add(node);
                     }
                  }
               }
               else
               {
                  if (logger.isWarnEnabled())
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
               FacesContext.getCurrentInstance(), Repository.ERROR_NODEREF), new Object[] {refErr.getNodeRef()}) );
         this.forums = Collections.<Node>emptyList();
         this.topics = Collections.<Node>emptyList();
         this.posts = Collections.<Node>emptyList();
         try { if (tx != null) {tx.rollback();} } catch (Exception tex) {}
      }
      catch (Throwable err)
      {
         Utils.addErrorMessage(MessageFormat.format(Application.getMessage(
               FacesContext.getCurrentInstance(), Repository.ERROR_GENERIC), err.getMessage()), err);
         this.forums = Collections.<Node>emptyList();
         this.topics = Collections.<Node>emptyList();
         this.posts = Collections.<Node>emptyList();
         try { if (tx != null) {tx.rollback();} } catch (Exception tex) {}
      }
      
      if (logger.isDebugEnabled())
      {
         long endTime = System.currentTimeMillis();
         logger.debug("Time to query and build forums nodes: " + (endTime - startTime) + "ms");
      }
   }
   
   
   // ------------------------------------------------------------------------------
   // IContextListener implementation 
   
   /**
    * @see org.alfresco.web.app.context.IContextListener#contextUpdated()
    */
   public void contextUpdated()
   {
      if (logger.isDebugEnabled())
         logger.debug("Invalidating forums components...");
      
      // clear the value for the list components - will cause re-bind to it's data and refresh
      if (this.forumsRichList != null)
      {
         this.forumsRichList.setValue(null);
         if (this.forumsRichList.getInitialSortColumn() == null)
         {
            // set the initial sort column and direction
            this.forumsRichList.setInitialSortColumn(
                  clientConfig.getDefaultSortColumn(PAGE_NAME_FORUMS));
            this.forumsRichList.setInitialSortDescending(
                  clientConfig.hasDescendingSort(PAGE_NAME_FORUMS));
         }
      }
      
      if (this.forumRichList != null)
      {
         this.forumRichList.setValue(null);
         if (this.forumRichList.getInitialSortColumn() == null)
         {
            // set the initial sort column and direction
            this.forumRichList.setInitialSortColumn(
                  clientConfig.getDefaultSortColumn(PAGE_NAME_FORUM));
            this.forumRichList.setInitialSortDescending(
                  clientConfig.hasDescendingSort(PAGE_NAME_FORUM));
         }
      }
      
      if (this.topicRichList != null)
      {
         this.topicRichList.setValue(null);
         if (this.topicRichList.getInitialSortColumn() == null)
         {
            // set the initial sort column and direction
            this.topicRichList.setInitialSortColumn(
                  clientConfig.getDefaultSortColumn(PAGE_NAME_TOPIC));
            this.topicRichList.setInitialSortDescending(
                  clientConfig.hasDescendingSort(PAGE_NAME_TOPIC));
         }
      }
      
      // reset the lists
      this.forums = null;
      this.topics = null;
      this.posts = null;
   }
   
   // ------------------------------------------------------------------------------
   // Navigation action event handlers 
   
   /**
    * Change the current forums view mode based on user selection
    * 
    * @param event      ActionEvent
    */
   public void forumsViewModeChanged(ActionEvent event)
   {
      UIModeList viewList = (UIModeList)event.getComponent();
      
      // get the view mode ID
      String viewMode = viewList.getValue().toString();
      
      // push the view mode into the lists
      setForumsViewMode(viewMode);
   }
   
   /**
    * Change the current forum view mode based on user selection
    * 
    * @param event      ActionEvent
    */
   public void forumViewModeChanged(ActionEvent event)
   {
      UIModeList viewList = (UIModeList)event.getComponent();
      
      // get the view mode ID
      String viewMode = viewList.getValue().toString();
      
      // push the view mode into the lists
      setForumViewMode(viewMode);
   }
   
   /**
    * Change the current topic view mode based on user selection
    * 
    * @param event      ActionEvent
    */
   public void topicViewModeChanged(ActionEvent event)
   {
      UIModeList viewList = (UIModeList)event.getComponent();
      
      // get the view mode ID
      String viewMode = viewList.getValue().toString();
      
      // push the view mode into the lists
      setTopicViewMode(viewMode);
   }
   
   // ------------------------------------------------------------------------------
   // Property Resolvers
   
   public NodePropertyResolver resolverReplies = new NodePropertyResolver() {
      public Object get(Node node) 
      {
         // query for the number of posts within the given node
         String repliesXPath = "./*[(subtypeOf('" + ForumModel.TYPE_POST + "'))]";         
         List<NodeRef> replies = searchService.selectNodes(
                node.getNodeRef(),
                repliesXPath,
                new QueryParameterDefinition[] {},
                namespaceService,
                false);
         
         // reduce the count by 1 as one of the posts will be the initial post
         int noReplies = replies.size() - 1;
         
         if (noReplies < 0)
         {
            noReplies = 0;
         }
         
         return new Integer(noReplies);
      }
   };
   
   public NodePropertyResolver resolverContent = new NodePropertyResolver() {
      public Object get(Node node) 
      {
         String content = null;
         
         // get the content property from the node and retrieve the 
         // full content as a string (obviously should only be used
         // for small amounts of content)
         
         ContentReader reader = contentService.getReader(node.getNodeRef(), 
               ContentModel.PROP_CONTENT);
         
         if (reader != null)
         {
            content = reader.getContentString();
         }
         
         return content;
      }
   };
   
   public NodePropertyResolver resolverReplyTo = new NodePropertyResolver() {
      public Object get(Node node) 
      {
         // determine if this node is a reply to another post, if so find
         // the creator of the original poster
         
         String replyTo = null;
         
         List<AssociationRef> assocs = nodeService.getTargetAssocs(node.getNodeRef(),
               ContentModel.ASSOC_REFERENCES);
         
         // there should only be one association, if there is more than one
         // just get the first one
         if (assocs.size() > 0)
         {
            AssociationRef assoc = assocs.get(0); 
            NodeRef target = assoc.getTargetRef();
            Node targetNode = new Node(target);
            replyTo = (String)targetNode.getProperties().get("creator");
         }
         
         return replyTo;
      }
   };
   
   // ------------------------------------------------------------------------------
   // Private helpers
   
   /**
    * Initialise default values from client configuration
    */
   private void initFromClientConfig()
   {
      this.clientConfig = (ClientConfigElement)Application.getConfigService(
            FacesContext.getCurrentInstance()).getGlobalConfig().
            getConfigElement(ClientConfigElement.CONFIG_ELEMENT_ID);
      
      // get the defaults for the forums page
      this.forumsViewMode = clientConfig.getDefaultView(PAGE_NAME_FORUMS);
      this.forumsPageSize = this.clientConfig.getDefaultPageSize(PAGE_NAME_FORUMS,
            this.forumsViewMode);
      
      // get the default for the forum page
      this.forumViewMode = clientConfig.getDefaultView(PAGE_NAME_FORUM);
      this.forumPageSize = this.clientConfig.getDefaultPageSize(PAGE_NAME_FORUM, 
            this.forumViewMode);
      
      // get the default for the topic page
      this.topicViewMode = clientConfig.getDefaultView(PAGE_NAME_TOPIC);
      this.topicPageSize = this.clientConfig.getDefaultPageSize(PAGE_NAME_TOPIC, 
            this.topicViewMode);
      
      if (logger.isDebugEnabled())
      {
         logger.debug("Set default forums view mode to: " + this.forumsViewMode);
         logger.debug("Set default forums page size to: " + this.forumsPageSize);
         logger.debug("Set default forum view mode to: " + this.forumViewMode);
         logger.debug("Set default forum page size to: " + this.forumPageSize);
         logger.debug("Set default topic view mode to: " + this.topicViewMode);
         logger.debug("Set default topic page size to: " + this.topicPageSize);
      }
   }
   
   /**
    * Class to implement a bubble view for the RichList component used in the topics screen
    * 
    * @author gavinc
    */
   public static class TopicBubbleViewRenderer implements IRichListRenderer
   {
      public static final String VIEWMODEID = "bubble";
      
      /**
       * @see org.alfresco.web.ui.common.renderer.data.IRichListRenderer#getViewModeID()
       */
      public String getViewModeID()
      {
         return VIEWMODEID;
      }
      
      /**
       * @see org.alfresco.web.ui.common.renderer.data.IRichListRenderer#renderListBefore(javax.faces.context.FacesContext, org.alfresco.web.ui.common.component.data.UIColumn[])
       */
      public void renderListBefore(FacesContext context, UIRichList richList, UIColumn[] columns)
            throws IOException
      {
         // nothing to do
      }
      
      /**
       * @see org.alfresco.web.ui.common.renderer.data.IRichListRenderer#renderListRow(javax.faces.context.FacesContext, org.alfresco.web.ui.common.component.data.UIColumn[], java.lang.Object)
       */
      public void renderListRow(FacesContext context, UIRichList richList, UIColumn[] columns, Object row)
            throws IOException
      {
         ResponseWriter out = context.getResponseWriter();
         
         // find primary column (which must exist) and the actions column (which doesn't 
         // have to exist)
         UIColumn primaryColumn = null;
         UIColumn actionsColumn = null;
         for (int i = 0; i < columns.length; i++)
         {
            if (columns[i].isRendered())
            {
               if (columns[i].getPrimary())
               {
                  primaryColumn = columns[i];
               }
               else if (columns[i].getActions())
               {
                  actionsColumn = columns[i];
               }
            }
         }
         
         if (primaryColumn == null)
         {
            if (logger.isWarnEnabled())
               logger.warn("No primary column found for RichList definition: " + richList.getId());
         }
         
         out.write("<tr>");
         
         Node node = (Node)row;
         if (node.getProperties().get("replyTo") == null)
         {
            renderNewPostBubble(context, out, node, primaryColumn, actionsColumn, columns);
         }
         else
         {
            renderReplyToBubble(context, out, node, primaryColumn, actionsColumn, columns);
         }
         
         out.write("</tr>");
         
         // add a little padding
         out.write("<tr><td><div style='padding:3px'></div></td></tr>");
      }
      
      /**
       * @see org.alfresco.web.ui.common.renderer.data.IRichListRenderer#renderListAfter(javax.faces.context.FacesContext, org.alfresco.web.ui.common.component.data.UIColumn[])
       */
      public void renderListAfter(FacesContext context, UIRichList richList, UIColumn[] columns)
            throws IOException
      {
         ResponseWriter out = context.getResponseWriter();
         
         out.write("<tr><td colspan='99' align='right'>");
         for (Iterator i = richList.getChildren().iterator(); i.hasNext(); /**/)
         {
            // output all remaining child components that are not UIColumn
            UIComponent child = (UIComponent)i.next();
            if (child instanceof UIColumn == false)
            {
               Utils.encodeRecursive(context, child);
            }
         }
         out.write("</td></tr>");
      }
      
      /**
       * Renders the new post speech bubble
       * 
       * @param context Faces context
       * @param out The response writer
       * @param node The Node for the row being rendered
       * @param primaryColumn The primary column containing the message content
       * @param actionsColumn The actions column containing all the actions
       * @param columns All configured columns
       */
      private void renderNewPostBubble(FacesContext context, ResponseWriter out, Node node, 
            UIColumn primaryColumn, UIColumn actionsColumn, UIColumn[] columns) throws IOException
      {
         out.write("<td><table border='0' cellpadding='0' cellspacing='0' width='100%'><tr>");
         out.write("<td><img src='");
         out.write(context.getExternalContext().getRequestContextPath());
         out.write("/images/icons/user_large.gif'/><br/>");
         out.write((String)node.getProperties().get("creator"));
         out.write("</td><td width='100%'>");
         renderBubble(context, out, "orange", "#FCC75E", primaryColumn, actionsColumn, columns);
         out.write("</td><td><div style='width:32px;' /></td></table></td>");
      }
      
      /**
       * Renders the reply to post speech bubble
       * 
       * @param context Faces context
       * @param out The response writer
       * @param node The Node for the row being rendered
       * @param primaryColumn The primary column containing the message content
       * @param actionsColumn The actions column containing all the actions
       * @param columns All configured columns
       */
      private void renderReplyToBubble(FacesContext context, ResponseWriter out, Node node, 
            UIColumn primaryColumn, UIColumn actionsColumn, UIColumn[] columns) throws IOException
      {
         out.write("<td width='100%'><table border='0' cellpadding='0' cellspacing='0' width='100%'><tr>");
         out.write("<td><div style='width:32px;' /></td><td width='100%'>");
         renderBubble(context, out, "yellow", "#FFF5A3", primaryColumn, actionsColumn, columns);
         out.write("</td><td><img src='");
         out.write(context.getExternalContext().getRequestContextPath());
         out.write("/images/icons/user_large.gif'/><br/>");
         out.write((String)node.getProperties().get("creator"));
         out.write("</td></table></td>");
      }

      /**
       * Renders the speech bubble
       * 
       * @param context Faces context
       * @param out The response writer
       * @param colour The colour of the bubble
       * @param titleBgColour The colour of the background of the title area (#rrggbb)
       * @param primaryColumn The primary column containing the message content
       * @param actionsColumn The actions column containing all the actions
       * @param columns All configured columns
       */
      private void renderBubble(FacesContext context, ResponseWriter out,
            String colour, String titleBgColour, 
            UIColumn primaryColumn, UIColumn actionsColumn, UIColumn[] columns)
            throws IOException
      {
         String contextPath = context.getExternalContext().getRequestContextPath();
         
         out.write("<table border='0' cellpadding='0' cellspacing='0' width='100%'><tr>");
         out.write("<td style='background: url(");
         out.write(contextPath);
         out.write("/images/parts/");
         out.write(colour);
         out.write("header_1.gif) no-repeat #FFFFFF;' width='24' height='24'></td>");
         out.write("<td style='background: url(");
         out.write(contextPath);
         out.write("/images/parts/");
         out.write(colour);
         out.write("header_2.gif) repeat-x ");
         out.write(titleBgColour);
         out.write("'>");
         
         // render the header area with the configured columns
         out.write("<table width='100%' cellpadding='2' cellspacing='2' border='0'><tr>");
         
         for (int i = 0; i < columns.length; i++)
         {
            UIColumn column = columns[i];
            
            if (column.isRendered() == true &&
                column.getPrimary() == false && 
                column.getActions() == false)
            {
               // render the column header as the label
               UIComponent header = column.getFacet("header");
               if (header != null)
               {
                  out.write("<td><nobr><b>");
                  Utils.encodeRecursive(context, header);
                  out.write("</nobr></b></td>");
               }
               
               // render the contents of the column
               if (column.getChildCount() != 0)
               {
                  out.write("<td><nobr>");
                  Utils.encodeRecursive(context, column);
                  out.write("</nobr></td>");
               }
            }
         }
         
         // render the actions column
         out.write("<td align='right' width='100%'>");
         if (actionsColumn != null && actionsColumn.getChildCount() != 0)
         {
            Utils.encodeRecursive(context, actionsColumn);
         }
         out.write("</td></tr></table>");
         
         out.write("</td>");
         out.write("<td style='background: url(");
         out.write(contextPath);
         out.write("/images/parts/");
         out.write(colour);
         out.write("header_3.gif) no-repeat #FFFFFF;' width='24' height='24'></td>");
         out.write("</tr></tr>");
         out.write("<td style='background: url(");
         out.write(contextPath);
         out.write("/images/parts/");
         out.write(colour);
         out.write("body_1.gif) no-repeat #FFFFFF;' width='24' height='13'></td>");
         out.write("<td style='background: url(");
         out.write(contextPath);
         out.write("/images/parts/");
         out.write(colour);
         out.write("body_2.gif) repeat-x #FFFFFF;'></td>");
         out.write("<td style='background: url(");
         out.write(contextPath);
         out.write("/images/parts/");
         out.write(colour);
         out.write("body_3.gif) no-repeat #FFFFFF;' width='24' height='13'></td>");
         out.write("</tr><tr>");
         out.write("<td style='background: url(");
         out.write(contextPath);
         out.write("/images/parts/");
         out.write(colour);
         out.write("body_4.gif) repeat-y #FFFFFF;' width='24' height='13'></td>");
         out.write("<td>");
         
         // render the primary column
         if (primaryColumn != null && primaryColumn.getChildCount() != 0)
         {
            Utils.encodeRecursive(context, primaryColumn);
         }
         
         out.write("</td>");
         
         out.write("<td style='background: url(");
         out.write(contextPath);
         out.write("/images/parts/");
         out.write(colour);
         out.write("body_6.gif) repeat-y #FFFFFF;' width='24' height='13'></td>");
         out.write("</tr><tr>");
         out.write("<td style='background: url(");
         out.write(contextPath);
         out.write("/images/parts/");
         out.write(colour);
         out.write("body_7.gif) no-repeat #FFFFFF;' width='24' height='13'></td>");
         out.write("<td style='background: url(");
         out.write(contextPath);
         out.write("/images/parts/");
         out.write(colour);
         out.write("body_8.gif) repeat-x #FFFFFF;'></td>");
         out.write("<td style='background: url(");
         out.write(contextPath);
         out.write("/images/parts/");
         out.write(colour);
         out.write("body_9.gif) no-repeat #FFFFFF;' width='24' height='13'></td>");
         out.write("</tr></table>");
      }
   }
}
