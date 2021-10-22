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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import io.opentelemetry.exporter.otlp.internal.traces.ResourceSpansMarshaler;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.sdk.trace.data.SpanData;

import org.springframework.cloud.sleuth.autoconfig.actuate.FinishedSpanWriter;
import org.springframework.cloud.sleuth.autoconfig.actuate.TextOutputFormat;
import org.springframework.cloud.sleuth.exporter.FinishedSpan;
import org.springframework.cloud.sleuth.otel.bridge.OtelFinishedSpan;

/**
 * Converts the {@link FinishedSpan}s into OTLP bytes.
 *
 * @author Marcin Grzejszczak
 * @since 1.1.0
 */
class OtelOtlpFinishedSpanWriter implements FinishedSpanWriter<byte[]> {

	@Override
	public byte[] write(TextOutputFormat format, List<FinishedSpan> spans) {
		if (format == TextOutputFormat.CONTENT_TYPE_OTLP_PROTOBUF) {
			List<SpanData> spanData = spans.stream().map(OtelFinishedSpan::toOtel).collect(Collectors.toList());
			List<ResourceSpans> resourceSpans = toProtoResourceSpans(spanData);
			if (resourceSpans.isEmpty()) {
				return null;
			}
			// We can safely assume that there's only 1 TracerProvider so the list will
			// contain 1 element
			ResourceSpans resources = resourceSpans.get(0);
			return resources.toByteArray();
		}
		return null;
	}

	private List<ResourceSpans> toProtoResourceSpans(List<SpanData> spanData) {
		return Arrays.stream(ResourceSpansMarshaler.create(spanData)).map(this::toResourceSpans)
				.collect(Collectors.toList());
	}

	private ResourceSpans toResourceSpans(ResourceSpansMarshaler resourceSpansMarshaler) {
		try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			resourceSpansMarshaler.writeBinaryTo(os);
			return ResourceSpans.parseFrom(os.toByteArray());
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
