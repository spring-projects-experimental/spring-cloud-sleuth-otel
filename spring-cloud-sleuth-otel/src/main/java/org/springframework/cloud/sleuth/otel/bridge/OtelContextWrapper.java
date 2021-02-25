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

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.BaggageBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextStorage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.sleuth.CurrentTraceContext;
import org.springframework.cloud.sleuth.TraceContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;

/**
 * OpenTelemetry implementation of a {@link CurrentTraceContext}.
 *
 */
public class OtelContextWrapper implements CurrentTraceContext {

	private static final Log log = LogFactory.getLog(OtelContextWrapper.class);

	public OtelContextWrapper(ApplicationEventPublisher publisher) {
		ContextStorage.addWrapper(contextStorage -> new ContextStorage() {
			@Override
			public io.opentelemetry.context.Scope attach(Context context) {
				Span currentSpan = Span.fromContextOrNull(Context.current());
				io.opentelemetry.context.Scope scope = contextStorage.attach(context);
				if (scope == io.opentelemetry.context.Scope.noop()) {
					return scope;
				}
				Span attachingSpan = Span.fromContext(context);
				publisher.publishEvent(new ScopeAttached(this, attachingSpan));
				return () -> {
					scope.close();
					publisher.publishEvent(new ScopeClosed(this));
					publisher.publishEvent(new ScopeRestored(this, currentSpan));
				};
			}

			@Override
			public Context current() {
				return contextStorage.current();
			}
		});
	}

	@Override
	public TraceContext context() {
		Span currentSpan = Span.current();
		if (Span.getInvalid().equals(currentSpan)) {
			return null;
		}
		if (currentSpan instanceof SpanFromSpanContext) {
			return new OtelTraceContext((SpanFromSpanContext) currentSpan);
		}
		return new OtelTraceContext(currentSpan);
	}

	@Override
	public Scope newScope(TraceContext context) {
		OtelTraceContext otelTraceContext = (OtelTraceContext) context;
		if (otelTraceContext == null) {
			return io.opentelemetry.context.Scope::noop;
		}
		Context current = Context.current();
		Context old = otelTraceContext.context();

		Span currentSpan = Span.fromContext(current);
		Span oldSpan = Span.fromContext(otelTraceContext.context());
		SpanContext spanContext = otelTraceContext.delegate;
		boolean sameSpan = currentSpan.getSpanContext().equals(oldSpan.getSpanContext())
				&& currentSpan.getSpanContext().equals(spanContext);
		SpanFromSpanContext fromContext = new SpanFromSpanContext(((OtelTraceContext) context).span, spanContext,
				otelTraceContext);

		Baggage currentBaggage = Baggage.fromContext(current);
		Baggage oldBaggage = Baggage.fromContext(old);
		boolean sameBaggage = sameBaggage(currentBaggage, oldBaggage);

		if (sameSpan && sameBaggage) {
			return io.opentelemetry.context.Scope::noop;
		}

		BaggageBuilder baggageBuilder = currentBaggage.toBuilder();
		oldBaggage.forEach(
				(key, baggageEntry) -> baggageBuilder.put(key, baggageEntry.getValue(), baggageEntry.getMetadata()));
		Baggage updatedBaggage = baggageBuilder.build();

		io.opentelemetry.context.Scope attach = old.with(fromContext).with(updatedBaggage).makeCurrent();
		return attach::close;
	}

	private boolean sameBaggage(Baggage currentBaggage, Baggage oldBaggage) {
		return currentBaggage.equals(oldBaggage);
	}

	@Override
	public Scope maybeScope(TraceContext context) {
		return newScope(context);
	}

	@Override
	public <C> Callable<C> wrap(Callable<C> task) {
		return Context.current().wrap(task);
	}

	@Override
	public Runnable wrap(Runnable task) {
		return Context.current().wrap(task);
	}

	@Override
	public Executor wrap(Executor delegate) {
		return Context.current().wrap(delegate);
	}

	@Override
	public ExecutorService wrap(ExecutorService delegate) {
		return Context.current().wrap(delegate);
	}

	static class ScopeAttached extends ApplicationEvent {

		/**
		 * Span corresponding to the attached scope. Might be {@code null}.
		 */
		final Span span;

		/**
		 * Create a new {@code ApplicationEvent}.
		 * @param source the object on which the event initially occurred or with which
		 * the event is associated (never {@code null})
		 * @param span corresponding trace context
		 */
		ScopeAttached(Object source, @Nullable Span span) {
			super(source);
			this.span = span;
		}

		@Override
		public String toString() {
			return "ScopeAttached{span=" + span + "}";
		}

	}

	static class ScopeRestored extends ApplicationEvent {

		/**
		 * Span corresponding to the scope being restored. Might be {@code null}.
		 */
		final Span span;

		/**
		 * Create a new {@code ApplicationEvent}.
		 * @param source the object on which the event initially occurred or with which
		 * the event is associated (never {@code null})
		 * @param span corresponding trace context
		 */
		ScopeRestored(Object source, @Nullable Span span) {
			super(source);
			this.span = span;
		}

		@Override
		public String toString() {
			return "ScopeRestored{span=" + span + "}";
		}

	}

	static class ScopeClosed extends ApplicationEvent {

		/**
		 * Create a new {@code ApplicationEvent}.
		 * @param source the object on which the event initially occurred or with which
		 * the event is associated (never {@code null})
		 */
		ScopeClosed(Object source) {
			super(source);
		}

	}

}
