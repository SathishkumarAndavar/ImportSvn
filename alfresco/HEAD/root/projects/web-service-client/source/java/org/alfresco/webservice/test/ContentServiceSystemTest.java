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
package org.alfresco.webservice.test;

import java.io.File;
import java.io.InputStream;

import org.alfresco.webservice.content.Content;
import org.alfresco.webservice.repository.UpdateResult;
import org.alfresco.webservice.types.CML;
import org.alfresco.webservice.types.CMLCreate;
import org.alfresco.webservice.types.ContentFormat;
import org.alfresco.webservice.types.NamedValue;
import org.alfresco.webservice.types.ParentReference;
import org.alfresco.webservice.types.Predicate;
import org.alfresco.webservice.types.Reference;
import org.alfresco.webservice.util.Constants;
import org.alfresco.webservice.util.ContentUtils;

public class ContentServiceSystemTest extends BaseWebServiceSystemTest
{
   private static final String CONTENT = "This is a small piece of content to test the create service call";
   private static final String UPDATED_CONTENT = "This is some updated content to test the write service call";
   
   private String fileName = "unit-test.txt";
   
   public void testContentService() 
       throws Exception
   {
       ParentReference parentRef = new ParentReference();
       parentRef.setStore(BaseWebServiceSystemTest.store);
       parentRef.setUuid(BaseWebServiceSystemTest.rootReference.getUuid());
       parentRef.setAssociationType(Constants.ASSOC_CHILDREN);
       parentRef.setChildName(Constants.ASSOC_CHILDREN);
       
       NamedValue[] properties = new NamedValue[]{new NamedValue(Constants.PROP_NAME, false, this.fileName, null)};
       CMLCreate create = new CMLCreate("1", parentRef, null, null, null, Constants.TYPE_CONTENT, properties);
       CML cml = new CML();
       cml.setCreate(new CMLCreate[]{create});
       UpdateResult[] result = this.repositoryService.update(cml);     
       
       Reference newContentNode = result[0].getDestination();       
       String property = Constants.PROP_CONTENT;
       Predicate predicate = new Predicate(new Reference[]{newContentNode}, BaseWebServiceSystemTest.store, null);
              
       // First check a node that has no content set
       Content[] contents1 = this.contentService.read(predicate, property);
       assertNotNull(contents1);
       assertEquals(1, contents1.length);
       Content content1 = contents1[0];
       assertNotNull(content1);
       assertEquals(0, content1.getLength());
       assertEquals(newContentNode.getUuid(), content1.getNode().getUuid());
       assertEquals(property, content1.getProperty());
       assertNull(content1.getUrl());
       assertNull(content1.getFormat());
       
       // Write content 
       Content content2 = this.contentService.write(newContentNode, property, CONTENT.getBytes(), new ContentFormat(Constants.MIMETYPE_TEXT_PLAIN, "UTF-8"));
       assertNotNull(content2);
       assertTrue((content2.getLength() > 0));
       assertEquals(newContentNode.getUuid(), content2.getNode().getUuid());
       assertEquals(property, content2.getProperty());
       assertNotNull(content2.getUrl());
       assertNotNull(content2.getFormat());       
       ContentFormat format2 = content2.getFormat();
       assertEquals(Constants.MIMETYPE_TEXT_PLAIN, format2.getMimetype());
       assertEquals("UTF-8", format2.getEncoding());
       assertEquals(CONTENT, ContentUtils.getContentAsString(content2));
              
       // Read content
       Content[] contents3 = this.contentService.read(predicate, property);
       assertNotNull(contents3);
       assertEquals(1, contents3.length);
       Content content3 = contents3[0];
       assertNotNull(content3);
       assertTrue((content3.getLength() > 0));
       assertEquals(newContentNode.getUuid(), content3.getNode().getUuid());
       assertEquals(property, content3.getProperty());
       assertNotNull(content3.getUrl());
       assertNotNull(content3.getFormat());       
       ContentFormat format3 = content3.getFormat();
       assertEquals(Constants.MIMETYPE_TEXT_PLAIN, format3.getMimetype());
       assertEquals("UTF-8", format3.getEncoding());
       assertEquals(CONTENT, ContentUtils.getContentAsString(content3));
       
       // Update content
       Content content4 = this.contentService.write(newContentNode, property, UPDATED_CONTENT.getBytes(), new ContentFormat(Constants.MIMETYPE_TEXT_CSS, "UTF-8"));
       assertNotNull(content4);
       assertTrue((content4.getLength() > 0));
       assertEquals(newContentNode.getUuid(), content4.getNode().getUuid());
       assertEquals(property, content4.getProperty());
       assertNotNull(content4.getUrl());
       assertNotNull(content4.getFormat());       
       ContentFormat format4 = content4.getFormat();
       assertEquals(Constants.MIMETYPE_TEXT_CSS, format4.getMimetype());
       assertEquals("UTF-8", format4.getEncoding());
       assertEquals(UPDATED_CONTENT, ContentUtils.getContentAsString(content4));
       
       // Read updated content
       Content[] contents5 = this.contentService.read(predicate, property);
       assertNotNull(contents5);
       assertEquals(1, contents5.length);
       Content content5 = contents5[0];
       assertNotNull(content5);
       assertTrue((content5.getLength() > 0));
       assertEquals(newContentNode.getUuid(), content5.getNode().getUuid());
       assertEquals(property, content5.getProperty());
       assertNotNull(content5.getUrl());
       assertNotNull(content5.getFormat());       
       ContentFormat format5 = content5.getFormat();
       assertEquals(Constants.MIMETYPE_TEXT_CSS, format5.getMimetype());
       assertEquals("UTF-8", format5.getEncoding());
       assertEquals(UPDATED_CONTENT, ContentUtils.getContentAsString(content5));
       
       // Clear content
       Content[] contents6 = this.contentService.clear(predicate, property);
       assertNotNull(contents6);
       assertEquals(1, contents6.length);
       Content content6 = contents6[0];
       assertNotNull(content6);
       assertEquals(0, content6.getLength());
       assertEquals(newContentNode.getUuid(), content6.getNode().getUuid());
       assertEquals(property, content6.getProperty());
       assertNull(content6.getUrl());
       assertNull(content6.getFormat());
       
       // Read cleared content
       Content[] contents7 = this.contentService.read(predicate, property);
       assertNotNull(contents7);
       assertEquals(1, contents7.length);
       Content content7 = contents7[0];
       assertNotNull(content7);
       assertEquals(0, content7.getLength());
       assertEquals(newContentNode.getUuid(), content7.getNode().getUuid());
       assertEquals(property, content7.getProperty());
       assertNull(content7.getUrl());
       assertNull(content7.getFormat());
   }
   
