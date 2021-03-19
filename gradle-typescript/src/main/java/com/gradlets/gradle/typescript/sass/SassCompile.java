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

package com.gradlets.gradle.typescript.sass;

import com.google.common.io.Files;
import java.io.File;
import java.nio.file.Path;
import javax.inject.Inject;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileType;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.InputChanges;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

public class SassCompile extends SourceTask {
    private final DirectoryProperty cssOutputDirectory =
            getProject().getObjects().directoryProperty();
    private final WorkerExecutor workerExecutor;
    private File rootDirectory;

    @OutputDirectory
    public final DirectoryProperty getCssOutputDirectory() {
        return cssOutputDirectory;
    }

    @InputDirectory
    public final File getRootDirectory() {
        return rootDirectory;
    }

    public final void setRootDirectory(File rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    @Inject
    public SassCompile(WorkerExecutor workerExecutor) {
        this.workerExecutor = workerExecutor;
    }

    @TaskAction
    public final void compile(InputChanges inputs) {
        if (!inputs.isIncremental()) {
            getProject().delete(getCssOutputDirectory().getAsFileTree().getFiles());
        }

        WorkQueue workQueue = workerExecutor.noIsolation();
        inputs.getFileChanges(getSource()).forEach(f -> {
            switch (f.getChangeType()) {
                case ADDED:
                case MODIFIED:
                    if (f.getFileType().equals(FileType.DIRECTORY)) {
                        return;
                    }
                    File outputFile = getOutputFile(f.getFile());
                    getProject().mkdir(outputFile.getParent());
                    workQueue.submit(SassCompileFile.class, sassWorkParameters -> {
                        sassWorkParameters.inputFile().set(f.getFile());
                        sassWorkParameters.outputFile().set(outputFile);
                    });
                    break;
                case REMOVED:
                    getProject().delete(getOutputFile(f.getFile()));
                    break;
            }
        });
        workQueue.await();
    }

    private File getOutputFile(File inputFile) {
        Path inputPath = inputFile.toPath();
        Path rootPath = getRootDirectory().toPath();
        String nameWithoutExtension = Files.getNameWithoutExtension(inputFile.getName());
        String parentPath = inputPath.getParent().toString();
        String relativeFilePath = parentPath.substring(rootPath.toString().length());
        return new File(
                getCssOutputDirectory().getAsFile().get(),
                String.format("%s/%s.%s", relativeFilePath, nameWithoutExtension, "css"));
    }
}
