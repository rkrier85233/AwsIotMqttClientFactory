package com.cleo.amazonaws.services.iot.client;

import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.internal.security.SSLSocketFactoryFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.ssl.SSLSocketFactory;

public class ProxyAwareSSLSocketFactory extends SSLSocketFactory {
    private SSLSocketFactory delegate;

    public ProxyAwareSSLSocketFactory() throws MqttSecurityException {
        SSLSocketFactoryFactory wSSFactoryFactory = new SSLSocketFactoryFactory();
        delegate = wSSFactoryFactory.createSocketFactory(null);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public Socket createSocket() throws IOException {
        return new DeferredSSLSocket(delegate);
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        InetSocketAddress address = new InetSocketAddress(host, port);
        DeferredSSLSocket sslSocket = new DeferredSSLSocket(delegate);
        sslSocket.connect(address);
        return sslSocket;
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
        return delegate.createSocket(host, port, localHost, localPort);
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return delegate.createSocket(host, port);
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return delegate.createSocket(address, port, localAddress, localPort);
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return delegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
        return delegate.createSocket(socket, host, port, autoClose);
    }
}
