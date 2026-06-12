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

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Serializes / deserializes the form model to the {@code #!key=value&...} fragment format
 * the React UI documents in {@code USING.adoc}. Preserving this contract means existing
 * share links keep working after the migration.
 *
 * @author Vaadin UI Migration
 */
public final class ShareUrlCodec {

	private ShareUrlCodec() {
	}

	/**
	 * Encode the form model as a {@code #!}-prefixed fragment string ready to append to a
	 * base URL.
	 * @param model the source model.
	 * @return the share fragment (always starts with {@code #!}).
	 */
	public static String encode(InitializrFormModel model) {
		Map<String, String> params = new LinkedHashMap<>();
		put(params, "type", model.getType());
		put(params, "language", model.getLanguage());
		put(params, "bootVersion", model.getBootVersion());
		put(params, "groupId", model.getGroupId());
		put(params, "artifactId", model.getArtifactId());
		put(params, "name", model.getName());
		put(params, "description", model.getDescription());
		put(params, "packageName", model.getPackageName());
		put(params, "packaging", model.getPackaging());
		put(params, "javaVersion", model.getJavaVersion());
		put(params, "configurationFileFormat", model.getConfigurationFileFormat());
		if (model.getDependencies() != null && !model.getDependencies().isEmpty()) {
			params.put("dependencies", String.join(",", model.getDependencies()));
		}
		StringBuilder builder = new StringBuilder("#!");
		boolean first = true;
		for (Map.Entry<String, String> entry : params.entrySet()) {
			if (!first) {
				builder.append('&');
			}
			builder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
				.append('=')
				.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
			first = false;
		}
		return builder.toString();
	}

	/**
	 * Apply the values in a {@code #!}-prefixed fragment to the supplied model. Unknown
	 * keys are ignored; missing keys leave the existing model value.
	 * @param hash the fragment string read from {@code window.location.hash} (may be
	 * {@code null}, empty, or with/without the leading {@code #!}).
	 * @param model the model to mutate in place.
	 * @return {@code true} if the fragment was recognized and applied, {@code false}
	 * otherwise (caller can use this to decide whether to clear the hash).
	 */
	public static boolean decodeInto(String hash, InitializrFormModel model) {
		if (hash == null || hash.isEmpty()) {
			return false;
		}
		String body = hash;
		if (body.startsWith("#!")) {
			body = body.substring(2);
		}
		else if (body.startsWith("#")) {
			body = body.substring(1);
		}
		if (body.isEmpty()) {
			return false;
		}
		Map<String, String> params = new LinkedHashMap<>();
		for (String part : body.split("&")) {
			int eq = part.indexOf('=');
			if (eq <= 0) {
				continue;
			}
			String key = URLDecoder.decode(part.substring(0, eq), StandardCharsets.UTF_8);
			String value = URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8);
			params.put(key, value);
		}
		if (params.isEmpty()) {
			return false;
		}
		applyIfPresent(params, "type", model::setType);
		applyIfPresent(params, "language", model::setLanguage);
		applyIfPresent(params, "bootVersion", model::setBootVersion);
		applyIfPresent(params, "groupId", model::setGroupId);
		applyIfPresent(params, "artifactId", model::setArtifactId);
		applyIfPresent(params, "name", model::setName);
		applyIfPresent(params, "description", model::setDescription);
		applyIfPresent(params, "packageName", model::setPackageName);
		applyIfPresent(params, "packaging", model::setPackaging);
		applyIfPresent(params, "javaVersion", model::setJavaVersion);
		applyIfPresent(params, "configurationFileFormat", model::setConfigurationFileFormat);
		String dependencies = params.get("dependencies");
		if (dependencies != null) {
			Set<String> ids = new LinkedHashSet<>();
			for (String id : dependencies.split(",")) {
				if (!id.isBlank()) {
					ids.add(id.trim());
				}
			}
			model.setDependencies(ids);
		}
		return true;
	}

	private static void put(Map<String, String> params, String key, String value) {
		if (value != null && !value.isBlank()) {
			params.put(key, value);
		}
	}

	private static void applyIfPresent(Map<String, String> params, String key,
			java.util.function.Consumer<String> setter) {
		String value = params.get(key);
		if (value != null) {
			setter.accept(value);
		}
	}

}
