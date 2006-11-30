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
package org.alfresco.repo.action.evaluator;

import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.evaluator.compare.ContentPropertyName;
import org.alfresco.service.cmr.action.ActionCondition;
import org.alfresco.service.cmr.action.ActionServiceException;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

/**
 * Compare mime type evaluator
 * 
 * @author Roy Wetherall
 */
public class CompareMimeTypeEvaluator extends ComparePropertyValueEvaluator
{
	/**
	 * Evaluator constants
	 */
	public static final String NAME = "compare-mime-type";
    
    /**
     * 
     */
    private static final String ERRID_NOT_A_CONTENT_TYPE = "compare_mime_type_evaluator.not_a_content_type";
    private static final String ERRID_NO_PROPERTY_DEFINTION_FOUND = "compare_mime_type_evaluator.no_property_definition_found"; 
    
    /**
     * @see org.alfresco.repo.action.evaluator.ActionConditionEvaluatorAbstractBase#evaluateImpl(org.alfresco.service.cmr.action.ActionCondition, org.alfresco.service.cmr.repository.NodeRef)
     */
    public boolean evaluateImpl(ActionCondition actionCondition, NodeRef actionedUponNodeRef)
    {
        QName propertyQName = (QName)actionCondition.getParameterValue(ComparePropertyValueEvaluator.PARAM_PROPERTY);
        if (propertyQName == null)
        {
            // Default to the standard content property
            actionCondition.setParameterValue(ComparePropertyValueEvaluator.PARAM_PROPERTY, ContentModel.PROP_CONTENT);
        }
        else
        {
            // Ensure that we are dealing with a content property
            QName propertyTypeQName = null;
            PropertyDefinition propertyDefintion = this.dictionaryService.getProperty(propertyQName);
            if (propertyDefintion != null)
            {
                propertyTypeQName = propertyDefintion.getDataType().getName();
                if (DataTypeDefinition.CONTENT.equals(propertyTypeQName) == false)
                {
                    throw new ActionServiceException(ERRID_NOT_A_CONTENT_TYPE);
                }
            }
            else
            {
                throw new ActionServiceException(ERRID_NO_PROPERTY_DEFINTION_FOUND);
            }
        }
        
        // Set the content property to be MIMETYPE
        actionCondition.setParameterValue(ComparePropertyValueEvaluator.PARAM_CONTENT_PROPERTY, ContentPropertyName.MIME_TYPE.toString());

        return super.evaluateImpl(actionCondition, actionedUponNodeRef);
    }

    /**
     * @see org.alfresco.repo.action.ParameterizedItemAbstractBase#addParameterDefinitions(java.util.List)
     */
	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) 
	{
        super.addParameterDefinitions(paramList);
	}
}
