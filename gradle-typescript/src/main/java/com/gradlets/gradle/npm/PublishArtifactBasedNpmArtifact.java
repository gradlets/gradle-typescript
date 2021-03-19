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
import javax.inject.Inject;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.internal.tasks.AbstractTaskDependency;
import org.gradle.api.internal.tasks.DefaultTaskDependency;
import org.gradle.api.internal.tasks.TaskDependencyInternal;
import org.gradle.api.internal.tasks.TaskDependencyResolveContext;
import org.gradle.api.tasks.TaskDependency;

public class PublishArtifactBasedNpmArtifact implements NpmPublicationArtifact {
    private final PublishArtifact artifact;
    private final DefaultTaskDependency additionalDependencies;
    private final CompositeTaskDependency buildDependencies;

    @Inject
    public PublishArtifactBasedNpmArtifact(PublishArtifact artifact) {
        this.artifact = artifact;
        additionalDependencies = new DefaultTaskDependency();
        buildDependencies = new CompositeTaskDependency();
    }

    @Override
    public final File getFile() {
        return artifact.getFile();
    }

    @Override
    public final void builtBy(Object... tasks) {
        additionalDependencies.add(tasks);
    }

    @Override
    public final TaskDependency getBuildDependencies() {
        return buildDependencies;
    }

    private final class CompositeTaskDependency extends AbstractTaskDependency {

        @Override
        public void visitDependencies(TaskDependencyResolveContext context) {
            ((TaskDependencyInternal) artifact.getBuildDependencies()).visitDependencies(context);
            additionalDependencies.visitDependencies(context);
        }
    }
}
