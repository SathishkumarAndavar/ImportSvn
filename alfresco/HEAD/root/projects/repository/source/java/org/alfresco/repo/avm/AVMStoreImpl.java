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

package org.alfresco.repo.avm;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.avm.util.RawServices;
import org.alfresco.repo.avm.util.SimplePath;
import org.alfresco.repo.domain.DbAccessControlList;
import org.alfresco.repo.domain.PropertyValue;
import org.alfresco.service.cmr.avm.AVMBadArgumentException;
import org.alfresco.service.cmr.avm.AVMException;
import org.alfresco.service.cmr.avm.AVMExistsException;
import org.alfresco.service.cmr.avm.AVMNodeDescriptor;
import org.alfresco.service.cmr.avm.AVMNotFoundException;
import org.alfresco.service.cmr.avm.AVMStoreDescriptor;
import org.alfresco.service.cmr.avm.AVMWrongTypeException;
import org.alfresco.service.cmr.avm.VersionDescriptor;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

/**
 * A Repository contains a current root directory and a list of
 * root versions.  Each root version corresponds to a separate snapshot
 * operation.
 * @author britt
 */
public class AVMStoreImpl implements AVMStore, Serializable
{
    static final long serialVersionUID = -1485972568675732904L;

    /**
     * The name of this AVMStore.
     */
    private String fName;
    
    /**
     * The current root directory.
     */
    private DirectoryNode fRoot;
    
    /**
     * The next version id.
     */
    private int fNextVersionID;
    
    /**
     * The version (for concurrency control).
     */
    private long fVers;
    
    /**
     * The AVMRepository.
     */
    transient private AVMRepository fAVMRepository;
    
    /**
     * Default constructor.
     */
    protected AVMStoreImpl()
    {
        fAVMRepository = AVMRepository.GetInstance();
    }
    
    /**
     * Make a brand new AVMStore.
     * @param repo The AVMRepository.
     * @param name The name of the AVMStore.
     */
    public AVMStoreImpl(AVMRepository repo, String name)
    {
        // Make ourselves up and save.
        fAVMRepository = repo;
        fName = name;
        fNextVersionID = 0;
        fRoot = null;
        AVMDAOs.Instance().fAVMStoreDAO.save(this);
        String creator = RawServices.Instance().getAuthenticationComponent().getCurrentUserName();
        if (creator == null)
        {
            creator = RawServices.Instance().getAuthenticationComponent().getSystemUserName();
        }
        setProperty(ContentModel.PROP_CREATOR, new PropertyValue(null, creator));
        setProperty(ContentModel.PROP_CREATED, new PropertyValue(null, new Date(System.currentTimeMillis())));
        // Make up the initial version record and save.
        long time = System.currentTimeMillis();
        fRoot = new PlainDirectoryNodeImpl(this);
        fRoot.setIsRoot(true);
        AVMDAOs.Instance().fAVMNodeDAO.save(fRoot);
        VersionRoot versionRoot = new VersionRootImpl(this,
                                                      fRoot,
                                                      fNextVersionID,
                                                      time,
                                                      creator,
                                                      "Initial Empty Version.",
                                                      "Initial Empty Version.");
        fNextVersionID++;
        AVMDAOs.Instance().fVersionRootDAO.save(versionRoot);
    }
    
    /**
     * Set a new root for this.
     * @param root
     */
    public void setNewRoot(DirectoryNode root)
    {
        fRoot = root;
        fRoot.setIsRoot(true);
    }

    /**
     * Snapshot this store.  This creates a new version record.
     * @return The version id of the new snapshot.
     */
    @SuppressWarnings("unchecked")
    public int createSnapshot(String tag, String description)
    {
        // If the root isn't new, we can't take a snapshot since nothing has changed.
        if (!fRoot.getIsNew())
        {
            // So we just return the most recent snapshot.
            return AVMDAOs.Instance().fVersionRootDAO.getMaxVersionID(this);
        }
        // Clear out the new nodes.
        List<AVMNode> newInRep = AVMDAOs.Instance().fAVMNodeDAO.getNewInStore(this);
        for (AVMNode newGuy : newInRep)
        {
            newGuy.setStoreNew(null);
        }
        // Make up a new version record.
        String user = RawServices.Instance().getAuthenticationComponent().getCurrentUserName();
        if (user == null)
        {
            user = RawServices.Instance().getAuthenticationComponent().getSystemUserName();
        }
        VersionRoot versionRoot = new VersionRootImpl(this,
                                                      fRoot,
                                                      fNextVersionID,
                                                      System.currentTimeMillis(),
                                                      user,
                                                      tag,
                                                      description);
        AVMDAOs.Instance().fVersionRootDAO.save(versionRoot);
        // Increment the version id.
        fNextVersionID++;
        return fNextVersionID - 1;
    }

