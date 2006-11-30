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
package org.alfresco.benchmark.framework.dataprovider;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.benchmark.framework.BenchmarkUtils;
import org.alfresco.benchmark.framework.dataprovider.PropertyProfile.PropertyRestriction;

/**
 * @author Roy Wetherall
 */

public class DataProviderComponent
{   
    /** Singleton instance */
    private static DataProviderComponent instance;
    
    /** Content cache */
    private List<ContentData> contentCache; 
    
    public static DataProviderComponent getInstance()
    {
        if (instance == null)
        {
            instance = new DataProviderComponent();
        }
        return instance;
    }
    
    private DataProviderComponent()
    {
        this.contentCache = new ArrayList<ContentData>();
               
        for (String dataLocation : BenchmarkUtils.getDataContentLocations())
        {
            cacheContentData(dataLocation); 
        }        
    }
    
    /**
     * Cache the content data
     * 
     * @param locations     the content data locations
     */
    private void cacheContentData(String location)
    {
        File folder = new File(location);
        if (folder == null)
        {
            throw new RuntimeException("Unable to find folder at location " + location);
        }
        
        if (!folder.exists())
        {
            folder.mkdirs();
        }
        
        if (folder.listFiles().length == 0)
        {
            File newFile = new File(folder, "xyz.txt");
            OutputStream os = null;
            try
            {
                os = new BufferedOutputStream(new FileOutputStream(newFile));
                os.write("content".getBytes());
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
            finally
            {
                try { os.close(); } catch (Throwable e) {}
            }
        }
        
        for (File file : folder.listFiles())
        {
            if (file.isDirectory() == true)
            {
                if (file.getName().contains("svn") == false)
                {
                    cacheContentData(file.getPath());
                }
            }
            else
            {
                String filename = file.getName();
                String mimetype = "application/octet-stream";
                String extension = ".bin";
                // TODO need to sort this out ...
                if (filename.endsWith(".txt"))
                {
                    mimetype = "text/plain";
                    extension  = ".txt";
                }
                else if (filename.endsWith(".doc"))
                {
                    mimetype = "application/msword";
                    extension  = ".txt";
                }
                else if (filename.endsWith(".pdf"))
                {
                    mimetype = "application/pdf";
                    extension  = ".pdf";
                }
               
                ContentData contentData = new ContentData(file.getPath(),
                                                          mimetype,
                                                          "UTF-8",
                                                          (int)file.length(),
                                                          extension);
                
                this.contentCache.add(contentData);
            }
        }
    }
    
    /**
     * @see org.alfresco.benchmark.framework.dataprovider.DataProviderComponent#getPropertyData(org.alfresco.benchmark.framework.dataprovider.RepositoryProfile, java.util.List)
     */
    public Map<String, Object> getPropertyData(
            List<PropertyProfile> propertyProfiles)
    {
        // TODO need to consider the repository profile ??
        Map<String, Object> result = new HashMap<String, Object>();
        
        for (PropertyProfile propertyProfile : propertyProfiles)
        {
            Object value = null;
            
            switch (propertyProfile.getPropertyType())
            {
                case TEXT:
                {
                    value = getTextPropertyData(propertyProfile);                    
                    break;
                }
                case CONTENT:
                {
                    value = getContentPropertyData(propertyProfile);
                    break;
                }
                default:
                {
                    throw new RuntimeException("Property type not supported.  Yet!!");
                }
            }
            
            result.put(propertyProfile.getPropertyName(), value);
        }
        
        return result; 
    }
    
    private ContentData getContentPropertyData(PropertyProfile propertyProfile)
    {
        if (this.contentCache == null || this.contentCache.size() == 0)
        {
            if (this.contentCache == null)
            {
                System.out.println("WARNING:  the content cache is null");
            }
            throw new RuntimeException("There is no content data available.  Please place some in one of the content folders specified.");
        }
        
        int randPos = BenchmarkUtils.rand.nextInt(this.contentCache.size());
        return this.contentCache.get(randPos);
    }
    
    private String getTextPropertyData(PropertyProfile propertyProfile)
    {
        int minLength = 5;
        Object minLengthValue = propertyProfile.getRestriction(PropertyRestriction.MIN_LENGTH);
        if (minLengthValue != null)
        {
            minLength = ((Integer)minLengthValue).intValue();
        }
        
        int maxLength = 35;
        Object maxLengthValue = propertyProfile.getRestriction(PropertyRestriction.MAX_LENGTH);
        if (maxLengthValue != null)
        {
            maxLength = ((Integer)maxLengthValue).intValue();
        }
        
        int randSize = BenchmarkUtils.rand.nextInt(maxLength-minLength+1);
        StringBuilder builder = new StringBuilder(minLength+randSize);
        for (int i = 0; i < minLength+randSize; i++)
        {
            builder.append(generateRandomChar());
        }
        return builder.toString();
    }
    
    private char generateRandomChar()
    {
        char result = ' ';
        
        // Dermine whether we want puncuation or alphabet
        int randCharType = BenchmarkUtils.rand.nextInt(100);
        if (randCharType >= 18)
        {
            // Its a normal alphabetic character
            int randAlpa = BenchmarkUtils.rand.nextInt(this.alphabet.length);
            result = this.alphabet[randAlpa];
            
            int randToUpper = BenchmarkUtils.rand.nextInt(100);
            if (randToUpper < 3)
            {
                result = Character.toUpperCase(result);
            }
        }
        
        // else the char returned will be a space
        
        return result;
    }
    
    private char[] alphabet = new char[] {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};

}
