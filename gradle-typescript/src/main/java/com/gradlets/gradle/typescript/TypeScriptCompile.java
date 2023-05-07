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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.internal.artifacts.ArtifactAttributes;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecResult;

public class TypeScriptCompile extends SourceTask {

    private final Property<String> typeScriptVersion = getProject().getObjects().property(String.class);
    private final ConfigurableFileCollection typeRoots =
            getProject().getObjects().fileCollection();
    private final ConfigurableFileCollection classpath =
            getProject().getObjects().fileCollection();
    private final DirectoryProperty outputDir = getProject().getObjects().directoryProperty();
    private final MapProperty<String, Object> compilerOptions =
            getProject().getObjects().mapProperty(String.class, Object.class);

    @Input
    final Property<String> getTypeScriptVersion() {
        return typeScriptVersion;
    }

    @InputFiles
    @PathSensitive(PathSensitivity.ABSOLUTE)
    final ConfigurableFileCollection getTypeRoots() {
        return typeRoots;
    }

    @Classpath
    final ConfigurableFileCollection getClasspath() {
        return classpath;
    }

    @OutputDirectory
    final DirectoryProperty getOutputDir() {
        return outputDir;
    }

    @Input
    final MapProperty<String, Object> getCompilerOptions() {
        return compilerOptions;
    }

    @TaskAction
    public final void compile() throws IOException {
        Dependency tscDependency = getProject().getDependencies().create("npm:typescript:" + typeScriptVersion.get());
        Configuration tscConfiguration = getProject().getConfigurations().detachedConfiguration(tscDependency);
        tscConfiguration.getAttributes().attribute(ArtifactAttributes.ARTIFACT_FORMAT, "module");

        Path configFile = getTemporaryDir().toPath().resolve("tsconfig.json");
        Files.createDirectories(configFile.getParent());
        ObjectMappers.MAPPER.writeValue(
                configFile.toFile(),
                TypeScriptConfigs.createGradleTsConfig(
                        outputDir.get().getAsFile().toPath(),
                        getSource().getFiles(),
                        classpath.getFiles(),
                        typeRoots.getFiles(),
                        compilerOptions.get()));

        ExecResult execResult = getProject().exec(execSpec -> {
            execSpec.setExecutable(getTscExecutable(tscConfiguration));
            execSpec.args("--project", configFile);
            // TODO(forozco): better support compilation errors
            execSpec.setStandardOutput(System.err);
            execSpec.setIgnoreExitValue(true);
        });

        if (execResult.getExitValue() != 0) {
            logAllDependantPackages(classpath.getFiles());
            throw new GradleException(String.format("Failed to compile with exit code %s", execResult.getExitValue()));
        }
    }

    private static String getTscExecutable(Configuration tscConfiguration) {
        return new File(tscConfiguration.getSingleFile(), "bin/tsc").getAbsolutePath();
    }

    @SuppressWarnings("StreamResourceLeak")
    private void logAllDependantPackages(Set<File> dependencies) throws IOException {
        for (File dependency : dependencies) {
            if (dependency.exists()) {
                getProject()
                        .getLogger()
                        .info(
                                "Dependency exists with files {}",
                                Files.list(dependency.toPath()).collect(Collectors.toList()));
            } else {
                getProject().getLogger().info("Missing dependency {}", dependency.getAbsolutePath());
            }
        }
    }
}