    /**
     * Create a new directory.
     * @param path The path to the containing directory.
     * @param name The name of the new directory.
     */
    public void createDirectory(String path, String name)
    {
        Lookup lPath = lookupDirectory(-1, path, true);
        if (lPath == null)
        {
            throw new AVMNotFoundException("Path not found.");
        }
        DirectoryNode dir = (DirectoryNode)lPath.getCurrentNode();
        if (dir.lookupChild(lPath, name, false) != null)
        {
            throw new AVMExistsException("Child exists: " + name);
        }
        DirectoryNode newDir = null;
        if (lPath.isLayered())  // Creating a directory in a layered context creates
                                // a LayeredDirectoryNode that gets its indirection from
                                // its parent.
        {
            newDir = new LayeredDirectoryNodeImpl((String)null, this, null);
            ((LayeredDirectoryNodeImpl)newDir).setPrimaryIndirection(false);
            ((LayeredDirectoryNodeImpl)newDir).setLayerID(lPath.getTopLayer().getLayerID());
        }
        else
        {
            newDir = new PlainDirectoryNodeImpl(this);
        }
        newDir.setVersionID(getNextVersionID());
        dir.putChild(name, newDir);
        dir.updateModTime();
    }

    /**
     * Create a new layered directory.
     * @param srcPath The target indirection for a layered node.
     * @param dstPath The containing directory for the new node.
     * @param name The name of the new node.
     */
    public void createLayeredDirectory(String srcPath, String dstPath,
                                       String name)
    {
        Lookup lPath = lookupDirectory(-1, dstPath, true);
        if (lPath == null)
        {
            throw new AVMNotFoundException("Path not found.");
        }
        DirectoryNode dir = (DirectoryNode)lPath.getCurrentNode();
        if (dir.lookupChild(lPath, name, false) != null)
        {
            throw new AVMExistsException("Child exists: " +  name);
        }
        LayeredDirectoryNode newDir =
            new LayeredDirectoryNodeImpl(srcPath, this, null);
        if (lPath.isLayered())
        {
            // When a layered directory is made inside of a layered context,
            // it gets its layer id from the topmost layer in its lookup
            // path.
            LayeredDirectoryNode top = lPath.getTopLayer();
            newDir.setLayerID(top.getLayerID());
        }
        else
        {
            // Otherwise we issue a brand new layer id.
            newDir.setLayerID(fAVMRepository.issueLayerID());
        }
        dir.putChild(name, newDir);
        dir.updateModTime();
        newDir.setVersionID(getNextVersionID());
    }

    /**
     * Create a new file.
     * @param path The path to the directory to contain the new file.
     * @param name The name to give the new file.
     * initial content.
     */
    public OutputStream createFile(String path, String name)
    {
        Lookup lPath = lookupDirectory(-1, path, true);
        if (lPath == null)
        {
            throw new AVMNotFoundException("Path not found.");
        }
        DirectoryNode dir = (DirectoryNode)lPath.getCurrentNode();
        if (dir.lookupChild(lPath, name, false) != null)
        {
            throw new AVMExistsException("Child exists: " + name);
        }
        PlainFileNodeImpl file = new PlainFileNodeImpl(this);
        file.setVersionID(getNextVersionID());
        dir.putChild(name, file);
        dir.updateModTime();
        file.setContentData(new ContentData(null, 
                RawServices.Instance().getMimetypeService().guessMimetype(name),
                -1,
                "UTF-8"));
        ContentWriter writer = createContentWriter(AVMNodeConverter.ExtendAVMPath(path, name));
        return writer.getContentOutputStream();
    }

    /**
     * Create a file with the given contents.
     * @param path The path to the containing directory.
     * @param name The name to give the new file.
     * @param data The contents.
     */
    public void createFile(String path, String name, File data)
    {
        Lookup lPath = lookupDirectory(-1, path, true);
        if (lPath == null)
        {
            throw new AVMNotFoundException("Path not found.");
        }
        DirectoryNode dir = (DirectoryNode)lPath.getCurrentNode();
        if (dir.lookupChild(lPath, name, false) != null)
        {
            throw new AVMExistsException("Child exists: " + name);
        }
        PlainFileNodeImpl file = new PlainFileNodeImpl(this);
        file.setVersionID(getNextVersionID());
        dir.putChild(name, file);
        dir.updateModTime();
        file.setContentData(new ContentData(null, 
                RawServices.Instance().getMimetypeService().guessMimetype(name),
                -1,
                "UTF-8"));
        ContentWriter writer = createContentWriter(AVMNodeConverter.ExtendAVMPath(path, name));
        writer.putContent(data);
    }

