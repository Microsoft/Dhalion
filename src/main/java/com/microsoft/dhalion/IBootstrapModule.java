/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * This program is made available under the terms of the MIT License.
 * See the LICENSE file in the project root for more information.
 */
package com.microsoft.dhalion;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

/**
 * {@link IBootstrapModule} should be used to customize the injector with additional bindings required for injecting
 * the metrics provider and the health policies.
 */
public interface IBootstrapModule {

  /**
   * Dhalion will create a new injector that will inherit all its state from the existing injector and add this module
   * (and binding definitions) to it.
   * 
   * @return {@link Module} with additional binding definitions
   */
  default Module get() {
    return new AbstractModule() {
      @Override
      protected void configure() {
      }
    };
  }

  public static class DefaultModule implements IBootstrapModule{}
}
