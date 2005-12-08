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
package org.alfresco.web.bean;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.faces.validator.ValidatorException;
import javax.portlet.PortletRequest;
import javax.servlet.http.HttpServletRequest;

import org.alfresco.config.ConfigService;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.webdav.WebDAVServlet;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.web.app.Application;
import org.alfresco.web.app.servlet.AuthenticationHelper;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.bean.repository.User;
import org.alfresco.web.config.ClientConfigElement;
import org.alfresco.web.ui.common.Utils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * JSF Managed Bean. Backs the "login.jsp" view to provide the form fields used
 * to enter user data for login. Also contains bean methods to validate form
 * fields and action event fired in response to the Login button being pressed.
 * 
 * @author Kevin Roast
 */
public class LoginBean
{
   // ------------------------------------------------------------------------------
   // Managed bean properties

   /**
    * @param authenticationService      The AuthenticationService to set.
    */
   public void setAuthenticationService(AuthenticationService authenticationService)
   {
      this.authenticationService = authenticationService;
   }

   /**
    * @param personService             The personService to set.
    */
   public void setPersonService(PersonService personService)
   {
      this.personService = personService;
   }

   /**
    * @param nodeService                The nodeService to set.
    */
   public void setNodeService(NodeService nodeService)
   {
      this.nodeService = nodeService;
   }

   /**
    * @param browseBean                 The BrowseBean to set.
    */
   public void setBrowseBean(BrowseBean browseBean)
   {
      this.browseBean = browseBean;
   }
   
   /**
    * @param navigator The NavigationBean to set.
    */
   public void setNavigator(NavigationBean navigator)
   {
      this.navigator = navigator;
   }

   /**
    * @param configService              The ConfigService to set.
    */
   public void setConfigService(ConfigService configService)
   {
      this.configService = configService;
   }
   
   /**
    * @param fileFolderService          The FileFolderService to set.
    */
   public void setFileFolderService(FileFolderService fileFolderService)
   {
      this.fileFolderService = fileFolderService;
   }

   /**
    * @param val        Username from login dialog
    */
   public void setUsername(String val)
   {
      this.username = val;
   }

   /**
    * @return The username string from login dialog
    */
   public String getUsername()
   {
      return this.username;
   }

   /**
    * @param val         Password from login dialog
    */
   public void setPassword(String val)
   {
      this.password = val;
   }

   /**
    * @return The password string from login dialog
    */
   public String getPassword()
   {
      return this.password;
   }

   /**
    * @return the available languages
    */
   public SelectItem[] getLanguages()
   {
      ClientConfigElement config = (ClientConfigElement) this.configService.getGlobalConfig()
            .getConfigElement(ClientConfigElement.CONFIG_ELEMENT_ID);
      
      List<String> languages = config.getLanguages();
      SelectItem[] items = new SelectItem[languages.size()];
      int count = 0;
      for (String locale : languages)
      {
         // get label associated to the locale
         String label = config.getLabelForLanguage(locale);

         // set default selection
         if (count == 0 && this.language == null)
         {
            // first try to get the language that the current user is using
            Locale lastLocale = Application.getLanguage(FacesContext.getCurrentInstance());
            if (lastLocale != null)
            {
               this.language = lastLocale.toString();
            }
            // else we default to the first item in the list
            else
            {
               this.language = locale;
            }
         }
         
         items[count++] = new SelectItem(locale, label);
      }
      
      return items;
   }

   /**
    * @return Returns the language selection.
    */
   public String getLanguage()
   {
      return this.language;
   }

   /**
    * @param language       The language selection to set.
    */
   public void setLanguage(String language)
   {
      this.language = language;
      Application.setLanguage(FacesContext.getCurrentInstance(), this.language);
   }


   // ------------------------------------------------------------------------------
   // Validator methods

   /**
    * Validate password field data is acceptable
    */
   public void validatePassword(FacesContext context, UIComponent component, Object value)
         throws ValidatorException
   {
      String pass = (String) value;
      if (pass.length() < 3 || pass.length() > 32)
      {
         String err = MessageFormat.format(Application.getMessage(context, MSG_PASSWORD_LENGTH),
               new Object[]{3, 32});
         throw new ValidatorException(new FacesMessage(err));
      }
   }

   /**
    * Validate Username field data is acceptable
    */
   public void validateUsername(FacesContext context, UIComponent component, Object value)
         throws ValidatorException
   {
      String pass = (String) value;
      if (pass.length() < 3 || pass.length() > 32)
      {
         String err = MessageFormat.format(Application.getMessage(context, MSG_USERNAME_LENGTH),
               new Object[]{3, 32});
         throw new ValidatorException(new FacesMessage(err));
      }
   }

   
   // ------------------------------------------------------------------------------
   // Action event methods

