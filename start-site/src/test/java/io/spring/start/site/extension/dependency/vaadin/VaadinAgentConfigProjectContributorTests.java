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

package io.spring.start.site.extension.dependency.vaadin;

import java.nio.file.Path;

import io.spring.initializr.generator.test.project.ProjectStructure;
import io.spring.initializr.web.project.ProjectRequest;
import io.spring.start.site.SupportedBootVersion;
import io.spring.start.site.extension.AbstractExtensionTests;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

/**
 * Tests for {@link VaadinAgentConfigProjectContributor}.
 *
 * @author Sami Ekblad
 */
class VaadinAgentConfigProjectContributorTests extends AbstractExtensionTests {

	@Test
	void agentConfigIsContributedWithVaadin() {
		ProjectRequest request = createProjectRequest(SupportedBootVersion.V4_0, "vaadin");
		Path project = generateProject(request).getProjectDirectory();
		assertThat(project.resolve(".mcp.json")).isRegularFile();
		assertThat(project.resolve(".codex/config.toml")).isRegularFile();
		assertThat(project.resolve(".agents/skills/project-guide/SKILL.md")).isRegularFile();
		assertThat(project.resolve(".claude/skills/project-guide/SKILL.md")).isRegularFile();
		assertThat(project.resolve("AGENTS.md")).isRegularFile();
		assertThat(project.resolve("CLAUDE.md")).isRegularFile();
		assertThat(contentOf(project.resolve(".mcp.json").toFile())).contains("\"type\": \"http\"")
			.contains("https://mcp.vaadin.com/docs");
		assertThat(contentOf(project.resolve(".codex/config.toml").toFile())).contains("[mcp_servers.vaadin]")
			.contains("https://mcp.vaadin.com/docs");
	}

	@Test
	void skillCopiesAreIdentical() {
		ProjectRequest request = createProjectRequest(SupportedBootVersion.V4_0, "vaadin");
		Path project = generateProject(request).getProjectDirectory();
		String agents = contentOf(project.resolve(".agents/skills/project-guide/SKILL.md").toFile());
		String claude = contentOf(project.resolve(".claude/skills/project-guide/SKILL.md").toFile());
		assertThat(agents).isEqualTo(claude).contains("name: project-guide");
	}

	@Test
	void agentConfigIsNotContributedWithoutVaadin() {
		ProjectStructure structure = generateProject(createProjectRequest("web"));
		Path project = structure.getProjectDirectory();
		assertThat(project.resolve(".mcp.json")).doesNotExist();
		assertThat(project.resolve(".codex")).doesNotExist();
		assertThat(project.resolve(".agents")).doesNotExist();
		assertThat(project.resolve(".claude")).doesNotExist();
		assertThat(project.resolve("AGENTS.md")).doesNotExist();
		assertThat(project.resolve("CLAUDE.md")).doesNotExist();
	}

}
