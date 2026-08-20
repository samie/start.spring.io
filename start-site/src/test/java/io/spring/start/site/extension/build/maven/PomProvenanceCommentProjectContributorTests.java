/*
 * Copyright 2012 - present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.spring.start.site.extension.build.maven;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.spring.initializr.web.project.ProjectRequest;
import io.spring.start.site.extension.AbstractExtensionTests;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PomProvenanceCommentProjectContributor}.
 *
 * @author Sami Ekblad
 */
class PomProvenanceCommentProjectContributorTests extends AbstractExtensionTests {

	private static final String COMMENT = "<!-- Project from https://pro.startvaadin.com/ -->";

	@Test
	void provenanceCommentIsAddedRightAfterXmlDeclaration() throws IOException {
		ProjectRequest request = createProjectRequest("web");
		Path pom = generateProject(request).getProjectDirectory().resolve("pom.xml");
		String content = Files.readString(pom);
		assertThat(content).startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + COMMENT + "\n<project ");
	}

	@Test
	void provenanceCommentIsNotAddedForGradle() throws IOException {
		ProjectRequest request = createProjectRequest("web");
		request.setType("gradle-project");
		Path build = generateProject(request).getProjectDirectory().resolve("build.gradle");
		assertThat(Files.readString(build)).doesNotContain(COMMENT);
	}

}
