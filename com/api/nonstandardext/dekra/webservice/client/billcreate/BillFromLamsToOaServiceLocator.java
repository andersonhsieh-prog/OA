/**
 * BillFromLamsToOaServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.api.nonstandardext.dekra.webservice.client.billcreate;

public class BillFromLamsToOaServiceLocator extends org.apache.axis.client.Service implements BillFromLamsToOaService {

    public BillFromLamsToOaServiceLocator() {
    }


    public BillFromLamsToOaServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public BillFromLamsToOaServiceLocator(String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for BillFromLamsToOaServiceHttpPort
    private String BillFromLamsToOaServiceHttpPort_address = "http://10.194.46.190/services/BillFromLamsToOaService";

    public String getBillFromLamsToOaServiceHttpPortAddress() {
        return BillFromLamsToOaServiceHttpPort_address;
    }

    // The WSDD service name defaults to the port name.
    private String BillFromLamsToOaServiceHttpPortWSDDServiceName = "BillFromLamsToOaServiceHttpPort";

    public String getBillFromLamsToOaServiceHttpPortWSDDServiceName() {
        return BillFromLamsToOaServiceHttpPortWSDDServiceName;
    }

    public void setBillFromLamsToOaServiceHttpPortWSDDServiceName(String name) {
        BillFromLamsToOaServiceHttpPortWSDDServiceName = name;
    }

    public BillFromLamsToOaServicePortType getBillFromLamsToOaServiceHttpPort() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(BillFromLamsToOaServiceHttpPort_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getBillFromLamsToOaServiceHttpPort(endpoint);
    }

    public BillFromLamsToOaServicePortType getBillFromLamsToOaServiceHttpPort(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            BillFromLamsToOaServiceHttpBindingStub _stub = new BillFromLamsToOaServiceHttpBindingStub(portAddress, this);
            _stub.setPortName(getBillFromLamsToOaServiceHttpPortWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setBillFromLamsToOaServiceHttpPortEndpointAddress(String address) {
        BillFromLamsToOaServiceHttpPort_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (BillFromLamsToOaServicePortType.class.isAssignableFrom(serviceEndpointInterface)) {
                BillFromLamsToOaServiceHttpBindingStub _stub = new BillFromLamsToOaServiceHttpBindingStub(new java.net.URL(BillFromLamsToOaServiceHttpPort_address), this);
                _stub.setPortName(getBillFromLamsToOaServiceHttpPortWSDDServiceName());
                return _stub;
            }
        }
        catch (Throwable t) {
            throw new javax.xml.rpc.ServiceException(t);
        }
        throw new javax.xml.rpc.ServiceException("There is no stub implementation for the interface:  " + (serviceEndpointInterface == null ? "null" : serviceEndpointInterface.getName()));
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(javax.xml.namespace.QName portName, Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        if (portName == null) {
            return getPort(serviceEndpointInterface);
        }
        String inputPortName = portName.getLocalPart();
        if ("BillFromLamsToOaServiceHttpPort".equals(inputPortName)) {
            return getBillFromLamsToOaServiceHttpPort();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("webservices.services.weaver.com.cn", "BillFromLamsToOaService");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("webservices.services.weaver.com.cn", "BillFromLamsToOaServiceHttpPort"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(String portName, String address) throws javax.xml.rpc.ServiceException {
        
if ("BillFromLamsToOaServiceHttpPort".equals(portName)) {
            setBillFromLamsToOaServiceHttpPortEndpointAddress(address);
        }
        else 
{ // Unknown Port Name
            throw new javax.xml.rpc.ServiceException(" Cannot set Endpoint Address for Unknown Port" + portName);
        }
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(javax.xml.namespace.QName portName, String address) throws javax.xml.rpc.ServiceException {
        setEndpointAddress(portName.getLocalPart(), address);
    }

}
