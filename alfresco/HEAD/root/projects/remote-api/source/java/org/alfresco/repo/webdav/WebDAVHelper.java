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
package org.alfresco.repo.webdav;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.CopyService;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.util.EqualsHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.helpers.AttributesImpl;

/**
 * WebDAV Protocol Helper Class
 * 
 * <p>Provides helper methods for repository access using the WebDAV protocol.
 * 
 * @author GKSpencer
 */
public class WebDAVHelper
{
    // Constants
    
    // Path seperator
    public static final String PathSeperator   = "/";
    public static final char PathSeperatorChar = '/';
    
    // Logging
    private static Log logger = LogFactory.getLog("org.alfresco.protocol.webdav");
    
    // Service registry
    private ServiceRegistry m_serviceRegistry;

    // Services
    private NodeService m_nodeService;
    private FileFolderService m_fileFolderService;
    private SearchService m_searchService;
    private NamespaceService m_namespaceService;
    private DictionaryService m_dictionaryService;
    private MimetypeService m_mimetypeService;
    private LockService m_lockService;
    private AuthenticationService m_authService;
    
    //  Empty XML attribute list
    
    private AttributesImpl m_nullAttribs = new AttributesImpl();
    
    /**
     * Class constructor
     * 
     * @param serviceRegistry ServiceRegistry
     * @param authService AuthenticationService
     */
    protected WebDAVHelper(ServiceRegistry serviceRegistry, AuthenticationService authService)
    {
        m_serviceRegistry = serviceRegistry;
        
        m_nodeService       = m_serviceRegistry.getNodeService();
        m_fileFolderService = m_serviceRegistry.getFileFolderService();
        m_searchService     = m_serviceRegistry.getSearchService();
        m_namespaceService  = m_serviceRegistry.getNamespaceService();
        m_dictionaryService = m_serviceRegistry.getDictionaryService();
        m_mimetypeService   = m_serviceRegistry.getMimetypeService();
        m_lockService       = m_serviceRegistry.getLockService();
        
        m_authService       = authService;
    }
    
    /**
     * Return the authentication service
     * 
     * @return AuthenticationService
     */
    public final AuthenticationService getAuthenticationService()
    {
        return m_authService;
    }
    
    /**
     * Return the service registry
     * 
     * @return ServiceRegistry
     */
    public final ServiceRegistry getServiceRegistry()
    {
        return m_serviceRegistry;
    }
    
    /**
     * Return the node service
     * 
     * @return NodeService
     */
    public final NodeService getNodeService()
    {
        return m_nodeService;
    }
    
    public FileFolderService getFileFolderService()
    {
        return m_fileFolderService;
    }

    /**
     * Return the search service
     * 
     * @return  SearchService
     */
    public final SearchService getSearchService()
    {
        return m_searchService;
    }
    
    /**
     * Return the namespace service
     * 
     * @return  NamespaceService
     */
    public final NamespaceService getNamespaceService()
    {
        return m_namespaceService;
    }
    
    /**
     * Return the dictionary service
     * 
     * @return  DictionaryService
     */
    public final DictionaryService getDictionaryService()
    {
        return m_dictionaryService;
    }

    /**
     * Return the mimetype service
     * 
     * @return MimetypeService
     */
    public final MimetypeService getMimetypeService()
    {
        return m_mimetypeService;
    }
    
    /**
     * Return the lock service
     * 
     * @return LockService
     */
    public final LockService getLockService()
    {
        return m_lockService;
    }
    
    /**
     * Return the copy service
     * 
     * @return CopyService
     */
    public final CopyService getCopyService()
    {
        return getServiceRegistry().getCopyService();
    }
    
    /**
     * Split the path into seperate directory path and file name strings.
     * If the path is not empty, then there will always be an entry for the filename
     * 
     * @param path Full path string.
     * @return Returns a String[2] with the folder path and file path.
     */
    public final String[] splitPath(String path)
    {
        if (path == null)
            throw new IllegalArgumentException("path may not be null");
        
        // Create an array of strings to hold the path and file name strings
        String[] pathStr = new String[] {"", ""};

        // Check if the path has a trailing seperator, if so then there is no file name.

        int pos = path.lastIndexOf(PathSeperatorChar);
        if (pos == -1 || pos == (path.length() - 1))
        {
            // Set the path string in the returned string array
            pathStr[1] = path;
        }
        else
        {
            pathStr[0] = path.substring(0, pos);
            pathStr[1] = path.substring(pos + 1);
        }
        // Return the path strings
        return pathStr;
    }
    
