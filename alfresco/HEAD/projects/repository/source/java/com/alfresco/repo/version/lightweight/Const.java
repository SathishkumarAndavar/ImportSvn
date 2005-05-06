/**
 * Created on Apr 21, 2005
 */
package org.alfresco.repo.version.lightweight;

import org.alfresco.repo.dictionary.ClassRef;
import org.alfresco.repo.ref.QName;
import org.alfresco.repo.version.Version;

/**
 * interface conating the constants used by the light weight 
 * version store implementation
 * 
 * @author Roy Wetherall
 */
public interface Const
{
    /**
     * Namespace
     */
    public static final String NAMESPACE_URI = "http://www.alfresco.com/lightWeightVersionStore/1.0";
    
    /**
     * Version history type
     */
    public static final String TYPE_VERSION_HISTORY = "versionHistory";
    public static final QName TYPE_QNAME_VERSION_HISTORY = QName.createQName(NAMESPACE_URI, TYPE_VERSION_HISTORY);
    public static final ClassRef CLASS_REF_VERSION_HISTORY = new ClassRef(TYPE_QNAME_VERSION_HISTORY);
    
    /**
     * Version history properties and associations
     */
    public static final String PROP_VERSIONED_NODE_ID = "versionedNodeId";
    public static final QName PROP_QNAME_VERSIONED_NODE_ID = QName.createQName(NAMESPACE_URI, PROP_VERSIONED_NODE_ID);        
    public static final QName ASSOC_ROOT_VERSION = QName.createQName(NAMESPACE_URI, "rootVersion");
    
    /**
     * Verison type
     */
    public static final String TYPE_VERSION = "version";
    public static final QName TYPE_QNAME_VERSION = QName.createQName(NAMESPACE_URI, TYPE_VERSION);
    public static final ClassRef CLASS_REF_VERSION = new ClassRef(TYPE_QNAME_VERSION);
    
    /**
     * Version type properties and associations
     */
    public static final QName PROP_QNAME_VERSION_LABEL = QName.createQName(NAMESPACE_URI, Version.PROP_VERSION_LABEL);
    public static final QName PROP_QNAME_VERSION_NUMBER = QName.createQName(NAMESPACE_URI, Version.PROP_VERSION_NUMBER);
    public static final QName PROP_QNAME_VERSION_CREATED_DATE = QName.createQName(NAMESPACE_URI, Version.PROP_CREATED_DATE);
    public static final QName PROP_QNAME_FROZEN_NODE_ID = QName.createQName(NAMESPACE_URI, Version.PROP_FROZEN_NODE_ID);
    public static final QName PROP_QNAME_FROZEN_NODE_TYPE = QName.createQName(NAMESPACE_URI, Version.PROP_FROZEN_NODE_TYPE);
    public static final QName PROP_QNAME_FROZEN_NODE_STORE_PROTOCOL = QName.createQName(NAMESPACE_URI, Version.PROP_FROZEN_NODE_STORE_PROTOCOL);
    public static final QName PROP_QNAME_FROZEN_NODE_STORE_ID = QName.createQName(NAMESPACE_URI, Version.PROP_FROZEN_NODE_STORE_ID);
    public static final QName PROP_QNAME_FROZEN_ASPECTS = QName.createQName(NAMESPACE_URI, Version.PROP_FROZEN_ASPECTS);
    public static final QName ASSOC_SUCCESSOR = QName.createQName(NAMESPACE_URI, "successor");    
    
    /**
     * Versioned attribute type
     */
    public static final String TYPE_VERSIONED_PROPERTY = "versionedProperty";
    public static final QName TYPE_QNAME_VERSIONED_PROPERTY = QName.createQName(NAMESPACE_URI, TYPE_VERSIONED_PROPERTY);
    public static final ClassRef CLASS_REF_VERSIONED_PROPERTY = new ClassRef(TYPE_QNAME_VERSIONED_PROPERTY);
    
    /**
     * Versioned attribute properties
     */
    public static final String PROP_QNAME = "qname";
    public static final String PROP_VALUE = "value";
    public static final QName PROP_QNAME_QNAME = QName.createQName(NAMESPACE_URI, PROP_QNAME);
    public static final QName PROP_QNAME_VALUE = QName.createQName(NAMESPACE_URI, PROP_VALUE);
    
    /**
     * Versioned child assoc type
     */
    public static final String TYPE_VERSIONED_CHILD_ASSOC = "versionedChildAssoc";
    public static final QName TYPE_QNAME_VERSIONED_CHILD_ASSOC = QName.createQName(NAMESPACE_URI, TYPE_VERSIONED_CHILD_ASSOC);
    public static final ClassRef CLASS_REF_VERSIONED_CHILD_ASSOC = new ClassRef(TYPE_QNAME_VERSIONED_CHILD_ASSOC);
    
    /**
     * Versioned child assoc properties
     */
    public static final String PROP_ASSOC_QNAME = "assocQName";
    public static final String PROP_IS_PRIMARY = "isPrimary";
    public static final String PROP_NTH_SIBLING = "nthSibling";
    public static final QName PROP_QNAME_ASSOC_QNAME = QName.createQName(NAMESPACE_URI, PROP_ASSOC_QNAME);
    public static final QName PROP_QNAME_IS_PRIMARY = QName.createQName(NAMESPACE_URI, PROP_IS_PRIMARY);
    public static final QName PROP_QNAME_NTH_SIBLING = QName.createQName(NAMESPACE_URI, PROP_NTH_SIBLING);
    
    /**
     * Versioned assoc type
     */
    public static final String TYPE_VERSIONED_ASSOC = "versionedAssoc";
    public static final QName TYPE_QNAME_VERSIONED_ASSOC = QName.createQName(NAMESPACE_URI, TYPE_VERSIONED_ASSOC);
    public static final ClassRef CLASS_REF_VERSIONED_ASSOC = new ClassRef(TYPE_QNAME_VERSIONED_ASSOC);
    
    /**
     * Child relationship names
     */
    public static final String CHILD_VERSION_HISTORIES = "versionHistory";
    public static final String CHILD_VERSIONS = "version";
    public static final String CHILD_VERSIONED_ATTRIBUTES = "versionedAttributes";
    public static final String CHILD_VERSIONED_CHILD_ASSOCS = "versionedChildAssocs";
    public static final String CHILD_VERSIONED_ASSOCS = "versionedAssocs";
    
    public static final QName CHILD_QNAME_VERSION_HISTORIES = QName.createQName(NAMESPACE_URI, CHILD_VERSION_HISTORIES);
    public static final QName CHILD_QNAME_VERSIONS = QName.createQName(NAMESPACE_URI, CHILD_VERSIONS);
    public static final QName CHILD_QNAME_VERSIONED_ATTRIBUTES = QName.createQName(NAMESPACE_URI, CHILD_VERSIONED_ATTRIBUTES);
    public static final QName CHILD_QNAME_VERSIONED_CHILD_ASSOCS = QName.createQName(NAMESPACE_URI, CHILD_VERSIONED_CHILD_ASSOCS);
    public static final QName CHILD_QNAME_VERSIONED_ASSOCS = QName.createQName(NAMESPACE_URI, CHILD_VERSIONED_ASSOCS);
}