    /**
     * Create a new layered file.
     * @param srcPath The target indirection for the layered file.
     * @param dstPath The path to the directory to contain the new file.
     * @param name The name of the new file.
     */
    public void createLayeredFile(String srcPath, String dstPath, String name)
    {
        Lookup lPath = lookupDirectory(-1, dstPath, true);
        if (lPath == null)
        {
            throw new AVMNotFoundException("Path not found.");
        }
        DirectoryNode dir = (DirectoryNode)lPath.getCurrentNode();
        if (dir.lookupChild(lPath, name, false) != null)
        {
            throw new AVMExistsException("Child exists: " + name);
        }
        // TODO Reexamine decision to not check validity of srcPath.
        LayeredFileNodeImpl newFile =
            new LayeredFileNodeImpl(srcPath, this);
        dir.putChild(name, newFile);
        dir.updateModTime();
        newFile.setVersionID(getNextVersionID());
    }

    /**
     * Get an input stream from a file.
     * @param version The version id to look under.
     * @param path The path to the file.
     * @return An InputStream.
     */
    public InputStream getInputStream(int version, String path)
    {
        ContentReader reader = getContentReader(version, path);
        if (reader == null)
        {
            // TODO This is wrong, wrong, wrong. Do something about it
            // sooner rather than later.
            throw new AVMNotFoundException(path + " has no content.");
        }
        return reader.getContentInputStream();
    }

    /**
     * Get a ContentReader from a file.
     * @param version The version to look under.
     * @param path The path to the file.
     * @return A ContentReader.
     */
    public ContentReader getContentReader(int version, String path)
    {
        NodeRef nodeRef = AVMNodeConverter.ToNodeRef(version, fName + ":" + path);
        return RawServices.Instance().getContentService().getReader(nodeRef, ContentModel.PROP_CONTENT);
    }

    /**
     * Get a ContentWriter to a file.
     * @param path The path to the file.
     * @return A ContentWriter.
     */
    public ContentWriter createContentWriter(String path)
    {
        NodeRef nodeRef = AVMNodeConverter.ToNodeRef(-1, fName + ":" + path);
        ContentWriter writer = 
            RawServices.Instance().getContentService().getWriter(nodeRef, ContentModel.PROP_CONTENT, true);
        return writer;
    }

    /**
     * Get a listing from a directory.
     * @param version The version to look under.
     * @param path The path to the directory.
     * @return A List of FolderEntries.
     */
    public SortedMap<String, AVMNodeDescriptor> getListing(int version, String path, 
                                                           boolean includeDeleted)
    {
        Lookup lPath = lookupDirectory(version, path, false);
        if (lPath == null)
        {
            throw new AVMNotFoundException("Path not found.");
        }
        DirectoryNode dir = (DirectoryNode)lPath.getCurrentNode();
        Map<String, AVMNode> listing = dir.getListing(lPath, includeDeleted);
        return translateListing(listing, lPath);
    }

    /**
     * Get the list of nodes directly contained in a directory.
     * @param version The version to look under.
     * @param path The path to the directory.
     * @return A Map of names to descriptors.
     */
    public SortedMap<String, AVMNodeDescriptor> getListingDirect(int version, String path,
                                                                 boolean includeDeleted)
    {
        Lookup lPath = lookupDirectory(version, path, false);
        if (lPath == null)
        {
            throw new AVMNotFoundException("Path not found.");
        }
        DirectoryNode dir = (DirectoryNode)lPath.getCurrentNode();
        if (lPath.isLayered() && dir.getType() != AVMNodeType.LAYERED_DIRECTORY)
        {
            return new TreeMap<String, AVMNodeDescriptor>();
        }
        Map<String, AVMNode> listing = dir.getListingDirect(lPath, includeDeleted);
        return translateListing(listing, lPath);
    }

    /**
     * Helper to convert an internal representation of a directory listing
     * to an external representation.
     * @param listing The internal listing, a Map of names to nodes.
     * @param lPath The Lookup for the directory.
     * @return A Map of names to descriptors.
     */
    private SortedMap<String, AVMNodeDescriptor> 
        translateListing(Map<String, AVMNode> listing, Lookup lPath)
    {
        SortedMap<String, AVMNodeDescriptor> results = new TreeMap<String, AVMNodeDescriptor>();
        for (String name : listing.keySet())
        {
            AVMNode child = listing.get(name);
            AVMNodeDescriptor desc = child.getDescriptor(lPath, name);
            results.put(name, desc);
        }
        return results;
    }

