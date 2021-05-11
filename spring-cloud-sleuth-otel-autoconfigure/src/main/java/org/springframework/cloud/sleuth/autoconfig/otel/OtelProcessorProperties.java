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

import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OpenTelemetry span process properties.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
@ConfigurationProperties("spring.sleuth.otel.processor")
public class OtelProcessorProperties {

	private Batch batch = new Batch();

	public Batch getBatch() {
		return this.batch;
	}

	public void setBatch(Batch batch) {
		this.batch = batch;
	}

	/**
	 * Configuration of the {@link BatchSpanProcessor}.
	 */
	public static class Batch {

		/**
		 * Schedule delay in millis.
		 */
		private Long scheduleDelay;

		/**
		 * Max queue size.
		 */
		private Integer maxQueueSize;

		/**
		 * Max export batch size.
		 */
		private Integer maxExportBatchSize;

		/**
		 * Exporter timeout in millis.
		 */
		private Long exporterTimeout;

		public Long getScheduleDelay() {
			return scheduleDelay;
		}

		public void setScheduleDelay(long scheduleDelay) {
			this.scheduleDelay = scheduleDelay;
		}

		public Integer getMaxQueueSize() {
			return maxQueueSize;
		}

		public void setMaxQueueSize(int maxQueueSize) {
			this.maxQueueSize = maxQueueSize;
		}

		public Integer getMaxExportBatchSize() {
			return maxExportBatchSize;
		}

		public void setMaxExportBatchSize(int maxExportBatchSize) {
			this.maxExportBatchSize = maxExportBatchSize;
		}

		public Long getExporterTimeout() {
			return exporterTimeout;
		}

		public void setExporterTimeout(long exporterTimeout) {
			this.exporterTimeout = exporterTimeout;
		}

	}

}
