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

package com.gradlets.gradle.typescript;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.gradlets.gradle.ImmutablesStyle;
import java.util.Map;
import java.util.Set;
import org.immutables.value.Value;

@Value.Immutable
@ImmutablesStyle
@JsonSerialize(as = ImmutableTsConfig.class)
@JsonDeserialize(as = ImmutableTsConfig.class)
public interface TsConfig {
    Map<String, Object> compilerOptions();

    Set<String> files();

    Set<String> include();

    class Builder extends ImmutableTsConfig.Builder {}

    static Builder builder() {
        return new Builder();
    }

    static TsConfig of(Map<String, Object> compilerOptions, Set<String> files) {
        return builder().compilerOptions(compilerOptions).files(files).build();
    }
}
