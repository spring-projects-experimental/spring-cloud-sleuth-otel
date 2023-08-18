/*
 * Copyright 2013-2021 the original author or authors.
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

package org.springframework.cloud.sleuth.otel.bridge;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import io.opentelemetry.instrumentation.api.instrumenter.http.HttpServerAttributesGetter;

import org.springframework.cloud.sleuth.http.HttpServerRequest;
import org.springframework.cloud.sleuth.http.HttpServerResponse;
import org.springframework.lang.Nullable;

/**
 * Extracts OpenTelemetry http semantic attributes value for server http spans.
 *
 * @author Nikita Salnikov-Tarnovski
 */
public class SpringHttpServerAttributesGetter
		implements HttpServerAttributesGetter<HttpServerRequest, HttpServerResponse> {

	@Nullable
	@Override
	public String getHttpRoute(HttpServerRequest httpServerRequest) {
		return httpServerRequest.route();
	}

	@Nullable
	@Override
	public String getUrlScheme(HttpServerRequest httpServerRequest) {
		String url = httpServerRequest.url();
		if (url == null) {
			return null;
		}
		if (url.startsWith("https:")) {
			return "https";
		}
		if (url.startsWith("http:")) {
			return "http";
		}
		return null;
	}

	@Override
	public String getUrlPath(HttpServerRequest httpServerRequest) {
		URI uri = toUri(httpServerRequest);
		if (uri == null) {
			return null;
		}
		return uri.getPath();
	}

	@Override
	public String getUrlQuery(HttpServerRequest httpServerRequest) {
		URI uri = toUri(httpServerRequest);
		if (uri == null) {
			return null;
		}
		return uri.getQuery();
	}

	private URI toUri(HttpServerRequest request) {
		String url = request.url();
		return url == null ? null : URI.create(url);
	}

	@Nullable
	@Override
	public String getHttpRequestMethod(HttpServerRequest httpServerRequest) {
		return httpServerRequest.method();
	}

	@Override
	public List<String> getHttpRequestHeader(HttpServerRequest httpServerRequest, String name) {
		String value = httpServerRequest.header(name);
		return value == null ? Collections.emptyList() : Collections.singletonList(value);
	}

	@Override
	public Integer getHttpResponseStatusCode(HttpServerRequest httpServerRequest, HttpServerResponse httpServerResponse,
			Throwable error) {
		return httpServerResponse.statusCode();
	}

	@Override
	public List<String> getHttpResponseHeader(HttpServerRequest httpServerRequest,
			HttpServerResponse httpServerResponse, String name) {
		String value = httpServerResponse.header(name);
		return value == null ? Collections.emptyList() : Collections.singletonList(value);
	}

}
