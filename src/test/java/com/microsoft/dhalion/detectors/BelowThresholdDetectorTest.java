package com.microsoft.dhalion.detectors;

import com.microsoft.dhalion.Utils;
import com.microsoft.dhalion.api.IDetector;
import com.microsoft.dhalion.conf.PolicyConfig;
import com.microsoft.dhalion.core.Measurement;
import com.microsoft.dhalion.core.Symptom;
import com.microsoft.dhalion.core.SymptomsTable;
import com.microsoft.dhalion.detectors.BelowThresholdDetector;
import com.microsoft.dhalion.policy.PoliciesExecutor.ExecutionContext;
import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;

import static com.microsoft.dhalion.detectors.BelowThresholdDetector.LOW_THRESHOLD_CONF;
import static com.microsoft.dhalion.detectors.BelowThresholdDetector.SYMPTOM_LOW;
import static com.microsoft.dhalion.examples.MetricName.METRIC_CPU;
import static com.microsoft.dhalion.examples.MetricName.METRIC_MEMORY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BelowThresholdDetectorTest {
  private ExecutionContext context;
  private Instant currentInstant;
  private IDetector detector;
  private PolicyConfig policyConf;

  private Collection<Measurement> initialize() {
    currentInstant = Instant.now();
    context = mock(ExecutionContext.class);
    when(context.checkpoint()).thenReturn(currentInstant);
    policyConf = mock(PolicyConfig.class);
    when(policyConf.getConfig(Utils.getCompositeName(LOW_THRESHOLD_CONF,METRIC_MEMORY.text()))).thenReturn(11.0);

    detector = new BelowThresholdDetector(policyConf, METRIC_MEMORY.text());
    detector.initialize(context);

    Measurement instance1 = new Measurement("c1", "i1", METRIC_MEMORY.text(), currentInstant, 10);
    Measurement instance2 = new Measurement("c2", "i2", METRIC_MEMORY.text(), currentInstant, 91.0);
    Measurement instance3 = new Measurement("c3", "i3", METRIC_MEMORY.text(), currentInstant, 50);
    Measurement instance4 = new Measurement("c4", "i4", METRIC_MEMORY.text(), currentInstant, 60);
    Measurement instance5 = new Measurement("c5", "i5", METRIC_MEMORY.text(), currentInstant, 20.0);
    Measurement instance6 = new Measurement("c6", "i6", METRIC_MEMORY.text(), currentInstant, 80);
    Measurement instance7 = new Measurement("c7", "i7", METRIC_MEMORY.text(), currentInstant, 9);
    Measurement instance8 = new Measurement("c8", "i8", METRIC_MEMORY.text(), currentInstant, 60);

    Collection<Measurement> metrics = new ArrayList<>();
    metrics.add(instance1);
    metrics.add(instance2);
    metrics.add(instance3);
    metrics.add(instance4);
    metrics.add(instance5);
    metrics.add(instance6);
    metrics.add(instance7);
    metrics.add(instance8);

    return metrics;
  }

  @Test
  public void testThresholdBasedDetector() {
    Collection<Measurement> metrics = initialize();

    Collection<Symptom> symptoms = detector.detect(metrics);
    SymptomsTable symptomsTable = SymptomsTable.of(symptoms);

    assertEquals(2, symptomsTable.size());
    assertTrue(symptomsTable.type(Utils.getCompositeName(SYMPTOM_LOW ,METRIC_MEMORY.text())).assignment("i1").size() > 0);
    assertTrue(symptomsTable.type(Utils.getCompositeName(SYMPTOM_LOW ,METRIC_MEMORY.text())).assignment("i7").size() > 0);

  }

}
