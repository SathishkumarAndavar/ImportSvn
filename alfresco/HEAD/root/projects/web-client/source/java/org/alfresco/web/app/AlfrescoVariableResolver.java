package org.alfresco.web.app;

import javax.faces.context.FacesContext;
import javax.faces.el.EvaluationException;
import javax.faces.el.VariableResolver;

import org.alfresco.config.Config;
import org.alfresco.config.ConfigService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.jsf.DelegatingVariableResolver;

/**
 * JSF VariableResolver that first delegates to the Spring JSF variable 
 * resolver. The sole purpose of this variable resolver is to look out
 * for the <code>Container</code> variable. If this variable is encountered
 * the current viewId is examined. If the current viewId matches the 
 * configured dialog or wizard container the appropriate manager object is
 * returned i.e. DialogManager or WizardManager.
 * 
 * <p>Configure this resolver in your <code>faces-config.xml</code> file as follows:
 *
 * <pre>
 * &lt;application&gt;
 *   ...
 *   &lt;variable-resolver&gt;org.alfresco.web.app.AlfrescoVariableResolver&lt;/variable-resolver&gt;
 * &lt;/application&gt;</pre>
 * 
 * @see org.alfresco.web.bean.dialog.DialogManager
 * @see org.alfresco.web.bean.wizard.WizardManager
 * @author gavinc
 */
public class AlfrescoVariableResolver extends DelegatingVariableResolver
{
   protected String dialogContainer = null;
   protected String wizardContainer = null;
   
   private static final String CONTAINER = "Container";
   
   private static final Log logger = LogFactory.getLog(AlfrescoVariableResolver.class);
   
   /**
    * Creates a new VariableResolver.
    * 
    * @param originalVariableResolver The original variable resolver
    */
   public AlfrescoVariableResolver(VariableResolver originalVariableResolver)
   {
      super(originalVariableResolver);
   }
   
   /**
    * Resolves the variable with the given name.
    * <p>
    * This implementation will first delegate to the Spring variable resolver.
    * If the variable is not found by the Spring resolver and the variable name
    * is <code>Container</code> the current viewId is examined.
    * If the current viewId matches the configured dialog or wizard container 
    * the appropriate manager object is returned i.e. DialogManager or WizardManager.
    * 
    * @param context FacesContext
    * @param name The name of the variable to resolve
    */
   public Object resolveVariable(FacesContext context, String name) 
      throws EvaluationException 
   {
      Object variable = super.resolveVariable(context, name);
      
      if (variable == null)
      {
         // if the variable was not resolved see if the name is "Container"
         if (name.equals(CONTAINER))
         {
            // get the current view id and the configured dialog and wizard 
            // container pages
            String viewId = context.getViewRoot().getViewId();
            String dialogContainer = getDialogContainer(context);
            String wizardContainer = getWizardContainer(context);
            
            // see if we are currently in a wizard or a dialog
            if (viewId.equals(dialogContainer))
            {
               variable = Application.getDialogManager();
            }
            else if (viewId.equals(wizardContainer))
            {
               variable = Application.getWizardManager();   
            }
            
            if (variable != null && logger.isDebugEnabled())
            {
               logger.debug("Resolved 'Container' variable to: " + variable);
            }
         }
      }
      
      return variable;
   }
   
   /**
    * Retrieves the configured dialog container page
    * 
    * @param context FacesContext
    * @return The container page
    */
   protected String getDialogContainer(FacesContext context)
   {
      if (this.dialogContainer == null)
      {
         ConfigService configSvc = Application.getConfigService(context);
         Config globalConfig = configSvc.getGlobalConfig();
         
         if (globalConfig != null)
         {
            this.dialogContainer = globalConfig.getConfigElement("dialog-container").getValue();
         }
      }
      
      return this.dialogContainer;
   }
   
   /**
    * Retrieves the configured wizard container page
    * 
    * @param context FacesContext
    * @return The container page
    */
   protected String getWizardContainer(FacesContext context)
   {
      if (this.wizardContainer == null)
      {
         ConfigService configSvc = Application.getConfigService(context);
         Config globalConfig = configSvc.getGlobalConfig();
         
         if (globalConfig != null)
         {
            this.wizardContainer = globalConfig.getConfigElement("wizard-container").getValue();
         }
      }
      
      return this.wizardContainer;
   }
}
