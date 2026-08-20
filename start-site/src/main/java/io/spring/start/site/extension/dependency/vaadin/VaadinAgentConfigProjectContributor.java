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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import io.spring.initializr.generator.project.contributor.ProjectContributor;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

/**
 * A {@link ProjectContributor} that adds agent configuration (Claude Code and OpenAI
 * Codex) to generated Vaadin projects. It configures the public Vaadin MCP server, a
 * shared "project-guide" skill, and per-agent instruction files so the project is ready
 * to use with AI coding agents without manual setup.
 *
 * @author Sami Ekblad
 */
class VaadinAgentConfigProjectContributor implements ProjectContributor {

	private static final String RESOURCE_BASE = "vaadin/agent-config/";

	@Override
	public void contribute(Path projectRoot) throws IOException {
		// The two SKILL.md copies are generated from a single source so they cannot
		// drift.
		String skill = readResource("SKILL.md");
		writeString(projectRoot.resolve(".agents/skills/project-guide/SKILL.md"), skill);
		writeString(projectRoot.resolve(".claude/skills/project-guide/SKILL.md"), skill);
		writeString(projectRoot.resolve(".mcp.json"), readResource("mcp.json"));
		writeString(projectRoot.resolve(".codex/config.toml"), readResource("codex-config.toml"));
		writeString(projectRoot.resolve("AGENTS.md"), readResource("AGENTS.md"));
		writeString(projectRoot.resolve("CLAUDE.md"), readResource("CLAUDE.md"));
	}

	private String readResource(String name) throws IOException {
		try (InputStream input = new ClassPathResource(RESOURCE_BASE + name).getInputStream()) {
			return StreamUtils.copyToString(input, StandardCharsets.UTF_8);
		}
	}

	private void writeString(Path target, String content) throws IOException {
		Files.createDirectories(target.getParent());
		Files.writeString(target, content);
	}

}
