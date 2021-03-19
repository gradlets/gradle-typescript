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

package com.gradlets.gradle.typescript.shim.clients;

import com.palantir.dialogue.Channel;
import com.palantir.dialogue.ConjureRuntime;
import com.palantir.dialogue.Deserializer;
import com.palantir.dialogue.Request;
import com.palantir.dialogue.TypeMarker;

public interface NpmRepository {
    PackageJson getPackageJson(String packageName, String packageVersion);

    static NpmRepository of(Channel channel, ConjureRuntime runtime) {
        return new NpmRepository() {
            private final Deserializer<PackageJson> getPackageJsonDeserializer =
                    runtime.bodySerDe().deserializer(new TypeMarker<>() {});

            @Override
            public PackageJson getPackageJson(String packageName, String packageVersion) {
                Request.Builder request = Request.builder();
                request.putPathParams("packageName", packageName);
                request.putPathParams("packageVersion", packageVersion);
                return runtime.clients()
                        .block(runtime.clients()
                                .call(
                                        channel,
                                        NpmRepositoryEndpoints.getPackageJson,
                                        request.build(),
                                        getPackageJsonDeserializer));
            }
        };
    }
}
