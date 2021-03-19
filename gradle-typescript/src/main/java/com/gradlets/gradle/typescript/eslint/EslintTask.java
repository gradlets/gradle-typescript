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

package com.gradlets.gradle.typescript.eslint;

import com.gradlets.gradle.npm.NodeExec;
import com.gradlets.gradle.typescript.ObjectMappers;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Collectors;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.util.GFileUtils;

public abstract class EslintTask extends SourceTask {

    private static final String TYPESCRIPT_ESLINT_PARSER = "@typescript-eslint/parser";

    @Classpath
    abstract ConfigurableFileCollection getPluginClasspath();

    @Classpath
    abstract ConfigurableFileCollection getCompileClasspath();

    @Input
    abstract Property<Boolean> getShouldFix();

    @Input
    abstract MapProperty<String, Object> getParserOptions();

    @Input
    abstract ListProperty<String> getPlugins();

    @Input
    abstract ListProperty<String> getInheritedConfig();

    @Input
    abstract MapProperty<String, Object> getRules();

    @TaskAction
    public final void lint() throws IOException {
        File configFile = new File(getTemporaryDir(), ".eslintrc.json");
        GFileUtils.parentMkdirs(configFile);
        ObjectMappers.MAPPER.writeValue(configFile, createEslintRc());

        ConfigurableFileCollection unifiedClasspath = getProject().getObjects().fileCollection();
        unifiedClasspath.from(getPluginClasspath(), getCompileClasspath());
        NodeExec.exec(getProject(), getPluginClasspath(), execSpec -> {
            execSpec.setExecutable(getEslintExecutable(getPluginClasspath()));
            execSpec.args("--config", configFile.getAbsolutePath());
            execSpec.args(
                    getSource().getFiles().stream().map(File::getAbsolutePath).collect(Collectors.toSet()));

            if (getShouldFix().get()) {
                execSpec.args("--fix");
            }
        });
    }

    private EslintRc createEslintRc() {
        return EslintRc.builder()
                .parser(TYPESCRIPT_ESLINT_PARSER)
                .parserOptions(getParserOptions().get())
                .inheritedConfig(getInheritedConfig().get())
                .plugins(getPlugins().get())
                .rules(getRules().get())
                .build();
    }

    private static Path getEslintExecutable(FileCollection files) {
        return files.getFiles().stream()
                .filter(file -> file.getName().equals("eslint"))
                .map(file -> file.toPath().resolve("bin").resolve("eslint.js"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Eslint dependency must exists"));
    }
}
