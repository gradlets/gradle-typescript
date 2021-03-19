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

package com.gradlets.gradle.typescript.shim.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.gradlets.gradle.typescript.shim.clients.PackageJson;
import com.gradlets.gradle.typescript.shim.clients.PackageJson.Dist;
import org.junit.jupiter.api.Test;

class IvyDescriptorsTest {

    @Test
    public void convertsPackageJson() {
        PackageJson packageJson = PackageJson.builder()
                .name("foo")
                .version("1.0.0")
                .putDependencies("bar", "^1.0.0")
                .dist(Dist.builder().shasum("").tarball("").build())
                .build();
        assertThat(IvyDescriptors.createDescriptor("npm", packageJson))
                .isEqualTo("<ivy-module version=\"2.0\" xmlns:e=\"http://ant.apache.org/ivy/extra\""
                        + " xmlns:m=\"http://ant.apache.org/ivy/maven\">\n"
                        + "<info organisation=\"npm\" module=\"foo\" revision=\"1.0.0\"/>\n"
                        + "<publications>\n"
                        + "<artifact name=\"foo\" ext=\"tgz\" conf=\"default\" type=\"tgz\"/>\n"
                        + "</publications>\n"
                        + "<dependencies>\n"
                        + "<dependency org=\"npm\" name=\"bar\" rev=\"1.0.0\" conf=\"default\"/>\n"
                        + "</dependencies>\n"
                        + "</ivy-module>");
    }

    @Test
    void sanitizesVersions() {
        assertThat(IvyDescriptors.sanitizeConstraint("1")).isEqualTo("1");
        assertThat(IvyDescriptors.sanitizeConstraint("1.0.0")).isEqualTo("1.0.0");
        assertThat(IvyDescriptors.sanitizeConstraint("^1.0.0")).isEqualTo("1.0.0");
        assertThat(IvyDescriptors.sanitizeConstraint("~1.0.0")).isEqualTo("1.0.0");

        assertThat(IvyDescriptors.sanitizeConstraint("1.0.x")).isEqualTo("1.0.0");
        assertThat(IvyDescriptors.sanitizeConstraint("~1.0.x")).isEqualTo("1.0.0");

        assertThat(IvyDescriptors.sanitizeConstraint(">= 0.3.2 < 0.4.0")).isEqualTo("0.3.2");
        assertThat(IvyDescriptors.sanitizeConstraint("= 0.1.1")).isEqualTo("0.1.1");
        assertThat(IvyDescriptors.sanitizeConstraint("^7.0.0-beta.35")).isEqualTo("7.0.0-beta.35");
    }
}
