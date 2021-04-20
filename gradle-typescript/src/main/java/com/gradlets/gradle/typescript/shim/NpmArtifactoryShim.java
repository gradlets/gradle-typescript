/*
 * (c) Copyright 2021 Felipe Orozco, Robert Kruszewski. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gradlets.gradle.typescript.shim;

import com.gradlets.gradle.typescript.shim.cache.CachedDescriptor;
import com.gradlets.gradle.typescript.shim.cache.DescriptorCache;
import com.gradlets.gradle.typescript.shim.cache.DescriptorLoader;
import com.palantir.conjure.java.api.errors.UnknownRemoteException;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.protocols.ssl.UndertowXnioSsl;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.proxy.ProxyClient;
import io.undertow.server.handlers.proxy.ProxyHandler;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import java.io.Closeable;
import java.net.BindException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.OptionMap;
import org.xnio.Xnio;

public final class NpmArtifactoryShim {
    private static final Logger log = LoggerFactory.getLogger(NpmArtifactoryShim.class);

    private static final AtomicInteger PORT_INCREMENT = new AtomicInteger(0);

    // Its actually kinda convenient for testing to be able to quickly spin up a server
    @SuppressWarnings("BanSystemOut")
    public static void main(String[] _args) {
        ShimServer shimServer = startServer("https://registry.npmjs.org");
        System.out.println("Listening on: " + shimServer.getUri());
    }

    public static ShimServer startServer(String uri) {
        DescriptorCache descriptorCache = new DescriptorCache(ShimConfiguration.getCacheDir());
        PackageJsonLoader packageJsonLoader = new PackageJsonLoader(uri);
        DescriptorLoader descriptorLoader = new DescriptorLoader(descriptorCache, packageJsonLoader);
        ProxyHandler proxyHandler = ProxyHandler.builder()
                .setProxyClient(getProxyClient(uri, descriptorLoader))
                .setMaxRequestTime(30000)
                .setRewriteHostHeader(true)
                .build();

        RoutingHandler routingHandler = Handlers.routing()
                // Pure hacks to convert package.json to ivy descriptors and properly handle npm scope.
                .add(
                        "HEAD",
                        IvyPatterns.IVY_DESCRIPTOR_TEMPLATE,
                        new ScopedNpmHttpHandler(descriptorLoader, NpmArtifactoryShim::handleHeadIvyDescriptor))
                .add(
                        "HEAD",
                        "/{packageName}/{packageVersion}/descriptor.ivy",
                        new NpmHttpHandler(descriptorLoader, NpmArtifactoryShim::handleHeadIvyDescriptor))
                .add(
                        "GET",
                        IvyPatterns.IVY_DESCRIPTOR_TEMPLATE,
                        new ScopedNpmHttpHandler(descriptorLoader, NpmArtifactoryShim::handleGetIvyDescriptor))
                .add(
                        "GET",
                        "/{packageName}/{packageVersion}/descriptor.ivy",
                        new NpmHttpHandler(descriptorLoader, NpmArtifactoryShim::handleGetIvyDescriptor))
                .setFallbackHandler(proxyHandler);

        do {
            try {
                int shimProxyPort = ShimConfiguration.getProxyPort() + PORT_INCREMENT.getAndIncrement();
                return tryStartServer(shimProxyPort, routingHandler);
            } catch (RuntimeException e) {
                if (e.getCause() == null || !(e.getCause() instanceof BindException)) {
                    throw e;
                }
            }
        } while (true);
    }

    private static ShimServer tryStartServer(int port, RoutingHandler routingHandler) {
        Undertow server = Undertow.builder()
                .addHttpListener(port, "localhost")
                .setHandler(routingHandler)
                .setIoThreads(4)
                .build();

        server.start();

        return new ShimServer() {
            @Override
            public URI getUri() {
                return URI.create("http://localhost:" + port);
            }

            @Override
            public void close() {
                server.stop();
            }
        };
    }

    private static void handleHeadIvyDescriptor(
            DescriptorLoader descriptorLoader, HttpServerExchange exchange, String packageName, String packageVersion) {
        try {
            CachedDescriptor cachedDescriptor = descriptorLoader.getIvyDescriptor(packageName, packageVersion);
            String sha1Etag = cachedDescriptor.ivySha1Checksum().get();
            exchange.getResponseHeaders().put(HttpString.tryFromString("X-Checksum-Sha1"), sha1Etag);
            exchange.getResponseHeaders().put(HttpString.tryFromString("ETag"), String.format("{SHA1{%s}}", sha1Etag));
            exchange.setResponseContentLength(
                    cachedDescriptor.ivyDescriptor().get().length());
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/xml");
            exchange.setStatusCode(200);
        } catch (RuntimeException t) {
            exchange.setStatusCode(404);
        }
    }

    private static void handleGetIvyDescriptor(
            DescriptorLoader descriptorLoader, HttpServerExchange exchange, String packageName, String packageVersion) {
        try {
            CachedDescriptor cachedDescriptor = descriptorLoader.getIvyDescriptor(packageName, packageVersion);
            String sha1Etag = cachedDescriptor.ivySha1Checksum().get();
            exchange.getResponseHeaders().put(HttpString.tryFromString("X-Checksum-Sha1"), sha1Etag);
            exchange.getResponseHeaders().put(HttpString.tryFromString("ETag"), String.format("{SHA1{%s}}", sha1Etag));
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/xml");
            String content = cachedDescriptor.ivyDescriptor().get();
            exchange.setResponseContentLength(content.length());
            exchange.getResponseSender().send(content);
        } catch (RuntimeException t) {
            if (t instanceof UnknownRemoteException && ((UnknownRemoteException) t).getStatus() == 404) {
                exchange.setStatusCode(404);
            } else {
                log.error("Failed to download npm package.json", t);
                exchange.setStatusCode(500);
            }
        }
    }

    private static ProxyClient getProxyClient(String uri, DescriptorLoader packageJsonCache) {
        Xnio instance = Xnio.getInstance();
        try {
            return new NpmProxyClient(
                    packageJsonCache,
                    new LoadBalancingProxyClient()
                            .addHost(URI.create(uri), new UndertowXnioSsl(instance, OptionMap.EMPTY))
                            .setConnectionsPerThread(20));
        } catch (NoSuchAlgorithmException | NoSuchProviderException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    public interface ShimServer extends Closeable {
        URI getUri();

        @Override
        void close();
    }

    private NpmArtifactoryShim() {}
}
