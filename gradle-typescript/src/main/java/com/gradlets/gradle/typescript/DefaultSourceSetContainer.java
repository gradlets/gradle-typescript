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

import javax.inject.Inject;
import org.gradle.api.internal.AbstractValidatingNamedDomainObjectContainer;
import org.gradle.api.internal.CollectionCallbackActionDecorator;
import org.gradle.api.internal.file.FileCollectionFactory;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.reflect.TypeOf;

public class DefaultSourceSetContainer extends AbstractValidatingNamedDomainObjectContainer<SourceSet>
        implements SourceSetContainer {

    private final ObjectFactory objectFactory;
    private final FileCollectionFactory fileCollectionFactory;

    @Inject
    public DefaultSourceSetContainer(
            ObjectFactory objectFactory,
            FileCollectionFactory fileCollectionFactory,
            CollectionCallbackActionDecorator collectionCallbackActionDecorator) {
        super(SourceSet.class, objectFactory::newInstance, SourceSet::getName, collectionCallbackActionDecorator);
        this.objectFactory = objectFactory;
        this.fileCollectionFactory = fileCollectionFactory;
    }

    @Override
    protected final SourceSet doCreate(String name) {
        DefaultTypeScriptSourceSet sourceSet =
                objectFactory.newInstance(DefaultTypeScriptSourceSet.class, name, objectFactory);
        sourceSet.setOutput(objectFactory.newInstance(
                DefaultSourceSetOutput.class, sourceSet.getDisplayName(), objectFactory, fileCollectionFactory));
        return sourceSet;
    }

    @Override
    public final TypeOf<?> getPublicType() {
        return TypeOf.typeOf(SourceSetContainer.class);
    }
}
