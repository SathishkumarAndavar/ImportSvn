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
package org.alfresco.web.ui.repo.renderer;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.transaction.UserTransaction;

import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.renderer.BaseRenderer;
import org.alfresco.web.ui.repo.component.UINodeDescendants;
import org.alfresco.web.ui.repo.component.UINodePath;

/**
 * @author Kevin Roast
 */
public class NodePathLinkRenderer extends BaseRenderer
{
   // ------------------------------------------------------------------------------
   // Renderer implementation
   
   /**
    * @see javax.faces.render.Renderer#decode(javax.faces.context.FacesContext, javax.faces.component.UIComponent)
    */
   public void decode(FacesContext context, UIComponent component)
   {
      Map requestMap = context.getExternalContext().getRequestParameterMap();
      String fieldId = getHiddenFieldName(context, component);
      String value = (String)requestMap.get(fieldId);
      
      // we encoded the value to start with our Id
      if (value != null && value.startsWith(component.getClientId(context) + NamingContainer.SEPARATOR_CHAR))
      {
         // found a new selected value for this component
         // queue an event to represent the change
         String selectedNodeId = value.substring(component.getClientId(context).length() + 1);
         NodeRef ref = new NodeRef(Repository.getStoreRef(), selectedNodeId);
         
         UINodePath.PathElementEvent event = new UINodePath.PathElementEvent(component, ref); 
         component.queueEvent(event);
      }
   }
   
   /**
    * @see javax.faces.render.Renderer#encodeEnd(javax.faces.context.FacesContext, javax.faces.component.UIComponent)
    */
   public void encodeEnd(FacesContext context, UIComponent component) throws IOException
   {
      // always check for this flag - as per the spec
      if (component.isRendered() == false)
      {
         return;
      }
      
      Writer out = context.getResponseWriter();
      
      // make sure we have a NodeRef or Path from the 'value' property ValueBinding
      Path path = null;
      NodeRef nodeRef = null;
      Object val = ((UINodePath)component).getValue();
      if (val instanceof NodeRef == true)
      {
         nodeRef = (NodeRef)val;
      }
      else if (val instanceof Path == true)
      {
         path = (Path)val;
      }
      else
      {
         throw new IllegalArgumentException("UINodePath component 'value' property must resolve to a NodeRef or Path!");
      }
      
      boolean isBreadcrumb = false;
      Boolean breadcrumb = (Boolean)component.getAttributes().get("breadcrumb");
      if (breadcrumb != null)
      {
         isBreadcrumb = breadcrumb.booleanValue();
      }
      
      boolean isDisabled = false;
      Boolean disabled = (Boolean)component.getAttributes().get("disabled");
      if (disabled != null)
      {
         isDisabled = disabled.booleanValue();
      }
      
      boolean showLeaf = false;
      Boolean showLeafBool = (Boolean)component.getAttributes().get("showLeaf");
      if (showLeafBool != null)
      {
         showLeaf = showLeafBool.booleanValue();
      }
      
      // use Spring JSF integration to get the node service bean
      NodeService service = getNodeService(context);
      UserTransaction tx = null;
      try
      {
         tx = Repository.getUserTransaction(FacesContext.getCurrentInstance(), true);
         tx.begin();
         
         if (path == null)
         {
            path = service.getPath(nodeRef);
         }
         
         if (isBreadcrumb == false || isDisabled == true)
         {
            out.write(buildPathAsSingular(context, component, path, showLeaf, isDisabled));
         }
         else
         {
            out.write(buildPathAsBreadcrumb(context, component, path, showLeaf));
         }
         
         tx.commit();
      }
      catch (InvalidNodeRefException refErr)
      {
         // this error simple means we cannot output the path
         try { if (tx != null) {tx.rollback();} } catch (Exception tex) {}
      }
      catch (AccessDeniedException accessErr)
      {
         // this error simple means we cannot output the path
         try { if (tx != null) {tx.rollback();} } catch (Exception tex) {}
      }
      catch (Throwable err)
      {
         try { if (tx != null) {tx.rollback();} } catch (Exception tex) {}
         throw new RuntimeException(err);
      }
   }
   