   /**
    * Login action handler
    * 
    * @return outcome view name
    */
   public String login()
   {
      String outcome = null;
      
      FacesContext fc = FacesContext.getCurrentInstance();
      
      if (this.username != null && this.password != null)
      {
         // Authenticate via the authentication service, then save the details of user in an object
         // in the session - this is used by the servlet filter etc. on each page to check for login
         try
         {
            this.authenticationService.authenticate(this.username, this.password.toCharArray());
            
            // setup User object and Home space ID
            User user = new User(this.authenticationService.getCurrentUserName(), this.authenticationService.getCurrentTicket(),
                  personService.getPerson(this.username));
            NodeRef homeSpaceRef = (NodeRef) this.nodeService.getProperty(personService.getPerson(this.username), ContentModel.PROP_HOMEFOLDER);
            
            // check that the home space node exists - else user cannot login
            if (this.nodeService.exists(homeSpaceRef) == false)
            {
               throw new InvalidNodeRefException(homeSpaceRef);
            }
            user.setHomeSpaceId(homeSpaceRef.getId());
            
            // put the User object in the Session - the authentication servlet will then allow
            // the app to continue without redirecting to the login page
            Map session = fc.getExternalContext().getSessionMap();
            session.put(AuthenticationHelper.AUTHENTICATION_USER, user);
            
            // if an external outcome has been provided then use that, else use default
            String externalOutcome = (String)fc.getExternalContext().getSessionMap().get(LOGIN_OUTCOME_KEY);
            if (externalOutcome != null)
            {
               // TODO: This is a quick solution. It would be better to specify the (identifier?)
               // of a handler class that would be responsible for processing specific outcome arguments.
               
               if (logger.isDebugEnabled())
                  logger.debug("External outcome found: " + externalOutcome);
               
               // setup is required for certain outcome requests
               if (OUTCOME_DOCDETAILS.equals(externalOutcome))
               {
                  NodeRef nodeRef = null;
                  
                  String[] args = (String[]) fc.getExternalContext().getSessionMap().get(LOGIN_OUTCOME_ARGS);
                  if (args[0].equals(WebDAVServlet.WEBDAV_PREFIX))
                  {
                     nodeRef = resolveWebDAVPath(fc, args);
                  }
                  else if (args.length == 3)
                  {
                     StoreRef storeRef = new StoreRef(args[0], args[1]);
                     nodeRef = new NodeRef(storeRef, args[2]);
                  }
                  
                  if (nodeRef != null)
                  {
                     // setup the Document on the browse bean
                     // TODO: the browse bean should accept a full NodeRef - not just an ID
                     this.browseBean.setupContentAction(nodeRef.getId(), true);
                  }
               }
               else if (OUTCOME_SPACEDETAILS.equals(externalOutcome))
               {
                  NodeRef nodeRef = null;
                  
                  String[] args = (String[]) fc.getExternalContext().getSessionMap().get(LOGIN_OUTCOME_ARGS);
                  if (args[0].equals(WebDAVServlet.WEBDAV_PREFIX))
                  {
                     nodeRef = resolveWebDAVPath(fc, args);
                  }
                  else if (args.length == 3)
                  {
                     StoreRef storeRef = new StoreRef(args[0], args[1]);
                     nodeRef = new NodeRef(storeRef, args[2]);
                  }
                  
                  if (nodeRef != null)
                  {
                     // setup the Space on the browse bean
                     // TODO: the browse bean should accept a full NodeRef - not just an ID
                     this.browseBean.setupSpaceAction(nodeRef.getId(), true);
                  }
               }
               else if (OUTCOME_BROWSE.equals(externalOutcome))
               {
                  String[] args = (String[]) fc.getExternalContext().getSessionMap().get(LOGIN_OUTCOME_ARGS);
                  if (args != null)
                  {
                     NodeRef nodeRef = null;
                     int offset = 0;
                     if (args.length >= 3)
                     {
                        offset = args.length - 3;
                        StoreRef storeRef = new StoreRef(args[0+offset], args[1+offset]);
                        nodeRef = new NodeRef(storeRef, args[2+offset]);
                        
                        // setup the ref as current Id in the global navigation bean
                        this.navigator.setCurrentNodeId(nodeRef.getId());
                        
                        // check for view mode first argument
                        if (args[0].equals(LOGIN_ARG_TEMPLATE))
                        {
                           this.browseBean.setDashboardView(true);
                           // the above call will auto-navigate to the correct outcome - so we don't!
                           externalOutcome = null;
                        }
                     }
                  }
               }
               
               fc.getExternalContext().getSessionMap().remove(LOGIN_OUTCOME_KEY);
               return externalOutcome;
            }
            else
            {
               // if a redirect URL has been provided then use that
               String redirectURL = (String)fc.getExternalContext().getSessionMap().get(LOGIN_REDIRECT_KEY);
               if (redirectURL != null)
               {
                  if (logger.isDebugEnabled())
                     logger.debug("Redirect URL found: " + redirectURL);
                  
                  // remove URL from session
                  fc.getExternalContext().getSessionMap().remove(LOGIN_REDIRECT_KEY);
                  
                  try
                  {
                     fc.getExternalContext().redirect(redirectURL);
                     fc.responseComplete();
                     return null;
                  }
                  catch (IOException ioErr)
                  {
                     logger.warn("Unable to redirect to url: " + redirectURL);
                  }
               }
               else
               {
                  return "success";
               }
            }
         }
         catch (AuthenticationException aerr)
         {
            Utils.addErrorMessage(Application.getMessage(fc, MSG_ERROR_UNKNOWN_USER));
         }
         catch (InvalidNodeRefException refErr)
         {
            Utils.addErrorMessage(MessageFormat.format(Application.getMessage(fc,
                  Repository.ERROR_NOHOME), refErr.getNodeRef().getId()));
         }
      }
      else
      {
         Utils.addErrorMessage(Application.getMessage(fc, MSG_ERROR_MISSING));
      }

      return outcome;
   }

