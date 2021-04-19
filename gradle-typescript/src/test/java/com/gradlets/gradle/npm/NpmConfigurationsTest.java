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
}
