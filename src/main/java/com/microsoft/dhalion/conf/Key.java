package com.microsoft.dhalion.conf;

public enum Key {
  CONF_DIR("healthmgr.conf.dir"),
  DATA_DIR("healthmgr.data.dir"),

  HEALTHMGR_CONF("healthmgr.config.filename", "healthmgr.yaml"),
  POLICY_CONF("healthmgr.policy.conf.filename", "policyconf.yaml"),
  HEALTHMGR_MODE("healthmgr.mode", "online"),

  // health policy config keys
  POLICIES("health.policies"),
  POLICY_CLASS("health.policy.class"),
  POLICY_INTERVAL("health.policy.interval.ms"),
  POLICY_CONF_SENSOR_DURATION_SUFFIX(".duration"),
  CONF_COMPONENT_NAMES("component.names"),

  BOOTSTRAP_MODULE_CLASS("bootstrap.module.class", "com.microsoft.dhalion.IBootstrapModule$DefaultModule"),
  METRICS_PROVIDER_CLASS("metrics.provider.class");

  private final String value;
  private final Object defaultValue;
  
  Key(String value) {
    this(value, null);
  }

  Key(String value, Object defaultValue) {
    this.value = value;
    this.defaultValue = defaultValue;
  }

  /**
   * Get the key value for this enum
   *
   * @return key value
   */
  public String value() {
    return value;
  }

  /**
   * Return the default value
   */
  public Object getDefault() {
    return this.defaultValue;
  }
}
