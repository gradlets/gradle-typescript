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

import com.google.common.hash.Hashing;
import com.gradlets.gradle.typescript.shim.PackageJsonLoader;
import java.nio.charset.StandardCharsets;

public final class DescriptorLoader {

    private final DescriptorCache ivyDescriptorCache;
    private final PackageJsonLoader packageJsonLoader;

    public DescriptorLoader(DescriptorCache ivyDescriptorCache, PackageJsonLoader packageJsonLoader) {
        this.ivyDescriptorCache = ivyDescriptorCache;
        this.packageJsonLoader = packageJsonLoader;
    }

    @SuppressWarnings("deprecation")
    public CachedDescriptor getIvyDescriptor(String packageName, String packageVersion) {
        NpmArtifactKey cacheKey = NpmArtifactKey.builder()
                .packageName(packageName)
                .version(packageVersion)
                .build();
        return ivyDescriptorCache.getMetadata(cacheKey).orElseGet(() -> {
            String ivyDescriptor = IvyDescriptors.createDescriptor(
                    "npm", packageJsonLoader.getPackageJson(packageName, packageVersion));
            CachedDescriptor cachedDescriptor = CachedDescriptor.builder()
                    .ivySha1Checksum(() -> Hashing.sha1()
                            .hashBytes(ivyDescriptor.getBytes(StandardCharsets.UTF_8))
                            .toString())
                    .cacheKey(cacheKey)
                    .ivyDescriptor(() -> ivyDescriptor)
                    .build();
            ivyDescriptorCache.storeMetadata(cachedDescriptor);
            return cachedDescriptor;
        });
    }
}
