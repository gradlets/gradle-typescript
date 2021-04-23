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

import com.gradlets.gradle.typescript.idea.CreateTsConfigTask;
import com.gradlets.gradle.typescript.idea.GradleTypeScriptRootIdeaPlugin;
import com.gradlets.gradle.typescript.pdeps.TypeScriptProductDependenciesPlugin;
import java.util.stream.Collectors;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.ConfigurationPublications;
import org.gradle.api.artifacts.ConfigurationVariant;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.attributes.Bundling;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.attributes.Usage;
import org.gradle.api.internal.artifacts.ArtifactAttributes;
import org.gradle.api.internal.artifacts.ConfigurationVariantInternal;
import org.gradle.api.internal.artifacts.dsl.LazyPublishArtifact;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Compression;
import org.gradle.api.tasks.bundling.Tar;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

public class TypeScriptPlugin implements Plugin<Project> {
    @Override
    public final void apply(Project project) {
        project.getPluginManager().apply(TypeScriptBasePlugin.class);
        project.getRootProject().getPluginManager().apply(GradleTypeScriptRootIdeaPlugin.class);

        TypeScriptPluginExtension typeScriptPluginExtension =
                project.getExtensions().getByType(TypeScriptPluginExtension.class);
        PackageJsonExtension packageJsonExtension =
                project.getExtensions().create(PackageJsonExtension.EXTENSION_NAME, PackageJsonExtension.class);
        JestExtension jestExtension = DefaultJestExtension.register(project);

        configureSourceSets(project, typeScriptPluginExtension);
        configureTest(project, typeScriptPluginExtension, jestExtension);
        configureConfigurations(project, typeScriptPluginExtension);
        configureArchivesAndComponent(project, packageJsonExtension, typeScriptPluginExtension);
        configureIdeIntegration(project);

        project.getPluginManager().apply(TypeScriptProductDependenciesPlugin.class);
    }

    private static void configureSourceSets(Project project, TypeScriptPluginExtension extension) {
        SourceSet main = extension.getSourceSets().create("main");
        SourceSet test = extension.getSourceSets().create("test");
        test.getCompileClasspath().from(main.getCompileClasspath());
        project.getDependencies().add(test.getCompileConfigurationName(), project);
    }

    private static void configureTest(
            Project project, TypeScriptPluginExtension pluginExtension, JestExtension jestExtension) {
        SourceSet testSourceSet = pluginExtension.getSourceSets().getByName("test");
        TaskProvider<CreateTsConfigTask> testTsconfig =
                project.getTasks().named(testSourceSet.getCreateTsConfigTaskName(), CreateTsConfigTask.class);
        JestTestSupport.addDependencyConstraints(project, testSourceSet);
        Provider<JestTestTask> test = project.getTasks().register("jestTest", JestTestTask.class, task -> {
            task.setDescription("Runs the unit tests.");
            task.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
            task.setSource(testSourceSet.getSource());
            task.getOutputDir().set(testSourceSet.getSource().getClassesDirectory());
            task.getTsconfigFile().set(testTsconfig.flatMap(CreateTsConfigTask::getTsConfig));
            task.getPreset().set(jestExtension.getPreset());
            task.getClasspath().from(testSourceSet.getCompileClasspath());
        });

        project.getTasks().named(LifecycleBasePlugin.CHECK_TASK_NAME, task -> task.dependsOn(test));
    }

    private static void configureConfigurations(Project project, TypeScriptPluginExtension pluginExtension) {
        ConfigurationContainer configurations = project.getConfigurations();
        ObjectFactory objectFactory = project.getObjects();
        SourceSet mainSourceSet = pluginExtension.getSourceSets().getByName("main");

        Configuration compileConfiguration = configurations.getByName(mainSourceSet.getCompileConfigurationName());

        Configuration apiElementsConfiguration =
                configurations.maybeCreate(mainSourceSet.getApiElementsConfigurationName());
        apiElementsConfiguration.setVisible(false);
        apiElementsConfiguration.setDescription("API elements for main.");
        apiElementsConfiguration.setCanBeResolved(false);
        apiElementsConfiguration.setCanBeConsumed(true);
        apiElementsConfiguration
                .getAttributes()
                .attribute(
                        Usage.USAGE_ATTRIBUTE, objectFactory.named(Usage.class, TypeScriptAttributes.TYPESCRIPT_API));
        apiElementsConfiguration
                .getAttributes()
                .attribute(
                        LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                        objectFactory.named(LibraryElements.class, TypeScriptAttributes.MODULE));
        apiElementsConfiguration
                .getAttributes()
                .attribute(Bundling.BUNDLING_ATTRIBUTE, objectFactory.named(Bundling.class, Bundling.EXTERNAL));
        apiElementsConfiguration
                .getAttributes()
                .attribute(Category.CATEGORY_ATTRIBUTE, objectFactory.named(Category.class, Category.LIBRARY));
        apiElementsConfiguration.extendsFrom(compileConfiguration);
    }

