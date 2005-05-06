/*
 * Created on 13-Apr-2005
 */
package org.alfresco.web.jsf.tag;

import javax.faces.component.UIComponent;

/**
 * @author kevinr
 */
public class ModeListItemTag extends BaseComponentTag
{
   /**
    * @see javax.faces.webapp.UIComponentTag#getComponentType()
    */
   public String getComponentType()
   {
      return "awc.faces.ModeListItem";
   }

   /**
    * @see javax.faces.webapp.UIComponentTag#getRendererType()
    */
   public String getRendererType()
   {
      // this component is rendered by its parent container
      return null;
   }

   /**
    * @see javax.faces.webapp.UIComponentTag#setProperties(javax.faces.component.UIComponent)
    */
   protected void setProperties(UIComponent component)
   {
      super.setProperties(component);
      setStringProperty(component, "tooltip", this.tooltip);
      setStringProperty(component, "label", this.label);
      setStringProperty(component, "image", this.image);
      setStringProperty(component, "value", this.value);
   }
   
   /**
    * @see javax.servlet.jsp.tagext.Tag#release()
    */
   public void release()
   {
      super.release();
      this.tooltip = null;
      this.label = null;
      this.image = null;
      this.value = null;
   }
   
   /**
    * Set the tooltip
    *
    * @param tooltip     the tooltip
    */
   public void setTooltip(String tooltip)
   {
      this.tooltip = tooltip;
   }

   /**
    * Set the label
    *
    * @param label     the label
    */
   public void setLabel(String label)
   {
      this.label = label;
   }

   /**
    * Set the image
    *
    * @param image     the image
    */
   public void setImage(String image)
   {
      this.image = image;
   }

   /**
    * Set the value to be selected initially 
    *
    * @param value     the value to be selected initially
    */
   public void setValue(String value)
   {
      this.value = value;
   }
   

   /** the tooltip */
   private String tooltip;

   /** the label */
   private String label;

   /** the image */
   private String image;

   /** the value to be selected initially */
   private String value;
}