    /**
     * Get the names of the deleted nodes in a directory.
     * @param version The version to look under.
     * @param path The path to the directory.
     * @return A List of names.
     */
    public List<String> getDeleted(int version, String path)
    {
        Lookup lPath = lookupDirectory(version, path, false);
        if (lPath == null)
        {
            throw new AVMNotFoundException("Path not found.");
        }
        DirectoryNode dir = (DirectoryNode)lPath.getCurrentNode();
        return dir.getDeletedNames();
    }

    /**
     * Get an output stream to a file.
     * @param path The path to the file.
     * @return An OutputStream.
     */
    public OutputStream getOutputStream(String path)
    {
        ContentWriter writer = createContentWriter(path);
        return writer.getContentOutputStream();
    }

    /**
     * Remove a node and everything underneath it.
     * @param path The path to the containing directory.
     * @param name The name of the node to remove.
     */
    public void removeNode(String path, String name)
    {
        Lookup lPath = lookupDirectory(-1, path, true);
        if (lPath == null)
        {
            throw new AVMNotFoundException("Path not found.");
        }
        DirectoryNode dir = (DirectoryNode)lPath.getCurrentNode();
        if (dir.lookupChild(lPath, name, false) == null)
        {
            throw new AVMNotFoundException("Does not exist: " + name);
        }
        dir.removeChild(lPath, name);
        dir.updateModTime();
    }

    /**
     * Allow a name which has been deleted to be visible through that layer.
     * @param dirPath The path to the containing directory.
     * @param name The name to uncover.
     */
    public void uncover(String dirPath, String name)
    {
        Lookup lPath = lookup(-1, dirPath, true, false);
        if (lPath == null)
        {
            throw new AVMNotFoundException("Path not found.");
        }
        AVMNode node = lPath.getCurrentNode();
        if (node.getType() != AVMNodeType.LAYERED_DIRECTORY)
        {
            throw new AVMWrongTypeException("Not a layered directory: " + dirPath);
        }
        ((LayeredDirectoryNode)node).uncover(lPath, name);
        node.updateModTime();
    }

    // TODO This is problematic.  As time goes on this returns
    // larger and larger data sets.  Perhaps what we should do is
    // provide methods for getting versions by date range, n most 
    // recent etc.
    /**
     * Get the set of all extant versions for this AVMStore.
     * @return A Set of version ids.
     */
    @SuppressWarnings("unchecked")
    public List<VersionDescriptor> getVersions()
    {
        List<VersionRoot> versions = AVMDAOs.Instance().fVersionRootDAO.getAllInAVMStore(this);
        List<VersionDescriptor> descs = new ArrayList<VersionDescriptor>();
        for (VersionRoot vr : versions)
        {
            VersionDescriptor desc = 
                new VersionDescriptor(fName,
                                      vr.getVersionID(),
                                      vr.getCreator(),
                                      vr.getCreateDate(),
                                      vr.getTag(),
                                      vr.getDescription());
            descs.add(desc);
        }
        return descs;
    }

    /**
     * Get the versions between the given dates (inclusive). From or
     * to may be null but not both.
     * @param from The earliest date.
     * @param to The latest date.
     * @return The Set of matching version IDs.
     */
    @SuppressWarnings("unchecked")
    public List<VersionDescriptor> getVersions(Date from, Date to)
    {
        List<VersionRoot> versions = AVMDAOs.Instance().fVersionRootDAO.getByDates(this, from, to);
        List<VersionDescriptor> descs = new ArrayList<VersionDescriptor>();
        for (VersionRoot vr : versions)
        {
            VersionDescriptor desc =
                new VersionDescriptor(fName,
                                      vr.getVersionID(),
                                      vr.getCreator(),
                                      vr.getCreateDate(),
                                      vr.getTag(),
                                      vr.getDescription());
            descs.add(desc);
        }
        return descs;
    }

    /**
     * Get the AVMRepository.
     * @return The AVMRepository
     */
    public AVMRepository getAVMRepository()
    {
        return fAVMRepository;
    }

    /**
     * Lookup up a path.
     * @param version The version to look in.
     * @param path The path to look up.
     * @param write Whether this is in the context of a write.
     * @return A Lookup object.
     */
    public Lookup lookup(int version, String path, boolean write, boolean includeDeleted)
    {
        SimplePath sPath = new SimplePath(path);
        return RawServices.Instance().getLookupCache().lookup(this, version, sPath, write, includeDeleted);
    }

