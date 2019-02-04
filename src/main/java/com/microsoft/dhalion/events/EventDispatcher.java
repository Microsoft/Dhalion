/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * This program is made available under the terms of the MIT License.
 * See the LICENSE file in the project root for more information.
 */

package com.microsoft.dhalion.events;

import java.util.HashSet;
import java.util.Set;

public class EventDispatcher<T> implements EventHandler<T> {
  Set<EventHandler<T>> handlers = new HashSet<>();

  public synchronized void addHandler(EventHandler<T> handler) {
    if (handlers.contains(handler)) {
      throw new IllegalArgumentException("Duplicate handler registration");
    }
    handlers.add(handler);
  }

  @Override
  public void onEvent(T event) {
    handlers.forEach(x -> x.onEvent(event));
  }
}
