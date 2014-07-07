/*
 * Copyright 2010 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.ning.http.client;

import static com.ning.http.util.MiscUtil.isNonEmpty;

import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.http.client.Request.EntityWriter;
import com.ning.http.client.cookie.Cookie;
import com.ning.http.client.uri.UriComponents;
import com.ning.http.util.AsyncHttpProviderUtils;
import com.ning.http.util.UTF8UrlEncoder;

/**
 * Builder for {@link Request}
 * 
 * @param <T>
 */
public abstract class RequestBuilderBase<T extends RequestBuilderBase<T>> {
    private final static Logger logger = LoggerFactory.getLogger(RequestBuilderBase.class);

    private static final UriComponents DEFAULT_REQUEST_URL = UriComponents.create("http://localhost");

    private static final class RequestImpl implements Request {
        private String method;
        private UriComponents uri;
        private InetAddress address;
        private InetAddress localAddress;
        private FluentCaseInsensitiveStringsMap headers = new FluentCaseInsensitiveStringsMap();
        private ArrayList<Cookie> cookies;
        private byte[] byteData;
        private String stringData;
        private InputStream streamData;
        private EntityWriter entityWriter;
        private BodyGenerator bodyGenerator;
        private List<Param> formParams;
        private List<Part> parts;
        private String virtualHost;
        private long length = -1;
        public ProxyServer proxyServer;
        private Realm realm;
        private File file;
        private Boolean followRedirects;
        private int requestTimeoutInMs;
        private long rangeOffset;
        public String charset;
        private ConnectionPoolKeyStrategy connectionPoolKeyStrategy = DefaultConnectionPoolStrategy.INSTANCE;
        private List<Param> queryParams;

        public RequestImpl() {
        }

        public RequestImpl(Request prototype) {
            if (prototype != null) {
                this.method = prototype.getMethod();
                this.uri = prototype.getURI();
                this.address = prototype.getInetAddress();
                this.localAddress = prototype.getLocalAddress();
                this.headers = new FluentCaseInsensitiveStringsMap(prototype.getHeaders());
                this.cookies = new ArrayList<Cookie>(prototype.getCookies());
                this.byteData = prototype.getByteData();
                this.stringData = prototype.getStringData();
                this.streamData = prototype.getStreamData();
                this.entityWriter = prototype.getEntityWriter();
                this.bodyGenerator = prototype.getBodyGenerator();
                this.formParams = prototype.getFormParams() == null ? null : new ArrayList<Param>(prototype.getFormParams());
                this.parts = prototype.getParts() == null ? null : new ArrayList<Part>(prototype.getParts());
                this.virtualHost = prototype.getVirtualHost();
                this.length = prototype.getContentLength();
                this.proxyServer = prototype.getProxyServer();
                this.realm = prototype.getRealm();
                this.file = prototype.getFile();
                this.followRedirects = prototype.getFollowRedirect();
                this.requestTimeoutInMs = prototype.getRequestTimeoutInMs();
                this.rangeOffset = prototype.getRangeOffset();
                this.charset = prototype.getBodyEncoding();
                this.connectionPoolKeyStrategy = prototype.getConnectionPoolKeyStrategy();
            }
        }

        public String getMethod() {
            return method;
        }

        public InetAddress getInetAddress() {
            return address;
        }

        public InetAddress getLocalAddress() {
            return localAddress;
        }

        private String removeTrailingSlash(UriComponents uri) {
            String uriString = uri.toString();
            if (uriString.endsWith("/")) {
                return uriString.substring(0, uriString.length() - 1);
            } else {
                return uriString;
            }
        }

        public String getUrl() {
            return removeTrailingSlash(getURI());
        }

        public UriComponents getURI() {
            return uri;
        }

        public FluentCaseInsensitiveStringsMap getHeaders() {
            return headers;
        }

        public Collection<Cookie> getCookies() {
            return cookies != null ? Collections.unmodifiableCollection(cookies) : Collections.<Cookie> emptyList();
        }