   /**
    * Invalidate ticket and logout user
    */
   public String logout()
   {
      FacesContext context = FacesContext.getCurrentInstance();
      
      Map session = context.getExternalContext().getSessionMap();
      User user = (User) session.get(AuthenticationHelper.AUTHENTICATION_USER);
      
      boolean alfrescoAuth = (session.get(LOGIN_EXTERNAL_AUTH) == null);
      
      // invalidate Session for this user
      if (Application.inPortalServer() == false)
      {
         HttpServletRequest request = (HttpServletRequest)FacesContext.getCurrentInstance().getExternalContext().getRequest();
         request.getSession().invalidate();
      }
      else
      {
         PortletRequest request = (PortletRequest)FacesContext.getCurrentInstance().getExternalContext().getRequest();
         request.getPortletSession().invalidate();
      }
      
      // invalidate User ticket
      if (user != null)
      {
         this.authenticationService.invalidateTicket(user.getTicket());
      }
      
      // set language to last used
      if (this.language != null && this.language.length() != 0)
      {
         Application.setLanguage(context, this.language);
      }
      
      return alfrescoAuth ? "logout" : "relogin";
   }

   
   // ------------------------------------------------------------------------------
   // Private helpers
   
   /**
    * Resolves the given path elements to a NodeRef in the current repository
    * 
    * @param context Faces context
    * @param args The elements of the path to lookup
    */
   private NodeRef resolveWebDAVPath(FacesContext context, String[] args)
   {
      NodeRef nodeRef = null;

      List<String> paths = new ArrayList<String>(args.length-1);
      
      FileInfo file = null;
      try
      {
         // create a list of path elements (decode the URL as we go)
         for (int x = 1; x < args.length; x++)
         {
            paths.add(URLDecoder.decode(args[x], "UTF-8"));
         }
         
         if (logger.isDebugEnabled())
            logger.debug("Attempting to resolve webdav path to NodeRef: " + paths);
         
         // get the company home node to start the search from
         NodeRef companyHome = new NodeRef(Repository.getStoreRef(), 
               Application.getCompanyRootId());
         
         file = this.fileFolderService.resolveNamePath(companyHome, paths);
         nodeRef = file.getNodeRef();
      }
      catch (UnsupportedEncodingException uee)
      {
         if (logger.isWarnEnabled())
            logger.warn("Failed to resolve webdav path", uee);
         
         nodeRef = null;
      }
      catch (FileNotFoundException fne)
      {
         if (logger.isWarnEnabled())
            logger.debug("Failed to resolve webdav path", fne);
         
         nodeRef = null;
      }
      
      return nodeRef;
   }
   
   
   // ------------------------------------------------------------------------------
   // Private data

   private static final Log logger = LogFactory.getLog(LoginBean.class);
   
   /** I18N messages */
   private static final String MSG_ERROR_MISSING = "error_login_missing";
   private static final String MSG_ERROR_UNKNOWN_USER = "error_login_user";
   private static final String MSG_USERNAME_CHARS = "login_err_username_chars";
   private static final String MSG_USERNAME_LENGTH = "login_err_username_length";
   private static final String MSG_PASSWORD_CHARS = "login_err_password_chars";
   private static final String MSG_PASSWORD_LENGTH = "login_err_password_length";

   public static final String LOGIN_REDIRECT_KEY = "_alfRedirect";
   public static final String LOGIN_OUTCOME_KEY  = "_alfOutcome";
   public static final String LOGIN_OUTCOME_ARGS = "_alfOutcomeArgs";
   public static final String LOGIN_EXTERNAL_AUTH= "_alfExternalAuth";

   public final static String OUTCOME_DOCDETAILS   = "showDocDetails";
   public final static String OUTCOME_SPACEDETAILS = "showSpaceDetails";
   public final static String OUTCOME_BROWSE       = "browse";
   
   private static final String LOGIN_ARG_TEMPLATE  = "template";

   /** user name */
   private String username = null;

   /** password */
   private String password = null;

   /** language locale selection */
   private String language = null;

   /** PersonService bean reference */
   private PersonService personService;
   
   /** AuthenticationService bean reference */
   private AuthenticationService authenticationService;

   /** NodeService bean reference */
   private NodeService nodeService;

   /** The BrowseBean reference */
   private BrowseBean browseBean;
   
   /** The NavigationBean bean reference */
   private NavigationBean navigator;

   /** ConfigService bean reference */
   private ConfigService configService;
   
   /** FileFolderService bean reference */
   private FileFolderService fileFolderService;
}
