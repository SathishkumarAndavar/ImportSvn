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
package org.alfresco.config.element;

import java.util.Iterator;
import java.util.List;

import org.alfresco.config.ConfigElement;

/**
 * Implementation of a generic configuration element. This class can handle the
 * representation of any config element in a generic manner.
 * 
 * @author gavinc
 */
public class GenericConfigElement extends ConfigElementAdapter
{
    /**
     * Default constructor
     * 
     * @param name Name of the config element
     */
    public GenericConfigElement(String name) 
    {
        super(name);
    }
    
    /**
     * @see org.alfresco.web.config.ConfigElement#combine(org.alfresco.web.config.ConfigElement)
     */
    public ConfigElement combine(ConfigElement configElement)
    {
        GenericConfigElement combined = new GenericConfigElement(this.name);
        combined.setValue(configElement.getValue());

        // add the existing attributes to the new instance
        if (this.attributes != null)
        {
            Iterator<String> attrs = this.getAttributes().keySet().iterator();
            while (attrs.hasNext())
            {
                String attrName = attrs.next();
                String attrValue = configElement.getAttribute(attrName);
                combined.addAttribute(attrName, attrValue);
            }
        }

        // add/combine the attributes from the given instance
        if (configElement.getAttributes() != null)
        {
            Iterator<String> attrs = configElement.getAttributes().keySet().iterator();
            while (attrs.hasNext())
            {
                String attrName = attrs.next();
                String attrValue = configElement.getAttribute(attrName);
                combined.addAttribute(attrName, attrValue);
            }
        }

        // add the existing children to the new instance
        List<ConfigElement> kids = this.getChildren();
        if (kids != null)
        {
            for (int x = 0; x < kids.size(); x++)
            {
                ConfigElement ce = kids.get(x);
                combined.addChild(ce);
            }
        }

        // add the children from the given instance
        kids = configElement.getChildren();
        if (kids != null)
        {
            for (int x = 0; x < kids.size(); x++)
            {
                ConfigElement ce = kids.get(x);
                combined.addChild(ce);
            }
        }

        return combined;
    }

    /**
     * Adds the attribute with the given name and value
     * 
     * @param name
     *            Name of the attribute
     * @param value
     *            Value of the attribute
     */
    public void addAttribute(String name, String value)
    {
        this.attributes.put(name, value);
    }

    /**
     * Adds the given config element as a child of this element
     * 
     * @param configElement
     *            The child config element
     */
    public void addChild(ConfigElement configElement)
    {
        this.children.add(configElement);
    }
}
