/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * This program is made available under the terms of the MIT License.
 * See the LICENSE file in the project root for more information.
 */

package com.microsoft.dhalion.api;

import com.microsoft.dhalion.core.Action;
import com.microsoft.dhalion.core.Diagnosis;
import com.microsoft.dhalion.core.Measurement;
import com.microsoft.dhalion.core.Symptom;
import com.microsoft.dhalion.policy.PoliciesExecutor.ExecutionContext;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;

/**
 * A {@link IHealthPolicy} strives to keep a distributed application healthy. It uses one or more of
 * {@link IDetector}s, {@link IDiagnoser}s and {@link IResolver}s to achieve this. Once initialized, a policy is
 * executed periodically. The policy executor invokes in order {@link ISensor}s, {@link IDetector}s,
 * {@link IDiagnoser}s and {@link IResolver}s.
 */
public interface IHealthPolicy {
  /**
   * Initializes this instance and should be invoked once by the system before its use.
   *
   * @param context execution context of the policy
   */
  void initialize(ExecutionContext context);

  /**
   * Invoked periodically, this method executes one or more {@link ISensor}s. Typically, {@link ISensor} execution
   * will result in addition of latest {@link Measurement}s in the {@link ExecutionContext}.
   *
   * @return most recently fetched {@link Measurement}s
   */
  Collection<Measurement> executeSensors();

  /**
   * Invoked after {@link ISensor}s this method executes one or more {@link IDetector}s. Most recently fetched
   * {@link Measurement}s are provided, while additional {@link Measurement}s can be obtained from
   * {@link ExecutionContext}.
   *
   * @param measurements most recently fetched {@link Measurement}s
   * @return newly identified {@link Symptom}s
   */
  Collection<Symptom> executeDetectors(Collection<Measurement> measurements);

  /**
   * Invoked after {@link IDetector}s, this method executes one or more {@link IDiagnoser}s.
   * newly identified {@link Symptom}s
   *
   * @param symptoms recently identified {@link Symptom}s
   * @return likely causes of the observed {@link Symptom}s
   */
  Collection<Diagnosis> executeDiagnosers(Collection<Symptom> symptoms);

  /**
   * Invoked after {@link IDiagnoser}s, this method executes one or more {@link IResolver} to fix any identified
   * issues. Typically, a policy will invoke the most advantageous {@link IResolver} from the resolvers belonging to
   * the policy to avoid overlapping {@link Action}s and conflicts.
   *
   * @param diagnosis recently identified likely-causes of the observed {@link Symptom}s
   * @return actions executed to mitigate health issues
   */
  Collection<Action> executeResolvers(Collection<Diagnosis> diagnosis);

  /**
   * Invoked in the event that the policy should be overridden, this method executes one {@link IResolver} to fix a
   * single identified issue.
   *
   * @param resolver a resolver to be executed
   * @return actions executed to mitigate health issues
   */
  Collection<Action> executeResolver(IResolver resolver);

  /**
   * @return the remaining delay before re-execution of this policy
   */
  Duration getDelay();

  /**
   * @return the timestamp/checkpoint to be use for next execution cycle
   */
  default Instant getNextCheckpoint() {
    return Instant.now();
  }


  /**
   * Release all acquired resources and prepare for termination of this instance
   */
  default void close() {
  }
}