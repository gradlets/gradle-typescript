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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.gradlets.gradle.typescript.shim.clients.PackageJson;
import com.palantir.logsafe.SafeArg;
import com.palantir.logsafe.exceptions.SafeRuntimeException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Optional;
import java.util.Set;

public final class DescriptorCache {
    private static final ObjectMapper MAPPER =
            new ObjectMapper().registerModule(new GuavaModule()).registerModule(new Jdk8Module());

    private static final String IVY_DESCRIPTOR_NAME = "descriptor.ivy";
    private static final String IVY_SHA_NAME = "descriptor.ivy.sha1";
    private static final String PACKAGE_JSON_NAME = "package.json";
    private final Path cacheLocation;

    public DescriptorCache(Path cacheLocation) {
        this.cacheLocation = cacheLocation;
    }

    public void storeMetadata(CachedDescriptor descriptor) {
        Path cacheDir = resolveCacheDir(descriptor.getCacheKey());
        Path ivyFilePath = cacheDir.resolve(IVY_DESCRIPTOR_NAME);
        if (!Files.exists(ivyFilePath)) {
            writeToFile(ivyFilePath, descriptor.ivyDescriptor().get().getBytes(StandardCharsets.UTF_8));
            writeToFile(
                    cacheDir.resolve(IVY_SHA_NAME),
                    descriptor.ivySha1Checksum().get().getBytes(StandardCharsets.UTF_8));
            try {
                writeToFile(
                        cacheDir.resolve(PACKAGE_JSON_NAME),
                        MAPPER.writeValueAsBytes(descriptor.packageJson().get()));
            } catch (JsonProcessingException e) {
                throw new SafeRuntimeException("Unable to serialize package.json", e);
            }
        }
    }

    public Optional<CachedDescriptor> getMetadata(NpmArtifactKey metadataKey) {
        Path cacheDir = resolveCacheDir(metadataKey);
        Path ivyFilePath = cacheDir.resolve(IVY_DESCRIPTOR_NAME);
        Path ivySha1ChecksumPath = cacheDir.resolve(IVY_SHA_NAME);
        Path packageJsonPath = cacheDir.resolve(PACKAGE_JSON_NAME);
        if (!Files.exists(ivyFilePath) || !Files.exists(ivySha1ChecksumPath) || !Files.exists(packageJsonPath)) {
            return Optional.empty();
        }

        return Optional.of(CachedDescriptor.builder()
                .cacheKey(metadataKey)
                .ivyDescriptor(() -> readStringContents(ivyFilePath))
                .ivySha1Checksum(() -> readStringContents(ivySha1ChecksumPath))
                .packageJson(() -> {
                    try {
                        return MAPPER.readValue(packageJsonPath.toFile(), PackageJson.class);
                    } catch (IOException e) {
                        throw new SafeRuntimeException(
                                "Failed to read package.json from {}",
                                e,
                                SafeArg.of("package.json", packageJsonPath.toString()));
                    }
                })
                .build());
    }

    private static String readStringContents(Path filePath) {
        try {
            return Files.readString(filePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new SafeRuntimeException(
                    "Failed to read file from {}", e, SafeArg.of("filePath", filePath.toString()));
        }
    }

    private static void writeToFile(Path outputPath, byte[] contents) {
        try {
            Files.write(outputPath, contents);
        } catch (IOException e) {
            throw new SafeRuntimeException(
                    "Unable to store under outputPath {}", e, SafeArg.of("outputPath", outputPath.toString()));
        }
    }

    private Path resolveCacheDir(NpmArtifactKey cacheKey) {
        Path storeDir = cacheLocation.resolve(cacheKey.packageName()).resolve(cacheKey.version());
        boolean exists = Files.exists(storeDir);
        if (!exists) {
            try {
                Files.createDirectories(
                        storeDir,
                        PosixFilePermissions.asFileAttribute(Set.of(
                                PosixFilePermission.OWNER_EXECUTE,
                                PosixFilePermission.OWNER_READ,
                                PosixFilePermission.OWNER_WRITE,
                                PosixFilePermission.GROUP_EXECUTE,
                                PosixFilePermission.GROUP_READ,
                                PosixFilePermission.OTHERS_EXECUTE,
                                PosixFilePermission.OTHERS_READ)));
            } catch (IOException e) {
                throw new SafeRuntimeException(
                        "Unable to create directory to store metadata, packageName {}, version {}",
                        e,
                        SafeArg.of("packageName", cacheKey.packageName()),
                        SafeArg.of("version", cacheKey.version()));
            }
        }
        return storeDir;
    }
}
