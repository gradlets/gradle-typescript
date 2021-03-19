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

import org.gradle.api.NamedDomainObjectFactory;
import org.gradle.api.model.ObjectFactory;
import org.gradle.internal.typeconversion.NotationParser;

public final class NpmPublicationFactory implements NamedDomainObjectFactory<NpmPublication> {
    private final ObjectFactory objects;
    private final NotationParser<Object, NpmPublicationArtifact> notationParser;

    public NpmPublicationFactory(ObjectFactory objects, NotationParser<Object, NpmPublicationArtifact> notationParser) {
        this.objects = objects;
        this.notationParser = notationParser;
    }

    @Override
    public NpmPublication create(String name) {
        return objects.newInstance(DefaultNpmPublication.class, name, notationParser);
    }
}
