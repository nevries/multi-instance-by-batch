package com.camunda.consulting.multi_instance_by_batch_plugin.batch;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.ProcessInstanceWithVariablesImpl;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.extension.batch.CustomBatchJobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.camunda.consulting.multi_instance_by_batch_plugin.batch.StartProcessInstanceBatchJobHandler.StartProcessInstanceData;

public class StartProcessInstanceBatchJobHandler extends CustomBatchJobHandler<StartProcessInstanceData> {

  private static final Logger LOGGER = LoggerFactory.getLogger(StartProcessInstanceBatchJobHandler.class);

  public static final StartProcessInstanceBatchJobHandler INSTANCE = new StartProcessInstanceBatchJobHandler();

  private StartProcessInstanceBatchJobHandler() {
  }

  @Override
  public String getType() {
    return "start-process-instance-batch-job-handler";
  }

  @Override
  public void execute(List<StartProcessInstanceData> data, CommandContext commandContext) {
    LOGGER.info("Start Process Instance Batch job for " + data.size() + " instances.");
    for (StartProcessInstanceData instanceData : data) {
      ProcessInstance processInstance = commandContext.getProcessEngineConfiguration().getRuntimeService().startProcessInstanceById(instanceData.getProcessDefinitionId(),
          instanceData.getBusinessKey(), instanceData.getVariables());
    }
  }

  public static class StartProcessInstanceData implements Serializable {
    private static final long serialVersionUID = -5068036256178434539L;

    String processDefinitionId;
    String executionId;
    String businessKey;
    Map<String, Object> variables;

    public StartProcessInstanceData(String processDefinitionId, String executionId, String businessKey, Map<String, Object> variables) {
      super();
      this.processDefinitionId = processDefinitionId;
      this.businessKey = businessKey;
      this.variables = variables;
    }

    public String getProcessDefinitionId() {
      return processDefinitionId;
    }

    public String getExecutionId() {
      return executionId;
    }

    public String getBusinessKey() {
      return businessKey;
    }

    public Map<String, Object> getVariables() {
      return variables;
    }
  }

}
