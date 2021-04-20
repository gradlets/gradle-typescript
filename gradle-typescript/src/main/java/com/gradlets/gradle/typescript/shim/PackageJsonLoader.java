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

import com.gradlets.gradle.typescript.shim.clients.NpmRepository;
import com.gradlets.gradle.typescript.shim.clients.PackageJson;
import com.palantir.conjure.java.api.config.service.PartialServiceConfiguration;
import com.palantir.conjure.java.api.config.service.ServicesConfigBlock;
import com.palantir.conjure.java.api.config.service.UserAgent;
import com.palantir.conjure.java.api.config.service.UserAgent.Agent;
import com.palantir.conjure.java.api.config.ssl.SslConfiguration;
import com.palantir.conjure.java.api.config.ssl.SslConfiguration.StoreType;
import com.palantir.dialogue.clients.DialogueClients;
import com.palantir.logsafe.exceptions.SafeRuntimeException;
import com.palantir.refreshable.Refreshable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

// Need to load package json through the NPM API since artifactory does not have a consistent package structure
// between internal and external packages. Upside is that we actually work with the oss npm registry
public final class PackageJsonLoader {
    private static final UserAgent USER_AGENT = UserAgent.of(Agent.of(
            "npm-shim",
            Optional.ofNullable(PackageJsonLoader.class.getPackage().getImplementationVersion())
                    .orElse("0.0.0")));

    private final NpmRepository repository;

    public PackageJsonLoader(String baseUrl) {
        repository = DialogueClients.create(Refreshable.only(ServicesConfigBlock.builder()
                        .putServices(
                                "service",
                                PartialServiceConfiguration.builder()
                                        .addUris(baseUrl)
                                        .security(SslConfiguration.builder()
                                                .trustStorePath(trustStorePath())
                                                .trustStoreType(storeType())
                                                .build())
                                        .build())
                        .build()))
                .withUserAgent(USER_AGENT)
                .get(NpmRepository.class, "service");
    }

    public PackageJson getPackageJson(String packageName, String packageVersion) {
        return repository.getPackageJson(packageName, packageVersion);
    }

    private static StoreType storeType() {
        return Optional.ofNullable(System.getProperty("javax.net.ssl.trustStoreType"))
                .map(StoreType::valueOf)
                .orElse(StoreType.JKS);
    }

    private static Path trustStorePath() {
        return Optional.ofNullable(System.getProperty("javax.net.ssl.trustStore"))
                .map(Paths::get)
                .orElseGet(() -> Optional.ofNullable(System.getProperty("java.home"))
                        .map(javaHome -> Paths.get(javaHome, "lib", "security", "cacerts"))
                        .orElseThrow(() -> new SafeRuntimeException("Unable to find trustStore")));
    }
}