    /**
     * Get the root node descriptor.
     * @param version The version to get.
     * @return The descriptor.
     */
    public AVMNodeDescriptor getRoot(int version)
    {
        AVMNode root = null;
        if (version < 0)
        {
            root = fRoot;
        }
        else
        {
            root = AVMDAOs.Instance().fAVMNodeDAO.getAVMStoreRoot(this, version);
        }            
        return root.getDescriptor("main:", "", null);
    }

    /**
     * Lookup a node and insist that it is a directory.
     * @param version The version to look under.
     * @param path The path to the directory.
     * @param write Whether this is in a write context.
     * @return A Lookup object.
     */
    public Lookup lookupDirectory(int version, String path, boolean write)
    {
        // Just do a regular lookup and assert that the last element
        // is a directory.
        Lookup lPath = lookup(version, path, write, false);
        if (lPath == null)
        {
            return null;
        }
        if (lPath.getCurrentNode().getType() != AVMNodeType.PLAIN_DIRECTORY &&
            lPath.getCurrentNode().getType() != AVMNodeType.LAYERED_DIRECTORY)
        {
            return null;
        }
        return lPath;
    }

    /**
     * Get the effective indirection path for a layered node.
     * @param version The version to look under.
     * @param path The path to the node.
     * @return The effective indirection.
     */
    public String getIndirectionPath(int version, String path)
    {
        Lookup lPath = lookup(version, path, false, false);
        if (lPath == null)
        {
            throw new AVMNotFoundException("Path not found.");
        }
        if (!lPath.isLayered())
        {
            return null;
        }
        AVMNode node = lPath.getCurrentNode();
        if (node.getType() == AVMNodeType.LAYERED_DIRECTORY)
        {
            LayeredDirectoryNode dir = (LayeredDirectoryNode)node;
            return dir.getUnderlying(lPath);
        }
        else if (node.getType() == AVMNodeType.LAYERED_FILE)
        {
            LayeredFileNode file = (LayeredFileNode)node;
            return file.getUnderlying(lPath);
        }
        return lPath.getIndirectionPath();
    }
    
    /**
     * Make the indicated node a primary indirection.
     * @param path The path to the node.
     */
    public void makePrimary(String path)
    {
        Lookup lPath = lookupDirectory(-1, path, true);
        if (lPath == null)
        {
            throw new AVMNotFoundException("Path not found.");
        }
        DirectoryNode dir = (DirectoryNode)lPath.getCurrentNode();
        if (!lPath.isLayered())
        {
            throw new AVMException("Not in a layered context: " + path);
        }
        dir.turnPrimary(lPath);
        dir.updateModTime();
    }

    /**
     * Change the indirection of a layered directory.
     * @param path The path to the layered directory.
     * @param target The target indirection to set.
     */
    public void retargetLayeredDirectory(String path, String target)
    {
        Lookup lPath = lookupDirectory(-1, path, true);
        if (lPath == null)
        {
            throw new AVMNotFoundException("Path not found.");
        }
        DirectoryNode dir = (DirectoryNode)lPath.getCurrentNode();
        if (!lPath.isLayered())
        {
            throw new AVMException("Not in a layered context: " + path);
        }
        dir.retarget(lPath, target);
        dir.updateModTime();
    }
    
    /**
     * Set the name of this AVMStore.  Hibernate.
     * @param name
     */
    protected void setName(String name)
    {
        fName = name;
    }
    
    /**
     * Get the name of this AVMStore.
     * @return The name.
     */
    public String getName()
    {
        return fName;
    }
    
    /**
     * Set the next version id.
     * @param nextVersionID
     */
    protected void setNextVersionID(int nextVersionID)
    {
        fNextVersionID = nextVersionID;
    }
    
    /**
     * Get the next version id.
     * @return The next version id.
     */
    public int getNextVersionID()
    {
        return fNextVersionID;
    }
    
    /**
     * This gets the last extant version id.
     */
    public int getLastVersionID()
    {
        return AVMDAOs.Instance().fVersionRootDAO.getMaxVersionID(this);
    }

    /**
     * Set the root directory.  Hibernate.
     * @param root
     */
    protected void setRoot(DirectoryNode root)
    {
        fRoot = root;
    }
    
    /**
     * Get the root directory.
     * @return The root directory.
     */
    public DirectoryNode getRoot()
    {
        return fRoot;
    }
    
    /**
     * Set the version (for concurrency control). Hibernate.
     * @param vers
     */
    protected void setVers(long vers)
    {
        fVers = vers;
    }
    
