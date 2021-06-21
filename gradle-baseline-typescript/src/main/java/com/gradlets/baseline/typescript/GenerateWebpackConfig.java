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

package com.gradlets.baseline.typescript;

import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
<<<<<<< HEAD
import java.nio.file.Files;
=======
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
>>>>>>> 72c406c (dev)
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

public abstract class GenerateWebpackConfig extends DefaultTask {

    @Input
    abstract Property<String> getWebpackOutputDir();

    @Input
    abstract Property<String> getTsEntryPoint();

    @Input
    abstract Property<String> getSassEntryPoint();

    @OutputFile
    abstract RegularFileProperty getOutputFile();

    @TaskAction
    public final void generate() throws IOException {
        List<String> entryPoints =
                        Stream.of(getTsEntryPoint().get(), getSassEntryPoint().get()).filter(file -> )
            Files.writeString(
                    getOutputFile().get().getAsFile().toPath(),
                    getWebpackConfig(getWebpackOutputDir().get(), getEntryPoint().get()),
                    StandardCharsets.UTF_8);
    }

    private static String getWebpackConfig(String outputDir, List<String> entryPoints) {
        try {
            return Resources.toString(Resources.getResource("webpack.template.js"), StandardCharsets.UTF_8)
                    .replace("__OUTPUT_DIR__", "'" + outputDir + "'")
                    .replace(
                            "__ENTRY_POINTS__",
                            entryPoints.stream().map(s -> "'" + s + "'").collect(Collectors.joining(", ", "[", "]")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
