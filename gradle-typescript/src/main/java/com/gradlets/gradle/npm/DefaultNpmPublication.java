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

import com.palantir.logsafe.exceptions.SafeRuntimeException;
import java.util.Map;
import javax.annotation.Nullable;
import javax.inject.Inject;
import org.gradle.api.Action;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.internal.CollectionCallbackActionDecorator;
import org.gradle.api.internal.artifacts.ArtifactAttributes;
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier;
import org.gradle.api.internal.attributes.ImmutableAttributes;
import org.gradle.api.internal.attributes.ImmutableAttributesFactory;
import org.gradle.api.internal.component.SoftwareComponentInternal;
import org.gradle.api.internal.file.FileCollectionFactory;
import org.gradle.api.publish.internal.PublicationArtifactSet;
import org.gradle.api.publish.internal.PublicationInternal;
import org.gradle.api.publish.internal.versionmapping.VersionMappingStrategyInternal;
import org.gradle.internal.DisplayName;
import org.gradle.internal.typeconversion.NotationParser;

public class DefaultNpmPublication implements NpmPublication, PublicationInternal<NpmPublicationArtifact> {
    private final String name;
    private final PublicationArtifactSet<NpmPublicationArtifact> publicationArtifacts;
    private final NotationParser<Object, NpmPublicationArtifact> npmArtifactNotationParser;
    private final ImmutableAttributesFactory attributesFactory;

    @Inject
    public DefaultNpmPublication(
            String name,
            NotationParser<Object, NpmPublicationArtifact> npmArtifactNotationParser,
            ImmutableAttributesFactory attributesFactory,
            FileCollectionFactory fileCollectionFactory,
            CollectionCallbackActionDecorator collectionCallbackActionDecorator) {
        this.name = name;
        this.npmArtifactNotationParser = npmArtifactNotationParser;
        this.attributesFactory = attributesFactory;
        publicationArtifacts = new DefaultNpmPublicationArtifactSet<>(
                NpmPublicationArtifact.class, name, fileCollectionFactory, collectionCallbackActionDecorator);
    }

    @Override
    public void withoutBuildIdentifier() {}

    @Override
    public void withBuildIdentifier() {}

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public final void artifact(Object artifact) {
        if (publicationArtifacts.size() > 0) {
            throw new SafeRuntimeException("NpmPublication supports only one artifact per publication");
        }
        publicationArtifacts.add(npmArtifactNotationParser.parseNotation(artifact));
    }

    @Override
    public final ModuleVersionIdentifier getCoordinates() {
        Map<String, Object> packageJson = PackageJsons.getPackageJson(
                getPublishableArtifacts().getFiles().getSingleFile().toPath());
        String packageName = (String) packageJson.get("name");
        String packageVersion = (String) packageJson.get("version");
        String strippedPackageName = packageName.startsWith("@") ? packageName.substring(1) : packageName;
        return DefaultModuleVersionIdentifier.newId("npm", strippedPackageName, packageVersion);
    }

    @Nullable
    @Override
    public final <T> T getCoordinates(Class<T> type) {
        if (type.isAssignableFrom(ModuleVersionIdentifier.class)) {
            return type.cast(getCoordinates());
        }
        return null;
    }

    @Override
    public final ImmutableAttributes getAttributes() {
        return attributesFactory.of(ArtifactAttributes.ARTIFACT_FORMAT, "tgz");
    }

    @Override
    public final void setAlias(boolean _alias) {}

    @Override
    public final PublicationArtifactSet<NpmPublicationArtifact> getPublishableArtifacts() {
        return publicationArtifacts;
    }

    @Override
    public final void allPublishableArtifacts(Action<? super NpmPublicationArtifact> action) {
        getPublishableArtifacts().all(action);
    }

    @Override
    public final void whenPublishableArtifactRemoved(Action<? super NpmPublicationArtifact> _action) {
        throw new UnsupportedOperationException("Adding derived artifacts is not supported for NPM publications");
    }

    @Override
    public final NpmPublicationArtifact addDerivedArtifact(
            NpmPublicationArtifact _npmPublicationArtifact, DerivedArtifact _derivedArtifact) {
        throw new UnsupportedOperationException("Adding derived artifacts is not supported for NPM publications");
    }

    @Override
    public final void removeDerivedArtifact(NpmPublicationArtifact _npmPublicationArtifact) {
        throw new UnsupportedOperationException("Removing derived artifacts is not supported for NPM publications");
    }

    @Override
    public final PublishedFile getPublishedFile(PublishArtifact publishArtifact) {
        return new PublishedFile() {
            @Override
            public String getName() {
                return publishArtifact.getName();
            }

            @Override
            public String getUri() {
                return publishArtifact.getName();
            }
        };
    }

    @Nullable
    @Override
    public final VersionMappingStrategyInternal getVersionMappingStrategy() {
        return null;
    }

    @Override
    public final boolean isPublishBuildId() {
        return false;
    }

    @Nullable
    @Override
    public final SoftwareComponentInternal getComponent() {
        return null;
    }

    @Override
    public final boolean isAlias() {
        return false;
    }

    @Override
    public final boolean isLegacy() {
        return false;
    }

    @Override
    public final DisplayName getDisplayName() {
        return new DisplayName() {
            @Override
            public String getCapitalizedDisplayName() {
                return String.format("%s NPM Publication", name);
            }

            @Override
            public String getDisplayName() {
                return String.format("%s NPM Publication", name);
            }
        };
    }
}
