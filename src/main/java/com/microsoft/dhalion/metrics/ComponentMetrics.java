/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * This program is made available under the terms of the MIT License.
 * See the LICENSE file in the project root for more information.
 */
package com.microsoft.dhalion.metrics;

import java.util.HashMap;
import java.util.Map;

/**
 * An {@link ComponentMetrics} holds metrics information for all instances of a component.
 */
public class ComponentMetrics {
  // id of the component
  protected String name;

  // a map of metric name and its value
  private HashMap<String, InstanceMetrics> metrics = new HashMap<>();

  public ComponentMetrics(String compName) {
    this(compName, null);
  }

  public ComponentMetrics(String compName, String instanceName, String metricName, double value) {
    this(compName, null);
    if (instanceName != null) {
      InstanceMetrics instanceMetrics = new InstanceMetrics(instanceName, metricName, value);
      addInstanceMetric(instanceMetrics);
    }
  }

  public ComponentMetrics(String compName, Map<String, InstanceMetrics> instanceMetricsData) {
    this.name = compName;
    if (instanceMetricsData != null) {
      instanceMetricsData.values().stream().forEach(x -> addInstanceMetric(x));
    }
  }

  public void addInstanceMetric(InstanceMetrics instanceMetrics) {
    String instanceName = instanceMetrics.getName();
    if (metrics.containsKey(instanceName)) {
      throw new IllegalArgumentException("Instance metrics exist: " + name);
    }
    metrics.put(instanceName, instanceMetrics);
  }

  public HashMap<String, InstanceMetrics> getMetrics() {
    return metrics;
  }

  public InstanceMetrics getMetrics(String instanceName) {
    return metrics.get(instanceName);
  }

  /**
   * @param instance name of the instance for which metrics are desired
   * @param metric metric name
   * @return all known metric values for the requested instance
   */
  public Map<Long, Double> getMetricValues(String instance, String metric) {
    InstanceMetrics instanceMetrics = getMetrics(instance);
    if (instanceMetrics == null) {
      return null;
    }

    return instanceMetrics.getMetricValues(metric);
  }

  /**
   * @param instance name of the instance for which metrics are desired
   * @param metric metric name
   * @return the only known metric values for the requested instance. Throw error if more than one
   * metric value is known. Use {@link ComponentMetrics#getMetricValues} in such a case.
   */
  public Double getMetricValue(String instance, String metric) {
    InstanceMetrics instanceMetrics = getMetrics(instance);
    if (instanceMetrics == null) {
      return null;
    }

    return instanceMetrics.getMetricValue(metric);
  }

  public String getName() {
    return name;
  }

  public boolean anyInstanceAboveLimit(String metricName, double limit) {
    return metrics.values().stream().anyMatch(x -> x.hasMetricAboveLimit(metricName, limit));
  }

  /**
   * Merges instance metrics in two different objects into one. Input objects are not modified. It
   * is assumed that the two input data sets belong to the same component. Hence the data sets
   * will contain the same instances.
   *
   * @return A new {@link ComponentMetrics} instance
   */
  public static ComponentMetrics merge(ComponentMetrics data1, ComponentMetrics data2) {
    ComponentMetrics mergedData = new ComponentMetrics(data1.getName());
    for (InstanceMetrics instance1 : data1.getMetrics().values()) {
      InstanceMetrics instance2 = data2.getMetrics(instance1.getName());
      if (instance2 != null) {
        instance1 = InstanceMetrics.merge(instance1, instance2);
      }
      mergedData.addInstanceMetric(instance1);
    }

    return mergedData;
  }

  /**
   * Get the aggregated metric values for a given component.
   * @param metricName The name of the metric.
   * @return The aggregated value of the metric metricName for the component componentName.
   */
  public Double getAggregatedMetricsValue(String metricName) {
    Double result = 0.0;
    final InstanceMetrics instanceMetrics = metrics.get(metricName);
    final Map<Long, Double> metricValues = instanceMetrics.getMetricValues(metricName);
    for (Double aDouble : metricValues.values()) {
      result += aDouble;
    }
    return result;
  }

  @Override
  public String toString() {
    return "ComponentMetrics{" +
        "name='" + name + '\'' +
        ", metrics=" + metrics +
        '}';
  }
}
