package com.camunda.consulting.multi_instance_by_batch.adapter.delegate;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.camunda.consulting.multi_instance_by_batch.CamundaApplication;

@Component
public class PrepareList implements JavaDelegate {

  private static final Logger LOGGER = LoggerFactory.getLogger(PrepareList.class);
  private static final Random RND = new Random();

  @Override
  public void execute(final DelegateExecution execution) throws Exception {
    LOGGER.info("Start: " + execution.getCurrentActivityId());

    final Long size = (Long) execution.getVariable(CamundaApplication.VARIABLE_MULTI_INSTANCE_SIZE);
    final List<Integer> list = new ArrayList<>();

    final int maximum = 2000;
    final int minimum = 1000;
    final int range = maximum - minimum + 1;
    for (int i = 1; i <= size; i++) {
      final int sleep = RND.nextInt(range) + minimum;
      list.add(sleep);
    }

    execution.setVariable(CamundaApplication.VARIABLE_LIST, list);
    LOGGER.info("Finish: " + execution.getCurrentActivityId());
  }

}