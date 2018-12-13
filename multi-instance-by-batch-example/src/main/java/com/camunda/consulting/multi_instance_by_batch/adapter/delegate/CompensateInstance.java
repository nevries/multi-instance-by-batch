package com.camunda.consulting.multi_instance_by_batch.adapter.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.camunda.consulting.multi_instance_by_batch.CamundaApplication;

@Component
public class CompensateInstance implements JavaDelegate {

  private static final Logger LOGGER = LoggerFactory.getLogger(CompensateInstance.class);

  @Override
  public void execute(final DelegateExecution execution) throws Exception {
    final Integer item = (Integer) execution.getVariable(CamundaApplication.VARIABLE_ITEM);

    // simulate real execution
    try {
      Thread.sleep(item);
    } catch (final InterruptedException e) {
      throw new RuntimeException(e);
    }

    LOGGER.info("Finish compensation");
  }

}