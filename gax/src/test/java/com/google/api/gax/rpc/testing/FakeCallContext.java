/*
 * Copyright 2017 Google LLC
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google LLC nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.google.api.gax.rpc.testing;

import com.google.api.core.InternalApi;
import com.google.api.gax.rpc.ApiCallContext;
import com.google.api.gax.rpc.ClientContext;
import com.google.api.gax.rpc.TransportChannel;
import com.google.api.gax.rpc.internal.Headers;
import com.google.api.gax.tracing.ApiTracer;
import com.google.auth.Credentials;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.threeten.bp.Duration;

@InternalApi("for testing")
public class FakeCallContext implements ApiCallContext {
  private final Credentials credentials;
  private final FakeChannel channel;
  private final Duration timeout;
  private final Duration streamWaitTimeout;
  private final Duration streamIdleTimeout;
  private final ImmutableMap<String, List<String>> extraHeaders;
  private final ApiTracer tracer;

  private FakeCallContext(
      Credentials credentials,
      FakeChannel channel,
      Duration timeout,
      Duration streamWaitTimeout,
      Duration streamIdleTimeout,
      ImmutableMap<String, List<String>> extraHeaders,
      ApiTracer tracer) {
    this.credentials = credentials;
    this.channel = channel;
    this.timeout = timeout;
    this.streamWaitTimeout = streamWaitTimeout;
    this.streamIdleTimeout = streamIdleTimeout;
    this.extraHeaders = extraHeaders;
    this.tracer = tracer;
  }

  public static FakeCallContext createDefault() {
    return new FakeCallContext(
        null, null, null, null, null, ImmutableMap.<String, List<String>>of(), null);
  }

  @Override
  public FakeCallContext nullToSelf(ApiCallContext inputContext) {
    FakeCallContext fakeCallContext;
    if (inputContext == null) {
      fakeCallContext = this;
    } else {
      if (!(inputContext instanceof FakeCallContext)) {
        throw new IllegalArgumentException(
            "context must be an instance of FakeCallContext, but found "
                + inputContext.getClass().getName());
      }
      fakeCallContext = (FakeCallContext) inputContext;
    }
    return fakeCallContext;
  }

  @Override
  public ApiCallContext merge(ApiCallContext inputCallContext) {
    if (inputCallContext == null) {
      return this;
    }
    if (!(inputCallContext instanceof FakeCallContext)) {
      throw new IllegalArgumentException(
          "context must be an instance of FakeCallContext, but found "
              + inputCallContext.getClass().getName());
    }
    FakeCallContext fakeCallContext = (FakeCallContext) inputCallContext;

    FakeChannel newChannel = fakeCallContext.channel;
    if (newChannel == null) {
      newChannel = channel;
    }

    Credentials newCallCredentials = fakeCallContext.credentials;
    if (newCallCredentials == null) {
      newCallCredentials = credentials;
    }

    Duration newTimeout = fakeCallContext.timeout;
    if (newTimeout == null) {
      newTimeout = timeout;
    }

    Duration newStreamWaitTimeout = fakeCallContext.streamWaitTimeout;
    if (newStreamWaitTimeout == null) {
      newStreamWaitTimeout = streamWaitTimeout;
    }

    Duration newStreamIdleTimeout = fakeCallContext.streamIdleTimeout;
    if (newStreamIdleTimeout == null) {
      newStreamIdleTimeout = streamIdleTimeout;
    }

    ApiTracer newTracer = fakeCallContext.tracer;
    if (newTracer == null) {
      newTracer = this.tracer;
    }

    ImmutableMap<String, List<String>> newExtraHeaders =
        Headers.mergeHeaders(extraHeaders, fakeCallContext.extraHeaders);
    return new FakeCallContext(
        newCallCredentials,
        newChannel,
        newTimeout,
        newStreamWaitTimeout,
        newStreamIdleTimeout,
        newExtraHeaders,
        newTracer);
  }

  public Credentials getCredentials() {
    return credentials;
  }

  public FakeChannel getChannel() {
    return channel;
  }

  @Override
  public Duration getTimeout() {
    return timeout;
  }

  @Nullable
  @Override
  public Duration getStreamWaitTimeout() {
    return streamWaitTimeout;
  }

  @Nullable
  @Override
  public Duration getStreamIdleTimeout() {
    return streamIdleTimeout;
  }

  @Override
  public FakeCallContext withCredentials(Credentials credentials) {
    return new FakeCallContext(
        credentials,
        this.channel,
        this.timeout,
        this.streamWaitTimeout,
        this.streamIdleTimeout,
        this.extraHeaders,
        this.tracer);
  }

  @Override
  public FakeCallContext withTransportChannel(TransportChannel inputChannel) {
    Preconditions.checkNotNull(inputChannel);
    if (!(inputChannel instanceof FakeTransportChannel)) {
      throw new IllegalArgumentException(
          "Expected FakeTransportChannel, got " + inputChannel.getClass().getName());
    }
    FakeTransportChannel transportChannel = (FakeTransportChannel) inputChannel;
    return withChannel(transportChannel.getChannel());
  }

  public FakeCallContext withChannel(FakeChannel channel) {
    return new FakeCallContext(
        this.credentials,
        channel,
        this.timeout,
        this.streamWaitTimeout,
        this.streamIdleTimeout,
        this.extraHeaders,
        this.tracer);
  }

  @Override
  public FakeCallContext withTimeout(Duration timeout) {
    // Default RetrySettings use 0 for RPC timeout. Treat that as disabled timeouts.
    if (timeout != null && (timeout.isZero() || timeout.isNegative())) {
      timeout = null;
    }

    // Prevent expanding timeouts
    if (timeout != null && this.timeout != null && this.timeout.compareTo(timeout) <= 0) {
      return this;
    }

    return new FakeCallContext(
        this.credentials,
        this.channel,
        timeout,
        this.streamWaitTimeout,
        this.streamIdleTimeout,
        this.extraHeaders,
        this.tracer);
  }

  @Override
  public ApiCallContext withStreamWaitTimeout(@Nullable Duration streamWaitTimeout) {
    return new FakeCallContext(
        this.credentials,
        this.channel,
        this.timeout,
        streamWaitTimeout,
        this.streamIdleTimeout,
        this.extraHeaders,
        this.tracer);
  }

  @Override
  public ApiCallContext withStreamIdleTimeout(@Nullable Duration streamIdleTimeout) {
    Preconditions.checkNotNull(streamIdleTimeout);
    return new FakeCallContext(
        this.credentials,
        this.channel,
        this.timeout,
        this.streamWaitTimeout,
        streamIdleTimeout,
        this.extraHeaders,
        this.tracer);
  }

  @Override
  public ApiCallContext withExtraHeaders(Map<String, List<String>> extraHeaders) {
    Preconditions.checkNotNull(extraHeaders);
    ImmutableMap<String, List<String>> newExtraHeaders =
        Headers.mergeHeaders(this.extraHeaders, extraHeaders);
    return new FakeCallContext(
        credentials,
        channel,
        timeout,
        streamWaitTimeout,
        streamIdleTimeout,
        newExtraHeaders,
        this.tracer);
  }

  @Override
  public Map<String, List<String>> getExtraHeaders() {
    return this.extraHeaders;
  }

  /** {@inheritDoc} */
  @Override
  @Nonnull
  public ApiTracer getTracer() {
    return tracer;
  }

  /** {@inheritDoc} */
  @Override
  public ApiCallContext withTracer(@Nonnull ApiTracer tracer) {
    Preconditions.checkNotNull(tracer);

    return new FakeCallContext(
        this.credentials,
        this.channel,
        this.timeout,
        this.streamWaitTimeout,
        this.streamIdleTimeout,
        this.extraHeaders,
        tracer);
  }

  public static FakeCallContext create(ClientContext clientContext) {
    return FakeCallContext.createDefault()
        .withTransportChannel(clientContext.getTransportChannel())
        .withCredentials(clientContext.getCredentials());
  }
}