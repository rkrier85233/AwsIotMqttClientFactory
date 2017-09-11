package com.cleo.amazonaws.services.iot.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URI;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings({"MethodDoesntCallSuperMethod", "WeakerAccess"})
public class DeferredSSLSocket extends SSLSocket {
    private static final String[] PROXY_SCHEMES = {"http", "https"};

    private SSLSocketFactory socketFactory;
    private ProxySelector proxySelector;
    private SSLSocket delegate;

    private SocketAddress deferredBindpoint;
    private Boolean deferredTcpNoDelay;
    private Boolean deferredSoLingerOn;
    private Integer deferredSoLingerValue;
    private Boolean deferredOOBInline;
    private Integer deferredSoTimeout;
    private Integer deferredSendBufferSize;
    private Integer deferredReceiveBufferSize;
    private Boolean deferredKeepAlive;
    private Integer deferredTrafficClass;
    private Boolean deferredReuseAddress;
    private Integer deferredPerformancePreferencesConnectionTime;
    private Integer deferredPerformancePreferencesLatency;
    private Integer deferredPerformancePreferencesBandwidth;
    private String[] deferredEnabledCipherSuites;
    private String[] deferredEnabledProtocols;
    private List<HandshakeCompletedListener> deferredHandshakeCompletedListeners = new ArrayList<>();
    private Boolean deferredUseClientMode;
    private Boolean deferredNeedClientAuth;
    private Boolean deferredWantClientAuth;
    private Boolean deferredEnableSessionCreation;

    public DeferredSSLSocket(SSLSocketFactory socketFactory) {
        this(socketFactory, ProxySelector.getDefault());
    }

    public DeferredSSLSocket(SSLSocketFactory socketFactory, ProxySelector proxySelector) {
        this.socketFactory = socketFactory;
        this.proxySelector = proxySelector;
    }

    @Override
    public void connect(SocketAddress endpoint) throws IOException {
        connectInternal(endpoint, null);
    }

