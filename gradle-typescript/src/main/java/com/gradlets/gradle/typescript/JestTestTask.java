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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.gradlets.gradle.npm.NodeExec;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.util.GFileUtils;

public abstract class JestTestTask extends SourceTask {
    @Input
    abstract Property<String> getPreset();

    @Classpath
    abstract ConfigurableFileCollection getClasspath();

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract RegularFileProperty getTsconfigFile();

    @OutputDirectory
    abstract DirectoryProperty getOutputDir();

    @Input
    abstract MapProperty<String, Object> getCompilerOptions();

    @TaskAction
    public final void exec() {
        File configFile = new File(getTemporaryDir(), "jestConfig.json");
        GFileUtils.parentMkdirs(configFile);

        try {
            ObjectMappers.MAPPER.writeValue(configFile, createJestConfig());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write config", e);
        }

        NodeExec.exec(getProject(), getClasspath(), execSpec -> {
            execSpec.setExecutable(getJestExecutable(getClasspath()));
            execSpec.args("--config", configFile.getAbsolutePath());
        });
    }

    private static Path getJestExecutable(FileCollection classpath) {
        return classpath.getFiles().stream()
                .filter(file -> file.getName().equals("jest-cli"))
                .map(file -> file.toPath().resolve("bin/jest.js"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Jest dependency must exists"));
    }

    private Map<String, Object> createJestConfig() throws IOException {
        ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
        builder.put("rootDir", getProject().getProjectDir().getAbsolutePath());
        builder.put("preset", getPreset().get());
        builder.put(
                "globals",
                // Pure hack wrt location of tsconfig
                ImmutableMap.of(
                        "ts-jest",
                        ImmutableMap.of(
                                "tsconfig", getTsconfigFile().get().getAsFile().getAbsolutePath())));
        // Required to specify js, jsx since the whitelist applies to all modules (including dependencies)
        builder.put("moduleFileExtensions", ImmutableList.of("js", "jsx", "ts", "tsx"));
        builder.put("testEnvironment", "node");
        builder.put(
                "testMatch",
                getSource().getFiles().stream().map(File::getAbsolutePath).collect(Collectors.toList()));
        builder.putAll(getCompilerOptions().get());
        return builder.build();
    }
}