    /**
     * Split the path into all the component directories and filename
     * 
     * @param path String
     * @return String[]
     */
    public final List<String> splitAllPaths(String path)
    {
        if (path == null || path.length() == 0)
        {
            return Collections.emptyList();
        }

        // split the path
        StringTokenizer token = new StringTokenizer(path, PathSeperator);
        List<String> results = new ArrayList<String>(10);
        while (token.hasMoreTokens())
        {
            results.add(token.nextToken());
        }
        return results;
    }

    /**
     * Get the file info for the given paths
     * 
     * @param rootNodeRef the acting webdav root
     * @param path the path to search for
     * @param servletPath the base servlet path, which may be null or empty
     * @return Return the file info for the path
     * @throws FileNotFoundException if the path doesn't refer to a valid node
     */
    public final FileInfo getNodeForPath(NodeRef rootNodeRef, String path, String servletPath) throws FileNotFoundException
    {
        if (rootNodeRef == null)
        {
            throw new IllegalArgumentException("Root node may not be null");
        }
        else if (path == null)
        {
            throw new IllegalArgumentException("Path may not be null");
        }
        
        FileFolderService fileFolderService = getFileFolderService();
        // Check for the root path
        if ( path.length() == 0 || path.equals(PathSeperator) || EqualsHelper.nullSafeEquals(path, servletPath))
        {
            return fileFolderService.getFileInfo(rootNodeRef);
        }
        
        // remove the servlet path from the path
        if (servletPath != null && servletPath.length() > 0 && path.startsWith(servletPath))
        {
            // Strip the servlet path from the relative path
            path = path.substring(servletPath.length());
        }
        
        // split the paths up
        List<String> splitPath = splitAllPaths(path);
        
        // find it
        FileInfo fileInfo = m_fileFolderService.resolveNamePath(rootNodeRef, splitPath);
        
        // done
        if (logger.isDebugEnabled())
        {
            logger.debug("Fetched node for path: \n" +
                    "   root: " + rootNodeRef + "\n" +
                    "   path: " + path + "\n" +
                    "   servlet path: " + servletPath + "\n" +
                    "   result: " + fileInfo);
        }
        return fileInfo;
    }
    
    public final FileInfo getParentNodeForPath(NodeRef rootNodeRef, String path, String servletPath) throws FileNotFoundException
    {
        if (rootNodeRef == null)
        {
            throw new IllegalArgumentException("Root node may not be null");
        }
        else if (path == null)
        {
            throw new IllegalArgumentException("Path may not be null");
        }
        // shorten the path
        String[] paths = splitPath(path);
        return getNodeForPath(rootNodeRef, paths[0], servletPath);
    }
    
    /**
     * Return the relative path for the node walking back to the specified root node
     * 
     * @param rootNodeRef the root below which the path will be valid
     * @param nodeRef the node's path to get
     * @return Returns string of form <b>/A/B/C</b> where C represents the from node and 
     */
    public final String getPathFromNode(NodeRef rootNodeRef, NodeRef nodeRef) throws FileNotFoundException
    {
        // Check if the nodes are valid, or equal
        if (rootNodeRef == null || nodeRef == null)
            throw new IllegalArgumentException("Invalid node(s) in getPathFromNode call");
        
        // short cut if the path node is the root node
        if (rootNodeRef.equals(nodeRef))
            return "";
        
        FileFolderService fileFolderService = getFileFolderService();
        
        // get the path elements
        List<FileInfo> pathInfos = fileFolderService.getNamePath(rootNodeRef, nodeRef);
        
        // build the path string
        StringBuilder sb = new StringBuilder(pathInfos.size() * 20);
        for (FileInfo fileInfo : pathInfos)
        {
            sb.append(WebDAVHelper.PathSeperatorChar);
            sb.append(fileInfo.getName());
        }
        // done
        if (logger.isDebugEnabled())
        {
            logger.debug("Build name path for node: \n" +
                    "   root: " + rootNodeRef + "\n" +
                    "   target: " + nodeRef + "\n" +
                    "   path: " + sb);
        }
        return sb.toString();
    }
    
    /**
     * Make an ETag value for a node using the GUID and modify date/time
     * 
     * @param node NodeRef
     * @return String
     */
    public final String makeETag(NodeRef node)
    {
        // Get the modify date/time property for the node
        
        StringBuilder etag = new StringBuilder();
        makeETagString(node, etag);
        return etag.toString();
    }
    
