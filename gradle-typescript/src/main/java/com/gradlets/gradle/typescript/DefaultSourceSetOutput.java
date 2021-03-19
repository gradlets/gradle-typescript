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
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javax.inject.Inject;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.internal.file.CompositeFileCollection;
import org.gradle.api.internal.file.FileCollectionFactory;
import org.gradle.api.internal.file.FileCollectionInternal;
import org.gradle.api.model.ObjectFactory;

public class DefaultSourceSetOutput extends CompositeFileCollection {
    private final ConfigurableFileCollection outputDirectories;
    private final ConfigurableFileCollection scriptsDirs;
    private final String displayName;
    private final FileCollectionFactory fileCollectionFactory;

    @Inject
    public DefaultSourceSetOutput(
            String displayName, ObjectFactory objectFactory, FileCollectionFactory fileCollectionFactory) {
        this.displayName = displayName;
        this.fileCollectionFactory = fileCollectionFactory;
        this.outputDirectories = objectFactory.fileCollection();
        outputDirectories.builtBy(this);
        this.scriptsDirs = objectFactory.fileCollection();
        outputDirectories.from(scriptsDirs);
    }

    @Override
    public final String getDisplayName() {
        return this.displayName;
    }

    public final ConfigurableFileCollection getScriptsDirs() {
        return scriptsDirs;
    }

    public final void addScriptsDirs(Callable<File> scriptsDir) {
        scriptsDirs.from(scriptsDir);
    }

    public final void builtBy(Object... taskPaths) {
        outputDirectories.builtBy(taskPaths);
    }

    @Override
    protected final void visitChildren(Consumer<FileCollectionInternal> consumer) {
        consumer.accept(fileCollectionFactory.resolving(outputDirectories));
        consumer.accept(fileCollectionFactory.resolving(scriptsDirs));
    }
}
