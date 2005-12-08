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
package org.alfresco.repo.action;

import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.transaction.TransactionUtil;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.transaction.TransactionService;

/**
 * The asynchronous action execution queue implementation
 * 
 * @author Roy Wetherall
 */
public class AsynchronousActionExecutionQueueImpl extends ThreadPoolExecutor implements
        AsynchronousActionExecutionQueue
{
    /**
     * Default pool values
     */
    private static final int CORE_POOL_SIZE = 2;

    private static final int MAX_POOL_SIZE = 5;

    private static final long KEEP_ALIVE = 30;

    private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;

    private static final int MAX_QUEUE_SIZE = 500;

    /**
     * The transaction service
     */
    private TransactionService transactionService;

    /**
     * The authentication component
     */
    private AuthenticationComponent authenticationComponent;

    /**
     * Default constructor
     */
    public AsynchronousActionExecutionQueueImpl()
    {
        super(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE, TIME_UNIT, new ArrayBlockingQueue<Runnable>(MAX_QUEUE_SIZE,
                true));
    }

    /**
     * Set the transaction service
     * 
     * @param transactionService
     *            the transaction service
     */
    public void setTransactionService(TransactionService transactionService)
    {
        this.transactionService = transactionService;
    }

    /**
     * Set the authentication component
     * 
     * @param authenticationComponent
     *            the authentication component
     */
    public void setAuthenticationComponent(AuthenticationComponent authenticationComponent)
    {
        this.authenticationComponent = authenticationComponent;
    }

    /**
     * @see org.alfresco.repo.action.AsynchronousActionExecutionQueue#executeAction(org.alfresco.service.cmr.repository.NodeRef,
     *      org.alfresco.service.cmr.action.Action, boolean)
     */
    public void executeAction(RuntimeActionService actionService, Action action, NodeRef actionedUponNodeRef,
            boolean checkConditions, Set<String> actionChain)
    {
        executeAction(actionService, action, actionedUponNodeRef, checkConditions, actionChain, null);
    }

    /**
     * @see org.alfresco.repo.action.AsynchronousActionExecutionQueue#executeAction(org.alfresco.service.cmr.repository.NodeRef,
     *      org.alfresco.service.cmr.action.Action, boolean,
     *      org.alfresco.service.cmr.repository.NodeRef)
     */
    public void executeAction(RuntimeActionService actionService, Action action, NodeRef actionedUponNodeRef,
            boolean checkConditions, Set<String> actionChain, NodeRef actionExecutionHistoryNodeRef)
    {
        execute(new ActionExecutionWrapper(actionService, transactionService, authenticationComponent, action,
                actionedUponNodeRef, checkConditions, actionExecutionHistoryNodeRef, actionChain));
    }

    /**
     * @see java.util.concurrent.ThreadPoolExecutor#beforeExecute(java.lang.Thread,
     *      java.lang.Runnable)
     */
    @Override
    protected void beforeExecute(Thread thread, Runnable runnable)
    {
        super.beforeExecute(thread, runnable);
    }

    /**
     * @see java.util.concurrent.ThreadPoolExecutor#afterExecute(java.lang.Runnable,
     *      java.lang.Throwable)
     */
    @Override
    protected void afterExecute(Runnable thread, Throwable runnable)
    {
        super.afterExecute(thread, runnable);
    }

    /**
     * Runnable class to wrap the execution of the action.
     */
    private class ActionExecutionWrapper implements Runnable
    {
        /**
         * Runtime action service
         */
        private RuntimeActionService actionService;

        /**
         * The transaction service
         */
        private TransactionService transactionService;

        /**
         * The authentication component
         */
        private AuthenticationComponent authenticationComponent;

        /**
         * The action
         */
        private Action action;

        /**
         * The actioned upon node reference
         */
        private NodeRef actionedUponNodeRef;

        /**
         * The check conditions value
         */
        private boolean checkConditions;

        /**
         * The action execution history node reference
         */
        private NodeRef actionExecutionHistoryNodeRef;

        /**
         * The action chain
         */
        private Set<String> actionChain;

        /**
         * Constructor
         * 
         * @param actionService
         * @param transactionService
         * @param authenticationComponent
         * @param action
         * @param actionedUponNodeRef
         * @param checkConditions
         * @param actionExecutionHistoryNodeRef
         */
        public ActionExecutionWrapper(RuntimeActionService actionService, TransactionService transactionService,
                AuthenticationComponent authenticationComponent, Action action, NodeRef actionedUponNodeRef,
                boolean checkConditions, NodeRef actionExecutionHistoryNodeRef, Set<String> actionChain)
        {
            this.actionService = actionService;
            this.transactionService = transactionService;
            this.authenticationComponent = authenticationComponent;
            this.actionedUponNodeRef = actionedUponNodeRef;
            this.action = action;
            this.checkConditions = checkConditions;
            this.actionExecutionHistoryNodeRef = actionExecutionHistoryNodeRef;
            this.actionChain = actionChain;
        }

        /**
         * Get the action
         * 
         * @return the action
         */
        public Action getAction()
        {
            return this.action;
        }

        /**
         * Get the actioned upon node reference
         * 
         * @return the actioned upon node reference
         */
        public NodeRef getActionedUponNodeRef()
        {
            return this.actionedUponNodeRef;
        }

        /**
         * Get the check conditions value
         * 
         * @return the check conditions value
         */
        public boolean getCheckCondtions()
        {
            return this.checkConditions;
        }

        /**
         * Get the action execution history node reference
         * 
         * @return the action execution history node reference
         */
        public NodeRef getActionExecutionHistoryNodeRef()
        {
            return this.actionExecutionHistoryNodeRef;
        }

        /**
         * Get the action chain
         * 
         * @return the action chain
         */
        public Set<String> getActionChain()
        {
            return actionChain;
        }

        /**
         * Executes the action via the action runtime service
         * 
         * @see java.lang.Runnable#run()
         */
        @SuppressWarnings("unchecked")
        public void run()
        {
            try
            {

                // For now run all actions in the background as the system user
                ActionExecutionWrapper.this.authenticationComponent
                        .setCurrentUser(ActionExecutionWrapper.this.authenticationComponent.getSystemUserName());
                try
                {
                    TransactionUtil.executeInNonPropagatingUserTransaction(this.transactionService,
                            new TransactionUtil.TransactionWork()
                            {
                                public Object doWork()
                                {

                                    ActionExecutionWrapper.this.actionService.executeActionImpl(
                                            ActionExecutionWrapper.this.action,
                                            ActionExecutionWrapper.this.actionedUponNodeRef,
                                            ActionExecutionWrapper.this.checkConditions, true,
                                            ActionExecutionWrapper.this.actionChain);

                                    return null;
                                }
                            });
                }
                finally
                {
                    ActionExecutionWrapper.this.authenticationComponent.clearCurrentSecurityContext();
                }
            }
            catch (Throwable exception)
            {
                exception.printStackTrace();
            }
        }
    }
}
