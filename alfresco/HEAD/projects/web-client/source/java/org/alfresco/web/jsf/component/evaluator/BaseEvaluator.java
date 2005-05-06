/*
 * Created on 07-Apr-2005
 */
package org.alfresco.web.jsf.component.evaluator;

import java.io.IOException;

import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;

import org.apache.log4j.Logger;

import org.alfresco.web.jsf.component.SelfRenderingComponent;

/**
 * @author kevinr
 */
public abstract class BaseEvaluator extends SelfRenderingComponent
{
   /**
    * @see javax.faces.component.UIComponent#getFamily()
    */
   public final String getFamily()
   {
      return "awc.faces.evaluators";
   }

   /**
    * @see javax.faces.component.UIComponentBase#getRendersChildren()
    */
   public final boolean getRendersChildren()
   {
      return !evaluate();
   }

   /**
    * @see javax.faces.component.UIComponentBase#encodeBegin(javax.faces.context.FacesContext)
    */
   public final void encodeBegin(FacesContext context) throws IOException
   {
      // no output for this component
   }

   /**
    * @see javax.faces.component.UIComponentBase#encodeChildren(javax.faces.context.FacesContext)
    */
   public final void encodeChildren(FacesContext context) throws IOException
   {
      // if this is called, then the evaluate returned false which means
      // the child components show not be allowed to render themselves
   }

   /**
    * @see javax.faces.component.UIComponentBase#encodeEnd(javax.faces.context.FacesContext)
    */
   public final void encodeEnd(FacesContext context) throws IOException
   {
      // no output for this component
   }
   
   /**
    * Get the value for this component to be evaluated against
    *
    * @return the value for this component to be evaluated against
    */
   public Object getValue()
   {
      ValueBinding vb = getValueBinding("value");
      if (vb != null)
      {
         this.value = vb.getValue(getFacesContext());
      }
      
      return this.value;
   }

   /**
    * Set the value for this component to be evaluated against
    *
    * @param value     the value for this component to be evaluated against
    */
   public void setValue(Object value)
   {
      this.value = value;
   }
   
   /**
    * Evaluate against the component attributes. Return true to allow the inner
    * components to render, false to hide them during rendering.
    * 
    * @return true to allow rendering of child components, false otherwise
    */
   public abstract boolean evaluate();
   
   
   protected static Logger s_logger = Logger.getLogger(BaseEvaluator.class);
   
   /** the value to be evaluated against */
   private Object value;
}
