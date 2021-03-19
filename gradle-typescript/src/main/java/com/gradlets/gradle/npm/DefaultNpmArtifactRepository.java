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

import com.palantir.logsafe.SafeArg;
import com.palantir.logsafe.exceptions.SafeIllegalArgumentException;
import java.util.Optional;
import javax.inject.Inject;
import org.gradle.api.Action;
import org.gradle.api.artifacts.repositories.RepositoryContentDescriptor;
import org.gradle.api.credentials.Credentials;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.internal.reflect.Instantiator;

public class DefaultNpmArtifactRepository implements NpmArtifactRepository {
    private String url;
    private String name;
    private Property<Credentials> credentials;
    private final Instantiator instantitor;

    @Inject
    public DefaultNpmArtifactRepository(ObjectFactory objectFactory) {
        this.credentials = objectFactory.property(Credentials.class);
        this.instantitor = objectFactory::newInstance;
    }

    @Override
    public final String getUrl() {
        return url;
    }

    @Override
    public final void setUrl(String url) {
        this.url = url;
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public final void setName(String name) {
        this.name = name;
    }

    @Override
    public final <T extends Credentials> void credentials(Class<T> credentialType, Action<? super T> action) {
        T credentialsValue = instantiateCredentials(credentialType);
        action.execute(credentialsValue);
        credentials.set(credentialsValue);
    }

    @Override
    public final <T extends Credentials> Optional<T> getCredentials(Class<T> credentialType) {
        return Optional.ofNullable(credentials
                .map(creds -> credentialType.isAssignableFrom(creds.getClass()) ? credentialType.cast(creds) : null)
                .getOrNull());
    }

    @Override
    public final void content(Action<? super RepositoryContentDescriptor> _configureAction) {
        throw new UnsupportedOperationException("Filtering npm repositories by content is unsupported");
    }

    private <T extends Credentials> T instantiateCredentials(Class<T> credentialType) {
        if (AuthHeaderCredentials.class.isAssignableFrom(credentialType)) {
            return credentialType.cast(instantitor.newInstance(DefaultAuthHeaderCredentials.class));
        } else if (PasswordCredentials.class.isAssignableFrom(credentialType)) {
            return credentialType.cast(instantitor.newInstance(DefaultPasswordCredentials.class));
        } else {
            throw new SafeIllegalArgumentException(
                    "Unrecognized credential type", SafeArg.of("credentialType", credentialType.getName()));
        }
    }
}
