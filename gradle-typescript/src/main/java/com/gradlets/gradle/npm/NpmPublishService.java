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

import com.palantir.dialogue.Channel;
import com.palantir.dialogue.ConjureRuntime;
import com.palantir.dialogue.EndpointChannel;
import com.palantir.dialogue.EndpointChannelFactory;
import com.palantir.dialogue.Request;
import com.palantir.dialogue.Serializer;
import com.palantir.dialogue.TypeMarker;
import com.palantir.tokens.auth.AuthHeader;

public interface NpmPublishService {
    void uploadPublication(NpmPackageRoot publication);

    void uploadPublication(AuthHeader authHeader, NpmPackageRoot publication);

    void uploadPublication(BasicAuthHeader basicAuthHeader, NpmPackageRoot publication);

    static NpmPublishService of(EndpointChannelFactory channel, ConjureRuntime runtime) {
        return new NpmPublishService() {
            private final Serializer<NpmPackageRoot> publishJsonSerializer =
                    runtime.bodySerDe().serializer(new TypeMarker<>() {});

            private final EndpointChannel publishPackageEndpoint =
                    channel.endpoint(NpmPublishServiceEndpoints.publishPackage);

            @Override
            public void uploadPublication(AuthHeader authHeader, NpmPackageRoot publication) {
                Request request = Request.builder()
                        .putHeaderParams("Authorization", authHeader.toString())
                        .putPathParams("packageName", publication.name())
                        .body(publishJsonSerializer.serialize(publication))
                        .build();

                runtime.clients()
                        .block(runtime.clients()
                                .call(publishPackageEndpoint, request, NpmResponseDeserializer.INSTANCE));
            }

            @Override
            public void uploadPublication(BasicAuthHeader basicAuthHeader, NpmPackageRoot publication) {
                Request request = Request.builder()
                        .putHeaderParams("Authorization", basicAuthHeader.toString())
                        .putPathParams("packageName", publication.name())
                        .body(publishJsonSerializer.serialize(publication))
                        .build();

                runtime.clients()
                        .block(runtime.clients()
                                .call(publishPackageEndpoint, request, NpmResponseDeserializer.INSTANCE));
            }

            @Override
            public void uploadPublication(NpmPackageRoot publication) {
                Request request = Request.builder()
                        .putPathParams("packageName", publication.name())
                        .body(publishJsonSerializer.serialize(publication))
                        .build();

                runtime.clients()
                        .block(runtime.clients()
                                .call(publishPackageEndpoint, request, NpmResponseDeserializer.INSTANCE));
            }
        };
    }

    static NpmPublishService of(Channel channel, ConjureRuntime runtime) {
        if (channel instanceof EndpointChannelFactory) {
            return of((EndpointChannelFactory) channel, runtime);
        }
        return of(endpoint -> runtime.clients().bind(channel, endpoint), runtime);
    }
}
