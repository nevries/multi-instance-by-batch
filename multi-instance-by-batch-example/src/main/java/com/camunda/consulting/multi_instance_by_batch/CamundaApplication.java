package com.camunda.consulting.multi_instance_by_batch;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.camunda.bpm.spring.boot.starter.event.PostDeployEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.camunda.consulting.multi_instance_by_batch_plugin.MultiInstanceByBatchProcessEnginePlugin;

@SpringBootApplication
@EnableProcessApplication("multi-instance-by-batch")
@EnableScheduling
public class CamundaApplication {
  private static final Logger LOGGER = LoggerFactory.getLogger(CamundaApplication.class);

  public static final String VARIABLE_MULTI_INSTANCE_SIZE = "multiInstanceSize";
  public static final String VARIABLE_LIST = "list";
  public static final String VARIABLE_ITEM = "item";

  public static final String VARIABLE_NO_BUSINESS_ERROR_PROBABILITY = "noBusinessErrorProbability";

  @Autowired
  RuntimeService runtimeService;

  public static void main(String... args) {
    SpringApplication.run(CamundaApplication.class, args);
  }

  @EventListener
  public void notify(final PostDeployEvent event) {
    LOGGER.info("Post Deploy");
    runtimeService.startProcessInstanceByKey("parent", "autostarted",
        Variables.putValue(VARIABLE_MULTI_INSTANCE_SIZE, 987l).putValue(VARIABLE_NO_BUSINESS_ERROR_PROBABILITY, "0.98"));
  }
  
  @Bean
  public ProcessEnginePlugin multiInstanceByBatchPlugin() {
    return new MultiInstanceByBatchProcessEnginePlugin();
  }

  @Bean
  public ProcessEnginePlugin configureJobExecutor() {
    return new AbstractProcessEnginePlugin() {
      @Override
      public void postInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
        processEngineConfiguration.setJobExecutorPreferTimerJobs(true);
      }
    };
  }

}
