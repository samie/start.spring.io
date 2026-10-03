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

package io.spring.start.site;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import io.spring.initializr.metadata.BillOfMaterials;
import io.spring.start.testsupport.Homes;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class DependencyResolverTests {

	@TempDir
	Path directory;

	@Test
	void resolvesRuntimeDependencies() throws IOException {
		writePom("starter", """
				<dependencies>
					<dependency>
						<groupId>org.example</groupId>
						<artifactId>runtime</artifactId>
						<version>1.0</version>
						<scope>runtime</scope>
					</dependency>
					<dependency>
						<groupId>org.example</groupId>
						<artifactId>optional</artifactId>
						<version>1.0</version>
						<optional>true</optional>
					</dependency>
					<dependency>
						<groupId>org.example</groupId>
						<artifactId>test</artifactId>
						<version>1.0</version>
						<scope>test</scope>
					</dependency>
				</dependencies>
				""");
		writePom("runtime", "");
		writePom("optional", "");
		writePom("test", "");
		assertThat(resolve("starter", "1.0", List.of())).containsExactly("org.example:starter", "org.example:runtime");
	}

	@Test
	void resolvesVersionManagedByBom() throws IOException {
		writePom("managed", "");
		writePom("bom", """
				<dependencyManagement>
					<dependencies>
						<dependency>
							<groupId>org.example</groupId>
							<artifactId>managed</artifactId>
							<version>1.0</version>
						</dependency>
					</dependencies>
				</dependencyManagement>
				""");
		assertThat(resolve("managed", null, List.of(BillOfMaterials.create("org.example", "bom", "1.0"))))
			.containsExactly("org.example:managed");
	}

	private List<String> resolve(String artifactId, String version, List<BillOfMaterials> boms) {
		Homes homes = mock(Homes.class);
		given(homes.get()).willReturn(this.directory.resolve("home"));
		RemoteRepository repository = DependencyResolver.createRemoteRepository("test",
				this.directory.resolve("remote").toUri().toString(), false);
		return DependencyResolver.resolveDependencies(homes, "org.example", artifactId, version, boms,
				List.of(repository));
	}

	private void writePom(String artifactId, String content) throws IOException {
		Path path = this.directory.resolve("remote/org/example/" + artifactId + "/1.0/" + artifactId + "-1.0.pom");
		Files.createDirectories(path.getParent());
		Files.writeString(path, """
				<project xmlns="http://maven.apache.org/POM/4.0.0">
					<modelVersion>4.0.0</modelVersion>
					<groupId>org.example</groupId>
					<artifactId>%s</artifactId>
					<version>1.0</version>
					<packaging>pom</packaging>
					%s
				</project>
				""".formatted(artifactId, content));
	}

}
