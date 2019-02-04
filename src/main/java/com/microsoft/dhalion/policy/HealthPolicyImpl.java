/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * This program is made available under the terms of the MIT License.
 * See the LICENSE file in the project root for more information.
 */

package com.microsoft.dhalion.policy;

import com.google.common.annotations.VisibleForTesting;
import com.microsoft.dhalion.api.IDetector;
import com.microsoft.dhalion.api.IDiagnoser;
import com.microsoft.dhalion.api.IHealthPolicy;
import com.microsoft.dhalion.api.IResolver;
import com.microsoft.dhalion.api.ISensor;
import com.microsoft.dhalion.core.Action;
import com.microsoft.dhalion.core.Diagnosis;
import com.microsoft.dhalion.core.Measurement;
import com.microsoft.dhalion.core.Symptom;
import com.microsoft.dhalion.policy.PoliciesExecutor.ExecutionContext;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class HealthPolicyImpl implements IHealthPolicy {
  protected Collection<ISensor> sensors = new ArrayList<>();
  protected Collection<IDetector> detectors = new ArrayList<>();
  protected Collection<IDiagnoser> diagnosers = new ArrayList<>();
  protected Collection<IResolver> resolvers = new ArrayList<>();

  protected Duration interval = Duration.ofMinutes(1);
  private Instant lastExecutionTimestamp;
  private Instant oneTimeDelay = null;

  private ExecutionContext executionContext;

  @VisibleForTesting
  ClockTimeProvider clock = new ClockTimeProvider();

  @Override
  public void initialize(ExecutionContext context) {
    this.executionContext = context;

    sensors.forEach(sensor -> sensor.initialize(executionContext));
    detectors.forEach(detector -> detector.initialize(executionContext));
    diagnosers.forEach(diagnoser -> diagnoser.initialize(executionContext));
    resolvers.forEach(resolver -> resolver.initialize(executionContext));
  }

  public void registerSensors(ISensor... sensors) {
    if (sensors == null) {
      throw new IllegalArgumentException("Null instance cannot be added.");
    }

    this.sensors.addAll(Arrays.asList(sensors));
  }

  public void registerDetectors(IDetector... detectors) {
    if (detectors == null) {
      throw new IllegalArgumentException("Null instance cannot be added.");
    }

    this.detectors.addAll(Arrays.asList(detectors));
  }

  public void registerDiagnosers(IDiagnoser... diagnosers) {
    if (diagnosers == null) {
      throw new IllegalArgumentException("Null instance cannot be added.");
    }

    this.diagnosers.addAll(Arrays.asList(diagnosers));
  }

  public void registerResolvers(IResolver... resolvers) {
    if (resolvers == null) {
      throw new IllegalArgumentException("Null instance cannot be added.");
    }

    this.resolvers.addAll(Arrays.asList(resolvers));
  }

  /**
   * @param value the delay after which this policy will be re-executed. For a one-time policy
   */
  public void setPolicyExecutionInterval(Duration value) {
    this.interval = value;
  }

  /**
   * One-time delay defers policy execution. One-time delay is used when the system is expected to be unstable,
   * typically after an {@link Action}. One-time delay value overrides the original policy execution interval. Policy
   * execution will resume after the set delay has elapsed.
   *
   * @param value the delay value
   * @see HealthPolicyImpl#setPolicyExecutionInterval
   */
  public void setOneTimeDelay(Duration value) {
    oneTimeDelay = clock.now().plus(value);
  }

  @Override
  public Collection<Measurement> executeSensors() {
    Collection<Measurement> measurements = new ArrayList<>();
    if (sensors == null) {
      return measurements;
    }

    sensors.stream().map(ISensor::fetch)
           .filter(Objects::nonNull)
           .forEach(measurements::addAll);

    return measurements;
  }

  @Override
  public Collection<Symptom> executeDetectors(Collection<Measurement> measurements) {
    List<Symptom> symptoms = new ArrayList<>();
    if (detectors == null) {
      return symptoms;
    }

    detectors.stream().map(detector -> detector.detect(measurements))
             .filter(Objects::nonNull)
             .forEach(symptoms::addAll);

    return symptoms;
  }

  @Override
  public Collection<Diagnosis> executeDiagnosers(Collection<Symptom> symptoms) {
    List<Diagnosis> diagnosis = new ArrayList<>();
    if (diagnosers == null) {
      return diagnosis;
    }

    diagnosers.stream().map(diagnoser -> diagnoser.diagnose(symptoms))
              .filter(Objects::nonNull)
              .forEach(diagnosis::addAll);

    return diagnosis;
  }

  @Override
  public Collection<Action> executeResolvers(Collection<Diagnosis> diagnosis) {
    if (oneTimeDelay != null && !oneTimeDelay.isAfter(clock.now())) {
      // reset one time delay timestamp
      oneTimeDelay = null;
    }

    Collection<Action> actions = new ArrayList<>();
    if (resolvers == null) {
      return actions;
    }

    resolvers.stream().map(resolver -> resolver.resolve(diagnosis))
             .filter(Objects::nonNull)
             .forEach(actions::addAll);

    lastExecutionTimestamp = clock.now();
    return actions;
  }

  @Override
  public Collection<Action> executeResolver(IResolver resolver) {
    if (oneTimeDelay != null && !oneTimeDelay.isAfter(clock.now())) {
      // reset one time delay timestamp
      oneTimeDelay = null;
    }

    Collection<Action> actions = new ArrayList<>();
    if (!resolvers.contains(resolver)) {
      return actions;
    }

    actions = resolver.resolve();

    lastExecutionTimestamp = clock.now();
    return actions;
  }

  @Override
  public Duration getDelay() {
    long delay;
    if (lastExecutionTimestamp == null) {
      // first time execution of the policy will start immediately.
      delay = 0;
    } else if (oneTimeDelay != null) {
      delay = oneTimeDelay.toEpochMilli() - clock.now().toEpochMilli();
    } else {
      delay = lastExecutionTimestamp.plus(interval).toEpochMilli() - clock.now().toEpochMilli();
    }
    delay = delay < 0 ? 0 : delay;

    return Duration.ofMillis(delay);
  }

  @Override
  public void close() {
    sensors.forEach(ISensor::close);
    detectors.forEach(IDetector::close);
    diagnosers.forEach(IDiagnoser::close);
    resolvers.forEach(IResolver::close);
  }
  
  @VisibleForTesting
  static class ClockTimeProvider {
    Instant now() {
      return Instant.now();
    }
  }
}
