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
package org.alfresco.web.config;

import java.util.Iterator;

import org.alfresco.config.ConfigElement;
import org.alfresco.config.ConfigException;
import org.alfresco.config.xml.elementreader.ConfigElementReader;
import org.alfresco.web.config.WizardsConfigElement.ConditionalPageConfig;
import org.alfresco.web.config.WizardsConfigElement.PageConfig;
import org.alfresco.web.config.WizardsConfigElement.StepConfig;
import org.dom4j.Element;

/**
 * Custom element reader to parse config for wizards
 * 
 * @author gavinc
 */
public class WizardsElementReader implements ConfigElementReader
{
   public static final String ELEMENT_WIZARDS = "wizards";
   public static final String ELEMENT_WIZARD = "wizard";
   public static final String ELEMENT_STEP = "step";
   public static final String ELEMENT_PAGE = "page";
   public static final String ELEMENT_CONDITION = "condition";
   
   public static final String ATTR_NAME = "name";
   public static final String ATTR_MANAGED_BEAN = "managed-bean";
   public static final String ATTR_ACTIONS_CONFIG_ID = "actions-config-id";
   public static final String ATTR_ICON = "icon";
   public static final String ATTR_TITLE = "title";
   public static final String ATTR_TITLE_ID = "title-id";
   public static final String ATTR_DESCRIPTION = "description";
   public static final String ATTR_DESCRIPTION_ID = "description-id";
   public static final String ATTR_INSTRUCTION = "instruction";
   public static final String ATTR_INSTRUCTION_ID = "instruction-id";
   public static final String ATTR_ERROR_MSG_ID = "error-message-id";
   public static final String ATTR_IF = "if";
   public static final String ATTR_PATH = "path";
   
   /**
    * @see org.alfresco.config.xml.elementreader.ConfigElementReader#parse(org.dom4j.Element)
    */
   public ConfigElement parse(Element element)
   {
      WizardsConfigElement configElement = null;
      
      if (element != null)
      {
         String elementName = element.getName();
         if (elementName.equals(ELEMENT_WIZARDS) == false)
         {
            throw new ConfigException("WizardsElementReader can only parse " +
                  ELEMENT_WIZARDS + "elements, the element passed was '" + 
                  elementName + "'");
         }
         
         configElement = new WizardsConfigElement();
         
         // go through the items to show
         Iterator<Element> items = element.elementIterator(ELEMENT_WIZARD);
         while (items.hasNext())
         {
            Element wizard = items.next();
            
            String name = wizard.attributeValue(ATTR_NAME);
            String bean = wizard.attributeValue(ATTR_MANAGED_BEAN);
            String actions = wizard.attributeValue(ATTR_ACTIONS_CONFIG_ID);
            String icon = wizard.attributeValue(ATTR_ICON);
            String title = wizard.attributeValue(ATTR_TITLE);
            String titleId = wizard.attributeValue(ATTR_TITLE_ID);
            String description = wizard.attributeValue(ATTR_DESCRIPTION);
            String descriptionId = wizard.attributeValue(ATTR_DESCRIPTION_ID);
            String errorMsgId = wizard.attributeValue(ATTR_ERROR_MSG_ID);
            
            // create the wizard config object
            WizardsConfigElement.WizardConfig wizardCfg = new WizardsConfigElement.WizardConfig(
                  name, bean, actions, icon, title, titleId, description, descriptionId, errorMsgId);
            
            Iterator<Element> steps = wizard.elementIterator(ELEMENT_STEP);
            while (steps.hasNext())
            {
               StepConfig stepCfg = parseStep(steps.next());
               wizardCfg.addStep(stepCfg);
            }
            
            configElement.addWizard(wizardCfg);
         }
      }
      
      return configElement;
   }

   /**
    * Parses the given element which represents a step in a wizard
    * 
    * @param step The Element representing the step
    * @return A StepConfig object
    */
   protected StepConfig parseStep(Element step)
   {
      // get the name of the step and create the config object
      String stepName = step.attributeValue(ATTR_NAME);
      String stepTitle = step.attributeValue(ATTR_TITLE);
      String stepTitleId = step.attributeValue(ATTR_TITLE_ID);
      String stepDescription = step.attributeValue(ATTR_DESCRIPTION);
      String stepDescriptionId = step.attributeValue(ATTR_DESCRIPTION_ID);
      StepConfig stepCfg = new StepConfig(stepName, stepTitle, stepTitleId,
            stepDescription, stepDescriptionId);
      
      // find and parse the default page
      Element defaultPageElem = step.element(ELEMENT_PAGE);
      if (defaultPageElem != null)
      {
         String path = defaultPageElem.attributeValue(ATTR_PATH);
         String title = defaultPageElem.attributeValue(ATTR_TITLE);
         String titleId = defaultPageElem.attributeValue(ATTR_TITLE_ID);
         String description = defaultPageElem.attributeValue(ATTR_DESCRIPTION);
         String descriptionId = defaultPageElem.attributeValue(ATTR_DESCRIPTION_ID);
         String instruction = defaultPageElem.attributeValue(ATTR_INSTRUCTION);
         String instructionId = defaultPageElem.attributeValue(ATTR_INSTRUCTION_ID);
      
         // create and set the page config on the step
         stepCfg.setDefaultPage(new PageConfig(path, title, titleId, description,
               descriptionId, instruction, instructionId));
      }
      
      // find and parse any conditions that are present
      Iterator<Element> conditions = step.elementIterator(ELEMENT_CONDITION);
      while (conditions.hasNext())
      {
         Element conditionElem = conditions.next();
         
         String ifAttr = conditionElem.attributeValue(ATTR_IF);
         Element conditionalPageElem = conditionElem.element(ELEMENT_PAGE);
         if (conditionalPageElem == null)
         {
            throw new ConfigException("A condition in step '" + stepCfg.getName() +
                  "' does not have a containing <page> element");
         }
         
         String path = conditionalPageElem.attributeValue(ATTR_PATH);
         String title = conditionalPageElem.attributeValue(ATTR_TITLE);
         String titleId = conditionalPageElem.attributeValue(ATTR_TITLE_ID);
         String description = conditionalPageElem.attributeValue(ATTR_DESCRIPTION);
         String descriptionId = conditionalPageElem.attributeValue(ATTR_DESCRIPTION_ID);
         String instruction = conditionalPageElem.attributeValue(ATTR_INSTRUCTION);
         String instructionId = conditionalPageElem.attributeValue(ATTR_INSTRUCTION_ID);
         
         // create and add the page to the step
         stepCfg.addConditionalPage(new ConditionalPageConfig(path, ifAttr, title,
               titleId, description, descriptionId, instruction, instructionId));
      }
      
      return stepCfg;
   }
}
