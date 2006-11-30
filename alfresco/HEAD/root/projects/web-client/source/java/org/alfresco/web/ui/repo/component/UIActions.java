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
package org.alfresco.web.ui.repo.component;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.faces.component.UIComponent;
import javax.faces.component.UIParameter;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.el.ValueBinding;

import org.alfresco.config.Config;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.Application;
import org.alfresco.web.config.ActionsConfigElement;
import org.alfresco.web.config.ActionsConfigElement.ActionDefinition;
import org.alfresco.web.config.ActionsConfigElement.ActionGroup;
import org.alfresco.web.ui.common.ComponentConstants;
import org.alfresco.web.ui.common.ConstantMethodBinding;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.component.SelfRenderingComponent;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.repo.component.evaluator.ActionInstanceEvaluator;
import org.alfresco.web.ui.repo.component.evaluator.PermissionEvaluator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.myfaces.taglib.UIComponentTagUtils;

/**
 * @author kevinr
 */
public class UIActions extends SelfRenderingComponent
{
   // ------------------------------------------------------------------------------
   // Component implementation
   
   private static final String ATTR_VALUE = "value";

   private static Log logger = LogFactory.getLog(UIActions.class);
   
   private static final String ATTR_SHOWLINK = "showLink";
   private static final String ATTR_STYLECLASS = "styleClass";
   private static final String ATTR_STYLE = "style";
   private static final String ACTION_CONTEXT = "actionContext";
   
   public static final String RENDERER_ACTIONLINK = "org.alfresco.faces.ActionLinkRenderer";
   public static final String COMPONENT_ACTIONLINK = "org.alfresco.faces.ActionLink";
   public static final String COMPONENT_PERMISSIONEVAL = "org.alfresco.faces.PermissionEvaluator";
   public static final String COMPONENT_ACTIONEVAL = "org.alfresco.faces.ActionInstanceEvaluator";
   
   public final static Class ACTION_CLASS_ARGS[] = {javax.faces.event.ActionEvent.class};

   /**
    * @see javax.faces.component.UIComponent#getFamily()
    */
   public String getFamily()
   {
      return "org.alfresco.faces.Controls";
   }
   
   /**
    * @see javax.faces.component.StateHolder#restoreState(javax.faces.context.FacesContext, java.lang.Object)
    */
   public void restoreState(FacesContext context, Object state)
   {
      Object values[] = (Object[])state;
      // standard component attributes are restored by the super class
      super.restoreState(context, values[0]);
      this.value = (String)values[1];
      this.showLink = (Boolean)values[2];
      this.verticalSpacing = (Integer)values[3];
   }
   
   /**
    * @see javax.faces.component.StateHolder#saveState(javax.faces.context.FacesContext)
    */
   public Object saveState(FacesContext context)
   {
      Object values[] = new Object[4];
      // standard component attributes are saved by the super class
      values[0] = super.saveState(context);
      values[1] = this.value;
      values[2] = this.showLink;
      values[3] = this.verticalSpacing;
      return (values);
   }   

   /**
    * @see javax.faces.component.UIComponentBase#encodeBegin(javax.faces.context.FacesContext)
    */
   @SuppressWarnings("unchecked")
   public void encodeBegin(FacesContext context) throws IOException
   {
      if (isRendered() == false)
      {
         return;
      }
      
      if (logger.isDebugEnabled())
         logger.debug("encodeBegin() for <r:actions/> Id: " + getId() + " groupId: " + getValue());
      
      // put the context object into the requestMap so it is accessable
      // by any child component value binding expressions
      Map requestMap = getFacesContext().getExternalContext().getRequestMap();
      requestMap.put(ACTION_CONTEXT, getContext());
      
      if (getChildCount() != 0)
      {
         if (logger.isDebugEnabled())
            logger.debug("---already built component tree for actions.");
         return;
      }
      
      String groupId = getValue();
      if (groupId != null && groupId.length() != 0)
      {
         Config config = Application.getConfigService(context).getGlobalConfig();
         if (config != null)
         {
            // find the Actions specific config element
            ActionsConfigElement actionConfig =
               (ActionsConfigElement)config.getConfigElement(ActionsConfigElement.CONFIG_ELEMENT_ID);
            if (actionConfig != null)
            {
               // and lookup our ActionGroup by Id
               ActionGroup actionGroup = actionConfig.getActionGroup(groupId);
               if (actionGroup != null)
               {
                  // render the action group component tree
                  if (logger.isDebugEnabled())
                     logger.debug("-constructing ActionGroup: " + groupId);
                  buildActionGroup(context, actionConfig, actionGroup);
               }
               else
               {
                  logger.warn("Unable to find specified Action Group config ID: " + groupId);
               }
            }
         }
      }
   }
   
