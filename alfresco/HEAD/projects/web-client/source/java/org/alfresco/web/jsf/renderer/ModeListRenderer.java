/*
 * Created on 13-Apr-2005
 */
package org.alfresco.web.jsf.renderer;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIForm;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.FacesEvent;

import org.alfresco.web.jsf.Utils;
import org.alfresco.web.jsf.component.UIModeList;
import org.alfresco.web.jsf.component.UIModeListItem;
import org.alfresco.web.jsf.component.UIBreadcrumb.BreadcrumbEvent;

/**
 * @author kevinr
 */
public class ModeListRenderer extends BaseRenderer
{
   // ------------------------------------------------------------------------------
   // Renderer implemenation
   
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
         // found a new selected value for this ModeList
         // queue an event to represent the change
         // TODO: NOTE: The value object is passed in as a String here - is this a problem?
         //             As the 'value' field for a ModeListItem can contain Object...  
         Object selectedValue = value.substring(component.getClientId(context).length() + 1);
         UIModeList.ModeListItemSelectedEvent event = new UIModeList.ModeListItemSelectedEvent(component, selectedValue);
         component.queueEvent(event);
      }
   }

   /**
    * @see javax.faces.render.Renderer#encodeBegin(javax.faces.context.FacesContext, javax.faces.component.UIComponent)
    */
   public void encodeBegin(FacesContext context, UIComponent component) throws IOException
   {
      if (component.isRendered() == false)
      {
         return;
      }
      
      UIModeList list = (UIModeList)component;
      
      ResponseWriter out = context.getResponseWriter();

      // start outer table container the list items
      Map attrs = list.getAttributes();
      out.write("<table cellspacing=1 cellpadding=0");
      outputAttribute(out, attrs.get("styleClass"), "class");
      outputAttribute(out, attrs.get("style"), "style");
      outputAttribute(out, attrs.get("width"), "width");
      out.write('>');
      
      // horizontal rendering outputs a single row with each item as a column cell
      if (list.isHorizontal() == true)
      {
         out.write("<tr>");
      }
      
      // output title row if present
      if (attrs.get("label") != null)
      {
         // each row is an inner table with a single row and 2 columns
         // first column contains an icon if present, second column contains text
         if (list.isHorizontal() == false)
         {
            out.write("<tr>");
         }
         
         out.write("<td><table cellpadding=0 width=100%");
         outputAttribute(out, attrs.get("itemSpacing"), "cellspacing");
         out.write("><tr>");
         
         // output icon column
         if (list.getIconColumnWidth() != 0)
         {
            out.write("<td");
            outputAttribute(out, list.getIconColumnWidth(), "width");
            out.write("></td>");
         }
         
         // output title label
         out.write("<td><span");
         outputAttribute(out, attrs.get("labelStyle"), "style");
         outputAttribute(out, attrs.get("labelStyleClass"), "class");
         out.write('>');
         out.write(Utils.encode((String)attrs.get("label")));
         out.write("</span></td></tr></table></td>");
         
         if (list.isHorizontal() == false)
         {
            out.write("</tr>");
         }
      }
   }
   
   /**
    * @see javax.faces.render.Renderer#encodeChildren(javax.faces.context.FacesContext, javax.faces.component.UIComponent)
    */
   public void encodeChildren(FacesContext context, UIComponent component) throws IOException
   {
      if (component.isRendered() == false)
      {
         return;
      }
      
      UIModeList list = (UIModeList)component;
      Map attrs = list.getAttributes();
      
      ResponseWriter out = context.getResponseWriter();
      
      // get the child components
      for (Iterator i=list.getChildren().iterator(); i.hasNext(); /**/)
      {
         UIComponent child = (UIComponent)i.next();
         if (child instanceof UIModeListItem && child.isRendered() == true)
         {
            // found a valid UIModeListItem child to render
            UIModeListItem item = (UIModeListItem)child;
            
            // each row is an inner table with a single row and 2 columns
            // first column contains an icon if present, second column contains text
            if (list.isHorizontal() == false)
            {
               out.write("<tr>");
            }
            
            out.write("<td><table cellpadding=0 width=100%");
            outputAttribute(out, attrs.get("itemSpacing"), "cellspacing");
            
            // if selected value render different style for the item
            if (item.getValue().equals(list.getValue()))
            {
               outputAttribute(out, attrs.get("selectedStyleClass"), "class");
               outputAttribute(out, attrs.get("selectedStyle"), "style");
            }
            else
            {
               outputAttribute(out, attrs.get("itemStyleClass"), "class");
               outputAttribute(out, attrs.get("itemStyle"), "style");
            }
            out.write("><tr>");
            
            // output icon column
            if (list.getIconColumnWidth() != 0)
            {
               out.write("<td");
               outputAttribute(out, list.getIconColumnWidth(), "width");
               out.write(">");
               
               String image = (String)child.getAttributes().get("image"); 
               if (image != null)
               {
                  out.write( Utils.buildImageTag(context, image, (String)child.getAttributes().get("tooltip")) );
               }
               
               out.write("</td>");
            }
            
            // output item link
            out.write("<td>");
            out.write("<a href='#' onclick=\"");
            // generate javascript to submit the value of the child component
            String value = list.getClientId(context) + NamingContainer.SEPARATOR_CHAR + (String)child.getAttributes().get("value");
            out.write(Utils.generateFormSubmit(context, list, getHiddenFieldName(context, list), value));
            out.write('"');
            
            // render style for the item link
            if (item.getValue().equals(list.getValue()))
            {
               outputAttribute(out, attrs.get("selectedLinkStyleClass"), "class");
               outputAttribute(out, attrs.get("selectedLinkStyle"), "style");
            }
            else
            {
               outputAttribute(out, attrs.get("itemLinkStyleClass"), "class");
               outputAttribute(out, attrs.get("itemLinkStyle"), "style");
            }
            
            outputAttribute(out, child.getAttributes().get("tooltip"), "title");
            out.write('>');
            out.write(Utils.encode((String)child.getAttributes().get("label")));
            out.write("</td></tr></table></td>");
            
            if (list.isHorizontal() == false)
            {
               out.write("</tr>");
            }
         }
      }
   }

   /**
    * @see javax.faces.render.Renderer#encodeEnd(javax.faces.context.FacesContext, javax.faces.component.UIComponent)
    */
   public void encodeEnd(FacesContext context, UIComponent component) throws IOException
   {
      if (component.isRendered() == false)
      {
         return;
      }
      
      ResponseWriter out = context.getResponseWriter();
      
      // end outer table
      if (((UIModeList)component).isHorizontal() == true)
      {
         out.write("</tr>");
      }
      out.write("</table>");
   }

   /**
    * @see javax.faces.render.Renderer#getRendersChildren()
    */
   public boolean getRendersChildren()
   {
      return true;
   }
   
   /**
    * We use a hidden field name based on the parent form component Id and
    * the string "modelist" to give a hidden field name that can be shared by all
    * ModeList components within a single UIForm component.
    * 
    * @return hidden field name
    */
   private static String getHiddenFieldName(FacesContext context, UIComponent component)
   {
      UIForm form = Utils.getParentForm(context, component);
      return form.getClientId(context) + NamingContainer.SEPARATOR_CHAR + "modelist";
   }
}
