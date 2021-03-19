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

import com.gradlets.gradle.typescript.shim.NpmArtifactoryShim;
import java.util.concurrent.ConcurrentHashMap;
import org.gradle.api.invocation.Gradle;

final class ShimManager {
    private static ConcurrentHashMap<String, NpmArtifactoryShim.ShimServer> shims = new ConcurrentHashMap<>();

    static NpmArtifactoryShim.ShimServer getOrCreateShim(Gradle gradle, String url) {
        return shims.computeIfAbsent(url, _url -> {
            NpmArtifactoryShim.ShimServer shimServer = NpmArtifactoryShim.startServer(url);
            BuildListeners.onBuildFinish(gradle, () -> {
                shims.remove(url);
                shimServer.close();
            });
            return shimServer;
        });
    }

    private ShimManager() {}
}
