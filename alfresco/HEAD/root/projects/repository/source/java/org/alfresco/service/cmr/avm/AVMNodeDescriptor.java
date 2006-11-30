/*
 * Copyright (C) 2006 Alfresco, Inc.
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

package org.alfresco.service.cmr.avm;

import java.io.Serializable;

import org.alfresco.repo.avm.AVMNodeType;

/**
 * This class describes an AVM node object.  
 * It serves a similar purpose to the data structure
 * returned by the stat() system call in UNIX.
 *
 * @author britt
 */
public class AVMNodeDescriptor implements Serializable
{
    private static final long serialVersionUID = -7959606980486852184L;

    /**
     * The path that this was looked up with.
     */
    private String fPath;
    
    /**
     * The base name of the path.
     */
    private String fName;
    
    /**
     * The type of this node.  AVMNodeType constants.
     */
    private int fType;
    
    /**
     * The Owner.
     */
    private String fOwner;
    
    /**
     * The Creator.
     */
    private String fCreator;
    
    /**
     * The last modifier.
     */
    private String fLastModifier;
    
    /**
     * The Create date.
     */
    private long fCreateDate;
    
    /**
     * The Modification date.
     */
    private long fModDate;
    
    /**
     * The Access date.
     */
    private long fAccessDate;

    /**
     * The object id.
     */
    private long fID;
    
    /**
     * The version number.
     */
    private int fVersionID;
    
    /**
     * The indirection if this is a layer.
     */
    private String fIndirection;
    
    /**
     * Is this a primary indirection node.
     */
    private boolean fIsPrimary;

    /**
     * The layer id or -1 if this is not a layered node.
     */
    private long fLayerID;
    
    /**
     * The length, if this is a file or -1 otherwise.
     */
    private long fLength;
    
    /**
     * The opacity for layered directories.
     */
    private boolean fOpacity;

    /**
     * Make one up.
     * @param path The looked up path.
     * @param type The type of the node.
     * @param creator The creator of the node.
     * @param owner The owner of the node.
     * @param lastModifier The last modifier of the node.
     * @param createDate The creation date.
     * @param modDate The modification date.
     * @param accessDate The access date.
     * @param id The object id.
     * @param versionID The version id.
     * @param indirection The indirection.
     * @param isPrimary Whether this is a primary indirection.
     * @param layerID The layer id.
     * @param length The file length.
     */
    public AVMNodeDescriptor(String path,
                             String name,
                             int type,
                             String creator,
                             String owner,
                             String lastModifier,
                             long createDate,
                             long modDate,
                             long accessDate,
                             long id,
                             int versionID,
                             String indirection,
                             boolean isPrimary,
                             long layerID,
                             boolean opacity,
                             long length)
    {
        fPath = path;
        fName = name;
        fType = type;
        fCreator = creator;
        fOwner = owner;
        fLastModifier = lastModifier;
        fCreateDate = createDate;
        fModDate = modDate;
        fAccessDate = accessDate;
        fID = id;
        fVersionID = versionID;
        fIndirection = indirection;
        fIsPrimary = isPrimary;
        fLayerID = layerID;
        fLength = length;
        fOpacity = opacity;
    }
    
    /**
     * Get the last access date in java milliseconds.
     * @return The last access date.
     */
    public long getAccessDate()
    {
        return fAccessDate;
    }

    /**
     * Get the creation date in java milliseconds.
     * @return The creation date.
     */
    public long getCreateDate()
    {
        return fCreateDate;
    }

    /**
     * Get the user who created this.
     * @return The creator.
     */
    public String getCreator()
    {
        return fCreator;
    }

    /**
     * Get the indirection path if this is layered or null.
     * @return The indirection path or null.
     */
    public String getIndirection()
    {
        return fIndirection;
    }

    /**
     * Is this a primary indirection node.  Will always
     * be false for non-layered nodes.
     * @return Whether this is a primary indirection node.
     */
    public boolean isPrimary()
    {
        return fIsPrimary;
    }

    /**
     * Determines whether this node corresponds to
     * either a plain or layered file.   
     *
     * @return true if AVMNodeDescriptor is a plain or layered file,
     *         otherwise false.
     */
    public boolean isFile()
    {
        return ( fType == AVMNodeType.PLAIN_FILE || 
                 fType == AVMNodeType.LAYERED_FILE
               );
    }

