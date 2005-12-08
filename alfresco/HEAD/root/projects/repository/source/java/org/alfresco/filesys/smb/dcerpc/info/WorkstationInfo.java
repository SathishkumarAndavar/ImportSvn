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
package org.alfresco.filesys.smb.dcerpc.info;

import org.alfresco.filesys.smb.dcerpc.DCEBuffer;
import org.alfresco.filesys.smb.dcerpc.DCEWriteable;

/**
 * Workstation Information Class
 */
public class WorkstationInfo implements DCEWriteable
{

    // Supported information levels

    public static final int InfoLevel100 = 100;

    // Information level

    private int m_infoLevel;

    // Server information

    private int m_platformId;
    private String m_name;
    private String m_domain;
    private int m_verMajor;
    private int m_verMinor;

    private String m_userName;
    private String m_logonDomain;
    private String m_otherDomain;

    /**
     * Default constructor
     */
    public WorkstationInfo()
    {
    }

    /**
     * Class constructor
     * 
     * @param lev int
     */
    public WorkstationInfo(int lev)
    {
        m_infoLevel = lev;
    }

    /**
     * Get the information level
     * 
     * @return int
     */
    public final int getInformationLevel()
    {
        return m_infoLevel;
    }

    /**
     * Get the workstation name
     * 
     * @return String
     */
    public final String getWorkstationName()
    {
        return m_name;
    }

    /**
     * Get the domain/workgroup
     * 
     * @return String
     */
    public final String getDomain()
    {
        return m_domain;
    }

    /**
     * Get the workstation platform id
     * 
     * @return int
     */
    public final int getPlatformId()
    {
        return m_platformId;
    }

    /**
     * Get the workstation major version
     * 
     * @return int
     */
    public final int getMajorVersion()
    {
        return m_verMajor;
    }

    /**
     * Get the workstation minor version
     * 
     * @return int
     */
    public final int getMinorVersion()
    {
        return m_verMinor;
    }

    /**
     * Reutrn the user name
     * 
     * @return String
     */
    public final String getUserName()
    {
        return m_userName;
    }

    /**
     * Return the workstations logon domain.
     * 
     * @return java.lang.String
     */
    public String getLogonDomain()
    {
        return m_logonDomain;
    }

    /**
     * Return the list of domains that the workstation is enlisted in.
     * 
     * @return java.lang.String
     */
    public String getOtherDomains()
    {
        return m_otherDomain;
    }

    /**
     * Set the logon domain name.
     * 
     * @param logdom java.lang.String
     */
    public void setLogonDomain(String logdom)
    {
        m_logonDomain = logdom;
    }

    /**
     * Set the other domains that this workstation is enlisted in.
     * 
     * @param othdom java.lang.String
     */
    public void setOtherDomains(String othdom)
    {
        m_otherDomain = othdom;
    }

    /**
     * Set the workstation name
     * 
     * @param name String
     */
    public final void setWorkstationName(String name)
    {
        m_name = name;
    }

    /**
     * Set the domain/workgroup
     * 
     * @param domain String
     */
    public final void setDomain(String domain)
    {
        m_domain = domain;
    }

    /**
     * Set the information level
     * 
     * @param lev int
     */
    public final void setInformationLevel(int lev)
    {
        m_infoLevel = lev;
    }

    /**
     * Set the platform id
     * 
     * @param id int
     */
    public final void setPlatformId(int id)
    {
        m_platformId = id;
    }

    /**
     * Set the version
     * 
     * @param verMajor int
     * @param verMinor int
     */
    public final void setVersion(int verMajor, int verMinor)
    {
        m_verMajor = verMajor;
        m_verMinor = verMinor;
    }

    /**
     * Set the logged in user name
     * 
     * @param user String
     */
    public final void setUserName(String user)
    {
        m_userName = user;
    }

    /**
     * Clear the string values
     */
    protected final void clearStrings()
    {

        // Clear the string values

        m_userName = null;
        m_domain = null;
        m_logonDomain = null;
        m_otherDomain = null;
    }

    /**
     * Write a workstation information structure
     * 
     * @param buf DCEBuffer
     * @param strBuf DCEBuffer
     */
    public void writeObject(DCEBuffer buf, DCEBuffer strBuf)
    {

        // Output the workstation information structure

        buf.putInt(getInformationLevel());
        buf.putPointer(true);

        // Output the required information level

        switch (getInformationLevel())
        {

        // Level 100

        case InfoLevel100:
            buf.putInt(getPlatformId());
            buf.putPointer(true);
            buf.putPointer(true);
            buf.putInt(getMajorVersion());
            buf.putInt(getMinorVersion());

            strBuf.putString(getWorkstationName(), DCEBuffer.ALIGN_INT, true);
            strBuf.putString(getDomain() != null ? getDomain() : "", DCEBuffer.ALIGN_INT, true);
            break;

        // Level 101

        case 101:
            break;

        // Level 102

        case 102:
            break;
        }
    }

    /**
     * Return the workstation information as a string
     * 
     * @return String
     */
    public String toString()
    {
        StringBuffer str = new StringBuffer();
        str.append("[");

        str.append(getWorkstationName());
        str.append(":Domain=");
        str.append(getDomain());
        str.append(":User=");
        str.append(getUserName());
        str.append(":Id=");
        str.append(getPlatformId());

        str.append(":v");
        str.append(getMajorVersion());
        str.append(".");
        str.append(getMinorVersion());

        // Optional strings

        if (getLogonDomain() != null)
        {
            str.append(":Logon=");
            str.append(getLogonDomain());
        }

        if (getOtherDomains() != null)
        {
            str.append(":Other=");
            str.append(getOtherDomains());
        }

        // Return the workstation information as a string

        str.append("]");
        return str.toString();
    }
}
