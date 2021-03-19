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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.gradle.api.artifacts.transform.CacheableTransform;
import org.gradle.api.artifacts.transform.InputArtifact;
import org.gradle.api.artifacts.transform.TransformAction;
import org.gradle.api.artifacts.transform.TransformOutputs;
import org.gradle.api.artifacts.transform.TransformParameters;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CacheableTransform
public abstract class NpmArtifactTransformAction implements TransformAction<TransformParameters.None> {
    private static final Logger log = LoggerFactory.getLogger(NpmArtifactTransformAction.class);
    private static final Splitter SPLITTER = Splitter.on("/");

    @InputArtifact
    @PathSensitive(PathSensitivity.NAME_ONLY)
    public abstract Provider<FileSystemLocation> getInputArtifact();

    @Override
    public final void transform(TransformOutputs outputs) {
        Path inputFile = getInputArtifact().get().getAsFile().toPath();
        Path outputDir = outputs.dir(outputDir(inputFile)).toPath();
        extract(inputFile, outputDir);
    }

    public static void extract(Path sourceFile, Path destDirectory) {
        if (!Files.exists(sourceFile)) {
            log.error("Unable to unpack non-existent file: {}", sourceFile.toAbsolutePath());
            return;
        }

        try (TarArchiveInputStream tgzInputStream =
                new TarArchiveInputStream(new GZIPInputStream(Files.newInputStream(sourceFile)))) {
            Files.createDirectories(destDirectory);

            TarArchiveEntry entry;
            while ((entry = (TarArchiveEntry) tgzInputStream.getNextEntry()) != null) {
                Path destPath = destDirectory.resolve(trimPackage(entry.getName()));
                Files.createDirectories(destPath.getParent());

                if (entry.isDirectory()) {
                    Files.createDirectories(destPath);
                } else if (entry.isSymbolicLink()) {
                    Files.createSymbolicLink(destPath, Paths.get(entry.getLinkName()));
                } else if (entry.isLink()) {
                    Path target = destDirectory.resolve(entry.getLinkName());
                    Files.createLink(destPath, target);
                } else {
                    Files.copy(tgzInputStream, destPath, StandardCopyOption.REPLACE_EXISTING);
                    // get permissions
                    ImmutableSet.Builder<PosixFilePermission> permissionsBuilder = ImmutableSet.builder();
                    PosixFilePermission[] orderedPermissions = PosixFilePermission.values();
                    for (int mask = 1, perm = orderedPermissions.length - 1; mask < 01000; mask <<= 1, perm -= 1) {
                        if ((entry.getMode() & mask) != 0) {
                            permissionsBuilder.add(orderedPermissions[perm]);
                        }
                    }
                    Files.setPosixFilePermissions(destPath, permissionsBuilder.build());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String outputDir(Path inputFile) {
        // Project dependency
        if (!inputFile.toAbsolutePath().toString().contains("/files-2.1/")) {
            String packageName = inputFile.getParent().getFileName().toString();
            String maybeScope = inputFile.getParent().getParent().getFileName().toString();
            // No scope in project dependency
            return com.google.common.io.Files.getNameWithoutExtension(
                            inputFile.getFileName().toString()) + "/"
                    + (maybeScope.equals("distributions") ? packageName : maybeScope + "/" + packageName);
        }

        List<String> pathSegments =
                SPLITTER.splitToList(inputFile.toAbsolutePath().toString());
        // Skip files-2.1 segment and "npm" ground segment;
        int prefix = pathSegments.indexOf("files-2.1") + 2;

        int index = 0;
        // There are either 2 or 3 segments depending on whether the package has a scope or not. We look for the
        // checksum segment to identify which case we are in.
        Optional<String> scope = !ArtifactLayout.isChecksumDirOrTransformed(pathSegments.get(prefix + 2))
                ? Optional.of(pathSegments.get(prefix + index++))
                : Optional.empty();
        String name = pathSegments.get(prefix + index++);
        String version = pathSegments.get(prefix + index);

        return scope.map(value -> value + "/").orElse("")
                + name + "-" + version
                + scope.map(value -> "/@" + value).orElse("") + "/" + name;
    }

    private static String trimPackage(String entryName) {
        return entryName.replaceFirst("package/", "");
    }

    @Value.Immutable
    @JsonDeserialize(as = ImmutablePackageJson.class)
    interface PackageJson {
        Map<String, String> bin();
    }
}
