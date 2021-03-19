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

package com.gradlets.gradle.typescript.shim.clients;

import com.palantir.dialogue.Endpoint;
import com.palantir.dialogue.HttpMethod;
import com.palantir.dialogue.PathTemplate;
import com.palantir.dialogue.UrlBuilder;
import java.util.Map;
import java.util.Optional;

enum NpmRepositoryEndpoints implements Endpoint {
    getPackageJson {
        private final PathTemplate pathTemplate = PathTemplate.builder()
                .variable("packageName")
                .variable("packageVersion")
                .build();

        @Override
        public void renderPath(Map<String, String> params, UrlBuilder url) {
            pathTemplate.fill(params, url);
        }

        @Override
        public HttpMethod httpMethod() {
            return HttpMethod.GET;
        }

        @Override
        public String serviceName() {
            return "NpmRepository";
        }

        @Override
        public String endpointName() {
            return "getPackageJson";
        }

        @Override
        public String version() {
            return VERSION;
        }
    };

    private static final String VERSION = Optional.ofNullable(
                    NpmRepositoryEndpoints.class.getPackage().getImplementationVersion())
            .orElse("0.0.0");
}
