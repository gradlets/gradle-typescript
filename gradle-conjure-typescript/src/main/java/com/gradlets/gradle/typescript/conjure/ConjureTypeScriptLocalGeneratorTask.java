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

package com.gradlets.gradle.typescript.conjure;

import com.google.common.collect.ImmutableList;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecResult;
import org.gradle.util.GFileUtils;

@CacheableTask
public class ConjureTypeScriptLocalGeneratorTask extends DefaultTask {
    private final RegularFileProperty inputFile = getProject().getObjects().fileProperty();
    private final Property<File> executablePath = getProject().getObjects().property(File.class);
    private final DirectoryProperty outputDirectory = getProject().getObjects().directoryProperty();

    @InputFile
    @SkipWhenEmpty
    @PathSensitive(PathSensitivity.RELATIVE)
    public final RegularFileProperty getInputFile() {
        return inputFile;
    }

    @OutputDirectory
    public final DirectoryProperty getOutputDirectory() {
        return outputDirectory;
    }

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public final Property<File> getExecutablePath() {
        return executablePath;
    }

    @TaskAction
    public final void generate() {
        File definitionFile = getInputFile().getAsFile().get();
        File outputDir = outputDirectory.getAsFile().get();

        GFileUtils.deleteDirectory(outputDir);
        getProject().mkdir(outputDir);

        List<String> generateCommand = ImmutableList.of(
                getExecutablePath().get().getAbsolutePath(),
                "generate",
                definitionFile.getAbsolutePath(),
                outputDir.getAbsolutePath(),
                "--rawSource");

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        ExecResult execResult = getProject().exec(execSpec -> {
            execSpec.commandLine(generateCommand);
            execSpec.setIgnoreExitValue(true);
            execSpec.setStandardOutput(output);
            execSpec.setErrorOutput(output);
        });

        if (execResult.getExitValue() != 0) {
            throw new GradleException(String.format(
                    "Failed to generate TypeScript. Command failed with exit code %d. Output:\n%s",
                    execResult.getExitValue(), output.toString()));
        }
    }
}
