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

package io.spring.start.site.web;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Owns {@code GET /} for {@code text/html} requests so the Vaadin UI is served to
 * browsers. Initializr's {@code ProjectMetadataController} continues to own the JSON /
 * HAL variants of the same path (the curl/HTTPie contract documented in
 * {@code USING.adoc}), and Spring MVC's content negotiation routes each request to the
 * right handler. A redirect (not a forward) is used so the browser URL updates to
 * {@code /ui/} — that way Vaadin's host page resolves its relative resource paths (e.g.
 * {@code ./VAADIN/build/*.js}) correctly. The browser preserves the {@code #!}-share
 * fragment across the redirect, so existing share links still work.
 *
 * @author Vaadin UI Migration
 */
@Controller
public class HomeController {

	@GetMapping(path = "/", produces = MediaType.TEXT_HTML_VALUE)
	public String home() {
		return "redirect:/ui/";
	}

}
