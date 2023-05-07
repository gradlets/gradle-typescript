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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.gradlets.gradle.npm.NpmConfigurations;
import com.palantir.logsafe.Preconditions;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class TypeScriptConfigs {

    private TypeScriptConfigs() {}

    // Need to create tsconfig.json since not all options can be specified through the CLI
    // https://github.com/microsoft/TypeScript/blob/eac073894b172ec719ca7f28b0b94fc6e6e7d4cf/src/compiler/commandLineParser.ts#L631
    public static TsConfig createGradleTsConfig(
            Path outputDir,
            Set<File> sourceFiles,
            Set<File> dependencies,
            Set<File> typeRoots,
            Map<String, Object> compilerOptions) {
        return TsConfig.builder()
                .files(sourceFiles.stream().map(File::getAbsolutePath).collect(ImmutableSet.toImmutableSet()))
                .putAllCompilerOptions(
                        buildCompilerOptions(outputDir, Map.of(), dependencies, typeRoots, compilerOptions))
                .build();
    }

    public static TsConfig createTsConfigForIntellij(
            Set<Path> sourceDirectories,
            Path outputDir,
            Map<String, List<Path>> projectDependencies,
            Set<File> dependencies,
            Set<File> typeRoots,
            Map<String, Object> compilerOptions) {
        return TsConfig.builder()
                .addAllInclude(
                        sourceDirectories.stream().map(dir -> dir + "/**/*").collect(Collectors.toUnmodifiableSet()))
                .putAllCompilerOptions(
                        buildCompilerOptions(outputDir, projectDependencies, dependencies, typeRoots, compilerOptions))
                .build();
    }

    private static Map<String, Object> buildCompilerOptions(
            Path outputDir,
            Map<String, List<Path>> projectDependencies,
            Set<File> dependencies,
            Set<File> typeRoots,
            Map<String, Object> compilerOptions) {
        checkValidOptions(compilerOptions);

        return ImmutableMap.<String, Object>builder()
                .putAll(compilerOptions)
                .put("typeRoots", typeRoots.stream().map(File::getAbsolutePath).collect(ImmutableSet.toImmutableSet()))
                .put("outDir", outputDir.toAbsolutePath().toString())
                .put("declaration", true)
                .put("baseUrl", ".")
                .put(
                        "paths",
                        ImmutableMap.builder()
                                .putAll(getClassPathAsPaths(ImmutableSet.<File>builder()
                                        .addAll(dependencies)
                                        .addAll(typeRoots)
                                        .build()))
                                .putAll(expandPathsWithWildcards(projectDependencies))
                                .buildOrThrow())
                .buildOrThrow();
    }

    private static Map<String, List<String>> getClassPathAsPaths(Set<File> dependencies) {
        return expandPathsWithWildcards(dependencies.stream()
                .map(File::toPath)
                .map(artifact -> NpmConfigurations.getTypesPackageNameFromArtifact(artifact)
                        .map(targetPackage -> Map.entry(
                                targetPackage,
                                artifact.resolve(artifact.getFileName()).toAbsolutePath()))
                        .orElseGet(() -> Map.entry(
                                NpmConfigurations.getPackageNameFromArtifact(artifact), artifact.toAbsolutePath())))
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toUnmodifiableList()))));
    }

    private static Map<String, List<String>> expandPathsWithWildcards(Map<String, List<Path>> dependencies) {
        return ImmutableMap.<String, List<String>>builder()
                .putAll(dependencies.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().stream()
                                .map(Path::toString)
                                .collect(Collectors.toUnmodifiableList()))))
                .putAll(dependencies.entrySet().stream()
                        .collect(Collectors.toMap(entry -> entry.getKey() + "/*", entry -> entry.getValue().stream()
                                .map(Path::toString)
                                .flatMap(t -> Stream.of(t + "/*", t))
                                .collect(Collectors.toUnmodifiableList()))))
                .buildOrThrow();
    }

    private static void checkValidOptions(Map<String, Object> rawOptions) {
        Preconditions.checkArgument(!rawOptions.containsKey("outDir"), "Users must not specify outDir");
        Preconditions.checkArgument(!rawOptions.containsKey("rootDir"), "Users must not specify rootDir");
        Preconditions.checkArgument(!rawOptions.containsKey("declaration"), "Users must not specify declaration");
        Preconditions.checkArgument(!rawOptions.containsKey("typeRoots"), "Users must not specify typeRoots");
        Preconditions.checkArgument(!rawOptions.containsKey("paths"), "Users must not specify paths");
    }
}
