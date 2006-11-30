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
package org.alfresco.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of a config section
 * 
 * @author gavinc
 */
public class ConfigSectionImpl implements ConfigSection
{
    private String evaluator;
    private String condition;
    private boolean replace = false;
    private List<ConfigElement> configElements;

    public ConfigSectionImpl(String evaluator, String condition, boolean replace)
    {
        this.evaluator = evaluator;
        this.condition = condition;
        this.replace = replace;
        this.configElements = new ArrayList<ConfigElement>();
        
        // don't allow empty strings
        if (this.evaluator != null && this.evaluator.length() == 0)
        {
           throw new ConfigException("The 'evaluator' attribute must have a value if it is present");
        }
        
        if (this.condition != null && this.condition.length() == 0)
        {
           throw new ConfigException("The 'condition' attribute must have a value if it is present");
        }
    }

    /**
     * @see org.alfresco.config.ConfigSection#getEvaluator()
     */
    public String getEvaluator()
    {
        return this.evaluator;
    }

    /**
     * @see org.alfresco.config.ConfigSection#getCondition()
     */
    public String getCondition()
    {
        return this.condition;
    }

    /**
     * @see org.alfresco.config.ConfigSection#getConfigElements()
     */
    public List<ConfigElement> getConfigElements()
    {
        return this.configElements;
    }

    /**
     * Adds a config element to the results for the lookup
     * 
     * @param configElement
     */
    public void addConfigElement(ConfigElement configElement)
    {
        this.configElements.add(configElement);
    }

    /**
     * @see org.alfresco.config.ConfigSection#isGlobal()
     */
    public boolean isGlobal()
    {
        boolean global = false;

        if (this.evaluator == null && this.condition == null)
        {
            global = true;
        }

        return global;
    }
    
    /**
     * @see org.alfresco.config.ConfigSection#isReplace()
     */
    public boolean isReplace()
    {
       return this.replace;
    }

    public String toString()
    {
        StringBuilder buffer = new StringBuilder(super.toString());
        buffer.append(" (evaluator=").append(this.evaluator);
        buffer.append(" condition=").append(this.condition);
        buffer.append(" replace=").append(this.replace).append(")");
        return buffer.toString();
    }
}
