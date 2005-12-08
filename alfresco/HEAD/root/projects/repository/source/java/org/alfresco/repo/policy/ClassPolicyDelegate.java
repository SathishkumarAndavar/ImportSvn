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
package org.alfresco.repo.policy;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.alfresco.service.cmr.dictionary.ClassDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

/**
 * Delegate for a Class-level Policy.  Provides access to Policy Interface
 * implementations which invoke the appropriate bound behaviours.
 *  
 * @author David Caruana
 *
 * @param <P>  the policy interface
 */
public class ClassPolicyDelegate<P extends ClassPolicy>
{
    private DictionaryService dictionary;
    private CachedPolicyFactory<ClassBehaviourBinding, P> factory;


    /**
     * Construct.
     * 
     * @param dictionary  the dictionary service
     * @param policyClass  the policy interface class
     * @param index  the behaviour index to query against
     */
    @SuppressWarnings("unchecked")
    /*package*/ ClassPolicyDelegate(DictionaryService dictionary, Class<P> policyClass, BehaviourIndex<ClassBehaviourBinding> index)
    {
        // Get list of all pre-registered behaviours for the policy and
        // ensure they are valid.
        Collection<BehaviourDefinition> definitions = index.getAll();
        for (BehaviourDefinition definition : definitions)
        {
            definition.getBehaviour().getInterface(policyClass);
        }

        // Rely on cached implementation of policy factory
        // Note: Could also use PolicyFactory (without caching)
        this.factory = new CachedPolicyFactory<ClassBehaviourBinding, P>(policyClass, index);
        this.dictionary = dictionary;
    }
    

    /**
     * Gets the Policy implementation for the specified Class
     * 
     * When multiple behaviours are bound to the policy for the class, an
     * aggregate policy implementation is returned which invokes each policy
     * in turn.
     * 
     * @param classQName  the class qualified name
     * @return  the policy
     */
    public P get(QName classQName)
    {
        return get(null, classQName);
    }

    /**
     * Gets the Policy implementation for the specified Class
     * 
     * @param nodeRef  the node reference
     * @param classQName  the class name
     * @return  the policy
     */
    public P get(NodeRef nodeRef, QName classQName)
    {
        ClassDefinition classDefinition = dictionary.getClass(classQName);
        if (classDefinition == null)
        {
            throw new IllegalArgumentException("Class " + classQName + " has not been defined in the data dictionary");
        }
        return factory.create(new ClassBehaviourBinding(dictionary, nodeRef, classQName));
    }
    
    /**
     * Gets the collection of Policy implementations for the specified Class
     * 
     * @param classQName  the class qualified name
     * @return  the collection of policies
     */
    public Collection<P> getList(QName classQName)
    {
        return getList(null, classQName);
    }
    
    /**
     * Gets the collection of Policy implementations for the specified Class
     * 
     * @param nodeRef  the node reference
     * @param classQName  the class qualified name
     * @return  the collection of policies
     */
    public Collection<P> getList(NodeRef nodeRef, QName classQName)
    {
        ClassDefinition classDefinition = dictionary.getClass(classQName);
        if (classDefinition == null)
        {
            throw new IllegalArgumentException("Class " + classQName + " has not been defined in the data dictionary");
        }
        return factory.createList(new ClassBehaviourBinding(dictionary, nodeRef, classQName));
    }

    /**
     * Gets the policy implementation for the given classes.  The single <tt>Policy</tt>
     * will be a wrapper of multiple appropriate policies.
     * 
     * @param classQNames the class qualified names
     * @return Returns the policy
     */
    public P get(Set<QName> classQNames)
    {
        return get(null, classQNames);
    }

    /**
     * Gets the policy implementation for the given classes.  The single <tt>Policy</tt>
     * will be a wrapper of multiple appropriate policies.
     *
     * @param nodeRef  the node reference
     * @param classQNames the class qualified names
     * @return Returns the policy
     */
    public P get(NodeRef nodeRef, Set<QName> classQNames)
    {
        return factory.toPolicy(getList(nodeRef, classQNames));
    }

    /**
     * Gets the collection of <tt>Policy</tt> implementations for the given classes
     * 
     * @param classQNames the class qualified names
     * @return Returns the collection of policies
     */
    public Collection<P> getList(Set<QName> classQNames)
    {
        return getList(null, classQNames);
    }

    /**
     * Gets the collection of <tt>Policy</tt> implementations for the given classes
     * 
     * @param classQNames the class qualified names
     * @return Returns the collection of policies
     */
    public Collection<P> getList(NodeRef nodeRef, Set<QName> classQNames)
    {
        Collection<P> policies = new HashSet<P>();
        for (QName classQName : classQNames)
        {
            P policy = factory.create(new ClassBehaviourBinding(dictionary, nodeRef, classQName));
			if (policy instanceof PolicyList)
			{
				policies.addAll(((PolicyList<P>)policy).getPolicies());
			}
			else
			{
				policies.add(policy);
			}
        }
        return policies;
    }
}