    /**
     * Get the version (for concurrency control). Hibernate.
     * @return The version.
     */
    protected long getVers()
    {
        return fVers;
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
        if (!(obj instanceof AVMStore))
        {
            return false;
        }
        return fName.equals(((AVMStore)obj).getName());
    }

    /**
     * Get a hash code.
     * @return The hash code.
     */
    @Override
    public int hashCode()
    {
        return fName.hashCode();
    }

    /**
     * Purge all nodes reachable only via this version and repostory.
     * @param version
     */
    @SuppressWarnings("unchecked")
    public void purgeVersion(int version)
    {
        if (version == 0)
        {
            throw new AVMBadArgumentException("Cannot purge initial version");
        }
        VersionRoot vRoot = AVMDAOs.Instance().fVersionRootDAO.getByVersionID(this, version);
        if (vRoot == null)
        {
            throw new AVMNotFoundException("Version not found.");
        }
        AVMNode root = vRoot.getRoot();
        root.setIsRoot(false);
        AVMDAOs.Instance().fAVMNodeDAO.update(root);
        AVMDAOs.Instance().fVersionRootDAO.delete(vRoot);
        if (root.equals(fRoot))
        {
            // We have to set a new current root.
            // TODO More hibernate goofiness to compensate for: fSuper.getSession().flush();
            vRoot = AVMDAOs.Instance().fVersionRootDAO.getMaxVersion(this);
            fRoot = vRoot.getRoot();
            AVMDAOs.Instance().fAVMStoreDAO.update(this);
        }
    }

    /**
     * Get the descriptor for this.
     * @return An AVMStoreDescriptor
     */
    public AVMStoreDescriptor getDescriptor()
    {
        return new AVMStoreDescriptor(fName, 
                getProperty(ContentModel.PROP_CREATOR).getStringValue(),
                ((Date)getProperty(ContentModel.PROP_CREATED).getValue(DataTypeDefinition.DATE)).getTime());
    }

    /**
     * Set the opacity of a layered directory. An opaque directory hides
     * what is pointed at by its indirection.
     * @param path The path to the layered directory.
     * @param opacity True is opaque; false is not.
     */
    public void setOpacity(String path, boolean opacity)
    {
        Lookup lPath = lookup(-1, path, true, false);
        if (lPath == null)
        {
            throw new AVMNotFoundException("Path not found.");
        }
        AVMNode node = lPath.getCurrentNode();
        if (!(node instanceof LayeredDirectoryNode))
        {
            throw new AVMWrongTypeException("Not a LayeredDirectoryNode.");
        }
        ((LayeredDirectoryNode)node).setOpacity(opacity);
        node.updateModTime();
    }
    
    // TODO Does it make sense to set properties on DeletedNodes?
    /**
     * Set a property on a node.
     * @param path The path to the node.
     * @param name The name of the property.
     * @param value The value to set.
     */
    public void setNodeProperty(String path, QName name, PropertyValue value)
    {
        Lookup lPath = lookup(-1, path, true, false);
        if (lPath == null)
        {
            throw new AVMNotFoundException("Path not found.");
        }
        AVMNode node = lPath.getCurrentNode();
        node.setProperty(name, value);
    }
    
    /**
     * Set a collection of properties on a node.
     * @param path The path to the node.
     * @param properties The Map of QNames to PropertyValues.
     */
    public void setNodeProperties(String path, Map<QName, PropertyValue> properties)
    {
        Lookup lPath = lookup(-1, path, true, false);
        if (lPath == null)
        {
            throw new AVMNotFoundException("Path not found.");
        }
        AVMNode node = lPath.getCurrentNode();
        node.setProperties(properties);
    }
    
    /**
     * Get a property by name.
     * @param version The version to lookup.
     * @param path The path to the node.
     * @param name The name of the property.
     * @return A PropertyValue or null if not found.
     */
    public PropertyValue getNodeProperty(int version, String path, QName name)
    {
        Lookup lPath = lookup(version, path, false, false);
        if (lPath == null)
        {
            throw new AVMNotFoundException("Path not found.");
        }
        AVMNode node = lPath.getCurrentNode();
        return node.getProperty(name);
    }
    
    /**
     * Get all the properties associated with a node.
     * @param version The version to lookup.
     * @param path The path to the node.
     * @return A Map of QNames to PropertyValues.
     */
    public Map<QName, PropertyValue> getNodeProperties(int version, String path)
    {
        Lookup lPath = lookup(version, path, false, false);
        if (lPath == null)
        {
            throw new AVMNotFoundException("Path not found.");
        }
        AVMNode node = lPath.getCurrentNode();
        return node.getProperties();
    }
    