    private static void configureArchivesAndComponent(
            Project project, PackageJsonExtension packageJsonExtension, TypeScriptPluginExtension pluginExtension) {
        SourceSet main = pluginExtension.getSourceSets().getByName("main");
        TaskProvider<PackageJsonTask> generatePackageJson = project.getTasks()
                .register("generatePackageJson", PackageJsonTask.class, task -> {
                    task.getPackageName()
                            .set(packageJsonExtension
                                    .getScope()
                                    .map(scope -> ProjectNames.addScope(scope, project.getName())));
                    task.getPackageJsonFields().set(packageJsonExtension.getFields());
                    task.getDependencies()
                            .set(project.getConfigurations().getByName(main.getCompileConfigurationName()));
                });

        TaskProvider<Tar> distNpm = project.getTasks().register("distNpm", Tar.class, task -> {
            task.setDescription("Assembles a npm package containing the main scripts");
            task.setGroup(BasePlugin.BUILD_GROUP);
            task.setCompression(Compression.GZIP);
            task.getArchiveExtension().set("tgz");
            task.getDestinationDirectory()
                    .set(project.getLayout()
                            .getBuildDirectory()
                            .dir(packageJsonExtension
                                    .getScope()
                                    .map(scope -> "distributions/" + ProjectNames.addScope(scope, project.getName()))));
            task.getArchiveBaseName().set(project.getName());

            task.getArchiveVersion()
                    .set(project.provider(() -> project.getVersion().toString()));
            task.from(generatePackageJson.map(PackageJsonTask::getOutputFile), copySpec -> copySpec.into("package"));
            task.from(main.getOutput(), copySpec -> copySpec.into("package/lib"));
            task.dependsOn(generatePackageJson);
        });

        PublishArtifact moduleArtifact = new LazyPublishArtifact(distNpm);
        PublishArtifact packageJsonArtifact = new LazyPublishArtifact(generatePackageJson);
        addModule(
                project,
                main,
                project.getConfigurations().getByName(main.getApiElementsConfigurationName()),
                moduleArtifact,
                packageJsonArtifact);
    }

    private static void configureIdeIntegration(Project project) {
        TaskCollection<CreateTsConfigTask> tsConfigTasks = project.getTasks().withType(CreateTsConfigTask.class);
        project.getTasks().register("vscode").configure(t -> t.dependsOn(tsConfigTasks));
        project.getPluginManager().withPlugin("idea", _ap -> {
            project.getTasks().named("idea").configure(t -> t.dependsOn(tsConfigTasks));
        });
    }

    private static void addModule(
            Project project,
            SourceSet mainSourceSet,
            Configuration configuration,
            PublishArtifact moduleArtifact,
            PublishArtifact packageJsonArtifact) {
        ConfigurationPublications publications = configuration.getOutgoing();
        ConfigurationVariant moduleVariant = publications.getVariants().create("module");
        publications.getAttributes().attribute(ArtifactAttributes.ARTIFACT_FORMAT, "tgz");
        publications
                .getAttributes()
                .attribute(
                        LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                        project.getObjects().named(LibraryElements.class, TypeScriptAttributes.MODULE));
        moduleVariant.artifact(moduleArtifact);

        ConfigurationVariant resourcesVariant = publications.getVariants().create("package-json");
        resourcesVariant.getAttributes().attribute(ArtifactAttributes.ARTIFACT_FORMAT, "package-json");
        resourcesVariant
                .getAttributes()
                .attribute(
                        LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                        project.getObjects().named(LibraryElements.class, TypeScriptAttributes.PACKAGE_JSON));
        resourcesVariant.artifact(packageJsonArtifact);

        ConfigurationVariantInternal sourceVariant = (ConfigurationVariantInternal)
                configuration.getOutgoing().getVariants().create("source-script-dirs");
        sourceVariant
                .getAttributes()
                .attribute(ArtifactAttributes.ARTIFACT_FORMAT, TypeScriptAttributes.SOURCE_SCRIPT_DIRS);
        sourceVariant
                .getAttributes()
                .attribute(
                        LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                        project.getObjects().named(LibraryElements.class, TypeScriptAttributes.SOURCE_SCRIPT_DIRS));
        sourceVariant.artifactsProvider(() -> mainSourceSet.getSource().getSourceDirectories().getFiles().stream()
                .map(sourceScriptDir -> new LazyPublishArtifact(project.provider(() -> sourceScriptDir)))
                .collect(Collectors.toUnmodifiableList()));
    }
}
