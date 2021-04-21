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

package com.gradlets.gradle.npm;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

public class NpmConfigurationsTest {

    @Test
    public void extractBaseName_simple() {
        assertThat(NpmConfigurations.extractBaseName("foo-1.2.3")).isEqualTo("foo");
        assertThat(NpmConfigurations.extractBaseName("foo-1.2.3.dirty")).isEqualTo("foo");
        assertThat(NpmConfigurations.extractBaseName("foo-1.2.3-1.dirty")).isEqualTo("foo");
        assertThat(NpmConfigurations.extractBaseName("foo-1.2.3-1-g1234adf.dirty"))
                .isEqualTo("foo");
        assertThat(NpmConfigurations.extractBaseName("foo-1.2.3-g1234adf")).isEqualTo("foo");
    }

    @Test
    void file_to_package() {
        assertThat(NpmConfigurations.getPackageNameFromArtifact(Paths.get("/Users/forozco/.gradle/caches/transforms-3/"
                        + "f62fd608b95c387c8f085fc0a5f8ae62/transformed/react-dom-16.8.3/react-dom")))
                .isEqualTo("react-dom");
        assertThat(NpmConfigurations.getPackageNameFromArtifact(Paths.get("/Users/forozco/.gradle/caches/transforms-3/"
                        + "d6fb9b5f2f27e61664e9f75880b5d612/transformed/types/react-16.8.3/@types/react")))
                .isEqualTo("@types/react");
        assertThat(NpmConfigurations.getPackageNameFromArtifact(Paths.get("/Users/forozco/.gradle/caches/transforms-3/"
                        + "0440a9ba8e020d1463bd6cb975acc5eb/transformed/types/"
                        + "google__maps-0.5.14/@types/google__maps")))
                .isEqualTo("@types/google__maps");
    }

    @Test
    void extract_types_package() {
        assertThat(NpmConfigurations.getTypesPackageNameFromArtifact(
                        Paths.get("/Users/forozco/.gradle/caches/transforms-3/"
                                + "f62fd608b95c387c8f085fc0a5f8ae62/transformed/react-dom-16.8.3/react-dom")))
                .isEmpty();
        assertThat(NpmConfigurations.getTypesPackageNameFromArtifact(
                        Paths.get("/Users/forozco/.gradle/caches/transforms-3/"
                                + "d6fb9b5f2f27e61664e9f75880b5d612/transformed/types/react-16.8.3/@types/react")))
                .hasValue("react");
        assertThat(NpmConfigurations.getTypesPackageNameFromArtifact(
                        Paths.get("/Users/forozco/.gradle/caches/transforms-3/"
                                + "0440a9ba8e020d1463bd6cb975acc5eb/transformed/types"
                                + "/google__maps-0.5.14/@types/google__maps")))
                .hasValue("@google/maps");
    }
}
