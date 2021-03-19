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

import com.gradlets.gradle.typescript.ObjectMappers;
import com.gradlets.gradle.typescript.shim.cache.IvyDescriptors;
import com.gradlets.gradle.typescript.shim.clients.PackageJson;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.Callable;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

public abstract class PublishToNpmLocal extends DefaultTask {
    static final Path LOCAL_NPM_REPOSITORY =
            Paths.get(System.getProperty("user.home")).resolve(".npm").resolve("repository");

    @SuppressWarnings("PublicConstructorForAbstractClass")
    public PublishToNpmLocal() {
        getInputs()
                .files((Callable<FileCollection>)
                        () -> getPublication().get().getPublishableArtifacts().getFiles())
                .withPropertyName("publication.publishableFiles")
                .withPathSensitivity(PathSensitivity.NAME_ONLY);
    }

    @Input
    public abstract Property<NpmPublication> getPublication();

    @TaskAction
    public final void upload() throws IOException {
        Path publicationPath = getPublication()
                .get()
                .getPublishableArtifacts()
                .getFiles()
                .getSingleFile()
                .toPath();
        NpmPackageRoot npmPackageRoot = PackageJsons.packageJsonForPublication(
                PackageJsons.getPackageJson(publicationPath),
                StringUtils.appendIfMissing(LOCAL_NPM_REPOSITORY.toString(), "/"),
                Files.readAllBytes(publicationPath));
        Map<String, Object> packageJson = npmPackageRoot.versions().get(npmPackageRoot.version());
        Path tarballPath = Paths.get((String) ((Map<String, Object>) packageJson.get("dist")).get("tarball"));

        Files.createDirectories(tarballPath.getParent());
        Files.copy(publicationPath, tarballPath, StandardCopyOption.REPLACE_EXISTING);
        Path descriptorPath =
                LOCAL_NPM_REPOSITORY.resolve(npmPackageRoot.name()).resolve(npmPackageRoot.version());
        Files.createDirectories(descriptorPath);
        Files.write(
                descriptorPath.resolve("descriptor.ivy"),
                IvyDescriptors.createDescriptor(
                                "npm", ObjectMappers.MAPPER.convertValue(packageJson, PackageJson.class))
                        .getBytes(StandardCharsets.UTF_8));
    }
}
