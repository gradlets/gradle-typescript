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

import groovy.lang.Closure;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.process.ExecResult;
import org.gradle.process.ExecSpec;
import org.gradle.util.internal.ConfigureUtil;

public final class NodeExec {

    public static void exec(Project project, FileCollection classpath, Closure<ExecSpec> configure) {
        exec(project, classpath, ConfigureUtil.configureUsing(configure));
    }

    public static void exec(Project project, FileCollection classpath, Action<? super ExecSpec> configure) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ExecResult execResult = project.exec(execSpec -> {
            execSpec.environment("NODE_PATH", toNodePath(classpath));
            execSpec.setIgnoreExitValue(true);
            execSpec.setStandardOutput(output);
            execSpec.setErrorOutput(output);
            configure.execute(execSpec);
        });

        if (execResult.getExitValue() != 0) {
            throw new GradleException(String.format(
                    "Failed with exit code %d. Output:\n%s",
                    execResult.getExitValue(), output.toString(StandardCharsets.UTF_8)));
        }
    }

    private static String toNodePath(FileCollection classpath) {
        return classpath.getFiles().stream()
                .map(file -> {
                    File maybeCheckSumDir = file.getParentFile().getParentFile();
                    if (ArtifactLayout.isChecksumDirOrTransformed(maybeCheckSumDir.getName())) {
                        return file.getParentFile().getAbsolutePath();
                    }
                    return maybeCheckSumDir.getAbsolutePath();
                })
                .collect(Collectors.joining(File.pathSeparator));
    }

    private NodeExec() {}
}
