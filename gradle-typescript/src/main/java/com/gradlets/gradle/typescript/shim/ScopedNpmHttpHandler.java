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

import com.gradlets.gradle.typescript.shim.cache.DescriptorLoader;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.PathTemplateMatch;
import java.util.Map;

public final class ScopedNpmHttpHandler implements HttpHandler {
    private final ScopedRequestHandler delegate;
    private final DescriptorLoader descriptorLoader;

    public ScopedNpmHttpHandler(DescriptorLoader descriptorLoader, ScopedRequestHandler delegate) {
        this.descriptorLoader = descriptorLoader;
        this.delegate = delegate;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        Map<String, String> parameters =
                exchange.getAttachment(PathTemplateMatch.ATTACHMENT_KEY).getParameters();
        String scopedName = "@" + parameters.get("packageScope") + "/" + parameters.get("packageName");
        delegate.handleRequest(descriptorLoader, exchange, scopedName, parameters.get("packageVersion"));
    }

    interface ScopedRequestHandler {
        void handleRequest(
                DescriptorLoader descriptorLoader,
                HttpServerExchange exchange,
                String packageName,
                String packageVersion);
    }
}