        public byte[] getByteData() {
            return byteData;
        }

        public String getStringData() {
            return stringData;
        }

        public InputStream getStreamData() {
            return streamData;
        }

        public EntityWriter getEntityWriter() {
            return entityWriter;
        }

        public BodyGenerator getBodyGenerator() {
            return bodyGenerator;
        }

        public long getContentLength() {
            return length;
        }

        public List<Param> getFormParams() {
            return formParams != null ? formParams : Collections.<Param> emptyList();
        }

        public List<Part> getParts() {
            return parts != null ? parts : Collections.<Part> emptyList();
        }

        public String getVirtualHost() {
            return virtualHost;
        }

        public ProxyServer getProxyServer() {
            return proxyServer;
        }

        public Realm getRealm() {
            return realm;
        }

        public File getFile() {
            return file;
        }

        public Boolean getFollowRedirect() {
            return followRedirects;
        }

        public int getRequestTimeoutInMs() {
            return requestTimeoutInMs;
        }

        public long getRangeOffset() {
            return rangeOffset;
        }

        public String getBodyEncoding() {
            return charset;
        }

        public ConnectionPoolKeyStrategy getConnectionPoolKeyStrategy() {
            return connectionPoolKeyStrategy;
        }

        @Override
        public List<Param> getQueryParams() {
            if (queryParams == null)
                // lazy load
                if (isNonEmpty(uri.getQuery())) {
                    queryParams = new ArrayList<Param>(1);
                    for (String queryStringParam : uri.getQuery().split("&")) {
                        int pos = queryStringParam.indexOf('=');
                        if (pos <= 0)
                            queryParams.add(new Param(queryStringParam, null));
                        else
                            queryParams.add(new Param(queryStringParam.substring(0, pos), queryStringParam.substring(pos + 1)));
                    }
                } else
                    queryParams = Collections.emptyList();
            return queryParams;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(getURI().toString());

            sb.append("\t");
            sb.append(method);
            sb.append("\theaders:");
            if (isNonEmpty(headers)) {
                for (String name : headers.keySet()) {
                    sb.append("\t");
                    sb.append(name);
                    sb.append(":");
                    sb.append(headers.getJoinedValue(name, ", "));
                }
            }
            if (isNonEmpty(formParams)) {
                sb.append("\tformParams:");
                for (Param param : formParams) {
                    sb.append("\t");
                    sb.append(param.getName());
                    sb.append(":");
                    sb.append(param.getValue());
                }
            }

            return sb.toString();
        }
    }

    private final Class<T> derived;
    protected final RequestImpl request;
    protected boolean disableUrlEncoding;
    protected List<Param> queryParams;
    protected SignatureCalculator signatureCalculator;

    protected RequestBuilderBase(Class<T> derived, String method, boolean disableUrlEncoding) {
        this.derived = derived;
        request = new RequestImpl();
        request.method = method;
        this.disableUrlEncoding = disableUrlEncoding;
    }

    protected RequestBuilderBase(Class<T> derived, Request prototype) {
        this.derived = derived;
        request = new RequestImpl(prototype);
    }

    public T setUrl(String url) {
        return setURI(UriComponents.create(url));
    }

    public T setURI(UriComponents uri) {
        if (uri.getPath() == null)
            throw new NullPointerException("uri.path");
        request.uri = uri;
        return derived.cast(this);
    }

    public T setInetAddress(InetAddress address) {
        request.address = address;
        return derived.cast(this);
    }

    public T setLocalInetAddress(InetAddress address) {
        request.localAddress = address;
        return derived.cast(this);
    }

    public T setVirtualHost(String virtualHost) {
        request.virtualHost = virtualHost;
        return derived.cast(this);
    }

    public T setHeader(String name, String value) {
        request.headers.replace(name, value);
        return derived.cast(this);
    }