   /**
    * @see javax.faces.component.UIComponentBase#getRendersChildren()
    */
   public boolean getRendersChildren()
   {
      return true;
   }
   
   /**
    * @see javax.faces.component.UIComponentBase#encodeChildren(javax.faces.context.FacesContext)
    */
   public void encodeChildren(FacesContext context) throws IOException
   {
      ResponseWriter out = context.getResponseWriter();
      int verticalSpacing = getVerticalSpacing();
      if (verticalSpacing != 0)
      {
         out.write("<table cellspacing='");
         out.write(verticalSpacing);
         out.write("'");
         if (getAttributes().get(ATTR_STYLE) != null)
         {
            outputAttribute(out, getAttributes().get(ATTR_STYLE), ATTR_STYLE);
         }
         if (getAttributes().get(ATTR_STYLECLASS) != null)
         {
            outputAttribute(out, getAttributes().get(ATTR_STYLECLASS), "class");
         }
         out.write(">");
      }
      
      for (Iterator i=getChildren().iterator(); i.hasNext(); /**/)
      {
         UIComponent child = (UIComponent)i.next();
         Utils.encodeRecursive(context, child);
      }
      
      if (verticalSpacing != 0)
      {
         out.write("</table>");
      }
   }

   /**
    * @see javax.faces.component.UIComponentBase#encodeEnd(javax.faces.context.FacesContext)
    */
   public void encodeEnd(FacesContext context) throws IOException
   {
      if (logger.isDebugEnabled())
         logger.debug("encodeEnd() for <r:actions/> Id: " + getId());
      
      Map requestMap = getFacesContext().getExternalContext().getRequestMap();
      requestMap.remove(ACTION_CONTEXT);
   }
   
