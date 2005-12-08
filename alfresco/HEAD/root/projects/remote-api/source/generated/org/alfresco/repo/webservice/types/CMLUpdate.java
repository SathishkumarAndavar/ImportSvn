/**
 * CMLUpdate.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.2.1 Jun 14, 2005 (09:15:57 EDT) WSDL2Java emitter.
 */

package org.alfresco.repo.webservice.types;

public class CMLUpdate  implements java.io.Serializable {
    private org.alfresco.repo.webservice.types.NamedValue[] property;
    private org.alfresco.repo.webservice.types.Predicate where;
    private java.lang.String where_id;

    public CMLUpdate() {
    }

    public CMLUpdate(
           org.alfresco.repo.webservice.types.NamedValue[] property,
           org.alfresco.repo.webservice.types.Predicate where,
           java.lang.String where_id) {
           this.property = property;
           this.where = where;
           this.where_id = where_id;
    }


    /**
     * Gets the property value for this CMLUpdate.
     * 
     * @return property
     */
    public org.alfresco.repo.webservice.types.NamedValue[] getProperty() {
        return property;
    }


    /**
     * Sets the property value for this CMLUpdate.
     * 
     * @param property
     */
    public void setProperty(org.alfresco.repo.webservice.types.NamedValue[] property) {
        this.property = property;
    }

    public org.alfresco.repo.webservice.types.NamedValue getProperty(int i) {
        return this.property[i];
    }

    public void setProperty(int i, org.alfresco.repo.webservice.types.NamedValue _value) {
        this.property[i] = _value;
    }


    /**
     * Gets the where value for this CMLUpdate.
     * 
     * @return where
     */
    public org.alfresco.repo.webservice.types.Predicate getWhere() {
        return where;
    }


    /**
     * Sets the where value for this CMLUpdate.
     * 
     * @param where
     */
    public void setWhere(org.alfresco.repo.webservice.types.Predicate where) {
        this.where = where;
    }


    /**
     * Gets the where_id value for this CMLUpdate.
     * 
     * @return where_id
     */
    public java.lang.String getWhere_id() {
        return where_id;
    }


    /**
     * Sets the where_id value for this CMLUpdate.
     * 
     * @param where_id
     */
    public void setWhere_id(java.lang.String where_id) {
        this.where_id = where_id;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof CMLUpdate)) return false;
        CMLUpdate other = (CMLUpdate) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.property==null && other.getProperty()==null) || 
             (this.property!=null &&
              java.util.Arrays.equals(this.property, other.getProperty()))) &&
            ((this.where==null && other.getWhere()==null) || 
             (this.where!=null &&
              this.where.equals(other.getWhere()))) &&
            ((this.where_id==null && other.getWhere_id()==null) || 
             (this.where_id!=null &&
              this.where_id.equals(other.getWhere_id())));
        __equalsCalc = null;
        return _equals;
    }

    private boolean __hashCodeCalc = false;
    public synchronized int hashCode() {
        if (__hashCodeCalc) {
            return 0;
        }
        __hashCodeCalc = true;
        int _hashCode = 1;
        if (getProperty() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getProperty());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getProperty(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getWhere() != null) {
            _hashCode += getWhere().hashCode();
        }
        if (getWhere_id() != null) {
            _hashCode += getWhere_id().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(CMLUpdate.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.alfresco.org/ws/cml/1.0", ">CML>update"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("property");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.alfresco.org/ws/cml/1.0", "property"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.alfresco.org/ws/model/content/1.0", "NamedValue"));
        elemField.setNillable(false);
        elemField.setMaxOccursUnbounded(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("where");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.alfresco.org/ws/cml/1.0", "where"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.alfresco.org/ws/model/content/1.0", "Predicate"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("where_id");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.alfresco.org/ws/cml/1.0", "where_id"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
    }

    /**
     * Return type metadata object
     */
    public static org.apache.axis.description.TypeDesc getTypeDesc() {
        return typeDesc;
    }

    /**
     * Get Custom Serializer
     */
    public static org.apache.axis.encoding.Serializer getSerializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanSerializer(
            _javaType, _xmlType, typeDesc);
    }

    /**
     * Get Custom Deserializer
     */
    public static org.apache.axis.encoding.Deserializer getDeserializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanDeserializer(
            _javaType, _xmlType, typeDesc);
    }

}