    /**
     * Delete a single property from a node.
     * @param path The path to the node.
     * @param name The name of the property.
     */
    public void deleteNodeProperty(String path, QName name)
    {
        Lookup lPath = lookup(-1, path, true, false);
        if (lPath == null)
        {
            throw new AVMNotFoundException("Path not found.");
        }
        AVMNode node = lPath.getCurrentNode();
        node.deleteProperty(name);
    }
    
    /**
     * Delete all properties from a node.
     * @param path The path to the node.
     */
    public void deleteNodeProperties(String path)
    {
        Lookup lPath = lookup(-1, path, true, false);
        if (lPath == null)
        {
            throw new AVMNotFoundException("Path not found.");
        }
        AVMNode node = lPath.getCurrentNode();
        node.deleteProperties();
    }

    /**
     * Set a property on this store. Replaces if property already exists.
     * @param name The QName of the property.
     * @param value The actual PropertyValue.
     */
    public void setProperty(QName name, PropertyValue value)
    {
        AVMStoreProperty prop = new AVMStorePropertyImpl();
        prop.setStore(this);
        prop.setName(name);
        prop.setValue(value);
        AVMDAOs.Instance().fAVMStorePropertyDAO.save(prop);
    }
    
    /**
     * Set a group of properties on this store. Replaces any property that exists.
     * @param properties A Map of QNames to PropertyValues to set.
     */
    public void setProperties(Map<QName, PropertyValue> properties)
    {
        for (QName name : properties.keySet())
        {
            setProperty(name, properties.get(name));
        }
    }
    
    /**
     * Get a property by name.
     * @param name The QName of the property to fetch.
     * @return The PropertyValue or null if non-existent.
     */
    public PropertyValue getProperty(QName name)
    {
        AVMStoreProperty prop = AVMDAOs.Instance().fAVMStorePropertyDAO.get(this, name);
        if (prop == null)
        {
            return null;
        }
        return prop.getValue();
    }
    
    /**
     * Get all the properties associated with this node.
     * @return A Map of the properties.
     */
    public Map<QName, PropertyValue> getProperties()
    {
        List<AVMStoreProperty> props = 
            AVMDAOs.Instance().fAVMStorePropertyDAO.get(this);
        Map<QName, PropertyValue> retVal = new HashMap<QName, PropertyValue>();
        for (AVMStoreProperty prop : props)
        {
            retVal.put(prop.getName(), prop.getValue());
        }
        return retVal;
    }
    
    /**
     * Delete a property.
     * @param name The name of the property to delete.
     */
    public void deleteProperty(QName name)
    {
        AVMDAOs.Instance().fAVMStorePropertyDAO.delete(this, name);
    }
    
    /**
     * Get the ContentData on a file.
     * @param version The version to look under.
     * @param path The path to the file.
     * @return The ContentData corresponding to the file.
     */
    public ContentData getContentDataForRead(int version, String path)
    {
        Lookup lPath = lookup(version, path, false, false);
        if (lPath == null)
        {
            throw new AVMNotFoundException("Path not found.");
        }
        AVMNode node = lPath.getCurrentNode();
        if (!(node instanceof FileNode))
        {
            throw new AVMWrongTypeException("File Expected.");
        }
        return ((FileNode)node).getContentData(lPath);
    }
    
    /**
     * Get the ContentData on a file for writing.
     * @param path The path to the file.
     * @return The ContentData corresponding to the file.
     */
    public ContentData getContentDataForWrite(String path)
    {
        Lookup lPath = lookup(-1, path, true, false);
        if (lPath == null)
        {
            throw new AVMNotFoundException("Path not found.");
        }
        AVMNode node = lPath.getCurrentNode();
        if (!(node instanceof FileNode))
        {
            throw new AVMWrongTypeException("File Expected.");
        }
        node.updateModTime();
        return ((FileNode)node).getContentData(lPath);
    }

    /**
     * Set the ContentData for a file.
     * @param path The path to the file.
     * @param data The ContentData to set.
     */
    public void setContentData(String path, ContentData data)
    {
        Lookup lPath = lookup(-1, path, true, false);
        if (lPath == null)
        {
            throw new AVMNotFoundException("Path not found.");
        }
        AVMNode node = lPath.getCurrentNode();
        if (!(node instanceof FileNode))
        {
            throw new AVMWrongTypeException("File Expected.");
        }
        ((FileNode)node).setContentData(data);
    }

