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

package io.spring.start.site.ui;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import io.spring.initializr.generator.version.Version;
import io.spring.initializr.metadata.DefaultMetadataElement;
import io.spring.initializr.metadata.Dependency;
import io.spring.initializr.metadata.DependencyGroup;
import io.spring.initializr.metadata.InitializrMetadata;
import io.spring.initializr.metadata.InitializrMetadataProvider;
import io.spring.initializr.web.project.DefaultProjectRequestToDescriptionConverter;
import io.spring.initializr.web.project.ProjectGenerationInvoker;
import io.spring.initializr.web.project.ProjectGenerationResult;
import io.spring.initializr.web.project.ProjectRequest;
import io.spring.initializr.web.project.WebProjectRequest;

import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

/**
 * Bridge between the Vaadin UI and the Initializr core. Wraps
 * {@link InitializrMetadataProvider} for read access to the catalog and
 * {@link ProjectGenerationInvoker} for in-JVM project generation, so the UI does not have
 * to round-trip through {@code /metadata/client} or {@code /starter.zip}.
 *
 * @author Vaadin UI Migration
 */
public class InitializrUiService {

	private final InitializrMetadataProvider metadataProvider;

	private final ProjectGenerationInvoker<ProjectRequest> invoker;

	public InitializrUiService(InitializrMetadataProvider metadataProvider, ApplicationContext applicationContext) {
		this.metadataProvider = metadataProvider;
		this.invoker = new ProjectGenerationInvoker<>(applicationContext,
				new DefaultProjectRequestToDescriptionConverter());
	}

	public InitializrMetadata getMetadata() {
		return this.metadataProvider.get();
	}

	public List<DefaultMetadataElement> getTypes() {
		return getMetadata().getTypes().getContent().stream().map(InitializrUiService::toDefaultElement).toList();
	}

	public List<DefaultMetadataElement> getLanguages() {
		return getMetadata().getLanguages().getContent();
	}

	public List<DefaultMetadataElement> getBootVersions() {
		return getMetadata().getBootVersions().getContent();
	}

	public List<DefaultMetadataElement> getPackagings() {
		return getMetadata().getPackagings().getContent();
	}

	public List<DefaultMetadataElement> getJavaVersions() {
		return getMetadata().getJavaVersions().getContent();
	}

	public List<DefaultMetadataElement> getConfigurationFileFormats() {
		return getMetadata().getConfigurationFileFormats().getContent();
	}

	public InitializrFormModel defaultFormModel() {
		InitializrMetadata metadata = getMetadata();
		InitializrFormModel model = new InitializrFormModel();
		model.setType(defaultId(getTypes(), "maven-project"));
		model.setLanguage(defaultId(getLanguages(), "java"));
		model.setBootVersion(defaultId(getBootVersions(), null));
		model.setPackaging(defaultId(getPackagings(), "jar"));
		model.setJavaVersion(defaultId(getJavaVersions(), null));
		model.setConfigurationFileFormat(defaultId(getConfigurationFileFormats(), "properties"));
		model.setGroupId(metadata.getGroupId().getContent());
		model.setArtifactId(metadata.getArtifactId().getContent());
		model.setName(metadata.getName().getContent());
		model.setDescription(metadata.getDescription().getContent());
		model.setPackageName(metadata.getPackageName().getContent());
		return model;
	}

	/**
	 * Return all dependency groups, with each entry tagged as valid/invalid for the
	 * supplied Spring Boot version. Mirrors the React UI: incompatible dependencies are
	 * shown but disabled (with a "Requires Spring Boot …" hint), never hidden.
	 * @param bootVersionId currently-selected Spring Boot version (may be null). When
	 * null, every entry is reported as valid.
	 * @return all dependency groups in catalog order; each {@code items} list preserves
	 * catalog order too.
	 */
	public List<DependencyGroupEntry> dependencyGroups(String bootVersionId) {
		Version bootVersion = (StringUtils.hasText(bootVersionId)) ? Version.safeParse(bootVersionId) : null;
		List<DependencyGroupEntry> result = new ArrayList<>();
		for (DependencyGroup group : getMetadata().getDependencies().getContent()) {
			List<DependencyEntry> items = new ArrayList<>();
			for (Dependency dependency : group.getContent()) {
				items.add(toEntry(dependency, bootVersion));
			}
			result.add(new DependencyGroupEntry(group.getName(), items));
		}
		return result;
	}

	public Optional<Dependency> getDependency(String id) {
		return Optional.ofNullable(getMetadata().getDependencies().get(id));
	}

	/**
	 * Compatibility hint for a single dependency at the supplied boot version. Used by
	 * the chip list to flag an already-selected dependency as no-longer-compatible when
	 * the user switches the Spring Boot version.
	 * @param dependency the dependency to check.
	 * @param bootVersionId currently-selected Spring Boot version (may be null).
	 * @return {@code null} when compatible (or when no boot version is in play); a
	 * user-facing "Requires Spring Boot …" message otherwise.
	 */
	public String incompatibilityHint(Dependency dependency, String bootVersionId) {
		Version bootVersion = (StringUtils.hasText(bootVersionId)) ? Version.safeParse(bootVersionId) : null;
		return toEntry(dependency, bootVersion).invalidMessage();
	}

