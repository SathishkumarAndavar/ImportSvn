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
package org.alfresco.repo.workflow.jbpm;


import java.util.List;

import junit.framework.TestCase;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.db.GraphSession;
import org.jbpm.db.TaskMgmtSession;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.exe.TaskInstance;


/**
 * Unit Test for reproducing constraint violation during JBPM process deletion
 *
 * http://jira.jboss.com/jira/browse/JBPM-757
 *  
 * @author davidc
 */

public class JBPMDeleteProcessTest extends TestCase {

    static JbpmConfiguration jbpmConfiguration = null; 
    static long processId = -1L;
    static String currentTokenPath = null;

    static {
      jbpmConfiguration = JbpmConfiguration.parseXmlString(
        "<jbpm-configuration>" +
        "  <jbpm-context>" +
        "    <service name='persistence' " +
        "             factory='org.jbpm.persistence.db.DbPersistenceServiceFactory' />" + 
        "  </jbpm-context>" +
        "  <string name='resource.hibernate.cfg.xml' " +
        "          value='jbpmresources/hibernate.cfg.xml' />" +
        "  <string name='resource.business.calendar' " +
        "          value='org/jbpm/calendar/jbpm.business.calendar.properties' />" +
        "  <string name='resource.default.modules' " +
        "          value='org/jbpm/graph/def/jbpm.default.modules.properties' />" +
        "  <string name='resource.converter' " +
        "          value='org/jbpm/db/hibernate/jbpm.converter.properties' />" +
        "  <string name='resource.action.types' " +
        "          value='org/jbpm/graph/action/action.types.xml' />" +
        "  <string name='resource.node.types' " +
        "          value='org/jbpm/graph/node/node.types.xml' />" +
        "  <string name='resource.varmapping' " +
        "          value='org/jbpm/context/exe/jbpm.varmapping.xml' />" +
        "</jbpm-configuration>"
      );
    }
    
    public void setUp() {
      jbpmConfiguration.createSchema();
    }
    
    public void tearDown() {
      jbpmConfiguration.dropSchema();
    }

    public void testDelete() {
      deployProcessDefinition();

      startProcess();
      step2TaskEnd();
      deleteProcess();
    }

    public void deployProcessDefinition() {
      ProcessDefinition processDefinition = ProcessDefinition.parseXmlString
      (
        "<process-definition name='deletetest'>" +
        "  <start-state name='start'> " +  
        "    <task name='startTask'> " +
        "      <controller> " +
        "        <variable name='var1' access='write'/> " +
        "      </controller> " +
        "    </task> " +
        "   <transition name='' to='step2'/> " +
        "  </start-state> " +
        "  <task-node name='step2'> " +
        "    <task name='step2Task'/> " +
        "    <transition name='' to='step3'/> " +
        "  </task-node>" +
        "  <task-node name='step3'> " +
        "    <task name='step3Task'/> " +
        "    <transition name='' to='end'/> " +
        "  </task-node> " +      
        "  <end-state name='end' />" +
        "</process-definition>"
      );
      
      JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
      try {
        jbpmContext.deployProcessDefinition(processDefinition);
      } finally {
        jbpmContext.close();
      }
    }

    public void startProcess() {

      JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
      try {

        GraphSession graphSession = jbpmContext.getGraphSession();
        
        ProcessDefinition processDefinition = graphSession.findLatestProcessDefinition("deletetest");
        ProcessInstance processInstance = new ProcessInstance(processDefinition);
        processId = processInstance.getId();

        TaskInstance taskInstance = processInstance.getTaskMgmtInstance().createStartTaskInstance();
        taskInstance.setVariableLocally("var1", "var1Value");
        taskInstance.end();
        Token token = taskInstance.getToken();
        currentTokenPath = token.getFullName();
        
        jbpmContext.save(processInstance);
      } finally {
        jbpmContext.close();
      }
    }

    public void step2TaskEnd() {

        JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
        try {

          GraphSession graphSession = jbpmContext.getGraphSession();
          ProcessInstance processInstance = graphSession.loadProcessInstance(processId);
          Token token = processInstance.findToken(currentTokenPath);
          TaskMgmtSession taskSession = jbpmContext.getTaskMgmtSession();
          List tasks = taskSession.findTaskInstancesByToken(token.getId());
          TaskInstance taskInstance = (TaskInstance)tasks.get(0);
          
          //
          // Uncomment the following line to force constraint violation
          //
          // taskInstance.setVariableLocally("var1", "var1TaskValue");
          
          taskInstance.setVariableLocally("var2", "var2UpdatedValue");
          taskInstance.end();
          token = taskInstance.getToken();
          currentTokenPath = token.getFullName();
          
          jbpmContext.save(processInstance);
        } finally {
          jbpmContext.close();
        }
      }

    
    public void deleteProcess()
    {
        JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
        try {

          GraphSession graphSession = jbpmContext.getGraphSession();
          ProcessInstance processInstance = graphSession.loadProcessInstance(processId);
          graphSession.deleteProcessInstance(processInstance, true, true, true);
        } finally {
          jbpmContext.close();
        }
    }
    
}