    /**
     * Set meta data, aspects, properties, acls, from another node.
     * @param path The path to the node to set metadata on.
     * @param from The node to get the metadata from.
     */
    public void setMetaDataFrom(String path, AVMNode from)
    {
        Lookup lPath = lookup(-1, path, true, false);
        if (lPath == null)
        {
            throw new AVMNotFoundException("Path not found: " + path);
        }
        AVMNode node = lPath.getCurrentNode();
        node.copyMetaDataFrom(from);
    }

    /**
     * Add an aspect to a node.
     * @param path The path to the node.
     * @param aspectName The name of the aspect.
     */
    public void addAspect(String path, QName aspectName)
    {
        Lookup lPath = lookup(-1, path, true, false);
        if (lPath == null)
        {
            throw new AVMNotFoundException("Path not found.");
        }
        AVMNode node = lPath.getCurrentNode();
        if (AVMDAOs.Instance().fAVMAspectNameDAO.exists(node, aspectName))
        {
            throw new AVMExistsException("Aspect exists.");
        }
        AVMAspectName newName = 
            new AVMAspectNameImpl();
        newName.setNode(node);
        newName.setName(aspectName);
        AVMDAOs.Instance().fAVMAspectNameDAO.save(newName);
    }
    
    /**
     * Get all aspects on a given node.
     * @param version The version to look under.
     * @param path The path to the node.
     * @return A List of the QNames of the aspects.
     */
    public List<QName> getAspects(int version, String path)
    {
        Lookup lPath = lookup(version, path, false, false);
        if (lPath == null)
        {
            throw new AVMNotFoundException("Path not found.");
        }
        AVMNode node = lPath.getCurrentNode();
        List<AVMAspectName> names = 
            AVMDAOs.Instance().fAVMAspectNameDAO.get(node);
        ArrayList<QName> result = new ArrayList<QName>();
        for (AVMAspectName name : names)
        {
            result.add(name.getName());
        }
        return result;
    }

    /**
     * Remove an aspect and all its properties from a node.
     * @param path The path to the node.
     * @param aspectName The name of the aspect.
     */
    public void removeAspect(String path, QName aspectName)
    {
        Lookup lPath = lookup(-1, path, true, false);
        if (lPath == null)
        {
            throw new AVMNotFoundException("Path not found.");
        }
        AVMNode node = lPath.getCurrentNode();
        AVMDAOs.Instance().fAVMAspectNameDAO.delete(node, aspectName);
        AspectDefinition def = RawServices.Instance().getDictionaryService().getAspect(aspectName);
        Map<QName, PropertyDefinition> properties =
            def.getProperties();
        for (QName name : properties.keySet())
        {
            AVMDAOs.Instance().fAVMNodePropertyDAO.delete(node, name);
        }
    }
    
    /**
     * Does a given node have a given aspect.
     * @param version The version to look under.
     * @param path The path to the node.
     * @param aspectName The name of the aspect.
     * @return Whether the node has the aspect.
     */
    public boolean hasAspect(int version, String path, QName aspectName)
    {
        Lookup lPath = lookup(version, path, false, false);
        if (lPath == null)
        {
            throw new AVMNotFoundException("Path not found.");
        }
        AVMNode node = lPath.getCurrentNode();
        return AVMDAOs.Instance().fAVMAspectNameDAO.exists(node, aspectName);
    }
    
    /**
     * Set the ACL on a node.
     * @param path The path to the node.
     * @param acl The ACL to set.
     */
    public void setACL(String path, DbAccessControlList acl)
    {
        Lookup lPath = lookup(-1, path, true, false);
        if (lPath == null)
        {
            throw new AVMNotFoundException("Path not found.");
        }
        AVMNode node = lPath.getCurrentNode();
        node.setAcl(acl);
    }
    
    /**
     * Get the ACL on a node.
     * @param version The version to look under.
     * @param path The path to the node.
     * @return The ACL.
     */
    public DbAccessControlList getACL(int version, String path)
    {
        Lookup lPath = lookup(version, path, false, false);
        if (lPath == null)
        {
            throw new AVMNotFoundException("Path not found.");
        }
        return lPath.getCurrentNode().getAcl();
    }
    
    /**
     * Link a node intro a directory, directly.
     * @param parentPath The path to the directory.
     * @param name The name to give the parent.
     * @param toLink The node to link.
     */
    public void link(String parentPath, String name, AVMNodeDescriptor toLink)
    {
        Lookup lPath = lookupDirectory(-1, parentPath, true);
        if (lPath == null)
        {
            throw new AVMNotFoundException("Path not found.");
        }
        DirectoryNode dir = (DirectoryNode)lPath.getCurrentNode();
        dir.link(lPath, name, toLink);
    }
}
