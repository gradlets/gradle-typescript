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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Paths;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(EnvironmentVariablesExtension.class)
public final class ShimConfigurationTest {

    @AfterEach
    public void afterEach() {
        System.clearProperty(ShimConfiguration.GRADLE_TYPESCRIPT_DIR_PROP_NAME);
        System.clearProperty(ShimConfiguration.PROXY_PORT_PROP_NAME);
    }

    @Test
    public void testCacheDirEnv(EnvironmentVariables env) {
        String cacheDir = "/a/b/c/d/";
        env.set(ShimConfiguration.GRADLE_TYPESCRIPT_DIR_ENV_NAME, cacheDir);
        System.setProperty(ShimConfiguration.GRADLE_TYPESCRIPT_DIR_PROP_NAME, "/e/f/g/h/");
        assertThat(ShimConfiguration.getCacheDir())
                .isEqualTo(Paths.get(cacheDir).resolve("cache").resolve("descriptors"));
    }

    @Test
    public void testCacheDirProp() {
        String cacheDir = "/a/b/c/d/";
        System.setProperty(ShimConfiguration.GRADLE_TYPESCRIPT_DIR_PROP_NAME, cacheDir);
        assertThat(ShimConfiguration.getCacheDir())
                .isEqualTo(Paths.get(cacheDir).resolve("cache").resolve("descriptors"));
    }

    @Test
    public void testCacheDirDefault() {
        assertThat(ShimConfiguration.getCacheDir())
                .isEqualTo(Paths.get(System.getProperty("user.home"))
                        .resolve(".gradle-typeScript")
                        .resolve("cache")
                        .resolve("descriptors"));
    }

    @Test
    public void testProxyPortEnv(EnvironmentVariables env) {
        int proxyPort = 6969;
        env.set(ShimConfiguration.PROXY_PORT_ENV_NAME, Integer.toString(proxyPort));
        System.setProperty(ShimConfiguration.PROXY_PORT_PROP_NAME, "6999");
        assertThat(ShimConfiguration.getProxyPort()).isEqualTo(proxyPort);
    }

    @Test
    public void testProxyPortProp() {
        int proxyPort = 6969;
        System.setProperty(ShimConfiguration.PROXY_PORT_PROP_NAME, Integer.toString(proxyPort));
        assertThat(ShimConfiguration.getProxyPort()).isEqualTo(proxyPort);
    }

    @Test
    public void testProxyPortDefault() {
        assertThat(ShimConfiguration.getProxyPort()).isEqualTo(7348);
    }
}
