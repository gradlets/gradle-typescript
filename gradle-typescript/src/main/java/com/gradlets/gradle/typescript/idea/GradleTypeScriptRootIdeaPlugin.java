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

package com.gradlets.gradle.typescript.idea;

import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import com.gradlets.gradle.typescript.TypeScriptPluginExtension;
import com.palantir.logsafe.exceptions.SafeIllegalStateException;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.function.Consumer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.internal.artifacts.ArtifactAttributes;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public final class GradleTypeScriptRootIdeaPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        Preconditions.checkArgument(
                project.getRootProject() == project,
                "GradleTypeScriptRootIdeaPlugin can only be applied to root project");
        project.getPluginManager().withPlugin("idea", _ap -> {
            project.getGradle().projectsEvaluated(_gradle -> {
                configureLegacyIdea(project);
                configureIntelliJImport(project);
            });
        });
    }

    private static void configureLegacyIdea(Project project) {
        project.getExtensions().getByType(IdeaModel.class).getProject().getIpr().withXml(xmlProvider -> {
            ConfigureGradleTypeScriptXml.configureTypeScriptCompiler(
                    xmlProvider.asElement(), tscLocationFromSubprojects(project));
        });
    }

    private static Path tscLocationFromSubprojects(Project project) {
        Project subProjectWithTsc = project.getAllprojects().stream()
                .filter(pr -> pr.getExtensions().findByType(TypeScriptPluginExtension.class) != null)
                .findFirst()
                .orElseThrow(() -> new SafeIllegalStateException("No project with gradle-typescript plugin applied"));
        Dependency tscDependency = subProjectWithTsc
                .getDependencies()
                .create("npm:typescript:"
                        + subProjectWithTsc
                                .getExtensions()
                                .findByType(TypeScriptPluginExtension.class)
                                .getSourceCompatibility()
                                .get());
        Configuration tscConfiguration = project.getConfigurations().detachedConfiguration(tscDependency);
        tscConfiguration.getAttributes().attribute(ArtifactAttributes.ARTIFACT_FORMAT, "module");
        return tscConfiguration.getSingleFile().toPath();
    }

    private static void configureIntelliJImport(Project project) {
        // Note: we tried using 'org.jetbrains.gradle.plugin.idea-ext' and afterSync triggers, but these are currently
        // very hard to manage as the tasks feel disconnected from the Sync operation, and you can't remove them once
        // you've added them. For that reason, we accept that we have to resolve this configuration at
        // configuration-time, but only do it when part of an IDEA import.
        if (!Boolean.getBoolean("idea.active")) {
            return;
        }
        createOrUpdateIdeaXmlFile(
                project.file(".idea/typescript-compiler.xml"),
                node -> ConfigureGradleTypeScriptXml.configureTypeScriptCompiler(
                        node, tscLocationFromSubprojects(project)));
    }

    private static void createOrUpdateIdeaXmlFile(File configurationFile, Consumer<Element> configure) {
        DocumentBuilder dBuilder;
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Unable to build xml parser");
        }

        Element rootElement;
        Document doc;
        if (configurationFile.isFile()) {
            try {
                doc = dBuilder.parse(configurationFile);
                rootElement = doc.getDocumentElement();
            } catch (IOException | SAXException e) {
                throw new RuntimeException("Couldn't parse existing configuration file: " + configurationFile, e);
            }
        } else {
            doc = dBuilder.newDocument();
            rootElement = doc.createElement("project");
            rootElement.setAttribute("version", "4");
        }

        configure.accept(rootElement);

        try (Writer writer = Files.newWriter(configurationFile, StandardCharsets.UTF_8)) {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
        } catch (IOException | TransformerException e) {
            throw new RuntimeException("Failed to write back to configuration file: " + configurationFile, e);
        }
    }
}
