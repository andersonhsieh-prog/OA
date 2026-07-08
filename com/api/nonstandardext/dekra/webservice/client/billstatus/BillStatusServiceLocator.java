/**
 * BillStatusServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.api.nonstandardext.dekra.webservice.client.billstatus;

public class BillStatusServiceLocator extends org.apache.axis.client.Service implements BillStatusService {

    public BillStatusServiceLocator() {
    }


    public BillStatusServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public BillStatusServiceLocator(String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for BillStatusServiceHttpPort
    private String BillStatusServiceHttpPort_address = "http://10.194.46.190/services/BillStatusService";

    public String getBillStatusServiceHttpPortAddress() {
        return BillStatusServiceHttpPort_address;
    }

    // The WSDD service name defaults to the port name.
    private String BillStatusServiceHttpPortWSDDServiceName = "BillStatusServiceHttpPort";

    public String getBillStatusServiceHttpPortWSDDServiceName() {
        return BillStatusServiceHttpPortWSDDServiceName;
    }

    public void setBillStatusServiceHttpPortWSDDServiceName(String name) {
        BillStatusServiceHttpPortWSDDServiceName = name;
    }

    public BillStatusServicePortType getBillStatusServiceHttpPort() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(BillStatusServiceHttpPort_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getBillStatusServiceHttpPort(endpoint);
    }

    public BillStatusServicePortType getBillStatusServiceHttpPort(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            BillStatusServiceHttpBindingStub _stub = new BillStatusServiceHttpBindingStub(portAddress, this);
            _stub.setPortName(getBillStatusServiceHttpPortWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setBillStatusServiceHttpPortEndpointAddress(String address) {
        BillStatusServiceHttpPort_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (BillStatusServicePortType.class.isAssignableFrom(serviceEndpointInterface)) {
                BillStatusServiceHttpBindingStub _stub = new BillStatusServiceHttpBindingStub(new java.net.URL(BillStatusServiceHttpPort_address), this);
                _stub.setPortName(getBillStatusServiceHttpPortWSDDServiceName());
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
        if ("BillStatusServiceHttpPort".equals(inputPortName)) {
            return getBillStatusServiceHttpPort();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("webservices.services.weaver.com.cn", "BillStatusService");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("webservices.services.weaver.com.cn", "BillStatusServiceHttpPort"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(String portName, String address) throws javax.xml.rpc.ServiceException {
        
if ("BillStatusServiceHttpPort".equals(portName)) {
            setBillStatusServiceHttpPortEndpointAddress(address);
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