   /**
    * Build an action group as reusable UIActionLink components.
    * 
    * @param context
    * @param actionGroup
    */
   @SuppressWarnings("unchecked")
   private void buildActionGroup(FacesContext context, ActionsConfigElement config, ActionGroup actionGroup)
      throws IOException
   {
      javax.faces.application.Application facesApp = context.getApplication();
      ResourceBundle messages = Application.getBundle(context);
      
      // get overriding display attributes
      String style = (String)getAttributes().get(ATTR_STYLE);
      String styleClass = (String)getAttributes().get(ATTR_STYLECLASS);
      Boolean showLink = null;
      if (getAttributes().get(ATTR_SHOWLINK) != null)
      {
         showLink = (Boolean)getAttributes().get(ATTR_SHOWLINK);
      }
      
      // process each ActionDefinition in the order they were defined
      for (String actionId : actionGroup)
      {
         if (logger.isDebugEnabled())
            logger.debug("---processing ActionDefinition: " + actionId);
         
         ActionDefinition actionDef = config.getActionDefinition(actionId);
         if (actionDef == null)
         {
            throw new AlfrescoRuntimeException("Unable to find configured ActionDefinition Id: " + actionId);
         }
         
         UIComponent currentParent = this;  
         
         // build a permissions evaluator component to wrap the actionlink 
         PermissionEvaluator permEval = null;
         List<String> allow = actionDef.getAllowPermissions();
         if (allow != null && allow.size() != 0)
         {
            // found some permissions to test
            permEval = (PermissionEvaluator)facesApp.createComponent(COMPONENT_PERMISSIONEVAL);
            String condition = allow.get(0);
            if (allow.size() != 1)
            {
               for (int i=1; i<allow.size(); i++)
               {
                  condition += "," + allow.get(i);
               }
            }
            permEval.setAllow(condition);
         }
         List<String> deny = actionDef.getDenyPermissions();
         if (deny != null && deny.size() != 0)
         {
            if (permEval == null)
            {
               permEval = (PermissionEvaluator)facesApp.createComponent(COMPONENT_PERMISSIONEVAL);
            }
            String condition = deny.get(0);
            if (deny.size() != 1)
            {
               for (int i=1; i<deny.size(); i++)
               {
                  condition += "," + deny.get(i);
               }
            }
            permEval.setDeny(condition);
         }
         if (permEval != null)
         {
            // add the permission evaluator component and walk down the hierarchy
            permEval.setId(createUniqueId());
            permEval.setValueBinding(ATTR_VALUE, facesApp.createValueBinding("#{" + ACTION_CONTEXT + "}"));
            if (logger.isDebugEnabled())
               logger.debug("-----adding PermissionEvaluator to action");
            currentParent.getChildren().add(permEval);
            currentParent = permEval;
         }
         
         // now prepare any code based evaluators that may be present
         if (actionDef.Evaluator != null)
         {
            ActionInstanceEvaluator evaluator =
               (ActionInstanceEvaluator)facesApp.createComponent(COMPONENT_ACTIONEVAL);
            evaluator.setId(createUniqueId());
            evaluator.setEvaluator(actionDef.Evaluator);
            evaluator.setValueBinding(ATTR_VALUE, facesApp.createValueBinding("#{" + ACTION_CONTEXT + "}"));
            
            // add the action evaluator component and walk down the hiearchy
            if (logger.isDebugEnabled())
               logger.debug("-----adding ActionEvaluator to action"); 
            currentParent.getChildren().add(evaluator);
            currentParent = evaluator;
         }
         
         // now build the UIActionLink component for this action
         UIActionLink control = (UIActionLink)facesApp.createComponent(COMPONENT_ACTIONLINK);
         
         control.setRendererType(RENDERER_ACTIONLINK);
         control.setId(actionDef.getId() + createUniqueId());
         
         if (actionDef.Action != null)
         {
            if (UIComponentTagUtils.isValueReference(actionDef.Action))
            {
               control.setAction(facesApp.createMethodBinding(actionDef.Action, null));
            }
            else
            {
               control.setAction(new ConstantMethodBinding(actionDef.Action));
            }
         }
         if (actionDef.ActionListener != null)
         {
            control.setActionListener(facesApp.createMethodBinding(actionDef.ActionListener, ACTION_CLASS_ARGS));
         }
         
         if (style != null)
         {
            control.getAttributes().put(ATTR_STYLE, style);
         }
         else if (actionDef.Style != null)
         {
            control.getAttributes().put(ATTR_STYLE, actionDef.Style);
         }
         if (styleClass != null)
         {
            control.getAttributes().put(ATTR_STYLECLASS, styleClass);
         }
         else if (actionDef.StyleClass != null)
         {
            control.getAttributes().put(ATTR_STYLECLASS, actionDef.StyleClass);
         }
         if (showLink != null)
         {
            control.setShowLink(showLink.booleanValue());
         }
         else
         {
            control.setShowLink(actionDef.ShowLink);
         }
         
         if (actionDef.Onclick != null)
         {
            if (UIComponentTagUtils.isValueReference(actionDef.Onclick))
            {
               control.setValueBinding("onclick", facesApp.createValueBinding(actionDef.Onclick));
            }
            else
            {
               control.setOnclick(actionDef.Onclick);
            }
         }
         
         if (actionDef.Href != null)
         {
            if (UIComponentTagUtils.isValueReference(actionDef.Href))
            {
               control.setValueBinding("href", facesApp.createValueBinding(actionDef.Href));
            }
            else
            {
               control.setHref(actionDef.Href);
            }
         }
         else if (actionDef.Script != null && actionDef.Script.length() != 0)
         {
            // found a script reference - may be a Path or a NodeRef
            StringBuilder scriptHref = new StringBuilder(100);
            scriptHref.append("/command/script/execute");
            if (actionDef.Script.charAt(0) == '/')
            {
               // found a Path - encode it as a URL argument
               scriptHref.append("?scriptPath=");
               scriptHref.append(Utils.replace(URLEncoder.encode(actionDef.Script, "UTF-8"), "+", "%20"));
            }
            else
            {
               // found a NodeRef string, encode as URL elements
               NodeRef ref = new NodeRef(actionDef.Script);
               scriptHref.append('/').append(ref.getStoreRef().getProtocol())
                         .append('/').append(ref.getStoreRef().getIdentifier())
                         .append('/').append(ref.getId());
            }
            // set the full script execution URL as the href for the control
            control.setHref(scriptHref.toString());
         }
         
         control.setTarget(actionDef.Target);
         control.setImage(actionDef.Image);
         
         if (actionDef.TooltipMsg != null)
         {
            control.setTooltip(messages.getString(actionDef.TooltipMsg));
         }
         else if (actionDef.Tooltip != null)
         {
            if (UIComponentTagUtils.isValueReference(actionDef.Tooltip))
            {
               control.setValueBinding("tooltip", facesApp.createValueBinding(actionDef.Tooltip));
            }
            else
            {
               control.setValue(actionDef.Tooltip);
            }
         }
         if (actionDef.LabelMsg != null)
         {
            control.setValue(messages.getString(actionDef.LabelMsg));
         }
         else if (actionDef.Label != null)
         {
            if (UIComponentTagUtils.isValueReference(actionDef.Label))
            {
               control.setValueBinding("value", facesApp.createValueBinding(actionDef.Label));
            }
            else
            {
               control.setValue(actionDef.Label);
            }
         }
         
         // build any child params <f:param> components that are needed.
         Map<String, String> params = actionDef.getParams();
         if (params != null)
         {
            for (String name : params.keySet())
            {
               UIParameter param =
                 (UIParameter)facesApp.createComponent(ComponentConstants.JAVAX_FACES_PARAMETER);
               param.setId(createUniqueId());
               param.setName(name);
               String value = params.get(name);
               if (UIComponentTagUtils.isValueReference(value))
               {
                  param.setValueBinding(ATTR_VALUE, facesApp.createValueBinding(value));
               }
               else
               {
                  param.setValue(value);
               }
               control.getChildren().add(param);
            }
         }
         
         if (logger.isDebugEnabled())
            logger.debug("-----adding UIActionLink component for: " + actionId);
         currentParent.getChildren().add(control);
      }
   }

   
   // ------------------------------------------------------------------------------
   // Strongly typed component property accessors 
   
