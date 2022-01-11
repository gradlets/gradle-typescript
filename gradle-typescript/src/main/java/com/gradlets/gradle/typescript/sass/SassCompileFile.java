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

package com.gradlets.gradle.typescript.sass;

import com.google.common.io.Files;
import com.palantir.logsafe.SafeArg;
import com.palantir.logsafe.exceptions.SafeRuntimeException;
import io.bit3.jsass.CompilationException;
import io.bit3.jsass.Compiler;
import io.bit3.jsass.Options;
import io.bit3.jsass.Output;
import io.bit3.jsass.context.FileContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.gradle.workers.WorkAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SassCompileFile implements WorkAction<SassWorkParameters> {
    private static final Logger log = LoggerFactory.getLogger(SassCompileFile.class);

    @Override
    public final void execute() {
        final Compiler compiler = new Compiler();
        final Output output;
        try {
            output = compiler.compile(new FileContext(
                    getParameters().inputFile().getAsFile().get().toURI(),
                    getParameters().outputFile().getAsFile().get().toURI(),
                    new Options()));
            Files.asCharSink(getParameters().outputFile().getAsFile().get(), StandardCharsets.UTF_8)
                    .write(output.getCss());
        } catch (IOException e) {
            log.error("Unable to write css file", e);
            throw new SafeRuntimeException(
                    "Unable to write css file",
                    SafeArg.of("file", getParameters().outputFile()));
        } catch (CompilationException e) {
            log.error("Error compiling scss", e);
            throw new SafeRuntimeException(
                    "Unable to compile scss",
                    SafeArg.of("file", getParameters().outputFile()),
                    SafeArg.of("message", e.getErrorMessage()));
        }
    }
}
