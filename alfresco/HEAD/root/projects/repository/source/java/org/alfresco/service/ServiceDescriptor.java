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
package org.alfresco.service;

import java.util.Collection;

import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;


/**
 * This interface represents service meta-data.
 * 
 * @author David Caruana
 */
public interface ServiceDescriptor
{
    /**
     * @return the qualified name of the service
     */
    public QName getQualifiedName();
    
    /**
     * @return the service description
     */
    public String getDescription();

    /**
     * @return the service interface class description
     */
    public Class getInterface();

    /**
     * @return the names of the protocols supported
     */
    public Collection<String> getSupportedStoreProtocols();
    
    /**
     * @return the Store Refs of the stores supported
     */
    public Collection<StoreRef> getSupportedStores();
}
