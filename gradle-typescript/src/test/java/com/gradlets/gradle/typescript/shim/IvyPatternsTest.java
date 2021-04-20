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

import java.util.Optional;
import org.junit.jupiter.api.Test;

class IvyPatternsTest {

    @Test
    void converts_typescript() {
        Optional<ModuleIdentifier> maybeId = IvyPatterns.parseArtifactPath(getPath("typescript", "3.7.5"));
        assertThat(maybeId).hasValue(ModuleIdentifier.of("typescript", "3.7.5"));
    }

    @Test
    void converts_plain_package() {
        Optional<ModuleIdentifier> maybeId = IvyPatterns.parseArtifactPath(getPath("conjure-client", "1.0.0"));
        assertThat(maybeId).hasValue(ModuleIdentifier.of("conjure-client", "1.0.0"));
    }

    @Test
    void converts_scoped_requests() {
        Optional<ModuleIdentifier> maybeId = IvyPatterns.parseArtifactPath(getPath("foundry/conjure-fe-lib", "1.0.0"));
        assertThat(maybeId).hasValueSatisfying(id -> {
            assertThat(id.packageName()).isEqualTo("@foundry/conjure-fe-lib");
            assertThat(id.packageVersion()).isEqualTo("1.0.0");
        });
    }

    @Test
    void converts_weird_versions() {
        Optional<ModuleIdentifier> maybeId =
                IvyPatterns.parseArtifactPath(getPath("foundry/conjure-fe-lib", "1.0.0-rc1-foo-bar.npm.sucks"));
        assertThat(maybeId).hasValueSatisfying(id -> {
            assertThat(id.packageName()).isEqualTo("@foundry/conjure-fe-lib");
            assertThat(id.packageVersion()).isEqualTo("1.0.0-rc1-foo-bar.npm.sucks");
        });
    }

    private static String getPath(String packageName, String packageVersion) {
        return String.format("/%s/-/%s-%s.tgz", packageName, packageName, packageVersion);
    }
}
