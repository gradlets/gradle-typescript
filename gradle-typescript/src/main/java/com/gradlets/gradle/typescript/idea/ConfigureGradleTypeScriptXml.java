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

package com.gradlets.gradle.typescript.idea;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class ConfigureGradleTypeScriptXml {
    private ConfigureGradleTypeScriptXml() {}

    static void configureTypeScriptCompiler(Element rootElement, Path tscLocation) {
        Element tscComponent = matchOrCreateChild(rootElement, "component", Map.of("name", "TypeScriptCompiler"));
        Element forProjectsWithoutConfig =
                matchOrCreateChild(tscComponent, "option", Map.of("name", "enableServiceForProjectsWithoutConfig"));
        forProjectsWithoutConfig.setAttribute("value", "false");
        Element versionType = matchOrCreateChild(tscComponent, "option", Map.of("name", "versionType"));
        versionType.setAttribute("value", "SERVICE_DIRECTORY");
        Element tscDirectory = matchOrCreateChild(tscComponent, "option", Map.of("name", "typeScriptServiceDirectory"));
        tscDirectory.setAttribute("value", tscLocation.toString());
    }

    private static Element matchOrCreateChild(Element base, String name, Map<String, String> attributes) {
        NodeList elementsByName = base.getElementsByTagName(name);
        return IntStream.range(0, elementsByName.getLength())
                .filter(elemIdx -> {
                    NamedNodeMap nodeAttributes = elementsByName.item(elemIdx).getAttributes();
                    return attributes.entrySet().stream().allMatch(entry -> entry.getValue()
                            .equals(Optional.ofNullable(nodeAttributes.getNamedItem(entry.getKey()))
                                    .map(Node::getNodeValue)
                                    .orElse(null)));
                })
                .mapToObj(elemIdx -> (Element) elementsByName.item(elemIdx))
                .findFirst()
                .orElseGet(() -> {
                    Element element = base.getOwnerDocument().createElement(name);
                    attributes.forEach(element::setAttribute);
                    base.appendChild(element);
                    return element;
                });
    }
}
