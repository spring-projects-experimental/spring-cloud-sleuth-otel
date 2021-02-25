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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.MDC;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

public class Slf4jApplicationListener implements ApplicationListener<ApplicationEvent> {

	private static final Log log = LogFactory.getLog(Slf4jApplicationListener.class);

	private void onScopeAttached(OtelContextWrapper.ScopeAttached event) {
		if (log.isTraceEnabled()) {
			log.trace("Got scope changed event [" + event + "]");
		}
		if (event.span != null) {
			MDC.put("traceId", event.span.getSpanContext().getTraceId());
			MDC.put("spanId", event.span.getSpanContext().getSpanId());
		}
	}

	private void onScopeRestored(OtelContextWrapper.ScopeRestored event) {
		if (log.isTraceEnabled()) {
			log.trace("Got scope changed event [" + event + "]");
		}
		if (event.span != null) {
			MDC.put("traceId", event.span.getSpanContext().getTraceId());
			MDC.put("spanId", event.span.getSpanContext().getSpanId());
		}
	}

	private void onScopeClosed(OtelContextWrapper.ScopeClosed event) {
		if (log.isTraceEnabled()) {
			log.trace("Got scope closed event [" + event + "]");
		}
		MDC.remove("traceId");
		MDC.remove("spanId");
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof OtelContextWrapper.ScopeAttached) {
			onScopeAttached((OtelContextWrapper.ScopeAttached) event);
		}
		else if (event instanceof OtelContextWrapper.ScopeClosed) {
			onScopeClosed((OtelContextWrapper.ScopeClosed) event);
		}
		else if (event instanceof OtelContextWrapper.ScopeRestored) {
			onScopeRestored((OtelContextWrapper.ScopeRestored) event);
		}
	}

}