    @Override
    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        connectInternal(endpoint, timeout);
    }

    private void connectInternal(SocketAddress endpoint, Integer timeout) throws IOException {
        if (delegate != null) {
            throw new SocketException("Already connected.");
        }

        List<Proxy> proxies = getProxies(endpoint);
        for (Proxy proxy : proxies) {
            log.debug("Attempting to connect to endpoint {} via proxy: {}.", endpoint, proxy.address());
            Socket socket = new Socket(proxy);
            try {
                if (timeout == null) {
                    socket.connect(endpoint);
                } else {
                    socket.connect(endpoint, timeout);
                }
                InetSocketAddress address = (InetSocketAddress) endpoint;
                delegate = (SSLSocket) socketFactory.createSocket(socket, address.getHostString(), address.getPort(), true);
                log.debug("Connection successful to endpoint {} via proxy: {}.", endpoint, proxy.address());
                break;
            } catch (IOException e) {
                log.debug("Connection failed to endpoint {} via proxy: {}, trying additional proxies.", endpoint, proxy.address());
            }
        }
        if (delegate == null) {
            InetSocketAddress address = (InetSocketAddress) endpoint;
            delegate = (SSLSocket) socketFactory.createSocket(address.getAddress(), address.getPort());
        }

        setDeferredValues();
    }

    @Override
    public void bind(SocketAddress bindpoint) throws IOException {
        if (delegate == null) {
            deferredBindpoint = bindpoint;
        } else {
            delegate.bind(bindpoint);
        }
    }

    @Override
    public InetAddress getInetAddress() {
        return delegate == null ? null : delegate.getInetAddress();
    }

    @Override
    public InetAddress getLocalAddress() {
        return delegate == null ? null : delegate.getLocalAddress();
    }

    @Override
    public int getPort() {
        return delegate == null ? -1 : delegate.getPort();
    }

    @Override
    public int getLocalPort() {
        return delegate == null ? -1 : delegate.getLocalPort();
    }

    @Override
    public SocketAddress getRemoteSocketAddress() {
        return delegate == null ? null : delegate.getRemoteSocketAddress();
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
        return delegate == null ? null : delegate.getLocalSocketAddress();
    }

    @Override
    public SocketChannel getChannel() {
        return delegate == null ? null : delegate.getChannel();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        checkConnected();
        return delegate.getInputStream();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        checkConnected();
        return delegate.getOutputStream();
    }

    @Override
    public void setTcpNoDelay(boolean on) throws SocketException {
        if (delegate == null) {
            deferredTcpNoDelay = on;
        } else {
            delegate.setTcpNoDelay(on);
        }
    }

    @Override
    public boolean getTcpNoDelay() throws SocketException {
        if (delegate == null) {
            return deferredTcpNoDelay == null ? false : deferredTcpNoDelay;
        } else {
            return delegate.getTcpNoDelay();
        }
    }

    @Override
    public void setSoLinger(boolean on, int linger) throws SocketException {
        if (delegate == null) {
            deferredSoLingerOn = on;
            deferredSoLingerValue = linger;
        } else {
            delegate.setSoLinger(on, linger);
        }
    }

    @Override
    public int getSoLinger() throws SocketException {
        if (delegate == null) {
            return deferredSoLingerValue == null ? -1 : deferredSoLingerValue;
        } else {
            return delegate.getSoLinger();
        }
    }

    @Override
    public void sendUrgentData(int data) throws IOException {
        checkConnected();
        delegate.sendUrgentData(data);
    }

    @Override
    public void setOOBInline(boolean on) throws SocketException {
        if (delegate == null) {
            deferredOOBInline = on;
        } else {
            delegate.setOOBInline(on);
        }
    }

    @Override
    public boolean getOOBInline() throws SocketException {
        if (delegate == null) {
            return deferredOOBInline == null ? false : deferredOOBInline;
        } else {
            return delegate.getOOBInline();
        }
    }

    @Override
    public void setSoTimeout(int timeout) throws SocketException {
        if (delegate == null) {
            deferredSoTimeout = timeout;
        } else {
            delegate.setSoTimeout(timeout);
        }
    }

    @Override
    public int getSoTimeout() throws SocketException {
        if (delegate == null) {
            return deferredSoTimeout == null ? -1 : deferredSoTimeout;
        } else {
            return delegate.getSoTimeout();
        }
    }

    @Override
    public void setSendBufferSize(int size) throws SocketException {
        if (delegate == null) {
            deferredSendBufferSize = size;
        } else {
            delegate.setSendBufferSize(size);
        }
    }

    @Override
    public int getSendBufferSize() throws SocketException {
        if (delegate == null) {
            return deferredSendBufferSize == null ? -1 : deferredSendBufferSize;
        } else {
            return delegate.getSendBufferSize();
        }
    }

    @Override
    public void setReceiveBufferSize(int size) throws SocketException {
        if (delegate == null) {
            deferredReceiveBufferSize = size;
        } else {
            delegate.setReceiveBufferSize(size);
        }
    }

    @Override
    public int getReceiveBufferSize() throws SocketException {
        if (delegate == null) {
            return deferredReceiveBufferSize == null ? -1 : deferredReceiveBufferSize;
        } else {
            return delegate.getReceiveBufferSize();
        }
    }

    @Override
    public void setKeepAlive(boolean on) throws SocketException {
        if (delegate == null) {
            deferredKeepAlive = on;
        } else {
            delegate.setKeepAlive(on);
        }
    }

    @Override
    public boolean getKeepAlive() throws SocketException {
        if (delegate == null) {
            return deferredKeepAlive == null ? false : deferredKeepAlive;
        } else {
            return delegate.getKeepAlive();
        }
    }

    @Override
    public void setTrafficClass(int tc) throws SocketException {
        if (delegate == null) {
            deferredTrafficClass = tc;
        } else {
            delegate.setTrafficClass(tc);
        }
    }

    @Override
    public int getTrafficClass() throws SocketException {
        if (delegate == null) {
            return deferredTrafficClass == null ? -1 : deferredTrafficClass;
        } else {
            return delegate.getTrafficClass();
        }
    }

    @Override
    public void setReuseAddress(boolean on) throws SocketException {
        if (delegate == null) {
            deferredReuseAddress = on;
        } else {
            delegate.setReuseAddress(on);
        }
    }

    @Override
    public boolean getReuseAddress() throws SocketException {
        if (delegate == null) {
            return deferredReuseAddress == null ? false : deferredReuseAddress;
        } else {
            return delegate.getReuseAddress();
        }
    }

    @Override
    public void close() throws IOException {
        checkConnected();
        delegate.close();
    }

    @Override
    public void shutdownInput() throws IOException {
        checkConnected();
        delegate.shutdownInput();
    }

    @Override
    public void shutdownOutput() throws IOException {
        checkConnected();
        delegate.shutdownOutput();
    }

    @Override
    public boolean isConnected() {
        return delegate != null && delegate.isConnected();
    }

    @Override
    public boolean isBound() {
        return delegate != null && delegate.isBound();
    }

    @Override
    public boolean isClosed() {
        return delegate != null && delegate.isClosed();
    }

    @Override
    public boolean isInputShutdown() {
        return delegate != null && delegate.isInputShutdown();
    }

    @Override
    public boolean isOutputShutdown() {
        return delegate != null && delegate.isOutputShutdown();
    }

    @Override
    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
        if (delegate == null) {
            deferredPerformancePreferencesConnectionTime = connectionTime;
            deferredPerformancePreferencesLatency = latency;
            deferredPerformancePreferencesBandwidth = bandwidth;
        } else {
            delegate.setPerformancePreferences(connectionTime, latency, bandwidth);
        }
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate == null ? socketFactory.getSupportedCipherSuites() : delegate.getSupportedCipherSuites();
    }

    @Override
    public String[] getEnabledCipherSuites() {
        return delegate == null ? deferredEnabledCipherSuites : delegate.getEnabledCipherSuites();
    }

    @Override
    public void setEnabledCipherSuites(String[] suites) {
        if (delegate == null) {
            deferredEnabledCipherSuites = suites;
        } else {
            delegate.setEnabledCipherSuites(suites);
        }
    }

    @Override
    public String[] getSupportedProtocols() {
        return delegate == null ? null : delegate.getSupportedProtocols();
    }

    @Override
    public String[] getEnabledProtocols() {
        return delegate == null ? deferredEnabledProtocols : delegate.getEnabledProtocols();
    }

    @Override
    public void setEnabledProtocols(String[] protocols) {
        if (delegate == null) {
            deferredEnabledProtocols = protocols;
        } else {
            delegate.setEnabledProtocols(protocols);
        }
    }

    @Override
    public SSLSession getSession() {
        return delegate == null ? null : delegate.getSession();
    }

    @Override
    public void addHandshakeCompletedListener(HandshakeCompletedListener listener) {
        if (delegate == null) {
            deferredHandshakeCompletedListeners.add(listener);
        } else {
            delegate.addHandshakeCompletedListener(listener);
        }
    }

    @Override
    public void removeHandshakeCompletedListener(HandshakeCompletedListener listener) {
        if (delegate == null) {
            deferredHandshakeCompletedListeners.remove(listener);
        } else {
            delegate.removeHandshakeCompletedListener(listener);
        }
    }

    @Override
    public void startHandshake() throws IOException {
        checkConnected();
        delegate.startHandshake();
    }

    @Override
    public void setUseClientMode(boolean mode) {
        if (delegate == null) {
            deferredUseClientMode = mode;
        } else {
            delegate.setUseClientMode(mode);
        }
    }

    @Override
    public boolean getUseClientMode() {
        if (delegate == null) {
            return deferredUseClientMode == null ? false : deferredUseClientMode;
        } else {
            return delegate.getUseClientMode();
        }
    }

    @Override
    public void setNeedClientAuth(boolean need) {
        if (delegate == null) {
            deferredNeedClientAuth = need;
        } else {
            delegate.setNeedClientAuth(need);
        }
    }

    @Override
    public boolean getNeedClientAuth() {
        if (delegate == null) {
            return deferredNeedClientAuth == null ? false : deferredNeedClientAuth;
        } else {
            return delegate.getNeedClientAuth();
        }
    }

    @Override
    public void setWantClientAuth(boolean want) {
        if (delegate == null) {
            deferredWantClientAuth = want;
        } else {
            delegate.setWantClientAuth(want);
        }
    }

    @Override
    public boolean getWantClientAuth() {
        if (delegate == null) {
            return deferredWantClientAuth == null ? false : deferredWantClientAuth;
        } else {
            return delegate.getWantClientAuth();
        }
    }

    @Override
    public void setEnableSessionCreation(boolean flag) {
        if (delegate == null) {
            deferredEnableSessionCreation = flag;
        } else {
            delegate.setEnableSessionCreation(flag);
        }
    }

    @Override
    public boolean getEnableSessionCreation() {
        if (delegate == null) {
            return deferredEnableSessionCreation == null ? false : deferredEnableSessionCreation;
        } else {
            return delegate.getEnableSessionCreation();
        }
    }

    private List<Proxy> getProxies(SocketAddress endpoint) {
        if (proxySelector == null) {
            return Collections.emptyList();
        }

        Set<Proxy> candidates = new LinkedHashSet<>();
        InetSocketAddress address = (InetSocketAddress) endpoint;
        for (int i = 0; i < PROXY_SCHEMES.length; i++) {
            URI uri = URI.create(String.format("%s://%s:%s", PROXY_SCHEMES[i], address.getHostString(), address.getPort()));
            List<Proxy> proxies = proxySelector.select(uri);
            if (proxies != null) {
                candidates.addAll(proxies);
            }
        }
        return candidates.stream().filter(p -> p.type() == Proxy.Type.HTTP || p.type() == Proxy.Type.SOCKS).collect(Collectors.toList());
    }

    private void checkConnected() throws SocketException {
        if (delegate == null) {
            throw new SocketException("Not connected.");
        }
    }

    private void setDeferredValues() throws IOException {
        if (deferredBindpoint != null)
            delegate.bind(deferredBindpoint);
        if (deferredTcpNoDelay != null)
            delegate.setTcpNoDelay(deferredTcpNoDelay);
        if (deferredSoLingerOn != null)
            delegate.setSoLinger(deferredSoLingerOn, deferredSoLingerValue);
        if (deferredOOBInline != null)
            delegate.setOOBInline(deferredOOBInline);
        if (deferredSoTimeout != null)
            delegate.setSoTimeout(deferredSoTimeout);
        if (deferredSendBufferSize != null)
            delegate.setSendBufferSize(deferredSendBufferSize);
        if (deferredReceiveBufferSize != null)
            delegate.setReceiveBufferSize(deferredReceiveBufferSize);
        if (deferredKeepAlive != null)
            delegate.setKeepAlive(deferredKeepAlive);
        if (deferredTrafficClass != null)
            delegate.setTrafficClass(deferredTrafficClass);
        if (deferredReuseAddress != null)
            delegate.setReuseAddress(deferredReuseAddress);
        if (deferredPerformancePreferencesConnectionTime != null)
            delegate.setPerformancePreferences(deferredPerformancePreferencesConnectionTime, deferredPerformancePreferencesLatency, deferredPerformancePreferencesBandwidth);
        if (deferredEnabledCipherSuites != null)
            delegate.setEnabledCipherSuites(deferredEnabledCipherSuites);
        if (deferredEnabledProtocols != null)
            delegate.setEnabledProtocols(deferredEnabledProtocols);
        deferredHandshakeCompletedListeners.forEach(delegate::addHandshakeCompletedListener);
        if (deferredUseClientMode != null)
            delegate.setUseClientMode(deferredUseClientMode);
        if (deferredNeedClientAuth != null)
            delegate.setNeedClientAuth(deferredNeedClientAuth);
        if (deferredWantClientAuth != null)
            delegate.setWantClientAuth(deferredWantClientAuth);
        if (deferredEnableSessionCreation != null)
            delegate.setEnableSessionCreation(deferredEnableSessionCreation);
    }
}