    /**
     * Make an ETag value for a node using the GUID and modify date/time
     * 
     * @param node NodeRef
     * @return String
     */
    public final String makeQuotedETag(NodeRef node)
    {
        StringBuilder etag = new StringBuilder();
        
        etag.append("\"");
        makeETagString(node, etag);
        etag.append("\"");
        return etag.toString();
    }
    
    /**
     * Make an ETag value for a node using the GUID and modify date/time
     * 
     * @param node NodeRef
     * @param str StringBuilder
     */
    protected final void makeETagString(NodeRef node, StringBuilder etag)
    {
        // Get the modify date/time property for the node
        
        Object modVal = getNodeService().getProperty(node, ContentModel.PROP_MODIFIED);
        
        etag.append(node.getId());
        
        if ( modVal != null)
        {
            etag.append("_");
            etag.append(DefaultTypeConverter.INSTANCE.longValue(modVal));
        }
    }
    
    /**
     * Return the null XML attribute list
     * 
     * @return AttributesImpl
     */
    public final AttributesImpl getNullAttributes()
    {
        return m_nullAttribs;
    }
    
    /**
     * Encodes the given string to valid URL format
     * 
     * @param s      the String to convert
     */
    public final static String encodeURL(String s)
    {
        try
        {
            return replace(URLEncoder.encode(s, "UTF-8"), "+", "%20");
        }
        catch (UnsupportedEncodingException err)
        {
            throw new RuntimeException(err);
        }
    }
    
    /**
     * Replace one string instance with another within the specified string
     * 
     * @param str
     * @param repl
     * @param with
     * 
     * @return replaced string
     */
    public static String replace(String str, String repl, String with)
    {
        int lastindex = 0;
        int pos = str.indexOf(repl);
        
        // If no replacement needed, return the original string
        // and save StringBuffer allocation/char copying
        if (pos < 0)
        {
            return str;
        }
        
        int len = repl.length();
        int lendiff = with.length() - repl.length();
        StringBuilder out = new StringBuilder((lendiff <= 0) ? str.length() : (str.length() + (lendiff << 3)));
        for (; pos >= 0; pos = str.indexOf(repl, lastindex = pos + len))
        {
            out.append(str.substring(lastindex, pos)).append(with);
        }
        
        return out.append(str.substring(lastindex, str.length())).toString();
    }
    
    /**
     * Encodes the given string to valid HTML format
     * 
     * @param string     the String to convert
     */
    public final static String encodeHTML(String string)
    {
        if (string == null)
        {
            return "";
        }
        
        StringBuilder sb = null;      //create on demand
        String enc;
        char c;
        for (int i = 0; i < string.length(); i++)
        {
            enc = null;
            c = string.charAt(i);
            switch (c)
            {
                case '"': enc = "&quot;"; break;    //"
                case '&': enc = "&amp;"; break;     //&
                case '<': enc = "&lt;"; break;      //<
                case '>': enc = "&gt;"; break;      //>
                
                //german umlauts
                case '\u00E4' : enc = "&auml;";  break;
                case '\u00C4' : enc = "&Auml;";  break;
                case '\u00F6' : enc = "&ouml;";  break;
                case '\u00D6' : enc = "&Ouml;";  break;
                case '\u00FC' : enc = "&uuml;";  break;
                case '\u00DC' : enc = "&Uuml;";  break;
                case '\u00DF' : enc = "&szlig;"; break;
                
                //misc
                //case 0x80: enc = "&euro;"; break;  sometimes euro symbol is ascii 128, should we suport it?
                case '\u20AC': enc = "&euro;";  break;
                case '\u00AB': enc = "&laquo;"; break;
                case '\u00BB': enc = "&raquo;"; break;
                case '\u00A0': enc = "&nbsp;"; break;
                
                default:
                    if (((int)c) >= 0x80)
                    {
                        //encode all non basic latin characters
                        enc = "&#" + ((int)c) + ";";
                    }
                break;
            }
            
            if (enc != null)
            {
                if (sb == null)
                {
                    String soFar = string.substring(0, i);
                    sb = new StringBuilder(i + 8);
                    sb.append(soFar);
                }
                sb.append(enc);
            }
            else
            {
                if (sb != null)
                {
                    sb.append(c);
                }
            }
        }
        
        if (sb == null)
        {
            return string;
        }
        else
        {
            return sb.toString();
        }
    }
}
