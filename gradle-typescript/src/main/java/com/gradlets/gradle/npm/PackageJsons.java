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

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import com.gradlets.gradle.typescript.ObjectMappers;
import com.palantir.conjure.java.lib.Bytes;
import com.palantir.logsafe.exceptions.SafeRuntimeException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

final class PackageJsons {
    private PackageJsons() {}

    static Map<String, Object> getPackageJson(Path npmDist) {
        try (TarArchiveInputStream tarIn = new TarArchiveInputStream(
                new GzipCompressorInputStream(Files.newInputStream(npmDist, StandardOpenOption.READ)))) {
            TarArchiveEntry entry;
            while ((entry = tarIn.getNextTarEntry()) != null) {
                if (entry.getName().endsWith("package.json")) {
                    return ObjectMappers.MAPPER.readValue(tarIn, new TypeReference<>() {});
                }
            }
        } catch (IOException e) {
            throw new SafeRuntimeException("Failed to read package.json from npm dist", e);
        }

        throw new SafeRuntimeException("No package.json found in npm dist");
    }

    static NpmPackageRoot packageJsonForPublication(
            Map<String, Object> packageJson, String repositoryUrl, byte[] artifactBytes) {
        Map<String, Object> mutablePackageJson = new HashMap<>(packageJson);
        String packageName = (String) mutablePackageJson.get("name");
        String packageVersion = (String) mutablePackageJson.get("version");
        String tarballName = String.format("%s-%s.tgz", packageName, packageVersion);
        String tarballSuffix = String.format("%s/-/%s", packageName, tarballName);
        String shasum = Hashing.sha1().hashBytes(artifactBytes).toString();
        String integrity = "sha512-"
                + BaseEncoding.base64()
                        .encode(Hashing.sha512().hashBytes(artifactBytes).asBytes());
        mutablePackageJson.put(
                "dist", Map.of("shasum", shasum, "integrity", integrity, "tarball", repositoryUrl + tarballSuffix));
        return NpmPackageRoot.builder()
                .name(packageName)
                .version(packageVersion)
                .putDistTags("latest", packageVersion)
                .putVersions(packageVersion, mutablePackageJson)
                .readme(Optional.ofNullable(mutablePackageJson.get("readme")).map(String.class::cast))
                .putAttachments(
                        tarballName,
                        NpmPackageRootAttachment.builder()
                                .data(Bytes.from(artifactBytes))
                                .build())
                .build();
    }
}