   /**
    * Get the value (for this component the value is the ID of an Action Group config block)
    *
    * @return the value
    */
   public String getValue()
   {
      if (this.value == null)
      {
         ValueBinding vb = getValueBinding(ATTR_VALUE);
         if (vb != null)
         {
            this.value = (String)vb.getValue(getFacesContext());
         }
      }
      return this.value;
   }

   /**
    * Set the value (for this component the value is the ID of an Action Group config block)
    *
    * @param value     the value
    */
   public void setValue(String value)
   {
      this.value = value;
   }
   
   /**
    * Get the object that forms the context for this group of actions
    *
    * @return the context
    */
   public Object getContext()
   {
      ValueBinding vb = getValueBinding("context");
      if (vb != null)
      {
         this.context = vb.getValue(getFacesContext());
      }
      
      return this.context;
   }

   /**
    * Set the the object that forms the context for this group of actions
    *
    * @param context     the context
    */
   public void setContext(Object context)
   {
      this.context = context;
   }
   
   /**
    * Get whether to show the link as well as the image if specified
    * 
    * @return true to show the link as well as the image if specified
    */
   public boolean getShowLink()
   {
      ValueBinding vb = getValueBinding(ATTR_SHOWLINK);
      if (vb != null)
      {
         this.showLink = (Boolean)vb.getValue(getFacesContext());
      }
      
      if (this.showLink != null)
      {
         return this.showLink.booleanValue();
      }
      else
      {
         // return default
         return true;
      }
   }
   
   /**
    * Set whether to show the link as well as the image if specified
    * 
    * @param showLink      Whether to show the link as well as the image if specified
    */
   public void setShowLink(boolean showLink)
   {
      this.showLink = Boolean.valueOf(showLink);
   }
   
   /**
    * @return Returns the vertical spacing value in pixels or zero if not set.
    */
   public int getVerticalSpacing()
   {
      ValueBinding vb = getValueBinding("verticalSpacing");
      if (vb != null)
      {
         this.verticalSpacing = (Integer)vb.getValue(getFacesContext());
      }
      
      if (this.verticalSpacing != null)
      {
         return this.verticalSpacing;
      }
      else
      {
         return 0;
      }
   }

   /**
    * @param verticalSpacing The verticalSpacing to set.
    */
   public void setVerticalSpacing(int verticalSpacing)
   {
      this.verticalSpacing = verticalSpacing;
   }
   
   /**
    * @return a unique ID for a JSF component
    */
   private static String createUniqueId()
   {
      return "_id_" + Short.toString(++id);
   }
   
   
   // ------------------------------------------------------------------------------
   // Private data
   
   /** True to show the link as well as the image if specified */
   private Boolean showLink = null;
   
   /** For this component the value is the ID of an Action Group config block */
   private String value = null;
   
   /** The context object for the action group */
   private Object context = null;
   
   /** Vertical layout spacing */
   private Integer verticalSpacing = null;
   
   private static short id = 0;
}