    /**
     * Determines whether this node corresponds to
     * a plain (non-layered) file.
     *
     * @return true if AVMNodeDescriptor is a plain file, otherwise false. 
     */
    public boolean isPlainFile()
    {
        return (fType == AVMNodeType.PLAIN_FILE);
    }

    /**
     * Determines whether this node corresponds to
     * a layered file.
     *
     * @return true if AVMNodeDescriptor is a layered file, 
     *         otherwise false. 
     */
    public boolean isLayeredFile()
    {
        return (fType == AVMNodeType.LAYERED_FILE);
    }

    /**
     * Determines whether this node corresponds to
     * either a plain or layered directory.   
     *
     * @return true if AVMNodeDescriptor is a plain or layered directory,
     *         otherwise false.
     */
    public boolean isDirectory()
    {
        return ( fType == AVMNodeType.PLAIN_DIRECTORY   || 
                 fType == AVMNodeType.LAYERED_DIRECTORY
               );
    }

    /**
     * Determines whether this node corresponds to
     * a plain (non-layered) directory.
     *
     * @return true if AVMNodeDescriptor is a plain directory, otherwise false. 
     */
    public boolean isPlainDirectory()
    {
        return (fType == AVMNodeType.PLAIN_DIRECTORY );
    }

    /**
     * Determines whether this node corresponds to
     * a layered directory.
     *
     * @return true if AVMNodeDescriptor is a layered directory, 
     *         otherwise false. 
     */
    public boolean isLayeredDirectory()
    {
        return (fType == AVMNodeType.LAYERED_DIRECTORY );
    }

    /**
     * Is this a deleted node.
     * @return Whether this node is a deleted node.
     */
    public boolean isDeleted()
    {
        return fType == AVMNodeType.DELETED_NODE;
    }
    
    /**
     * Get the user who last modified this node.
     * @return Who last modified this node.
     */
    public String getLastModifier()
    {
        return fLastModifier;
    }

    /**
     * Get the layer id of this node.
     * @return The layer id if there is one or -1.
     */
    public long getLayerID()
    {
        return fLayerID;
    }

    /**
     * Get the modification date of this node.
     * @return The modification date.
     */
    public long getModDate()
    {
        return fModDate;
    }

    /**
     * Get the owner of this node.
     * @return The owner of this node.
     */
    public String getOwner()
    {
        return fOwner;
    }

    /**
     * Get the path that this node was looked up by.
     * @return The path by which this was looked up.
     */
    public String getPath()
    {
        return fPath;
    }

    /**
     * Get the type of this node. AVMNodeType constants.
     * @return The type node.
     */
    public int getType()
    {
        return fType;
    }

    /**
     * Get the version id of this node.
     * @return The version id of this node.
     */
    public int getVersionID()
    {
        return fVersionID;
    }
    
    /**
     * Get the object id.
     * @return The object id.
     */
    public long getId()
    {
        return fID;
    }
    
    /**
     * Get the file length if applicable.
     * @return The file length.
     */
    public long getLength()
    {
        return fLength;
    }
    
    /**
     * Get the name of the node.
     */
    public String getName()
    {
        return fName;
    }

    /**
     * @return the opacity
     */
    public boolean getOpacity()
    {
        return fOpacity;
    }

    /**
     * Get a debuggable string representation of this.
     * @return A string representation of this.
     */
    @Override
    public String toString()
    {
        switch (fType)
        {
            case AVMNodeType.PLAIN_FILE :
                return "[PF:" + fID + "]";
            case AVMNodeType.PLAIN_DIRECTORY :
                return "[PD:" + fID + "]";
            case AVMNodeType.LAYERED_FILE :
                return "[LF:" + fID + ":" + fIndirection + "]";
            case AVMNodeType.LAYERED_DIRECTORY :
                return "[LD:" + fID + ":" + fIndirection + "]";
            case AVMNodeType.DELETED_NODE :
                return "[DN:" + fID + "]";
            default :
                throw new AVMException("Internal Error.");
        }
    }

    /**
     * Equals override.
     * @param obj
     * @return Equality.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!(obj instanceof AVMNodeDescriptor))
        {
            return false;
        }
        return fID == ((AVMNodeDescriptor)obj).fID;
    }

    /**
     * Hashcode override.
     * @return The objid as hashcode.
     */
    @Override
    public int hashCode()
    {
        return (int)fID;
    }
}
