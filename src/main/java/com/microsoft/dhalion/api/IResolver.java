/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * This program is made available under the terms of the MIT License.
 * See the LICENSE file in the project root for more information.
 */
package com.microsoft.dhalion.api;

import com.microsoft.dhalion.core.Action;
import com.microsoft.dhalion.core.Diagnosis;
import com.microsoft.dhalion.core.Symptom;
import com.microsoft.dhalion.policy.PoliciesExecutor.ExecutionContext;

import java.util.Collection;

/**
 * A {@link IResolver}'s major goal is to execute {@link Action}s to resolve an anomaly or health issue identified by a
 * {@link IDiagnoser}. {@link IResolver} typically consume {@link Diagnosis} and executes appropriate action to bring a
 * linked component or system back to a healthy state.
 */
public interface IResolver {
  /**
   * @return returns types of {@link Action}s created by this {@link IResolver}
   */
  default Collection<String> getActionTypes() {
    throw new UnsupportedOperationException();
  }

  /**
   * Initializes this instance and will be invoked once before this instance is used.
   *
   * @param context execution context for this instance
   */
  default void initialize(ExecutionContext context) {
  }

  /**
   * Triggers execution of {@link Action}s which are expected to improved system health.
   *
   * @return all the actions executed by this resolver
   */
  default Collection<Action> resolve() {
    throw new UnsupportedOperationException();
  }

  /**
   * Triggers execution of {@link Action}s which are expected to improved system health.
   *
   * @param diagnosis recently identified likely-causes of the observed {@link Symptom}s
   * @return all the actions executed by this resolver to mitigate the problems
   */
  default Collection<Action> resolve(Collection<Diagnosis> diagnosis) {
    throw new UnsupportedOperationException();
  }

  /**
   * Release all acquired resources and prepare for termination of this instance
   */
  default void close() {
  }
}
