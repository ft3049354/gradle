/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



package org.gradle.api.internal.artifacts.repositories.resolver

import org.gradle.api.artifacts.ArtifactIdentifier
import org.gradle.api.internal.artifacts.DefaultArtifactIdentifier
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier
import spock.lang.Specification

class M2ResourcePatternTest extends Specification {
    def "substitutes artifact attributes into pattern"() {
        def pattern = new M2ResourcePattern("prefix/[organisation]/[module]/[revision]/[type]s/[revision]/[artifact].[ext]")
        final String group = "group"
        final String name = "projectA"
        final String version = "1.2"
        def artifact1 = artifactId(group, name, version)
        def artifact2 = artifactId("org.group", "projectA", "1.2")

        expect:
        pattern.toPath(artifact1) == 'prefix/group/projectA/1.2/ivys/1.2/ivy.xml'
        pattern.toPath(artifact2) == 'prefix/org/group/projectA/1.2/ivys/1.2/ivy.xml'
    }

    def "substitutes module attributes into pattern to determine module pattern"() {
        def pattern = new M2ResourcePattern("prefix/[organisation]/[module]/[revision]/[type]s/[revision]/[artifact].[ext]")
        def artifact1 = artifactId("group", "projectA", "1.2")
        def artifact2 = artifactId("org.group", "projectA", "1.2")

        expect:
        pattern.toVersionListPattern(artifact1) == 'prefix/group/projectA/[revision]/ivys/[revision]/ivy.xml'
        pattern.toVersionListPattern(artifact2) == 'prefix/org/group/projectA/[revision]/ivys/[revision]/ivy.xml'
    }

    def "can build module path"() {
        def pattern = new M2ResourcePattern("prefix/" + MavenPattern.M2_PATTERN)
        def module1 = new DefaultModuleIdentifier("group", "projectA")
        def module2 = new DefaultModuleIdentifier("org.group", "projectA")

        expect:
        pattern.toModulePath(module1) == 'prefix/group/projectA'
        pattern.toModulePath(module2) == 'prefix/org/group/projectA'
    }

    def "can build module version path"() {
        def pattern = new M2ResourcePattern("prefix/" + MavenPattern.M2_PATTERN)
        def artifact1 = artifactId("group", "projectA", "1.2")
        def artifact2 = artifactId("org.group", "projectA", "1.2")

        expect:
        pattern.toModuleVersionPath(artifact1) == 'prefix/group/projectA/1.2'
        pattern.toModuleVersionPath(artifact2) == 'prefix/org/group/projectA/1.2'
    }

    def "throws UnsupportedOperationException for non M2 compatible pattern"() {
        def pattern = new M2ResourcePattern("/non/m2/pattern")

        when:
        pattern.toModulePath(new DefaultModuleIdentifier("group", "module"))

        then:
        thrown(UnsupportedOperationException)
    }

    private static ArtifactIdentifier artifactId(String group, String name, String version) {
        return new DefaultArtifactIdentifier(DefaultModuleVersionIdentifier.newId(group, name, version), "ivy", "ivy", "xml", null)
    }
}
