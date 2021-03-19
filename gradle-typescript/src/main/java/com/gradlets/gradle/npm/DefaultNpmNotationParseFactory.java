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
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.internal.artifacts.dsl.LazyPublishArtifact;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.tasks.TaskDependencyContainer;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.internal.Cast;
import org.gradle.internal.exceptions.DiagnosticsVisitor;
import org.gradle.internal.typeconversion.NotationConvertResult;
import org.gradle.internal.typeconversion.NotationConverter;
import org.gradle.internal.typeconversion.NotationParser;
import org.gradle.internal.typeconversion.NotationParserBuilder;
import org.gradle.internal.typeconversion.TypeConversionException;

public class DefaultNpmNotationParseFactory {
    private final ObjectFactory objectFactory;
    private final FileResolver fileResolver;

    public DefaultNpmNotationParseFactory(ObjectFactory objectFactory, FileResolver fileResolver) {
        this.objectFactory = objectFactory;
        this.fileResolver = fileResolver;
    }

    public final NotationParser<Object, NpmPublicationArtifact> create() {
        FileNotationConverter fileNotationConverter = new FileNotationConverter(fileResolver);
        ArchiveTaskNotationConverter archiveTaskNotationConverter = new ArchiveTaskNotationConverter();
        PublishArtifactNotationConverter publishArtifactNotationConverter = new PublishArtifactNotationConverter();
        ProviderNotationConverter providerNotationConverter = new ProviderNotationConverter();

        return NotationParserBuilder.toType(NpmPublicationArtifact.class)
                .fromType(AbstractArchiveTask.class, archiveTaskNotationConverter)
                .fromType(PublishArtifact.class, publishArtifactNotationConverter)
                .fromType(Provider.class, Cast.uncheckedCast(providerNotationConverter))
                .converter(fileNotationConverter)
                .toComposite();
    }

    private final class ArchiveTaskNotationConverter
            implements NotationConverter<AbstractArchiveTask, NpmPublicationArtifact> {
        @Override
        public void convert(
                AbstractArchiveTask archiveTask, NotationConvertResult<? super NpmPublicationArtifact> result)
                throws TypeConversionException {
            NpmPublicationArtifact artifact = objectFactory.newInstance(ArchiveTaskBasedNpmArtifact.class, archiveTask);
            result.converted(artifact);
        }

        @Override
        public void describe(DiagnosticsVisitor visitor) {
            visitor.candidate("Instances of AbstractArchiveTask").example("distNpm");
        }
    }

    private final class PublishArtifactNotationConverter
            implements NotationConverter<PublishArtifact, NpmPublicationArtifact> {
        @Override
        public void convert(
                PublishArtifact publishArtifact, NotationConvertResult<? super NpmPublicationArtifact> result)
                throws TypeConversionException {
            NpmPublicationArtifact artifact =
                    objectFactory.newInstance(PublishArtifactBasedNpmArtifact.class, publishArtifact);
            result.converted(artifact);
        }

        @Override
        public void describe(DiagnosticsVisitor visitor) {
            visitor.candidate("Instances of PublishArtifact");
        }
    }

    private final class ProviderNotationConverter implements NotationConverter<Provider<?>, NpmPublicationArtifact> {
        @Override
        public void convert(Provider<?> publishArtifact, NotationConvertResult<? super NpmPublicationArtifact> result)
                throws TypeConversionException {
            NpmPublicationArtifact artifact = objectFactory.newInstance(
                    PublishArtifactBasedNpmArtifact.class, new LazyPublishArtifact(publishArtifact, fileResolver));
            result.converted(artifact);
        }

        @Override
        public void describe(DiagnosticsVisitor visitor) {
            visitor.candidate("Instances of Provider");
        }
    }

    private final class FileNotationConverter implements NotationConverter<Object, NpmPublicationArtifact> {
        private final NotationParser<Object, File> fileResolverNotationParser;

        private FileNotationConverter(FileResolver fileResolver) {
            this.fileResolverNotationParser = fileResolver.asNotationParser();
        }

        @Override
        public void convert(Object notation, NotationConvertResult<? super NpmPublicationArtifact> result)
                throws TypeConversionException {
            File file = fileResolverNotationParser.parseNotation(notation);
            NpmPublicationArtifact mavenArtifact = objectFactory.newInstance(FileBasedNpmArtifact.class, file);
            if (notation instanceof TaskDependencyContainer) {
                TaskDependencyContainer taskDependencyContainer = (TaskDependencyContainer) notation;
                if (notation instanceof Provider) {
                    // wrap to disable special handling of providers by DefaultTaskDependency in this case
                    // (workaround for https://github.com/gradle/gradle/issues/11054)
                    taskDependencyContainer = context -> context.add(notation);
                }
                mavenArtifact.builtBy(taskDependencyContainer);
            }
            result.converted(mavenArtifact);
        }

        @Override
        public void describe(DiagnosticsVisitor visitor) {
            fileResolverNotationParser.describe(visitor);
        }
    }
}
