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
package org.alfresco.repo.audit.model;

import org.alfresco.repo.audit.AuditModel;
import org.alfresco.repo.audit.RecordOptions;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;

public class RecordOptionsImpl implements XMLModelElement, RecordOptions
{
    private static Log s_logger = LogFactory.getLog(RecordOptionsImpl.class);
    
    private TrueFalseUnset recordPath = TrueFalseUnset.UNSET;

    private TrueFalseUnset recordFilters = TrueFalseUnset.UNSET;

    private TrueFalseUnset recordSerializedReturnValue = TrueFalseUnset.UNSET;

    private TrueFalseUnset recordSerializedExceptions = TrueFalseUnset.UNSET;

    private TrueFalseUnset recordSerializedMethodArguments = TrueFalseUnset.UNSET;

    private TrueFalseUnset recordSerializedKeyPropertiesBeforeEvaluation = TrueFalseUnset.UNSET;

    private TrueFalseUnset recordSerializedKeyPropertiesAfterEvaluation = TrueFalseUnset.UNSET;

    public RecordOptionsImpl()
    {
        super();
    }

    public static RecordOptionsImpl mergeRecordOptions(RecordOptions primary, RecordOptions secondary)
    {
        RecordOptionsImpl answer = new RecordOptionsImpl();
        setOptions(answer, primary, true);
        setOptions(answer, secondary, false);
        return answer;
    }
    
    private static void setOptions(RecordOptionsImpl on, RecordOptions from, boolean force)
    {
        if(force || on.recordFilters.equals( TrueFalseUnset.UNSET))
        {
            on.recordFilters = from.getRecordFilters();
        }
        if(force || on.recordPath.equals( TrueFalseUnset.UNSET))
        {
            on.recordPath = from.getRecordPath();
        }
        if(force || on.recordSerializedExceptions.equals( TrueFalseUnset.UNSET))
        {
            on.recordSerializedExceptions = from.getRecordSerializedExceptions();
        }
        if(force || on.recordSerializedKeyPropertiesAfterEvaluation.equals( TrueFalseUnset.UNSET))
        {
            on.recordSerializedKeyPropertiesAfterEvaluation = from.getRecordSerializedKeyPropertiesAfterEvaluation();
        }
        if(force || on.recordSerializedKeyPropertiesBeforeEvaluation.equals( TrueFalseUnset.UNSET))
        {
            on.recordSerializedKeyPropertiesBeforeEvaluation = from.getRecordSerializedKeyPropertiesBeforeEvaluation();
        }
        if(force || on.recordSerializedMethodArguments.equals( TrueFalseUnset.UNSET))
        {
            on.recordSerializedMethodArguments = from.getRecordSerializedMethodArguments();
        }
        if(force || on.recordSerializedReturnValue.equals( TrueFalseUnset.UNSET))
        {
            on.recordSerializedReturnValue = from.getRecordSerializedReturnValue();
        }
    }
    
    public TrueFalseUnset getRecordFilters()
    {
        return recordFilters;
    }

    public TrueFalseUnset getRecordPath()
    {
        return recordPath;
    }

    public TrueFalseUnset getRecordSerializedExceptions()
    {
        return recordSerializedExceptions;
    }

    public TrueFalseUnset getRecordSerializedKeyPropertiesAfterEvaluation()
    {
        return recordSerializedKeyPropertiesAfterEvaluation;
    }

    public TrueFalseUnset getRecordSerializedKeyPropertiesBeforeEvaluation()
    {
        return recordSerializedKeyPropertiesBeforeEvaluation;
    }

    public TrueFalseUnset getRecordSerializedMethodArguments()
    {
        return recordSerializedMethodArguments;
    }

    public TrueFalseUnset getRecordSerializedReturnValue()
    {
        return recordSerializedReturnValue;
    }

    public void configure(Element recordOptionElement, NamespacePrefixResolver namespacePrefixResolver)
    {
        Element recordFiltersElement = recordOptionElement.element(AuditModel.EL_RECORD_FILTERS);
        if (recordFiltersElement != null)
        {
            recordFilters = TrueFalseUnset.getTrueFalseUnset(recordFiltersElement.getStringValue());
        }

        Element recordPathElement = recordOptionElement.element(AuditModel.EL_RECORD_PATH);
        if (recordPathElement != null)
        {
            recordPath = TrueFalseUnset.getTrueFalseUnset(recordPathElement.getStringValue());
        }

        Element recordSerAgrsElement = recordOptionElement.element(AuditModel.EL_RECORD_SER_ARGS);
        if (recordSerAgrsElement != null)
        {
            recordSerializedMethodArguments = TrueFalseUnset.getTrueFalseUnset(recordSerAgrsElement.getStringValue());
        }

        Element recordSerExElement = recordOptionElement.element(AuditModel.EL_RECORD_SER_EX);
        if (recordSerExElement != null)
        {
            recordSerializedExceptions = TrueFalseUnset.getTrueFalseUnset(recordSerExElement.getStringValue());
        }

        Element recordSerPropAfterElement = recordOptionElement.element(AuditModel.EL_RECORD_SER_PROP_AFTER);
        if (recordSerPropAfterElement != null)
        {
            recordSerializedKeyPropertiesAfterEvaluation = TrueFalseUnset.getTrueFalseUnset(recordSerPropAfterElement
                    .getStringValue());
        }

        Element recordSerPropBeforeElement = recordOptionElement.element(AuditModel.EL_RECORD_SER_PROP_BEFORE);
        if (recordSerPropBeforeElement != null)
        {
            recordSerializedKeyPropertiesBeforeEvaluation = TrueFalseUnset.getTrueFalseUnset(recordSerPropBeforeElement
                    .getStringValue());
        }

        Element recordSerRetElement = recordOptionElement.element(AuditModel.EL_RECORD_SER_RETURN_VAL);
        if (recordSerRetElement != null)
        {
            recordSerializedReturnValue = TrueFalseUnset.getTrueFalseUnset(recordSerRetElement.getStringValue());
        }

    }

    @Override
    public String toString()
    {
       StringBuilder builder = new StringBuilder();
       builder.append("Record Options(");
       builder.append("Filters=").append(getRecordFilters());
       builder.append(",Path=").append(getRecordPath());
       builder.append(",Exception=").append(getRecordSerializedExceptions());
       builder.append(",PropertiesBefore=").append(getRecordSerializedKeyPropertiesAfterEvaluation());
       builder.append(",PropertiesAfter=").append(getRecordSerializedKeyPropertiesBeforeEvaluation());
       builder.append(",Args=").append(getRecordSerializedMethodArguments());
       builder.append(",Return=").append(getRecordSerializedReturnValue());
       builder.append(")");
       return builder.toString();
    }

    
}