   /**
    * Return the path with each element as a single clickable link e.g. breadcrumb style
    * 
    * @param context        FacesContext
    * @param component      UIComponent to get display attribute from
    * @param path           Node Path to use
    * 
    * @return the path with each individual element clickable
    */
   private String buildPathAsBreadcrumb(FacesContext context, UIComponent component, Path path, boolean showLeaf)
   {
      StringBuilder buf = new StringBuilder(1024);
      
      int size = (showLeaf ? path.size() : path.size() - 1);
      for (int i=0; i<size; i++)
      {
         Path.Element element = path.get(i);
         String elementString = null;
         if (element instanceof Path.ChildAssocElement)
         {
            ChildAssociationRef elementRef = ((Path.ChildAssocElement)element).getRef();
            if (elementRef.getParentRef() != null)
            {
               String name = Repository.getNameForNode(getNodeService(context), elementRef.getChildRef());
               elementString = renderPathElement(context, component, elementRef.getChildRef(), name);
            }
         }
         else
         {
            elementString = element.getElementString();
         }
         
         if (elementString != null)
         {
            buf.append("/");
            buf.append(elementString);
         }
      }
      
      return buf.toString();
   }
   
   /**
    * Return the path with the entire path as a single clickable link
    * 
    * @param context        FacesContext
    * @param component      UIComponent to get display attribute from
    * @param path           Node Path to use
    * 
    * @return the entire path as a single clickable link
    */
   private String buildPathAsSingular(FacesContext context, UIComponent component, Path path, boolean showLeaf, boolean disabled)
   {
      StringBuilder buf = new StringBuilder(512);
      
      NodeRef lastElementRef = null;
      int size = (showLeaf ? path.size() : path.size() - 1);
      for (int i=0; i<size; i++)
      {
         Path.Element element = path.get(i);
         String elementString = null;
         if (element instanceof Path.ChildAssocElement)
         {
            ChildAssociationRef elementRef = ((Path.ChildAssocElement)element).getRef();
            if (elementRef.getParentRef() != null)
            {
               elementString = Repository.getNameForNode(getNodeService(context), elementRef.getChildRef());
            }
            if (i == path.size() - 2)
            {
               lastElementRef = elementRef.getChildRef();
            }
         }
         else
         {
            elementString = element.getElementString();
         }
         
         if (elementString != null)
         {
            buf.append("/");
            buf.append(elementString);
         }
      }
      
      if (disabled == false)
      {
         return renderPathElement(context, component, lastElementRef, buf.toString());
      }
      else
      {
         return buf.toString();
      }
   }
   
   /**
    * Render a path element as a clickable link
    * 
    * @param context    FacesContext
    * @param control    UIComponent to get attributes from
    * @param nodeRef    NodeRef of the path element
    * @param label      Display label to output with this link
    * 
    * @return HTML for a descendant link
    */
   private String renderPathElement(FacesContext context, UIComponent control, NodeRef nodeRef, String label)
   {
      StringBuilder buf = new StringBuilder(256);
      
      buf.append("<a href='#' onclick=\"");
      // build an HTML param that contains the client Id of this control, followed by the node Id
      String param = control.getClientId(context) + NamingContainer.SEPARATOR_CHAR + nodeRef.getId();
      buf.append(Utils.generateFormSubmit(context, control, getHiddenFieldName(context, control), param));
      buf.append('"');
      Map attrs = control.getAttributes();
      if (attrs.get("style") != null)
      {
         buf.append(" style=\"")
            .append(attrs.get("style"))
            .append('"');
      }
      if (attrs.get("styleClass") != null)
      {
         buf.append(" class=")
            .append(attrs.get("styleClass"));
      }
      buf.append('>');
      
      buf.append(Utils.encode(label));
      
      buf.append("</a>");
      
      return buf.toString();
   }

   
   // ------------------------------------------------------------------------------
   // Private helpers

   /**
    * Get the hidden field name for this node path component.
    * Build a shared field name from the parent form name and the string "npath".
    * 
    * @return hidden field name shared by all node path components within the Form.
    */
   private static String getHiddenFieldName(FacesContext context, UIComponent component)
   {
      return Utils.getParentForm(context, component).getClientId(context) + NamingContainer.SEPARATOR_CHAR + "npath";
   }
   
   /**
    * Use Spring JSF integration to return the node service bean instance
    * 
    * @param context    FacesContext
    * 
    * @return node service bean instance or throws runtime exception if not found
    */
   private static NodeService getNodeService(FacesContext context)
   {
      NodeService service = Repository.getServiceRegistry(context).getNodeService();
      if (service == null)
      {
         throw new IllegalStateException("Unable to obtain NodeService bean reference.");
      }
      
      return service;
   }
}
