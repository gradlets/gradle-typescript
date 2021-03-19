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

import javax.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.NamedDomainObjectList;
import org.gradle.api.NamedDomainObjectSet;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.artifacts.repositories.IvyArtifactRepository;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.plugins.PublishingPlugin;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.internal.typeconversion.NotationParser;

public final class NpmPublishPlugin implements Plugin<Project> {

    public static final String PUBLISH_LOCAL_LIFECYCLE_TASK_NAME = "publishToNpmLocal";

    private final FileResolver fileResolver;

    @Inject
    public NpmPublishPlugin(FileResolver fileResolver) {
        this.fileResolver = fileResolver;
    }

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(PublishingPlugin.class);

        project.getTasks().register(PUBLISH_LOCAL_LIFECYCLE_TASK_NAME, publish -> {
            publish.setDescription("Publishes all NPM publications produced by this project to the local NPM cache.");
            publish.setGroup("publishing");
        });
        project.getExtensions().configure(PublishingExtension.class, extension -> {
            NpmExtension npmExtension = project.getObjects()
                    .newInstance(
                            NpmExtension.class, project.getGradle(), project.getObjects(), extension.getRepositories());
            new DslObject(extension.getRepositories())
                    .getConvention()
                    .getPlugins()
                    .put("npm", npmExtension);
            NotationParser<Object, NpmPublicationArtifact> notationParser =
                    new DefaultNpmNotationParseFactory(project.getObjects(), fileResolver).create();
            extension
                    .getPublications()
                    .registerFactory(
                            NpmPublication.class, new NpmPublicationFactory(project.getObjects(), notationParser));
            realizePublishingTasksLater(project, extension);
        });
    }

    private void realizePublishingTasksLater(Project project, PublishingExtension extension) {
        NamedDomainObjectSet<NpmPublication> npmPublications =
                extension.getPublications().withType(NpmPublication.class);
        TaskContainer tasks = project.getTasks();

        TaskProvider<Task> publishLifecycleTask = tasks.named(PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME);
        NamedDomainObjectList<IvyArtifactRepository> repositories = extension
                .getRepositories()
                .withType(IvyArtifactRepository.class)
                .matching(repository -> repository.getName().startsWith(NpmExtension.NPM_REPO_DEFAULT_NAME));

        repositories.all(repository -> {
            TaskProvider<Task> repositorySpecificPublishTask =
                    tasks.register(publishAllToSingleRepoTaskName(repository), publish -> {
                        publish.setDescription("Publishes all NPM publications produced by this project to the "
                                + getRepositoryName(repository.getName()) + " repository.");
                        publish.setGroup(PublishingPlugin.PUBLISH_TASK_GROUP);
                    });
            publishLifecycleTask.configure(task -> task.dependsOn(repositorySpecificPublishTask));

            npmPublications.all(
                    publication -> createPublishTask(tasks, repository, repositorySpecificPublishTask, publication));
        });

        TaskProvider<Task> publishLocalLifecycleTask = tasks.named(PUBLISH_LOCAL_LIFECYCLE_TASK_NAME);
        npmPublications.all(publication -> createLocalPublishTask(tasks, publishLocalLifecycleTask, publication));
    }

    private void createPublishTask(
            TaskContainer tasks,
            IvyArtifactRepository repository,
            TaskProvider<Task> repositorySpecificPublishTask,
            NpmPublication publication) {
        String repositoryName = getRepositoryName(repository.getName());
        String publishTaskName = "publish" + StringUtils.capitalize(publication.getName()) + "PublicationTo"
                + StringUtils.capitalize(repositoryName) + "Repository";
        tasks.register(publishTaskName, PublishToNpmRepository.class, publishTask -> {
            publishTask.getPublication().set(publication);
            publishTask.getRepository().set(repository);
            publishTask.setGroup(PublishingPlugin.PUBLISH_TASK_GROUP);
            publishTask.setDescription("Publishes Npm publication '" + publication.getName() + "' to Npm repository '"
                    + repositoryName + "'.");
        });
        repositorySpecificPublishTask.configure(task -> task.dependsOn(publishTaskName));
    }

    private void createLocalPublishTask(
            TaskContainer tasks, TaskProvider<Task> publishLocalLifecycleTask, NpmPublication publication) {
        String publicationName = publication.getName();
        String installTaskName = "publish" + StringUtils.capitalize(publicationName) + "PublicationToNpmLocal";

        tasks.register(installTaskName, PublishToNpmLocal.class, publishLocalTask -> {
            publishLocalTask.getPublication().set(publication);
            publishLocalTask.setGroup(PublishingPlugin.PUBLISH_TASK_GROUP);
            publishLocalTask.setDescription(
                    "Publishes Npm publication '" + publicationName + "' to the local Npm repository.");
        });
        publishLocalLifecycleTask.configure(task -> task.dependsOn(installTaskName));
    }

    private String publishAllToSingleRepoTaskName(ArtifactRepository repository) {
        return "publishAllNpmPublicationsTo" + StringUtils.capitalize(getRepositoryName(repository.getName()))
                + "Repository";
    }

    private String getRepositoryName(String repoName) {
        return repoName.startsWith("npm_") ? repoName.substring(4) : repoName;
    }
}
