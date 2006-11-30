<%--
  Copyright (C) 2005 Alfresco, Inc.
 
  Licensed under the Mozilla Public License version 1.1 
  with a permitted attribution clause. You may obtain a
  copy of the License at
 
    http://www.alfresco.org/legal/license.txt
 
  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
  either express or implied. See the License for the specific
  language governing permissions and limitations under the
  License.
--%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a" %>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r" %>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8" %>
<%@ page isELIgnored="false" %>

<f:verbatim>
<script type="text/javascript" src="<%=request.getContextPath()%>/scripts/validation.js"> </script>

<script type="text/javascript">
   var finishButtonPressed = false;
   window.onload = pageLoaded;
   
   function pageLoaded()
   {
      document.getElementById("wizard:wizard-body:name").focus();
      document.getElementById("wizard").onsubmit = validate;
      document.getElementById("wizard:next-button").onclick = function() {finishButtonPressed = true; clear_wizard();}
      document.getElementById("wizard:finish-button").onclick = function() {finishButtonPressed = true; clear_wizard();}
      checkButtonState();
   }

   function checkButtonState()
   {
      <%--
      || (document.getElementById("wizard:wizard-body:saveAsTemplate").checked && 
          document.getElementById("wizard:wizard-body:templateName").value.length == 0) )
      --%>
      if (document.getElementById("wizard:wizard-body:name").value.length == 0)
      {
         document.getElementById("wizard:next-button").disabled = true;
         document.getElementById("wizard:finish-button").disabled = true;
      }
      else
      {
         document.getElementById("wizard:next-button").disabled = false;
         document.getElementById("wizard:finish-button").disabled = false;
      }
   }
   
   function toggleTemplateName()
   {
      document.getElementById('wizard:wizard-body:templateName').disabled = 
         !document.getElementById('wizard:wizard-body:saveAsTemplate').checked;
      
      if (document.getElementById('wizard:wizard-body:templateName').disabled == false)
      {
         document.getElementById('wizard:wizard-body:templateName').focus();
      }
      
      checkButtonState();
   }
   
   function validate()
   {
      if (finishButtonPressed)
      {
         finishButtonPressed = false;
         return validateName(document.getElementById("wizard:wizard-body:name"), 
                             '</f:verbatim><a:outputText value="#{msg.validation_invalid_character}" /><f:verbatim>',
                             true);
      }
      else
      {
         return true;
      }
   }
</script>

<table cellpadding="3" cellspacing="2" border="0" width="100%">
   <tr>
      <td colspan="3" class="wizardSectionHeading">
         </f:verbatim>
         <h:outputText value="#{msg.title_space_details}"/>
         <f:verbatim>
      </td>
   </tr>
   <tr>
      <td align="middle">
         </f:verbatim>
         <h:graphicImage value="/images/icons/required_field.gif" alt="Required Field" />
         <f:verbatim>
      </td>
      <td>
         </f:verbatim>
         <h:outputText value="#{msg.name}:"/>
         <f:verbatim>
      </td>
      <td width="85%">
         </f:verbatim>
         <h:inputText id="name" value="#{WizardManager.bean.name}" size="35" maxlength="1024"
                      onkeyup="javascript:checkButtonState();" />
         <f:verbatim>
      </td>
   </tr>
   <tr>
      <td></td>
      <td>
         </f:verbatim>
         <h:outputText value="#{msg.title}:" />
         <f:verbatim>
      </td>
      <td>
         </f:verbatim>
         <h:inputText id="title" value="#{WizardManager.bean.title}" size="35" maxlength="1024" />
         <f:verbatim>
      </td>
   </tr>
   <tr>
      <td></td>
      <td>
         </f:verbatim>
         <h:outputText value="#{msg.description}:"/>
         <f:verbatim>
      </td>
      <td>
         </f:verbatim>
         <h:inputText id="description" value="#{WizardManager.bean.description}" size="35" maxlength="1024" />
         <f:verbatim>
      </td>
   </tr>
   <tr>
      <td></td>
      <td>
         </f:verbatim>
         <h:outputText value="#{msg.icon}:"/>
         <f:verbatim>
      </td>
      <td>
         <table border="0" cellpadding="0" cellspacing="0"><tr><td>
         </f:verbatim>
         <a:imagePickerRadioPanel id="space-icon" columns="6" spacing="4" value="#{WizardManager.bean.icon}"
                                  panelBorder="blue" panelBgcolor="#D3E6FE">
            <a:listItems value="#{WizardManager.bean.icons}" />
         </a:imagePickerRadioPanel>
         <f:verbatim>
         </td></tr></table>
      </td>
   </tr>
   <%--
   <tr>
      <td colspan="2">
         <h:selectBooleanCheckbox id="saveAsTemplate" value="#{WizardManager.bean.saveAsTemplate}" 
            onclick="javascript:toggleTemplateName();" />&nbsp;
         <span style="vertical-align:20%"><h:outputText value="#{msg.save_as_template}"/></span>
      </td>
   </tr>
   <tr>
      <td align="middle"><h:outputText value="#{msg.name}"/>:</td>
      <td>
         <h:inputText id="templateName" value="#{WizardManager.bean.templateName}" 
                       size="35" disabled="#{!WizardManager.bean.saveAsTemplate}" 
                       onkeyup="javascript:checkButtonState();" maxlength="1024" />
                       &nbsp;*
      </td>
   </tr>
   --%>
</table>
</f:verbatim>