    public T addHeader(String name, String value) {
        if (value == null) {
            logger.warn("Value was null, set to \"\"");
            value = "";
        }

        request.headers.add(name, value);
        return derived.cast(this);
    }

    public T setHeaders(FluentCaseInsensitiveStringsMap headers) {
        request.headers = (headers == null ? new FluentCaseInsensitiveStringsMap() : new FluentCaseInsensitiveStringsMap(headers));
        return derived.cast(this);
    }

    public T setHeaders(Map<String, Collection<String>> headers) {
        request.headers = (headers == null ? new FluentCaseInsensitiveStringsMap() : new FluentCaseInsensitiveStringsMap(headers));
        return derived.cast(this);
    }

    public T setContentLength(int length) {
        request.length = length;
        return derived.cast(this);
    }

    private void lazyInitCookies() {
        if (request.cookies == null)
            request.cookies = new ArrayList<Cookie>(3);
    }

    public T setCookies(Collection<Cookie> cookies) {
        request.cookies = new ArrayList<Cookie>(cookies);
        return derived.cast(this);
    }

    public T addCookie(Cookie cookie) {
        lazyInitCookies();
        request.cookies.add(cookie);
        return derived.cast(this);
    }

    public T addOrReplaceCookie(Cookie cookie) {
        String cookieKey = cookie.getName();
        boolean replace = false;
        int index = 0;
        lazyInitCookies();
        for (Cookie c : request.cookies) {
            if (c.getName().equals(cookieKey)) {
                replace = true;
                break;
            }

            index++;
        }
        if (replace)
            request.cookies.set(index, cookie);
        else
            request.cookies.add(cookie);
        return derived.cast(this);
    }
    
    public void resetCookies() {
        if (request.cookies != null)
            request.cookies.clear();
    }
    
    public void resetQuery() {
        queryParams = null;
        request.uri = request.uri.withNewQuery(null);
    }
    
    public void resetFormParams() {
        request.formParams = null;
    }

    public void resetNonMultipartData() {
        request.byteData = null;
        request.stringData = null;
        request.streamData = null;
        request.entityWriter = null;
        request.length = -1;
    }

    public void resetMultipartData() {
        request.parts = null;
    }

    public T setBody(File file) {
        request.file = file;
        return derived.cast(this);
    }

    public T setBody(byte[] data) {
        resetFormParams();
        resetNonMultipartData();
        resetMultipartData();
        request.byteData = data;
        return derived.cast(this);
    }

    public T setBody(String data) {
        resetFormParams();
        resetNonMultipartData();
        resetMultipartData();
        request.stringData = data;
        return derived.cast(this);
    }

    public T setBody(InputStream stream) {
        resetFormParams();
        resetNonMultipartData();
        resetMultipartData();
        request.streamData = stream;
        return derived.cast(this);
    }

    public T setBody(EntityWriter dataWriter) {
        return setBody(dataWriter, -1);
    }

    public T setBody(EntityWriter dataWriter, long length) {
        resetFormParams();
        resetNonMultipartData();
        resetMultipartData();
        request.entityWriter = dataWriter;
        request.length = length;
        return derived.cast(this);
    }

    public T setBody(BodyGenerator bodyGenerator) {
        request.bodyGenerator = bodyGenerator;
        return derived.cast(this);
    }

    public T addQueryParam(String name, String value) {
        if (queryParams == null) {
            queryParams = new ArrayList<Param>(1);
        }
        queryParams.add(new Param(name, value));
        return derived.cast(this);
    }

    private List<Param> map2ParamList(Map<String, List<String>> map) {
        if (map == null)
            return null;

        List<Param> params = new ArrayList<Param>(map.size());
        for (Map.Entry<String, List<String>> entries : map.entrySet()) {
            String name = entries.getKey();
            for (String value : entries.getValue())
                params.add(new Param(name, value));
        }
        return params;
    }
    
    public T setQueryParams(Map<String, List<String>> map) {
        return setQueryParams(map2ParamList(map));
    }

