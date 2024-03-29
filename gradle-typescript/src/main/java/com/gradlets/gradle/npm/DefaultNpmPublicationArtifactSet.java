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

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.CollectionCallbackActionDecorator;
import org.gradle.api.internal.DefaultDomainObjectSet;
import org.gradle.api.internal.file.FileCollectionFactory;
import org.gradle.api.internal.file.collections.MinimalFileSet;
import org.gradle.api.internal.tasks.AbstractTaskDependency;
import org.gradle.api.internal.tasks.TaskDependencyResolveContext;
import org.gradle.api.publish.PublicationArtifact;
import org.gradle.api.publish.internal.PublicationArtifactSet;

public class DefaultNpmPublicationArtifactSet<T extends PublicationArtifact> extends DefaultDomainObjectSet<T>
        implements PublicationArtifactSet<T> {

    private final FileCollection files;

    public DefaultNpmPublicationArtifactSet(
            Class<T> type,
            String name,
            FileCollectionFactory fileCollectionFactory,
            CollectionCallbackActionDecorator collectionCallbackActionDecorator) {
        super(type, collectionCallbackActionDecorator);
        files = fileCollectionFactory.create(
                new AbstractTaskDependency() {
                    @Override
                    public void visitDependencies(TaskDependencyResolveContext context) {
                        for (PublicationArtifact artifact : DefaultNpmPublicationArtifactSet.this) {
                            context.add(artifact.getBuildDependencies());
                        }
                    }
                },
                new MinimalFileSet() {
                    @Override
                    public String getDisplayName() {
                        return name;
                    }

                    @Override
                    public Set<File> getFiles() {
                        Set<File> result = new LinkedHashSet<>();
                        for (PublicationArtifact artifact : DefaultNpmPublicationArtifactSet.this) {
                            result.add(artifact.getFile());
                        }
                        return result;
                    }
                });
    }

    @Override
    public final FileCollection getFiles() {
        return files;
    }
}
