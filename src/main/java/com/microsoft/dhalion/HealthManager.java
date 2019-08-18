package com.microsoft.dhalion;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.microsoft.dhalion.api.IHealthPolicy;
import com.microsoft.dhalion.api.MetricsProvider;
import com.microsoft.dhalion.conf.Config;
import com.microsoft.dhalion.conf.ConfigBuilder;
import com.microsoft.dhalion.conf.Key;
import com.microsoft.dhalion.conf.PolicyConfig;
import com.microsoft.dhalion.policy.PoliciesExecutor;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;


public class HealthManager {
  public static final String CONF_DIR = "conf.dir";

  private static final Logger LOG = Logger.getLogger(HealthManager.class.getName());
  private final String configDir;

  private Injector injector;
  private Config config;
  private List<IHealthPolicy> healthPolicies = new ArrayList<>();
  private final MetricsProvider metricsProvider;

  enum CliArgs {
    CONFIG_DIR("config_dir");

    private String text;

    CliArgs(String name) {
      this.text = name;
    }

    public String text() {
      return text;
    }
  }

  public static void main(String[] args) throws Exception {
    CommandLineParser parser = new DefaultParser();
    Options slaManagerCliOptions = constructCliOptions();

    // parse the help options first.
    Options helpOptions = constructHelpOptions();
    CommandLine cmd = parser.parse(helpOptions, args, true);
    if (cmd.hasOption("h")) {
      usage(slaManagerCliOptions);
      return;
    }

    try {
      cmd = parser.parse(slaManagerCliOptions, args);
    } catch (ParseException e) {
      usage(slaManagerCliOptions);
      throw new RuntimeException("Error parsing command line options: ", e);
    }

    LOG.info("Initializing the health manager");
    HealthManager healthmgr = new HealthManager(cmd);
    healthmgr.start();
  }

  private HealthManager(CommandLine cmd) throws ClassNotFoundException {
    this.configDir = getOptionValue(cmd, CliArgs.CONFIG_DIR);

    AbstractModule module = new AbstractModule() {
      @Override
      protected void configure() {
        bind(String.class)
            .annotatedWith(Names.named(CONF_DIR))
            .toInstance(configDir);
      }
    };

    injector = Guice.createInjector(module);

    //Read healthmgr.yaml and create a hashmap with the configurations
    ConfigBuilder confBuilder = new ConfigBuilder(configDir);
    confBuilder.loadConfig(Paths.get(configDir, (String) Key.HEALTHMGR_CONF.getDefault()));
    Map<String, Object> conf = confBuilder.getKeyValues();

    ConfigBuilder cb = injector.getInstance(ConfigBuilder.class);

    cb.loadConfig(conf).loadPolicyConf();
    config = cb.build();
    injector = injector.createChildInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(Config.class).toInstance(config);
      }
    });
    
    //Register additional bindings
    String bootstrapModuleClassName = (String) conf.get(Key.BOOTSTRAP_MODULE_CLASS.value());
    Class<IBootstrapModule> bootstrapModuleClass = loadClass(bootstrapModuleClassName);
    IBootstrapModule bootstrapModule = injector.getInstance(bootstrapModuleClass);

    injector = injector.createChildInjector(bootstrapModule.get());

    //Read the MetricsProvider class
    String metricsProviderClass = (String) conf.get(Key.METRICS_PROVIDER_CLASS.value());
    Class<MetricsProvider> mpClass = loadClass(metricsProviderClass);

    injector = injector.createChildInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(mpClass).in(Singleton.class);
      }
    });

    metricsProvider = injector.getInstance(mpClass);

    injector = injector.createChildInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(MetricsProvider.class).toInstance(metricsProvider);
      }
    });

    initializePolicies();
  }


  private void initializePolicies() throws ClassNotFoundException {
    for (PolicyConfig policyConf : config.policies()) {
      String policyClassName = policyConf.policyClass();
      LOG.info(String.format("Initializing %s with class %s", policyConf.id(), policyClassName));
      Class<IHealthPolicy> policyClass = loadClass(policyClassName);

      AbstractModule module = constructPolicySpecificModule(policyConf);
      IHealthPolicy policy = injector.createChildInjector(module).getInstance(policyClass);

      healthPolicies.add(policy);
    }
  }

  @SuppressWarnings("unchecked")
  private <T> Class<T> loadClass(String className) throws ClassNotFoundException {
      return (Class<T>) this.getClass().getClassLoader().loadClass(className);
  }

  private AbstractModule constructPolicySpecificModule(final PolicyConfig policyConfig) {
    return new AbstractModule() {
      @Override
      protected void configure() {
        bind(PolicyConfig.class).toInstance(policyConfig);
      }
    };
  }

  private void start() throws InterruptedException, ExecutionException {
    LOG.info("Starting Health Manager");
    PoliciesExecutor policyExecutor = new PoliciesExecutor(healthPolicies);
    ScheduledFuture<?> future = policyExecutor.start();
    try {
      future.get();
    } finally {
      policyExecutor.destroy();
      metricsProvider.close();
    }
  }

  private static Options constructCliOptions() {
    Options options = new Options();

    Option configFile = Option.builder("p")
                              .desc("Path of the config files")
                              .longOpt(CliArgs.CONFIG_DIR.text)
                              .hasArgs()
                              .required()
                              .argName("config path")
                              .build();

    options.addOption(configFile);
    return options;
  }

  // construct command line help options
  private static Options constructHelpOptions() {
    Options options = new Options();
    Option help = Option.builder("h")
                        .desc("List all options and their description")
                        .longOpt("help")
                        .build();

    options.addOption(help);
    return options;
  }

  // Print usage options
  private static void usage(Options options) {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(HealthManager.class.getSimpleName(), options);
  }

  private String getOptionValue(CommandLine cmd, CliArgs argName) {
    return cmd.getOptionValue(argName.text, null);
  }
}