    public T setQueryParams(List<Param> params) {
        queryParams = params;
        return derived.cast(this);
    }
    
    public T addFormParam(String name, String value) {
        resetNonMultipartData();
        resetMultipartData();
        if (request.formParams == null)
            request.formParams = new ArrayList<Param>(1);
        request.formParams.add(new Param(name, value));
        return derived.cast(this);
    }

    public T setFormParams(Map<String, List<String>> map) {
        return setFormParams(map2ParamList(map));
    }
    public T setFormParams(List<Param> params) {
        resetNonMultipartData();
        resetMultipartData();
        request.formParams = params;
        return derived.cast(this);
    }

    public T addBodyPart(Part part) {
        resetFormParams();
        resetNonMultipartData();
        if (request.parts == null)
            request.parts = new ArrayList<Part>();
        request.parts.add(part);
        return derived.cast(this);
    }

    public T setProxyServer(ProxyServer proxyServer) {
        request.proxyServer = proxyServer;
        return derived.cast(this);
    }

    public T setRealm(Realm realm) {
        request.realm = realm;
        return derived.cast(this);
    }

    public T setFollowRedirects(boolean followRedirects) {
        request.followRedirects = followRedirects;
        return derived.cast(this);
    }

    public T setRequestTimeoutInMs(int requestTimeoutInMs) {
        request.requestTimeoutInMs = requestTimeoutInMs;
        return derived.cast(this);
    }

    public T setRangeOffset(long rangeOffset) {
        request.rangeOffset = rangeOffset;
        return derived.cast(this);
    }

    public T setMethod(String method) {
        request.method = method;
        return derived.cast(this);
    }

    public T setBodyEncoding(String charset) {
        request.charset = charset;
        return derived.cast(this);
    }

    public T setConnectionPoolKeyStrategy(ConnectionPoolKeyStrategy connectionPoolKeyStrategy) {
        request.connectionPoolKeyStrategy = connectionPoolKeyStrategy;
        return derived.cast(this);
    }

    public T setSignatureCalculator(SignatureCalculator signatureCalculator) {
        this.signatureCalculator = signatureCalculator;
        return derived.cast(this);
    }

    private void executeSignatureCalculator() {
        /* Let's first calculate and inject signature, before finalizing actual build
         * (order does not matter with current implementation but may in future)
         */
        if (signatureCalculator != null) {
            // Should not include query parameters, ensure:
            String url = new UriComponents(request.uri.getScheme(), null, request.uri.getHost(), request.uri.getPort(), request.uri.getPath(), null).toString();
            signatureCalculator.calculateAndAddSignature(url, request, this);
        }
    }
    
    private void computeRequestCharset() {
        if (request.charset == null) {
            try {
                final String contentType = request.headers.getFirstValue("Content-Type");
                if (contentType != null) {
                    final String charset = AsyncHttpProviderUtils.parseCharset(contentType);
                    if (charset != null) {
                        // ensure that if charset is provided with the Content-Type header,
                        // we propagate that down to the charset of the Request object
                        request.charset = charset;
                    }
                }
            } catch (Throwable e) {
                // NoOp -- we can't fix the Content-Type or charset from here
            }
        }
    }
    
    private void computeRequestLength() {
        if (request.length < 0 && request.streamData == null) {
            // can't concatenate content-length
            final String contentLength = request.headers.getFirstValue("Content-Length");

            if (contentLength != null) {
                try {
                    request.length = Long.parseLong(contentLength);
                } catch (NumberFormatException e) {
                    // NoOp -- we wdn't specify length so it will be chunked?
                }
            }
        }
    }
    
    private void appendRawQueryParams(StringBuilder sb, List<Param> queryParams) {
        for (Param param : queryParams)
            appendRawQueryParam(sb, param.getName(), param.getValue());
    }
    
    private void appendRawQueryParam(StringBuilder sb, String name, String value) {
        sb.append(name);
        if (value != null)
            sb.append('=').append(value);
        sb.append('&');
    }
    
