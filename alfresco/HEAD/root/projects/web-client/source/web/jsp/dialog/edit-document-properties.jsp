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
<%@ page import="org.alfresco.web.ui.common.PanelGenerator" %>

<r:page titleId="title_edit_doc_props">

<script language="JavaScript1.2">
   function checkButtonState()
   {
      if (document.getElementById("edit-doc-properties:file-name").value.length == 0 ||
          document.getElementById("edit-doc-properties:title").value.length == 0)
      {
         document.getElementById("edit-doc-properties:ok-button").disabled = true;
      }
      else
      {
         document.getElementById("edit-doc-properties:ok-button").disabled = false;
      }
   }
</script>

<f:view>
   
   <%-- load a bundle of properties with I18N strings --%>
   <f:loadBundle basename="alfresco.messages.webclient" var="msg"/>
   
   <h:form acceptCharset="UTF-8" id="edit-doc-properties">
   
   <%-- Main outer table --%>
   <table cellspacing="0" cellpadding="2">
      
      <%-- Title bar --%>
      <tr>
         <td colspan="2">
            <%@ include file="../parts/titlebar.jsp" %>
         </td>
      </tr>
      
      <%-- Main area --%>
      <tr valign="top">
         <%-- Shelf --%>
         <td>
            <%@ include file="../parts/shelf.jsp" %>
         </td>
         
         <%-- Work Area --%>
         <td width="100%">
            <table cellspacing="0" cellpadding="0" width="100%">
               <%-- Breadcrumb --%>
               <%@ include file="../parts/breadcrumb.jsp" %>
               
               <%-- Status and Actions --%>
               <tr>
                  <td style="background-image: url(<%=request.getContextPath()%>/images/parts/statuspanel_4.gif)" width="4"></td>
                  <td bgcolor="#EEEEEE">
                  
                     <%-- Status and Actions inner contents table --%>
                     <%-- Generally this consists of an icon, textual summary and actions for the current object --%>
                     <table cellspacing="4" cellpadding="0" width="100%">
                        <tr valign="top">
                           <td width="32">
                              <h:graphicImage id="wizard-logo" url="/images/icons/details_large.gif" />
                           </td>
                           <td>
                              <div class="mainSubTitle"><h:outputText value="#{NavigationBean.nodeProperties.name}" /></div>
                              <div class="mainTitle"><h:outputText value="#{msg.modify_props_of}" /> '<h:outputText value="#{BrowseBean.document.name}" />'</div>
                              <div class="mainSubText"><h:outputText value="#{msg.editdocument_description}" /></div>
                           </td>
                        </tr>
                     </table>
                     
                  </td>
                  <td style="background-image: url(<%=request.getContextPath()%>/images/parts/statuspanel_6.gif)" width="4"></td>
               </tr>
               
               <%-- separator row with gradient shadow --%>
               <tr>
                  <td><img src="<%=request.getContextPath()%>/images/parts/statuspanel_7.gif" width="4" height="9"></td>
                  <td style="background-image: url(<%=request.getContextPath()%>/images/parts/statuspanel_8.gif)"></td>
                  <td><img src="<%=request.getContextPath()%>/images/parts/statuspanel_9.gif" width="4" height="9"></td>
               </tr>
               
               <%-- Details --%>
               <tr valign=top>
                  <td style="background-image: url(<%=request.getContextPath()%>/images/parts/whitepanel_4.gif)" width="4"></td>
                  <td>
                     <table cellspacing="0" cellpadding="3" border="0" width="100%">
                        <tr>
                           <td width="100%" valign="top">
                              <% PanelGenerator.generatePanelStart(out, request.getContextPath(), "white", "white"); %>
                              <table cellpadding="2" cellspacing="2" border="0" width="100%">
                                 <tr>
                                    <td colspan="2" class="wizardSectionHeading"><h:outputText value="#{msg.document_properties}" /></td>
                                 </tr>
                                 <tr><td colspan="2" class="paddingRow"></td></tr>
                                 <tr>
                                    <td><nobr><h:outputText value="#{msg.file_name}" />:</nobr></td>
                                    <td width="90%">
                                       <h:inputText id="file-name" value="#{EditDocPropsDialog.properties.name}" size="35" maxlength="1024"
                                                    onkeyup="javascript:checkButtonState();" />&nbsp;*
                                    </td>
                                 </tr>
                                 <tr>
                                    <td><nobr><h:outputText value="#{msg.content_type}" />:</nobr></td>
                                    <td>
                                       <h:selectOneMenu value="#{EditDocPropsDialog.properties.mimetype}">
                                          <f:selectItems value="#{EditDocPropsDialog.contentTypes}" />
                                       </h:selectOneMenu>&nbsp;*
                                    </td>
                                 </tr>
                                 <tr>
                                    <td><nobr><h:outputText value="#{msg.title}" />:</nobr></td>
                                    <td>
                                       <h:inputText id="title" value="#{EditDocPropsDialog.properties.title}" size="35" maxlength="1024"
                                                    onkeyup="javascript:checkButtonState();" />&nbsp;*
                                    </td>
                                 </tr>
                                 <tr>
                                    <td><nobr><h:outputText value="#{msg.description}" />:</nobr></td>
                                    <td>
                                       <h:inputText value="#{EditDocPropsDialog.properties.description}" size="35" maxlength="1024" />
                                    </td>
                                 </tr>
                                 <tr>
                                    <td><nobr><h:outputText value="#{msg.author}" />:</nobr></td>
                                    <td>
                                       <h:inputText value="#{EditDocPropsDialog.properties.creator}" size="35" maxlength="1024" />
                                    </td>
                                 </tr>
                                 <tr><td colspan="2" class="paddingRow"></td></tr>
                                 <tr>
                                    <td colspan="2" class="wizardSectionHeading"><h:outputText value="#{msg.other_properties}" /></td>
                                 </tr>
                                 <tr><td colspan="2" class="paddingRow"></td></tr>
                                 <tr>
                                    <td colspan="2">
                                       <r:propertySheetGrid id="document-props" value="#{EditDocPropsDialog.editableNode}" 
                                                            var="editDocProps" columns="1" 
                                                            rendered="#{EditDocPropsDialog.otherPropertiesPresent}"
                                                            externalConfig="true" configArea="edit-properties" />
                                       <h:outputText value="#{msg.no_other_properties}" 
                                                     rendered="#{EditDocPropsDialog.otherPropertiesPresent == false}" />
                                    </td>
                                 </tr>
                              </table>
                              <% PanelGenerator.generatePanelEnd(out, request.getContextPath(), "white"); %>
                           </td>
                           
                           <td valign="top">
                              <% PanelGenerator.generatePanelStart(out, request.getContextPath(), "blue", "#D3E6FE"); %>
                              <table cellpadding="1" cellspacing="1" border="0">
                                 <tr>
                                    <td align="center">
                                       <h:commandButton id="ok-button" value="#{msg.ok}" action="#{EditDocPropsDialog.save}" styleClass="wizardButton" />
                                    </td>
                                 </tr>
                                 <tr>
                                    <td align="center">
                                       <h:commandButton value="#{msg.cancel}" action="cancel" styleClass="wizardButton" />
                                    </td>
                                 </tr>
                              </table>
                              <% PanelGenerator.generatePanelEnd(out, request.getContextPath(), "blue"); %>
                           </td>
                        </tr>
                     </table>
                  </td>
                  <td style="background-image: url(<%=request.getContextPath()%>/images/parts/whitepanel_6.gif)" width="4"></td>
               </tr>
               
               <%-- Error Messages --%>
               <tr valign=top>
                  <td style="background-image: url(<%=request.getContextPath()%>/images/parts/whitepanel_4.gif)" width=4></td>
                  <td>
                     <%-- messages tag to show messages not handled by other specific message tags --%>
                     <h:messages globalOnly="true" styleClass="errorMessage" layout="table" />
                  </td>
                  <td style="background-image: url(<%=request.getContextPath()%>/images/parts/whitepanel_6.gif)" width=4></td>
               </tr>
               
               <%-- separator row with bottom panel graphics --%>
               <tr>
                  <td><img src="<%=request.getContextPath()%>/images/parts/whitepanel_7.gif" width="4" height="4"></td>
                  <td width="100%" align="center" style="background-image: url(<%=request.getContextPath()%>/images/parts/whitepanel_8.gif)"></td>
                  <td><img src="<%=request.getContextPath()%>/images/parts/whitepanel_9.gif" width="4" height="4"></td>
               </tr>
               
            </table>
          </td>
       </tr>
    </table>
    
    </h:form>
    
</f:view>

</r:page>