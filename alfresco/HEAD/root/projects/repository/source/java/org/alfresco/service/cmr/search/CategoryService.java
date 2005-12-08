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
package org.alfresco.service.cmr.search;

import java.util.Collection;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;

/**
 * Category Service
 *
 * The service for querying and creating categories.
 * All other management can be carried out using the node service.
 * 
 * Classification - the groupings of categories. There is a one-to-one mapping with aspects. For example, Region. 
 * Root Category - the top level categories in a classification. For example, Northern Europe
 * Category - any other category below a root category
 * 
 * @author Andy Hind
 *
 */
public interface CategoryService
{
    /**
     * Enumeration for navigation control.
     * 
     * MEMBERS - get only category members (the things that have been classified in a category, not the sub categories)
     * SUB_CATEGORIES - get sub categories only, not the things that hyave been classified.
     * ALL - get both of the above
     */
    public enum Mode {MEMBERS, SUB_CATEGORIES, ALL};
    
    /**
     * Depth from which to get nodes.
     * 
     * IMMEDIATE - only immediate sub categories or members
     * ANY - find subcategories or members at any level 
     */
    public enum Depth {IMMEDIATE, ANY};

    /**
     * Get the children of a given category node
     * 
     * @param categoryRef - the category node
     * @param mode - the enumeration mode for what to recover
     * @param depth - the enumeration depth for what level to recover
     * @return a collection of all the nodes found identified by their ChildAssocRef's
     */
    public Collection<ChildAssociationRef> getChildren(NodeRef categoryRef, Mode mode, Depth depth );

    /**
     * Get a list of all the categories appropriate for a given property.
     * The full list of categories that may be assigned for this aspect.
     * 
     * @param aspectQName
     * @param depth - the enumeration depth for what level to recover
     * @return a collection of all the nodes found identified by their ChildAssocRef's
     */
    public Collection<ChildAssociationRef> getCategories(StoreRef storeRef, QName aspectQName, Depth depth );

    /**
     * Get all the classification entries
     * 
     * @return
     */
    public Collection<ChildAssociationRef> getClassifications(StoreRef storeRef);

    /**
     * Get the root categories for an aspect/classification
     * 
     * @param storeRef
     * @param aspectName
     * @return
     */
    public Collection<ChildAssociationRef> getRootCategories(StoreRef storeRef, QName aspectName);
    
    /**
     * Get all the types that represent categories
     * 
     * @return
     */
    public Collection<QName> getClassificationAspects();

    /**
     * Create a new category.
     * 
     * This will extend the category types in the data dictionary
     * All it needs is the type name and the attribute in which to store noderefs to categories.
     * 
     * @param aspectName
     * @param attributeName
     */
    public NodeRef createClassifiction(StoreRef storeRef, QName aspectName, String attributeName);
    
    /**
     * Create a new root category in the given classification
     * 
     * @param storeRef
     * @param aspectName
     * @param name
     * @return
     */
    public NodeRef createRootCategory(StoreRef storeRef, QName aspectName, String name);
    
    /**
     *  Create a new category.
     * 
     * @param parent
     * @param name
     * @return
     */
    public NodeRef createCategory(NodeRef parent, String name);
    
    /**
     * Delete a classification
     * 
     * @param storeRef
     * @param aspectName
     */
    public void deleteClassification(StoreRef storeRef, QName aspectName);
    
    /**
     * Delete a category
     * 
     * @param nodeRef
     */
    public void deleteCategory(NodeRef nodeRef);
}
