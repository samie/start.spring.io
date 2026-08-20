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

import io.spring.initializr.generator.project.contributor.ProjectContributor;

import org.springframework.core.Ordered;

/**
 * A {@link ProjectContributor} that adds a provenance comment to the generated
 * {@code pom.xml}. The Maven build model has no API to emit a top-level comment, so this
 * runs after {@code pom.xml} has been written and inserts the comment right after the XML
 * declaration.
 *
 * @author Sami Ekblad
 */
class PomProvenanceCommentProjectContributor implements ProjectContributor {

	private static final String COMMENT = "<!-- Project from https://pro.startvaadin.com/ -->";

	@Override
	public void contribute(Path projectRoot) throws IOException {
		Path pom = projectRoot.resolve("pom.xml");
		if (!Files.exists(pom)) {
			return;
		}
		String content = Files.readString(pom);
		if (content.contains(COMMENT)) {
			return;
		}
		int newline = content.indexOf('\n');
		String result;
		if (newline >= 0 && content.startsWith("<?xml")) {
			// Insert right after the XML declaration line.
			result = content.substring(0, newline + 1) + COMMENT + "\n" + content.substring(newline + 1);
		}
		else {
			result = COMMENT + "\n" + content;
		}
		Files.writeString(pom, result);
	}

	@Override
	public int getOrder() {
		// Run after the pom.xml has been written by MavenBuildProjectContributor (order
		// 0).
		return Ordered.LOWEST_PRECEDENCE;
	}

}
