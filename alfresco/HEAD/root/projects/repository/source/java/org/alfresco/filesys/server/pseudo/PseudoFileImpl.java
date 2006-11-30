/*
 * Copyright (C) 2005-2006 Alfresco, Inc.
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
package org.alfresco.filesys.server.pseudo;

import java.util.Enumeration;

import org.alfresco.filesys.alfresco.DesktopAction;
import org.alfresco.filesys.alfresco.DesktopActionTable;
import org.alfresco.filesys.server.SrvSession;
import org.alfresco.filesys.server.filesys.FileName;
import org.alfresco.filesys.server.filesys.TreeConnection;
import org.alfresco.filesys.server.pseudo.MemoryPseudoFile;
import org.alfresco.filesys.server.pseudo.PseudoFile;
import org.alfresco.filesys.server.pseudo.PseudoFileInterface;
import org.alfresco.filesys.server.state.FileState;
import org.alfresco.filesys.smb.server.SMBSrvSession;
import org.alfresco.filesys.smb.server.repo.ContentContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Alfresco Filesystem Driver Pseudo File Implementation
 *
 * <p>Pseudo file implementation for the Alfresco filesystem drivers.
 * 
 * @author gkspencer
 */
public class PseudoFileImpl implements PseudoFileInterface
{
    // Logging
    
    private static final Log logger = LogFactory.getLog(PseudoFileImpl.class);
    
    /**
     * Check if the specified path refers to a pseudo file
     * 
     * @param sess SrvSession
     * @param tree TreeConnection
     * @param path String
     * @return boolean
     */
    public boolean isPseudoFile(SrvSession sess, TreeConnection tree, String path)
    {
        // Check if the path is for a pseudo file
        
        ContentContext ctx = (ContentContext) tree.getContext();
        boolean isPseudo = false;
        
        String[] paths = FileName.splitPath( path);
        FileState fstate = getStateForPath( ctx, paths[0]);
        
        if ( fstate != null && fstate.hasPseudoFiles())
        {
            // Check if there is a matching pseudo file
            
            PseudoFile pfile = fstate.getPseudoFileList().findFile( paths[1], false);
            if ( pfile != null)
                isPseudo = true;
        }
        else
        {
        	// Check if the file name matches a pseudo-file name in the desktop actions list
        	
        	if ( ctx.hasDesktopActions())
        	{
	            DesktopActionTable actions = ctx.getDesktopActions();
	        	if ( actions.getActionViaPseudoName( paths[1]) != null)
	        		isPseudo = true;
        	}

        	// Check if the URL file is enabled
        		
    		if ( isPseudo == false && ctx.hasURLFile())
    		{
    			// Check if it is the URL file name
    			
    			if ( ctx.getURLFileName().equals( paths[1]))
    				isPseudo = true;
    		}
        }
        
        // Return the pseudo file status
        
        return isPseudo;
    }

    /**
     * Return the pseudo file for the specified path, or null if the path is not a pseudo file
     * 
     * @param sess SrvSession
     * @param tree TreeConnection
     * @param path String
     * @return PseudoFile
     */
    public PseudoFile getPseudoFile(SrvSession sess, TreeConnection tree, String path)
    {
        // Check if the path is for a pseudo file
        
        ContentContext ctx = (ContentContext) tree.getContext();
        
        String[] paths = FileName.splitPath( path);
        FileState fstate = getStateForPath( ctx, paths[0]);
        
        if ( fstate != null && fstate.hasPseudoFiles())
        {
            // Check if there is a matching pseudo file
            
            PseudoFile pfile = fstate.getPseudoFileList().findFile( paths[1], false);
            if ( pfile != null)
                return pfile;
        }
        
        // Not a pseudo file
        
        return null;
    }

