/*
 * Copyright (C) 2006 Alfresco, Inc.
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

package org.alfresco.repo.avm.hibernate;

import java.util.List;

import org.alfresco.repo.avm.AVMAspectName;
import org.alfresco.repo.avm.AVMAspectNameDAO;
import org.alfresco.repo.avm.AVMNode;
import org.alfresco.service.namespace.QName;
import org.hibernate.Query;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * Hibernate implementation of AVMAspectNameDAO.
 * @author britt
 */
public class AVMAspectNameDAOHibernate extends HibernateDaoSupport 
    implements AVMAspectNameDAO
{
    /**
     * Persist an aspect name.
     * @param aspectName The item to persist.
     */
    public void save(AVMAspectName aspectName)
    {
        getSession().save(aspectName);
    }
    
    /**
     * Delete an Aspect Name.
     * @param aspectName The item to delete.
     */
    public void delete(AVMAspectName aspectName)
    {
        getSession().delete(aspectName);
    }

    /**
     * Delete a single aspect name from a node.
     * @param node The node.
     * @param aspectName The aspect name.
     */
    public void delete(AVMNode node, QName aspectName)
    {
        Query delete =
            getSession().createQuery(
                    "delete from AVMAspectNameImpl aa where aa.node = :node and aa.name = :name");
        delete.setEntity("node", node);
        delete.setParameter("name", aspectName);
        delete.executeUpdate();
    }

    /**
     * Delete all Aspect Names on a given node.
     * @param node The given node.
     */
    public void delete(AVMNode node)
    {
        Query delete =
            getSession().createQuery("delete from AVMAspectNameImpl aa where aa.node = :node");
        delete.setEntity("node", node);
        delete.executeUpdate();
    }

    /**
     * Get all Aspect Names for a given node.
     * @param node The AVM Node.
     * @return A List of AVMAspectNames.
     */
    @SuppressWarnings("unchecked")
    public List<AVMAspectName> get(AVMNode node)
    {
        Query query = 
            getSession().createQuery("from AVMAspectNameImpl aa where aa.node = :node");
        query.setEntity("node", node);
        return (List<AVMAspectName>)query.list();
    }
    
    /**
     * Does the given node have the given asset.
     * @param node The AVM node.
     * @param name The QName of the Aspect.
     * @return Whether the aspect is there.
     */
    public boolean exists(AVMNode node, QName name)
    {
        Query query =
            getSession().createQuery(
                    "from AVMAspectNameImpl aa where aa.node = :node and aa.name = :name");
        query.setEntity("node", node);
        query.setParameter("name", name);
        return query.uniqueResult() != null;
    }
}
