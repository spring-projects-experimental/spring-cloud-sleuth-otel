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

package org.springframework.cloud.sleuth.autoconfig.otel.actuate;

import java.util.List;
import java.util.stream.Collectors;

import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import zipkin2.Call;
import zipkin2.codec.Encoding;
import zipkin2.reporter.Sender;

import org.springframework.cloud.sleuth.autoconfig.actuate.FinishedSpanWriter;
import org.springframework.cloud.sleuth.autoconfig.actuate.TextOutputFormat;
import org.springframework.cloud.sleuth.exporter.FinishedSpan;
import org.springframework.cloud.sleuth.otel.bridge.OtelFinishedSpan;

/**
 * Converts the {@link FinishedSpan}s into Zipkin JSONs.
 *
 * @author Marcin Grzejszczak
 * @since 1.1.0
 */
class OtelZipkinFinishedSpanWriter implements FinishedSpanWriter<String> {

	@Override
	public String write(TextOutputFormat format, List<FinishedSpan> spans) {
		if (format == TextOutputFormat.CONTENT_TYPE_OPENZIPKIN_JSON_V2) {
			List<SpanData> spanData = spans.stream().map(OtelFinishedSpan::toOtel).collect(Collectors.toList());
			ArraySender arraySender = new ArraySender();
			ZipkinSpanExporter exporter = ZipkinSpanExporter.builder().setSender(arraySender).build();
			exporter.export(spanData);
			return arraySender.convertedJson;
		}
		return null;
	}

	static class ArraySender extends Sender {

		String convertedJson;

		@Override
		public Encoding encoding() {
			return Encoding.JSON;
		}

		@Override
		public int messageMaxBytes() {
			return 0;
		}

		@Override
		public int messageSizeInBytes(List<byte[]> encodedSpans) {
			return 0;
		}

		@Override
		public Call<Void> sendSpans(List<byte[]> encodedSpans) {
			this.convertedJson = '[' + encodedSpans.stream().map(String::new).collect(Collectors.joining(",")) + ']';
			return Call.create(null);
		}

	}

}