   /**
    * Test uploading content from file
    * 
    * @throws Exception
    */
   public void testUploadContentFromFile() throws Exception
   {
       // Create the parent reference
       ParentReference parentRef = new ParentReference();
       parentRef.setStore(BaseWebServiceSystemTest.store);
       parentRef.setUuid(BaseWebServiceSystemTest.rootReference.getUuid());
       parentRef.setAssociationType(Constants.ASSOC_CHILDREN);
       parentRef.setChildName(Constants.ASSOC_CHILDREN);
       
       // Create the content
       NamedValue[] properties = new NamedValue[]{new NamedValue(Constants.PROP_NAME, false, "quick.doc", null)};
       CMLCreate create = new CMLCreate("1", parentRef, null, null, null, Constants.TYPE_CONTENT, properties);
       CML cml = new CML();
       cml.setCreate(new CMLCreate[]{create});
       UpdateResult[] result = this.repositoryService.update(cml);     
       
       // Get the create node and create the format
       Reference newContentNode = result[0].getDestination();              
       ContentFormat format = new ContentFormat("application/msword", "UTF-8");  
       
       // Open the file and convert to byte array
       InputStream viewStream = getClass().getClassLoader().getResourceAsStream("org/alfresco/webservice/test/resources/quick.doc");
       byte[] bytes = ContentUtils.convertToByteArray(viewStream);
       
       // Write the content
       this.contentService.write(newContentNode, Constants.PROP_CONTENT, bytes, format);
       
       // Try and get the content, saving it to a file
       Content[] contents = this.contentService.read(convertToPredicate(newContentNode), Constants.PROP_CONTENT);
       assertNotNull(contents);
       assertEquals(1, contents.length);
       Content content = contents[0];
       File tempFile = File.createTempFile("testDoc", ".doc");
       System.out.println(tempFile.getPath());
       ContentUtils.copyContentToFile(content, tempFile);
   }
   
   /**
    * Test uploading image from file
    * 
    * @throws Exception
    */
   public void testUploadImageFromFile() throws Exception
   {
       // Create the parent reference
       ParentReference parentRef = new ParentReference();
       parentRef.setStore(BaseWebServiceSystemTest.store);
       parentRef.setUuid(BaseWebServiceSystemTest.rootReference.getUuid());
       parentRef.setAssociationType(Constants.ASSOC_CHILDREN);
       parentRef.setChildName(Constants.ASSOC_CHILDREN);
       
       // Create the content
       NamedValue[] properties = new NamedValue[]{new NamedValue(Constants.PROP_NAME, false, "test.jpg", null)};
       CMLCreate create = new CMLCreate("1", parentRef, null, null, null, Constants.TYPE_CONTENT, properties);
       CML cml = new CML();
       cml.setCreate(new CMLCreate[]{create});
       UpdateResult[] result = this.repositoryService.update(cml);     
       
       // Get the created node and create the format
       Reference newContentNode = result[0].getDestination();              
       ContentFormat format = new ContentFormat("image/jpeg", "UTF-8");  
       
       // Open the file and convert to byte array
       InputStream viewStream = getClass().getClassLoader().getResourceAsStream("org/alfresco/webservice/test/resources/test.jpg");
       byte[] bytes = ContentUtils.convertToByteArray(viewStream);
       
       // Write the content
       this.contentService.write(newContentNode, Constants.PROP_CONTENT, bytes, format);
       
       // Try and get the content, saving it to a file
       Content[] contents = this.contentService.read(convertToPredicate(newContentNode), Constants.PROP_CONTENT);
       assertNotNull(contents);
       assertEquals(1, contents.length);
       Content content = contents[0];
       File tempFile = File.createTempFile("testImage", ".jpg");
       System.out.println(tempFile.getPath());
       ContentUtils.copyContentToFile(content, tempFile);

   }
}
