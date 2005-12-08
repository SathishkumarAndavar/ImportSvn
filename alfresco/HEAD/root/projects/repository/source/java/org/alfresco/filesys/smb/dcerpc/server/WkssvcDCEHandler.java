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
package org.alfresco.filesys.smb.dcerpc.server;

import java.io.IOException;

import org.alfresco.filesys.server.config.ServerConfiguration;
import org.alfresco.filesys.smb.Dialect;
import org.alfresco.filesys.smb.SMBStatus;
import org.alfresco.filesys.smb.dcerpc.DCEBuffer;
import org.alfresco.filesys.smb.dcerpc.DCEBufferException;
import org.alfresco.filesys.smb.dcerpc.Wkssvc;
import org.alfresco.filesys.smb.dcerpc.info.ServerInfo;
import org.alfresco.filesys.smb.dcerpc.info.WorkstationInfo;
import org.alfresco.filesys.smb.server.SMBServer;
import org.alfresco.filesys.smb.server.SMBSrvException;
import org.alfresco.filesys.smb.server.SMBSrvSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Wkssvc DCE/RPC Handler Class
 */
public class WkssvcDCEHandler implements DCEHandler
{

    // Debug logging

    private static final Log logger = LogFactory.getLog("org.alfresco.smb.protocol");

    /**
     * Process a WksSvc DCE/RPC request
     * 
     * @param sess SMBSrvSession
     * @param inBuf DCEBuffer
     * @param pipeFile DCEPipeFile
     * @exception IOException
     * @exception SMBSrvException
     */
    public void processRequest(SMBSrvSession sess, DCEBuffer inBuf, DCEPipeFile pipeFile) throws IOException,
            SMBSrvException
    {

        // Get the operation code and move the buffer pointer to the start of the request data

        int opNum = inBuf.getHeaderValue(DCEBuffer.HDR_OPCODE);
        try
        {
            inBuf.skipBytes(DCEBuffer.OPERATIONDATA);
        }
        catch (DCEBufferException ex)
        {
        }

        // Debug

        if (logger.isDebugEnabled() && sess.hasDebug(SMBSrvSession.DBG_DCERPC))
            logger.debug("DCE/RPC WksSvc request=" + Wkssvc.getOpcodeName(opNum));

        // Create the output DCE buffer and add the response header

        DCEBuffer outBuf = new DCEBuffer();
        outBuf.putResponseHeader(inBuf.getHeaderValue(DCEBuffer.HDR_CALLID), 0);

        // Process the request

        boolean processed = false;

        switch (opNum)
        {

        // Get workstation information

        case Wkssvc.NetWkstaGetInfo:
            processed = netWkstaGetInfo(sess, inBuf, outBuf);
            break;

        // Unsupported function

        default:
            break;
        }

        // Return an error status if the request was not processed

        if (processed == false)
        {
            sess.sendErrorResponseSMB(SMBStatus.SRVNotSupported, SMBStatus.ErrSrv);
            return;
        }

        // Set the allocation hint for the response

        outBuf.setHeaderValue(DCEBuffer.HDR_ALLOCHINT, outBuf.getLength());

        // Attach the output buffer to the pipe file

        pipeFile.setBufferedData(outBuf);
    }

    /**
     * Get workstation infomation
     * 
     * @param sess SMBSrvSession
     * @param inBuf DCEPacket
     * @param outBuf DCEPacket
     * @return boolean
     */
    protected final boolean netWkstaGetInfo(SMBSrvSession sess, DCEBuffer inBuf, DCEBuffer outBuf)
    {

        // Decode the request

        String srvName = null;
        int infoLevel = 0;

        try
        {
            inBuf.skipPointer();
            srvName = inBuf.getString(DCEBuffer.ALIGN_INT);
            infoLevel = inBuf.getInt();
        }
        catch (DCEBufferException ex)
        {
            return false;
        }

        // Debug

        if (logger.isDebugEnabled() && sess.hasDebug(SMBSrvSession.DBG_DCERPC))
            logger.debug("NetWkstaGetInfo srvName=" + srvName + ", infoLevel=" + infoLevel);

        // Create the workstation information and set the common values

        WorkstationInfo wkstaInfo = new WorkstationInfo(infoLevel);

        SMBServer srv = sess.getSMBServer();
        wkstaInfo.setWorkstationName(srv.getServerName());
        wkstaInfo.setDomain(srv.getConfiguration().getDomainName());

        // Determine if the server is using the NT SMB dialect and set the platofmr id accordingly

        ServerConfiguration srvConfig = sess.getServer().getConfiguration();
        if (srvConfig != null && srvConfig.getEnabledDialects().hasDialect(Dialect.NT) == true)
        {
            wkstaInfo.setPlatformId(ServerInfo.PLATFORM_NT);
            wkstaInfo.setVersion(5, 1);
        }
        else
        {
            wkstaInfo.setPlatformId(ServerInfo.PLATFORM_OS2);
            wkstaInfo.setVersion(4, 0);
        }

        // Write the server information to the DCE response

        wkstaInfo.writeObject(outBuf, outBuf);
        outBuf.putInt(0);

        // Indicate that the request was processed successfully

        return true;
    }
}
