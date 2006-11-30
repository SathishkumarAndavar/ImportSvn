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
package org.alfresco.web.app.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import org.alfresco.config.Config;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.web.app.Application;
import org.alfresco.web.app.servlet.command.CommandFactory;
import org.alfresco.web.app.servlet.command.CommandProcessor;
import org.alfresco.web.config.CommandServletConfigElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Servlet responsible for executing commands upon node(s).
 * <p>
 * The URL to the servlet should be generated thus:
 * <pre>/alfresco/command/processor-name/command-name/args/...</pre>
 * <p>
 * The 'processor-name' identifies the command processor to execute the command. For example the
 * 'workflow' processor will execute workflow commands upon a node (e.g. "approve" or "reject").
 * For example:
 * <pre>/alfresco/command/workflow/approve/workspace/SpacesStore/0000-0000-0000-0000</pre>
 * The store protocol, followed by the store ID, followed by the content Node Id used to
 * identify the node to execute the workflow action upon.
 * <p>
 * A 'return-page' URL argument can be specified as the redirect page to navigate too after processing.
 * <p>
 * Like most Alfresco servlets, the URL may be followed by a valid 'ticket' argument for authentication:
 * ?ticket=1234567890
 * <p>
 * And/or also followed by the "?guest=true" argument to force guest access login for the URL. 
 * 
 * @author Kevin Roast
 */
public class CommandServlet extends BaseServlet
{
   private static final long serialVersionUID = -5432407921038376133L;
   
   private static Log logger = LogFactory.getLog(CommandServlet.class);
   
   private static CommandFactory commandfactory = CommandFactory.getInstance();
   
   public static final String ARG_RETURNPAGE = "return-page";
   
   /**
    * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
    */
   protected void service(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException
   {
      String uri = req.getRequestURI();
      
      if (logger.isDebugEnabled())
         logger.debug("Processing URL: " + uri + (req.getQueryString() != null ? ("?" + req.getQueryString()) : ""));
      
      AuthenticationStatus status = servletAuthenticate(req, res);
      if (status == AuthenticationStatus.Failure)
      {
         return;
      }
      
      uri = uri.substring(req.getContextPath().length());
      StringTokenizer t = new StringTokenizer(uri, "/");
      int tokenCount = t.countTokens();
      if (tokenCount < 3)
      {
         throw new IllegalArgumentException("Command Servlet URL did not contain all required args: " + uri); 
      }
      
      t.nextToken();    // skip servlet name
      
      // get the command processor to execute the command e.g. "workflow"
      String procName = t.nextToken();
      
      // get the command to perform
      String command = t.nextToken();
      
      // get any remaining uri elements to pass to the processor
      String[] urlElements = new String[tokenCount - 3];
      for (int i=0; i<tokenCount-3; i++)
      {
         urlElements[i] = t.nextToken();
      }
      
      // retrieve the URL arguments to pass to the processor
      Map<String, String> args = new HashMap<String, String>(8, 1.0f);
      Enumeration names = req.getParameterNames();
      while (names.hasMoreElements())
      {
         String name = (String)names.nextElement();
         args.put(name, req.getParameter(name));
      }
      
      try
      {
         // get configured command processor by name from Config Service
         CommandProcessor processor = createCommandProcessor(procName);
         
         // validate that the processor has everything it needs to run the command
         if (processor.validateArguments(getServletContext(), command, args, urlElements) == false)
         {
            redirectToLoginPage(req, res, getServletContext());
            return;
         }
         
         ServiceRegistry serviceRegistry = getServiceRegistry(getServletContext());
         UserTransaction txn = null;
         try
         {
            txn = serviceRegistry.getTransactionService().getUserTransaction();
            txn.begin();
            
            // inform the processor to execute the specified command
            processor.process(serviceRegistry, req, command);
            
            // commit the transaction
            txn.commit();
         }
         catch (Throwable txnErr)
         {
            try { if (txn != null) {txn.rollback();} } catch (Exception tex) {}
            throw txnErr;
         }
         
         String returnPage = req.getParameter(ARG_RETURNPAGE);
         if (returnPage != null && returnPage.length() != 0)
         {
            if (logger.isDebugEnabled())
               logger.debug("Redirecting to specified return page: " + returnPage);
            
            res.sendRedirect(returnPage);
         }
         else
         {
            if (logger.isDebugEnabled())
               logger.debug("No return page specified, displaying status output.");
            
            res.setContentType("text/html");
            
            // request that the processor output a useful status message
            PrintWriter out = res.getWriter();
            processor.outputStatus(out);
            out.close();
         }
      }
      catch (Throwable err)
      {
         throw new AlfrescoRuntimeException("Error during command servlet processing: " + err.getMessage(), err);
      }
   }

   /**
    * Created the specified CommandProcessor instance. The name of the processor is looked up
    * in the client config, it should find a valid class impl and then create it. 
    *  
    * @param procName      Name of the CommandProcessor to lookup in the client config.
    * 
    * @return CommandProcessor
    * 
    * @throws InstantiationException
    * @throws IllegalAccessException
    */
   private CommandProcessor createCommandProcessor(String procName)
      throws InstantiationException, IllegalAccessException
   {
      Config config = Application.getConfigService(getServletContext()).getConfig("Command Servlet");
      if (config == null)
      {
         throw new AlfrescoRuntimeException("No command processors configured - unable to process any commands.");
      }
      
      CommandServletConfigElement configElement = (CommandServletConfigElement)
         config.getConfigElement(CommandServletConfigElement.CONFIG_ELEMENT_ID);
      if (configElement == null)
      {
         throw new AlfrescoRuntimeException("No command processors configured - unable to process any commands.");
      }
      
      Class clazz = configElement.getCommandProcessor(procName);
      Object obj = clazz.newInstance();
      if (obj instanceof CommandProcessor == false)
      {
         throw new AlfrescoRuntimeException("Configured command processor '" + procName + "' is does not implement interface CommandProcessor!"); 
      }
      
      return (CommandProcessor)obj;
   }
}
