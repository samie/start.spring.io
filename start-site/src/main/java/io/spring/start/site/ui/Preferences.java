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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.WebStorage;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads / writes browser localStorage via Vaadin's async {@code WebStorage} API, using
 * the same key names as the React UI so existing browsers retain their data.
 *
 * <ul>
 * <li>{@code histories} — list of {@link Entry} snapshots, last-generated first, max
 * 100</li>
 * <li>{@code favorites} — list of {@link Entry} snapshots, named by the user</li>
 * <li>{@code springtheme} — {@code "dark"} or {@code "light"}</li>
 * </ul>
 *
 * @author Vaadin UI Migration
 */
public class Preferences {

	/** localStorage key for the histories list (matches the React UI). */
	public static final String HISTORIES_KEY = "histories";

	/** localStorage key for the favorites list (matches the React UI). */
	public static final String FAVORITES_KEY = "favorites";

	/** localStorage key for the theme preference (matches the React UI). */
	public static final String THEME_KEY = "springtheme";

	/** Cap on history list size, matching the React UI's behavior. */
	public static final int MAX_HISTORIES = 100;

	private final JsonMapper jsonMapper;

	public Preferences(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	public void loadHistories(Consumer<List<Entry>> callback) {
		loadEntries(HISTORIES_KEY, callback);
	}

	public void loadFavorites(Consumer<List<Entry>> callback) {
		loadEntries(FAVORITES_KEY, callback);
	}

	public void loadTheme(Consumer<String> callback) {
		WebStorage.getItem(THEME_KEY, callback::accept);
	}

	public void saveHistories(List<Entry> entries) {
		writeJson(HISTORIES_KEY, capHistories(entries));
	}

	public void saveFavorites(List<Entry> entries) {
		writeJson(FAVORITES_KEY, entries);
	}

	public void saveTheme(String theme) {
		WebStorage.setItem(THEME_KEY, theme);
	}

	private void loadEntries(String key, Consumer<List<Entry>> callback) {
		WebStorage.getItem(key, (json) -> callback.accept(parseEntries(json)));
	}

	private List<Entry> parseEntries(String json) {
		if (json == null || json.isBlank()) {
			return new ArrayList<>();
		}
		try {
			List<Entry> entries = this.jsonMapper.readValue(json, new TypeReference<>() {
			});
			return (entries != null) ? entries : new ArrayList<>();
		}
		catch (RuntimeException ex) {
			return new ArrayList<>();
		}
	}

	private void writeJson(String key, Object value) {
		try {
			WebStorage.setItem(key, this.jsonMapper.writeValueAsString(value));
		}
		catch (RuntimeException ex) {
			// best-effort; UI continues without persistence
		}
	}

	private static List<Entry> capHistories(List<Entry> entries) {
		if (entries.size() <= MAX_HISTORIES) {
			return entries;
		}
		return new ArrayList<>(entries.subList(0, MAX_HISTORIES));
	}

	/**
	 * Create an entry snapshot from the current form state.
	 * @param name display name for the entry.
	 * @param timestamp creation time in millis.
	 * @param model the form model to snapshot.
	 * @return a new immutable entry.
	 */
	public static Entry snapshot(String name, long timestamp, InitializrFormModel model) {
		return new Entry(name, timestamp, model.getType(), model.getLanguage(), model.getBootVersion(),
				model.getGroupId(), model.getArtifactId(), model.getName(), model.getDescription(),
				model.getPackageName(), model.getPackaging(), model.getJavaVersion(),
				model.getConfigurationFileFormat(), new ArrayList<>(model.getDependencies()));
	}

	/**
	 * Apply an entry's snapshot to the supplied model in place.
	 * @param entry the entry to read.
	 * @param model the model to mutate.
	 */
	public static void apply(Entry entry, InitializrFormModel model) {
		model.setType(entry.type());
		model.setLanguage(entry.language());
		model.setBootVersion(entry.bootVersion());
		model.setGroupId(entry.groupId());
		model.setArtifactId(entry.artifactId());
		model.setName(entry.projectName());
		model.setDescription(entry.description());
		model.setPackageName(entry.packageName());
		model.setPackaging(entry.packaging());
		model.setJavaVersion(entry.javaVersion());
		model.setConfigurationFileFormat(entry.configurationFileFormat());
		model.setDependencies(new LinkedHashSet<>(entry.dependencies()));
	}

	public static void runOnUi(UI ui, Runnable task) {
		if (ui != null) {
			ui.access(task::run);
		}
		else {
			task.run();
		}
	}

	/**
	 * Serializable snapshot of a form model. Field names line up with the React
	 * representation so a future React→Vaadin localStorage migration is trivial.
	 *
	 * @param name display name (e.g. artifactId)
	 * @param timestamp creation time in millis
	 * @param type project type id
	 * @param language language id
	 * @param bootVersion Spring Boot version id
	 * @param groupId Maven groupId
	 * @param artifactId Maven artifactId
	 * @param projectName the project name (model's "name" property)
	 * @param description project description
	 * @param packageName base package
	 * @param packaging packaging id
	 * @param javaVersion Java version id
	 * @param configurationFileFormat configuration file format id
	 * @param dependencies dependency ids
	 */
	public record Entry(String name, long timestamp, String type, String language, String bootVersion, String groupId,
			String artifactId, String projectName, String description, String packageName, String packaging,
			String javaVersion, String configurationFileFormat, List<String> dependencies) {

		public Entry name(String newName) {
			return new Entry(newName, this.timestamp, this.type, this.language, this.bootVersion, this.groupId,
					this.artifactId, this.projectName, this.description, this.packageName, this.packaging,
					this.javaVersion, this.configurationFileFormat, this.dependencies);
		}

	}

}
