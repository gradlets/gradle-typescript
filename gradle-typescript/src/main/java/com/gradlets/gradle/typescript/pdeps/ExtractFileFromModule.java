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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.GZIPInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.gradle.api.artifacts.transform.CacheableTransform;
import org.gradle.api.artifacts.transform.InputArtifact;
import org.gradle.api.artifacts.transform.TransformAction;
import org.gradle.api.artifacts.transform.TransformOutputs;
import org.gradle.api.artifacts.transform.TransformParameters;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;

@CacheableTransform
public abstract class ExtractFileFromModule implements TransformAction<ExtractFileFromModule.Parameters> {
    interface Parameters extends TransformParameters {
        @Input
        Property<String> getPathToExtract();
    }

    @PathSensitive(PathSensitivity.NAME_ONLY)
    @InputArtifact
    public abstract Provider<FileSystemLocation> getInputArtifact();

    @Override
    public final void transform(TransformOutputs outputs) {
        File moduleFile = getInputArtifact().get().getAsFile();
        String pathToExtract = getParameters().getPathToExtract().get();

        try (TarArchiveInputStream tgzInputStream =
                new TarArchiveInputStream(new GZIPInputStream(Files.newInputStream(moduleFile.toPath())))) {

            TarArchiveEntry entry;
            while ((entry = (TarArchiveEntry) tgzInputStream.getNextEntry()) != null) {
                if (entry.getName().equals(pathToExtract)) {
                    File outputFile = outputs.file("package.json");
                    Files.copy(tgzInputStream, outputFile.toPath());
                    return;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to extract '" + pathToExtract + "' from module: " + moduleFile, e);
        }
    }
}
