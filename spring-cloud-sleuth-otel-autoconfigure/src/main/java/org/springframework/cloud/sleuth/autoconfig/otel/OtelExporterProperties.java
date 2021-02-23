/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.cloud.sleuth.autoconfig.otel;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Sleuth settings for OpenTelemetry.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
@ConfigurationProperties("spring.sleuth.otel.exporter")
public class OtelExporterProperties {

	private SleuthSpanFilter sleuthSpanFilter = new SleuthSpanFilter();

	private Otlp otlp = new Otlp();

	private Jaeger jaeger = new Jaeger();

	public SleuthSpanFilter getSleuthSpanFilter() {
		return this.sleuthSpanFilter;
	}

	public void setSleuthSpanFilter(SleuthSpanFilter sleuthSpanFilter) {
		this.sleuthSpanFilter = sleuthSpanFilter;
	}

	public Otlp getOtlp() {
		return this.otlp;
	}

	public void setOtlp(Otlp otlp) {
		this.otlp = otlp;
	}

	public Jaeger getJaeger() {
		return this.jaeger;
	}

	public void setJaeger(Jaeger jaeger) {
		this.jaeger = jaeger;
	}

	/**
	 * Integrations with core Sleuth handler mechanism.
	 */
	public static class SleuthSpanFilter {

		/**
		 * Enables Sleuth span filter.
		 */
		private boolean enabled = true;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}

	/**
	 * Integrations with OTLP exporter.
	 */
	public static class Otlp {

		/**
		 * Timeout in millis.
		 */
		private Long timeout;

		/**
		 * Sets the OTLP endpoint to connect to.
		 */
		private String endpoint;

		/**
		 * Map of headers to be added.
		 */
		private Map<String, String> headers = new HashMap<>();

		public Long getTimeout() {
			return this.timeout;
		}

		public void setTimeout(Long timeout) {
			this.timeout = timeout;
		}

		public String getEndpoint() {
			return this.endpoint;
		}

		public void setEndpoint(String endpoint) {
			this.endpoint = endpoint;
		}

		public Map<String, String> getHeaders() {
			return this.headers;
		}

		public void setHeaders(Map<String, String> headers) {
			this.headers = headers;
		}

	}

	/**
	 * Integrations with Jaeger exporter.
	 */
	public static class Jaeger {

		/**
		 * Timeout in millis.
		 */
		private Long timeout;

		/**
		 * Sets the Jaeger endpoint to connect to.
		 */
		private String endpoint;

		public Long getTimeout() {
			return this.timeout;
		}

		public void setTimeout(Long timeout) {
			this.timeout = timeout;
		}

		public String getEndpoint() {
			return this.endpoint;
		}

		public void setEndpoint(String endpoint) {
			this.endpoint = endpoint;
		}

	}

}
