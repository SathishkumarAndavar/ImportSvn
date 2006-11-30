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
package org.alfresco.repo.webservice.repository;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.webservice.Utils;
import org.alfresco.repo.webservice.types.NamedValue;
import org.alfresco.repo.webservice.types.Reference;
import org.alfresco.repo.webservice.types.ResultSetRow;
import org.alfresco.repo.webservice.types.ResultSetRowNode;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Implementation of a QuerySession that stores the results from a query for
 * associations
 * 
 * @author Roy Wetherall
 */
public class AssociatedQuerySession extends AbstractQuerySession
{
    private static final long serialVersionUID = 6488008047324436124L;

    private transient static Log logger = LogFactory
            .getLog(AssociatedQuerySession.class);

    private Reference node;

    /**
     * Constructs a AssociatedQuerySession
     * 
     * @param batchSize
     *            The batch size to use for this session
     * @param node
     *            The node to retrieve the associations
     */
    public AssociatedQuerySession(int batchSize, Reference node)
    {
        super(batchSize);

        this.node = node;
    }

    /**
     * @see org.alfresco.repo.webservice.repository.QuerySession#getNextResultsBatch(org.alfresco.service.cmr.search.SearchService,
     *      org.alfresco.service.cmr.repository.NodeService,
     *      org.alfresco.service.namespace.NamespaceService)
     */
    public QueryResult getNextResultsBatch(SearchService searchService,
            NodeService nodeService, NamespaceService namespaceService, DictionaryService dictionaryService)
    {
        QueryResult queryResult = null;

        if (this.position != -1)
        {
            if (logger.isDebugEnabled())
                logger.debug("Before getNextResultsBatch: " + toString());

            // create the node ref and get the children from the repository
            NodeRef nodeRef = Utils.convertToNodeRef(this.node, nodeService,
                    searchService, namespaceService);
            List<AssociationRef> assocs = nodeService.getTargetAssocs(nodeRef,
                    RegexQNamePattern.MATCH_ALL);

            int totalRows = assocs.size();
            int lastRow = calculateLastRowIndex(totalRows);
            int currentBatchSize = lastRow - this.position;

            if (logger.isDebugEnabled())
                logger.debug("Total rows = " + totalRows
                        + ", current batch size = " + currentBatchSize);

            org.alfresco.repo.webservice.types.ResultSet batchResults = new org.alfresco.repo.webservice.types.ResultSet();
            org.alfresco.repo.webservice.types.ResultSetRow[] rows = new org.alfresco.repo.webservice.types.ResultSetRow[currentBatchSize];

            int arrPos = 0;
            for (int x = this.position; x < lastRow; x++)
            {
                AssociationRef assoc = assocs.get(x);
                NodeRef childNodeRef = assoc.getTargetRef();
                ResultSetRowNode rowNode = new ResultSetRowNode(childNodeRef
                        .getId(), nodeService.getType(childNodeRef).toString(),
                        null);

                // create columns for all the properties of the node
                // get the data for the row and build up the columns structure
                Map<QName, Serializable> props = nodeService
                        .getProperties(childNodeRef);
                NamedValue[] columns = new NamedValue[props.size()+1];
                int col = 0;
                for (QName propName : props.keySet())
                {
                    columns[col] = Utils.createNamedValue(dictionaryService, propName, props.get(propName)); 
                    col++;
                }
                
                // Now add the system columns containing the association details
                columns[col] = new NamedValue(SYS_COL_ASSOC_TYPE, Boolean.FALSE, assoc.getTypeQName().toString(), null);

                ResultSetRow row = new ResultSetRow();
                row.setRowIndex(x);
                row.setNode(rowNode);
                row.setColumns(columns);

                // add the row to the overall results
                rows[arrPos] = row;
                arrPos++;
            }

            // add the rows to the result set and set the total row count
            batchResults.setRows(rows);
            batchResults.setTotalRowCount(totalRows);

            queryResult = new QueryResult(getId(), batchResults);

            // move the position on
            updatePosition(totalRows, queryResult);

            if (logger.isDebugEnabled())
                logger.debug("After getNextResultsBatch: " + toString());
        }

        return queryResult;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(super.toString());
        builder.append(" (id=").append(getId());
        builder.append(" batchSize=").append(this.batchSize);
        builder.append(" position=").append(this.position);
        builder.append(" node-id=").append(this.node.getUuid()).append(")");
        return builder.toString();
    }
}
