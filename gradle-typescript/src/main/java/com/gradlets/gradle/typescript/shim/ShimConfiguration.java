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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

final class ShimConfiguration {
    static final String GRADLE_TYPESCRIPT_DIR_ENV_NAME = "GRADLE_TYPESCRIPT_CACHE_DIR";
    static final String GRADLE_TYPESCRIPT_DIR_PROP_NAME = "gradle.typeScript.cache.dir";
    static final String PROXY_PORT_ENV_NAME = "GRADLE_TYPESCRIPT_PROXY_PORT";
    static final String PROXY_PORT_PROP_NAME = "gradle.typeScript.proxy.port";

    private ShimConfiguration() {}

    public static Path getCacheDir() {
        return Paths.get(Optional.ofNullable(System.getenv(GRADLE_TYPESCRIPT_DIR_ENV_NAME))
                        .or(() -> Optional.ofNullable(System.getProperty(GRADLE_TYPESCRIPT_DIR_PROP_NAME)))
                        .map(t -> t.endsWith("/") ? t.substring(0, t.length() - 1) : t)
                        .orElseGet(() -> System.getProperty("user.home") + "/.gradle-typeScript"))
                .resolve("cache")
                .resolve("descriptors");
    }

    public static int getProxyPort() {
        return Optional.ofNullable(System.getenv(PROXY_PORT_ENV_NAME))
                .or(() -> Optional.ofNullable(System.getProperty(PROXY_PORT_PROP_NAME)))
                .map(Integer::valueOf)
                .orElse(7348);
    }
}
