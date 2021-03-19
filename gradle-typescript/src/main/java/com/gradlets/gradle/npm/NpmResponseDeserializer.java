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

import com.palantir.conjure.java.api.errors.UnknownRemoteException;
import com.palantir.dialogue.Deserializer;
import com.palantir.dialogue.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

enum NpmResponseDeserializer implements Deserializer<Void> {
    INSTANCE;

    @Override
    public Void deserialize(Response response) {
        // We should not fail if a server that previously returned nothing starts returning a response
        try (Response unused = response) {
            if (300 <= response.code() && response.code() <= 599) {
                String body;
                try {
                    body = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                } catch (NullPointerException | IOException e) {
                    UnknownRemoteException exception = new UnknownRemoteException(response.code(), "<unparseable>");
                    exception.initCause(e);
                    throw exception;
                }

                throw new RuntimeException(body);
            }
            return null;
        }
    }

    @Override
    public Optional<String> accepts() {
        return Optional.empty();
    }
}
