package org.alfresco.web.bean.actions.handlers;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.executer.CheckOutActionExecuter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.bean.wizard.IWizardBean;

/**
 * Action handler implementation for the "check-out" action.
 * 
 * @author gavinc
 */
public class CheckOutHandler extends BaseActionHandler
{
   public String getJSPPath()
   {
      return getJSPPath(CheckOutActionExecuter.NAME);
   }

   public void prepareForSave(Map<String, Serializable> actionProps,
         Map<String, Serializable> repoProps)
   {
      // specify the location the checked out working copy should go
      // add the destination space id to the action properties
      NodeRef destNodeRef = (NodeRef)actionProps.get(PROP_DESTINATION);
      repoProps.put(CheckOutActionExecuter.PARAM_DESTINATION_FOLDER, destNodeRef);
      
      // add the type and name of the association to create when the 
      // check out is performed
      repoProps.put(CheckOutActionExecuter.PARAM_ASSOC_TYPE_QNAME, 
            ContentModel.ASSOC_CONTAINS);
      repoProps.put(CheckOutActionExecuter.PARAM_ASSOC_QNAME, 
            QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "checkout"));
   }

   public void prepareForEdit(Map<String, Serializable> actionProps,
         Map<String, Serializable> repoProps)
   {
      NodeRef destNodeRef = (NodeRef)repoProps.get(CheckOutActionExecuter.PARAM_DESTINATION_FOLDER);
      actionProps.put(PROP_DESTINATION, destNodeRef);
   }

   public String generateSummary(FacesContext context, IWizardBean wizard,
         Map<String, Serializable> actionProps)
   {
      NodeRef space = (NodeRef)actionProps.get(PROP_DESTINATION);
      String spaceName = Repository.getNameForNode(
            Repository.getServiceRegistry(context).getNodeService(), space);
      
      return MessageFormat.format(Application.getMessage(context, "action_check_out"),
            new Object[] {spaceName});
   }
}