    /**
     * Add pseudo files to a folder so that they appear in a folder search
     * 
     * @param sess SrvSession
     * @param tree TreeConnection
     * @param path String
     * @return int
     */
    public int addPseudoFilesToFolder(SrvSession sess, TreeConnection tree, String path)
    {
        // Access the device context

        int pseudoCnt = 0;
        ContentContext ctx = (ContentContext) tree.getContext();
        FileState fstate = getStateForPath( ctx, path);

        // Check if pseudo files have already been added for this folder
        
        if ( fstate.hasPseudoFiles())
            return 0;
        
        // Check if this is a CIFS session
        
        boolean isCIFS = sess instanceof SMBSrvSession;
        
        // Add the desktop action pseudo files
        
        if ( isCIFS && ctx.numberOfDesktopActions() > 0)
        {
            // If the file state is null create a file state for the path

            if ( fstate == null)
                ctx.getStateTable().findFileState( path, true, true);
            
            // Add the desktop action pseudo files
            
            DesktopActionTable actions = ctx.getDesktopActions();
            Enumeration<String> actionNames = actions.enumerateActionNames();
            
            while(actionNames.hasMoreElements())
            {
            	// Get the current desktop action
            	
            	String name = actionNames.nextElement();
            	DesktopAction action = actions.getAction(name);
            	
            	// Add the pseudo file for the desktop action
            	
            	if ( action.hasPseudoFile())
            	{
            		fstate.addPseudoFile( action.getPseudoFile());
            		pseudoCnt++;

            		// DEBUG
                    
                    if ( logger.isInfoEnabled())
                        logger.info("Added desktop action " + action.getName() + " for " + path);
            	}
            }
        }

        // Add the URL link pseudo file, if enabled
        
        if ( isCIFS && ctx.hasURLFile())
        {
            // Make sure the state has the associated node details
            
            if ( fstate.getNodeRef() != null)
            {
                // Build the URL file data
    
                StringBuilder urlStr = new StringBuilder();
            
                urlStr.append("[InternetShortcut]\r\n");
                urlStr.append("URL=");
                urlStr.append(ctx.getURLPrefix());
                urlStr.append("navigate/browse/workspace/SpacesStore/");
                urlStr.append( fstate.getNodeRef().getId());
                urlStr.append("\r\n");
    
                // Create the in memory pseudo file for the URL link
                
                byte[] urlData = urlStr.toString().getBytes();
                
                MemoryPseudoFile urlFile = new MemoryPseudoFile( ctx.getURLFileName(), urlData);
                fstate.addPseudoFile( urlFile);
                
                // Update the count of files added
                
                pseudoCnt++;
                
                // DEBUG
                
                if ( logger.isInfoEnabled())
                    logger.info("Added URL link pseudo file for " + path);
            }
        }
        
        // Return the count of pseudo files added
        
        return pseudoCnt;
    }

    /**
     * Delete a pseudo file
     * 
     * @param sess SrvSession
     * @param tree TreeConnection
     * @param path String
     */
    public void deletePseudoFile(SrvSession sess, TreeConnection tree, String path)
    {
        // Access the device context

        ContentContext ctx = (ContentContext) tree.getContext();
        
        // Get the file state for the parent folder
        
        String[] paths = FileName.splitPath( path);
        FileState fstate = getStateForPath( ctx, paths[0]);

        // Check if the folder has any pseudo files
        
        if ( fstate == null || fstate.hasPseudoFiles() == false)
            return;

        // Remove the pseudo file from the list
        
        fstate.getPseudoFileList().removeFile( paths[1], false);
    }
    
    /**
     * Return the file state for the specified path
     * 
     * @param ctx ContentContext
     * @param path String
     * @return FileState
     */
    private final FileState getStateForPath(ContentContext ctx, String path)
    {
        // Check if there is a cached state for the path
        
        FileState fstate = null;
        
        if ( ctx.hasStateTable())
        {
            // Get the file state for a file/folder
            
            fstate = ctx.getStateTable().findFileState(path);
        }
        
        // Return the file state
        
        return fstate;
    }
}
