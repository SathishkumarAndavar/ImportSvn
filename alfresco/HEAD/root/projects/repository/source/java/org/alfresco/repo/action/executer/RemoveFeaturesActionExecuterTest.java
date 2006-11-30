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
package org.alfresco.repo.action.executer;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.ActionImpl;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.BaseSpringTest;
import org.alfresco.util.GUID;

/**
 * Remove features action execution test
 * 
 * @author Roy Wetherall
 */
public class RemoveFeaturesActionExecuterTest extends BaseSpringTest
{
    /**
     * The node service
     */
    private NodeService nodeService;
    
    /**
     * The store reference
     */
    private StoreRef testStoreRef;
    
    /**
     * The root node reference
     */
    private NodeRef rootNodeRef;
    
    /**
     * The test node reference
     */
    private NodeRef nodeRef;
    
    /**
     * The add features action executer
     */
    private RemoveFeaturesActionExecuter executer;
    
    /**
     * Id used to identify the test action created
     */
    private final static String ID = GUID.generate();
    
    /**
     * Called at the begining of all tests
     */
    @Override
    protected void onSetUpInTransaction() throws Exception
    {
        this.nodeService = (NodeService)this.applicationContext.getBean("nodeService");
        
        AuthenticationComponent authenticationComponent = (AuthenticationComponent)applicationContext.getBean("authenticationComponent");
        authenticationComponent.setCurrentUser(authenticationComponent.getSystemUserName());
        
        // Create the store and get the root node
        this.testStoreRef = this.nodeService.createStore(
                StoreRef.PROTOCOL_WORKSPACE, "Test_"
                        + System.currentTimeMillis());
        this.rootNodeRef = this.nodeService.getRootNode(this.testStoreRef);

        // Create the node used for tests
        this.nodeRef = this.nodeService.createNode(
                this.rootNodeRef,
                ContentModel.ASSOC_CHILDREN,
                QName.createQName("{test}testnode"),
                ContentModel.TYPE_CONTENT).getChildRef();
        this.nodeService.addAspect(nodeRef, ContentModel.ASPECT_CLASSIFIABLE, null);
        
        // Get the executer instance 
        this.executer = (RemoveFeaturesActionExecuter)this.applicationContext.getBean(RemoveFeaturesActionExecuter.NAME);
    }
    
    /**
     * Test execution
     */
    public void testExecution()
    {
        // Check that the node has the classifiable aspect
        assertTrue(this.nodeService.hasAspect(this.nodeRef, ContentModel.ASPECT_CLASSIFIABLE));
        
        // Execute the action
        ActionImpl action = new ActionImpl(null, ID, RemoveFeaturesActionExecuter.NAME, null);
        action.setParameterValue(RemoveFeaturesActionExecuter.PARAM_ASPECT_NAME, ContentModel.ASPECT_CLASSIFIABLE);
        this.executer.execute(action, this.nodeRef);
        
        // Check that the node now no longer has the classifiable aspect
        assertFalse(this.nodeService.hasAspect(this.nodeRef, ContentModel.ASPECT_CLASSIFIABLE));
        
        // Now try and remove an aspect that is not present 
        ActionImpl action2 = new ActionImpl(null, ID, RemoveFeaturesActionExecuter.NAME, null);
        action2.setParameterValue(RemoveFeaturesActionExecuter.PARAM_ASPECT_NAME, ContentModel.ASPECT_VERSIONABLE);
        this.executer.execute(action2, this.nodeRef);
    }
}