    private String decodeUTF8(String s) {
        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
    
    // FIXME super inefficient!!!
    private void appendEscapedQueryParam(StringBuilder sb, String name, String value) {
        UTF8UrlEncoder.appendEncoded(sb, name);
        if (value != null) {
            sb.append('=');
            UTF8UrlEncoder.appendEncoded(sb, value);
        }
        sb.append('&');
    }

    private void appendEscapeQuery(StringBuilder sb, String query) {
        int pos;
        for (String queryParamString : query.split("&")) {
            pos = queryParamString.indexOf('=');
            if (pos <= 0) {
                String decodedName = decodeUTF8(queryParamString);
                appendEscapedQueryParam(sb, decodedName, null);
            } else {
                String decodedName = decodeUTF8(queryParamString.substring(0, pos));
                String decodedValue = decodeUTF8(queryParamString.substring(pos + 1));
                appendEscapedQueryParam(sb, decodedName, decodedValue);
            }
        }
    }
    
    private void appendEscapeQueryParams(StringBuilder sb, List<Param> queryParams) {
        for (Param param: queryParams)
            appendEscapedQueryParam(sb, param.getName(), param.getValue());
    }
    
    private String computeFinalQueryString(String query, List<Param> queryParams) {
        
        boolean hasQuery = isNonEmpty(query);
        boolean hasQueryParams = isNonEmpty(queryParams);
        
        if (hasQuery) {
            if (hasQueryParams) {
                if (disableUrlEncoding) {
                    // concatenate raw query + raw query params
                    StringBuilder sb = new StringBuilder(query);
                    appendRawQueryParams(sb, queryParams);
                    sb.setLength(sb.length() - 1);
                    return sb.toString();
                } else {
                    // concatenate encoded query + encoded query params
                    StringBuilder sb = new StringBuilder(query.length() + 16);
                    appendEscapeQuery(sb, query);
                    appendEscapeQueryParams(sb, queryParams);
                    sb.setLength(sb.length() - 1);
                   return sb.toString();
                }
            } else {
                if (disableUrlEncoding) {
                    // return raw query as is
                    return query;
                } else {
                    // encode query
                    StringBuilder sb = new StringBuilder(query.length() + 16);
                    appendEscapeQuery(sb, query);
                    sb.setLength(sb.length() - 1);
                    return sb.toString();
                }
            }
        } else {
            if (hasQueryParams) {
                if (disableUrlEncoding) {
                    // concatenate raw queryParams
                    StringBuilder sb = new StringBuilder(queryParams.size() * 16);
                    appendRawQueryParams(sb, queryParams);
                    sb.setLength(sb.length() - 1);
                    return sb.toString();
                } else {
                    // concatenate encoded query params
                    StringBuilder sb = new StringBuilder(queryParams.size() * 16);
                    appendEscapeQueryParams(sb, queryParams);
                    sb.setLength(sb.length() - 1);
                    return sb.toString();
                }
            } else {
                // neither query nor query param
                return null;
            }
        }
    }
    
    private void computeFinalUri() {

        if (request.uri == null) {
            logger.debug("setUrl hasn't been invoked. Using http://localhost");
            request.uri = DEFAULT_REQUEST_URL;
        }

        AsyncHttpProviderUtils.validateSupportedScheme(request.uri);

        // FIXME is that right?
        String newPath = isNonEmpty(request.uri.getPath())? request.uri.getPath() : "/";
        String newQuery = computeFinalQueryString(request.uri.getQuery(), queryParams);

        request.uri = new UriComponents(//
                request.uri.getScheme(),//
                request.uri.getUserInfo(),//
                request.uri.getHost(),//
                request.uri.getPort(),//
                newPath,//
                newQuery);
    }
    
    public Request build() {
        computeFinalUri();
        executeSignatureCalculator();
        computeRequestCharset();
        computeRequestLength();
        return request;
    }
}
