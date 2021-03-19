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

package com.gradlets.gradle.typescript.pdeps;

import com.google.common.base.Splitter;
import com.gradlets.gradle.typescript.ObjectMappers;
import com.palantir.gradle.dist.ProductDependency;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

public abstract class DiscoverProductDependenciesTask extends DefaultTask {
    private static final Splitter SPLITTER = Splitter.on(":");

    private final RegularFileProperty outputFile = getProject()
            .getObjects()
            .fileProperty()
            .fileValue(new File(getTemporaryDir(), "discovered-product-dependencies.json"));

    @InputFiles
    @PathSensitive(value = PathSensitivity.NONE)
    public abstract ConfigurableFileCollection getPackageJsonFiles();

    @OutputFile
    public final RegularFileProperty getOutputFile() {
        return outputFile;
    }

    @TaskAction
    public final void discover() throws IOException {
        Set<ProductDependency> discoveredDependencies = getPackageJsonFiles().getFiles().stream()
                .flatMap(DiscoverProductDependenciesTask::parsePackageJson)
                .flatMap(slsBlock -> slsBlock.dependencies().entrySet().stream())
                .map(entry -> {
                    List<String> values = SPLITTER.splitToList(entry.getKey());
                    PackageJsonProductDependency pdep = entry.getValue();
                    return new ProductDependency(
                            values.get(0), values.get(1), pdep.minVersion(), pdep.maxVersion(), pdep.minVersion());
                })
                .collect(Collectors.toSet());

        ObjectMappers.MAPPER.writeValue(outputFile.getAsFile().get(), discoveredDependencies);
    }

    static Stream<SlsBlock> parsePackageJson(File file) {
        try {
            return ObjectMappers.MAPPER.readValue(file, ProductDependenciesPackageJson.class).sls().stream();
        } catch (IOException e) {
            return Stream.empty();
        }
    }
}
