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

import com.gradlets.gradle.ImmutablesStyle;
import com.gradlets.gradle.typescript.shim.clients.PackageJson;
import java.util.function.Supplier;
import org.immutables.value.Value;

@Value.Immutable
@ImmutablesStyle
public interface CachedDescriptor {
    NpmArtifactKey getCacheKey();

    Supplier<String> ivySha1Checksum();

    Supplier<String> ivyDescriptor();

    Supplier<PackageJson> packageJson();

    static Builder builder() {
        return new Builder();
    }

    class Builder extends ImmutableCachedDescriptor.Builder {}
}
