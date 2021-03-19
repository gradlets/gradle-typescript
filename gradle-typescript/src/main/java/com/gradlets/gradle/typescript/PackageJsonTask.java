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
import com.google.common.collect.Maps;
import java.io.File;
import java.io.Serializable;
import java.util.Map;
import java.util.stream.Collectors;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.immutables.value.Value;

public abstract class PackageJsonTask extends DefaultTask {
    // For compatibility with other dependency managers that assume devs know the future
    private static final String VERSION_PREFIX = ">=";
    private final RegularFileProperty outputFile =
            getProject().getObjects().fileProperty().fileValue(new File(getTemporaryDir(), "package.json"));

    @Input
    public abstract Property<String> getPackageName();

    @Input
    public abstract MapProperty<String, Object> getPackageJsonFields();

    @Classpath
    public abstract Property<Configuration> getDependencies();

    @OutputFile
    public final Provider<RegularFile> getOutputFile() {
        return outputFile;
    }

    @TaskAction
    public final void writePackageJson() throws Exception {
        Map<String, Object> packageJson = ImmutableMap.<String, Object>builder()
                .putAll(getPackageJsonFields().get())
                .put("name", getPackageName().get())
                .put("version", getProject().getVersion())
                .put("main", "lib/index.js")
                .put("types", "lib/index.d.ts")
                .put(
                        "dependencies",
                        Maps.transformValues(
                                getDirectDependencies(getDependencies().get()), version -> VERSION_PREFIX + version))
                .put("_id", getPackageName().get() + "@" + getProject().getVersion())
                .build();
        ObjectMappers.MAPPER.writeValue(getOutputFile().get().getAsFile(), packageJson);
    }

    private static Map<String, String> getDirectDependencies(Configuration conf) {
        Map<String, String> depToVersion = conf.getResolvedConfiguration().getFirstLevelModuleDependencies().stream()
                .filter(dep -> !dep.getModuleArtifacts().isEmpty())
                .collect(Collectors.toMap(ResolvedDependency::getModuleName, ResolvedDependency::getModuleVersion));
        return conf.getDependencies().stream()
                .collect(Collectors.toMap(
                        dependency -> dependency instanceof ProjectDependency
                                ? ProjectNames.getScopedProjectName(
                                        ((ProjectDependency) dependency).getDependencyProject())
                                : ProjectNames.getScopedPackageName(dependency.getName()),
                        dependency -> depToVersion.get(dependency.getName())));
    }

    // Task inputs must be serializable so we can't reuse Dependency from gradle
    @Value.Immutable
    interface SerializableDependency extends Serializable {
        String name();

        String version();

        static SerializableDependency of(String name, String version) {
            return ImmutableSerializableDependency.builder()
                    .name(ProjectNames.getScopedPackageName(name))
                    .version(version)
                    .build();
        }
    }
}
