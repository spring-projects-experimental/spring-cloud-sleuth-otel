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

package org.springframework.cloud.sleuth.otel.bridge;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.sleuth.Tracer;

class OtelSpanInScope implements Tracer.SpanInScope {

	private static final Log log = LogFactory.getLog(OtelSpanInScope.class);

	final Scope delegate;

	final OtelSpan sleuthSpan;

	final io.opentelemetry.api.trace.Span otelSpan;

	final SpanContext spanContext;

	OtelSpanInScope(OtelSpan sleuthSpan, io.opentelemetry.api.trace.Span otelSpan) {
		this.sleuthSpan = sleuthSpan;
		this.otelSpan = otelSpan;
		this.delegate = storedContext(otelSpan);
		this.spanContext = otelSpan.getSpanContext();
	}

	private Scope storedContext(io.opentelemetry.api.trace.Span otelSpan) {
		if (otelSpan instanceof SpanFromSpanContext) {
			SpanFromSpanContext spanFromSpanContext = (SpanFromSpanContext) otelSpan;
			return spanFromSpanContext.otelTraceContext.context().makeCurrent();
		}
		if (this.sleuthSpan == null || this.sleuthSpan.context() == null
				|| this.sleuthSpan.context().context() == null) {
			return otelSpan.makeCurrent();
		}
		Context context = this.sleuthSpan.context().context();
		return context.with(otelSpan).makeCurrent();
	}

	@Override
	public void close() {
		if (log.isTraceEnabled()) {
			log.trace("Will close scope for trace context [" + this.spanContext + "]");
		}
		this.delegate.close();
	}

}
