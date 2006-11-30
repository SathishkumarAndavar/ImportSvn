/*-----------------------------------------------------------------------------
*  Copyright 2006 Alfresco Inc.
*  
*  Licensed under the Mozilla Public License version 1.1
*  with a permitted attribution clause. You may obtain a
*  copy of the License at:
*  
*      http://www.alfresco.org/legal/license.txt
*  
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
*  either express or implied. See the License for the specific
*  language governing permissions and limitations under the
*  License.
*  
*  
*  Author  Jon Cox  <jcox@alfresco.com>
*  File    VirtWebappRegistryMBean.java
*----------------------------------------------------------------------------*/


package org.alfresco.mbeans;

public interface VirtWebappRegistryMBean
{
    public void setMoo(int moo);
    public int  getMoo();
    public void setVirtWebapp(String virtWebapp);
    public String[] getVirtWebapps();
}
