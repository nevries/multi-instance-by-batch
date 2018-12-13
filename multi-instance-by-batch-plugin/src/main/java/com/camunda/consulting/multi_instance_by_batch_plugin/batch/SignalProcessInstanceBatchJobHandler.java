package com.camunda.consulting.multi_instance_by_batch_plugin.batch;

import java.io.Serializable;
import java.util.List;

import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.extension.batch.CustomBatchJobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.camunda.consulting.multi_instance_by_batch_plugin.batch.SignalProcessInstanceBatchJobHandler.SignalProcessInstanceData;

public class SignalProcessInstanceBatchJobHandler extends CustomBatchJobHandler<SignalProcessInstanceData> {

  private static final Logger LOGGER = LoggerFactory.getLogger(SignalProcessInstanceBatchJobHandler.class);

  public static final SignalProcessInstanceBatchJobHandler INSTANCE = new SignalProcessInstanceBatchJobHandler();

  private SignalProcessInstanceBatchJobHandler() {
  }

  @Override
  public String getType() {
    return "signal-process-instance-batch-job-handler";
  }

  @Override
  public void execute(List<SignalProcessInstanceData> data, CommandContext commandContext) {
    LOGGER.info("Signal Process Instance Batch job for " + data.size() + " instances.");
    for (SignalProcessInstanceData signalData : data) {
      commandContext.getProcessEngineConfiguration().getRuntimeService().signalEventReceived(signalData.getSignal(), signalData.getExecutionId());
    }
  }

  public static class SignalProcessInstanceData implements Serializable {
    private static final long serialVersionUID = -3392662520214460253L;

    String executionId;
    String signal;

    public SignalProcessInstanceData(String executionId, String signal) {
      super();
      this.executionId = executionId;
      this.signal = signal;
    }

    public String getExecutionId() {
      return executionId;
    }

    public String getSignal() {
      return signal;
    }

  }
}