	private static DependencyEntry toEntry(Dependency dependency, Version bootVersion) {
		if (bootVersion == null || dependency.match(bootVersion)) {
			return new DependencyEntry(dependency, true, null);
		}
		String requirement = (dependency.getVersionRequirement() != null) ? dependency.getVersionRequirement()
				: dependency.getCompatibilityRange();
		String message = (requirement != null && !requirement.isBlank()) ? "Requires Spring Boot " + requirement
				: "Not compatible with the selected Spring Boot version";
		return new DependencyEntry(dependency, false, message);
	}

	public byte[] generateZip(InitializrFormModel model) {
		ProjectGenerationResult result = generate(model);
		try {
			return zip(result.getRootDirectory());
		}
		finally {
			this.invoker.cleanTempFiles(result.getRootDirectory());
		}
	}

	/**
	 * Generate the project and return its files as text for the Explore UI.
	 * @param model current form state.
	 * @return map of relative path → file content (text). Binary files are surfaced with
	 * a placeholder value to keep the Explore UI consistent.
	 */
	public Map<String, String> explore(InitializrFormModel model) {
		ProjectGenerationResult result = generate(model);
		try {
			return walkAsText(result.getRootDirectory());
		}
		finally {
			this.invoker.cleanTempFiles(result.getRootDirectory());
		}
	}

	private ProjectGenerationResult generate(InitializrFormModel model) {
		WebProjectRequest request = new WebProjectRequest();
		request.initialize(getMetadata());
		request.setType(model.getType());
		request.setLanguage(model.getLanguage());
		request.setBootVersion(model.getBootVersion());
		request.setGroupId(model.getGroupId());
		request.setArtifactId(model.getArtifactId());
		request.setName(model.getName());
		request.setDescription(model.getDescription());
		request.setPackageName(model.getPackageName());
		request.setPackaging(model.getPackaging());
		request.setJavaVersion(model.getJavaVersion());
		request.setConfigurationFileFormat(model.getConfigurationFileFormat());
		request.setBaseDir(model.getArtifactId());
		request.setDependencies(new ArrayList<>(model.getDependencies()));
		return this.invoker.invokeProjectStructureGeneration(request);
	}

	private byte[] zip(Path root) {
		try (ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				ZipOutputStream zip = new ZipOutputStream(buffer);
				Stream<Path> walk = Files.walk(root)) {
			for (Path path : (Iterable<Path>) walk::iterator) {
				if (path.equals(root)) {
					continue;
				}
				String entryName = root.relativize(path).toString().replace('\\', '/');
				if (Files.isDirectory(path)) {
					zip.putNextEntry(new ZipEntry(entryName + "/"));
					zip.closeEntry();
				}
				else {
					zip.putNextEntry(new ZipEntry(entryName));
					Files.copy(path, zip);
					zip.closeEntry();
				}
			}
			zip.finish();
			return buffer.toByteArray();
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private Map<String, String> walkAsText(Path root) {
		Map<String, String> files = new LinkedHashMap<>();
		try (Stream<Path> walk = Files.walk(root)) {
			for (Path path : (Iterable<Path>) walk::iterator) {
				if (Files.isDirectory(path) || path.equals(root)) {
					continue;
				}
				String relative = root.relativize(path).toString().replace('\\', '/');
				if (isLikelyText(path)) {
					files.put(relative, Files.readString(path, StandardCharsets.UTF_8));
				}
				else {
					files.put(relative, "(binary file, %d bytes)".formatted(Files.size(path)));
				}
			}
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
		return files;
	}

	private static boolean isLikelyText(Path path) {
		String name = path.getFileName().toString();
		return !name.endsWith(".jar") && !name.endsWith(".class") && !name.endsWith(".png") && !name.endsWith(".jpg")
				&& !name.endsWith(".gif") && !name.endsWith(".ico");
	}

	private static String defaultId(List<DefaultMetadataElement> elements, String fallback) {
		return elements.stream()
			.filter(DefaultMetadataElement::isDefault)
			.map(DefaultMetadataElement::getId)
			.findFirst()
			.orElse(fallback);
	}

	private static DefaultMetadataElement toDefaultElement(io.spring.initializr.metadata.Type type) {
		DefaultMetadataElement element = new DefaultMetadataElement();
		element.setId(type.getId());
		element.setName(type.getName());
		element.setDefault(type.isDefault());
		return element;
	}

	/**
	 * View of a single dependency for the picker / chip list, decorated with the
	 * Spring-Boot-version compatibility verdict.
	 *
	 * @param dependency the underlying Initializr {@link Dependency}.
	 * @param valid whether the dependency is compatible with the supplied Spring Boot
	 * version. {@code true} when no boot version is in play.
	 * @param invalidMessage user-facing hint when {@code valid} is {@code false} (e.g.
	 * {@code "Requires Spring Boot >=3.5.0 and <4.1.0-M1"}); {@code null} otherwise.
	 */
	public record DependencyEntry(Dependency dependency, boolean valid, String invalidMessage) {
	}

	/**
	 * Catalog group of {@link DependencyEntry dependencies} for the picker.
	 *
	 * @param name display name of the group (e.g. {@code "Web"}).
	 * @param items dependencies in this group, decorated with compatibility verdict.
	 */
	public record DependencyGroupEntry(String name, List<DependencyEntry> items) {
	}

}
