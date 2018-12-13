package com.camunda.consulting.multi_instance_by_batch.adapter.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.camunda.consulting.multi_instance_by_batch.CamundaApplication;

@Component
public class ExecuteInstance implements JavaDelegate {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteInstance.class);

  @Override
  public void execute(final DelegateExecution execution) throws Exception {
    final Integer item = (Integer) execution.getVariable(CamundaApplication.VARIABLE_ITEM);
    final double compensateProbability = Double.parseDouble(execution.getVariable(CamundaApplication.VARIABLE_NO_BUSINESS_ERROR_PROBABILITY).toString());

    // simulate real execution
    try {
      Thread.sleep(item);
    } catch (final InterruptedException e) {
      throw new RuntimeException(e);
    }

    final boolean noProblem = Math.random() < compensateProbability;
    execution.setVariable("ok", noProblem);

    LOGGER.info("Finish execution with noProblem = " + noProblem);
  }

}