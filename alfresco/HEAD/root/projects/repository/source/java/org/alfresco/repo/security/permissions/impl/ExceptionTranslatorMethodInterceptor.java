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
package org.alfresco.repo.security.permissions.impl;

import net.sf.acegisecurity.AccessDeniedException;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

public class ExceptionTranslatorMethodInterceptor implements MethodInterceptor
{
    private static final String MSG_ACCESS_DENIED = "permissions.err_access_denied";
    
    public ExceptionTranslatorMethodInterceptor()
    {
        super();
    }

    public Object invoke(MethodInvocation mi) throws Throwable
    {
        try
        {
            return mi.proceed();
        }
        catch(AccessDeniedException ade)
        {
            throw new org.alfresco.repo.security.permissions.AccessDeniedException(MSG_ACCESS_DENIED, ade);
        }
    }

}
