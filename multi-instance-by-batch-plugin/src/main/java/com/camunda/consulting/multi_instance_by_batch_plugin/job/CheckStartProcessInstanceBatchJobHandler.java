package com.camunda.consulting.multi_instance_by_batch_plugin.job;

import java.util.ArrayList;
import java.util.List;

import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.batch.Batch;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.impl.ProcessEngineLogger;
import org.camunda.bpm.engine.impl.batch.BatchEntity;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.jobexecutor.JobHandler;
import org.camunda.bpm.engine.impl.jobexecutor.JobHandlerConfiguration;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.JobEntity;
import org.camunda.bpm.engine.impl.persistence.entity.TimerEntity;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.extension.batch.CustomBatchBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.camunda.consulting.multi_instance_by_batch_plugin.batch.SignalProcessInstanceBatchJobHandler;
import com.camunda.consulting.multi_instance_by_batch_plugin.batch.SignalProcessInstanceBatchJobHandler.SignalProcessInstanceData;
import com.camunda.consulting.multi_instance_by_batch_plugin.job.CheckStartProcessInstanceBatchJobHandler.CheckStartProcessInstanceBatchJobConfiguration;
import com.camunda.consulting.multi_instance_by_batch_plugin.MultiInstanceByBatchActivityBehaviour;
import com.camunda.consulting.multi_instance_by_batch_plugin.MultiInstanceByBatchProcessEnginePlugin;

public class CheckStartProcessInstanceBatchJobHandler implements JobHandler<CheckStartProcessInstanceBatchJobConfiguration> {
  // TODO: make configurable
  private static final int INVOCATIONS_PER_BATCH_JOB = 10;
  private static final int JOBS_PER_SEED = 10;

  private static final String DELIMITER = ";";

  private static final Logger LOGGER = LoggerFactory.getLogger(CheckStartProcessInstanceBatchJobHandler.class);

  public static final String TYPE = "check-start-process-instance-batch";

  public static final CheckStartProcessInstanceBatchJobHandler INSTANCE = new CheckStartProcessInstanceBatchJobHandler();

  private CheckStartProcessInstanceBatchJobHandler() {
  }

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public void execute(CheckStartProcessInstanceBatchJobConfiguration configuration, ExecutionEntity execution, CommandContext commandContext, String tenantId) {
    LOGGER.info("Check...");

    MultiInstanceByBatchActivityBehaviour multiInstanceByBatchActivityBehaviour = (MultiInstanceByBatchActivityBehaviour) execution.getActivity()
        .getActivityBehavior();

    String calledProcessDefinitionId = execution.getVariable(MultiInstanceByBatchProcessEnginePlugin.CALLED_PROCDEF_ID).toString();

    boolean failed = execution.getProcessEngineServices().getRuntimeService().createProcessInstanceQuery().processDefinitionId(calledProcessDefinitionId)
        .variableValueEquals(MultiInstanceByBatchProcessEnginePlugin.PARENT_EXECUTION_ID, execution.getId())
        .variableValueEquals(MultiInstanceByBatchProcessEnginePlugin.VARIABLE_COMPENSATE, true).count() > 0;

    if (failed) {
      LOGGER.info("Batch " + configuration.getBatchId() + " failed");

      BatchEntity batchEntity = commandContext.getBatchManager().findBatchById(configuration.getBatchId());
      // if null, the batch is already finished - ok
      if (batchEntity != null) {
        LOGGER.info("Delete batch " + configuration.getBatchId());
        batchEntity.delete(false);
      }

      Batch batch = createSignalBatch(calledProcessDefinitionId, execution, MultiInstanceByBatchProcessEnginePlugin.SIGNAL_COMPENSATE);
      LOGGER.info("Created compensation batch " + batch.getId());

      LOGGER.info("Thow BPMN error");
      try {
        multiInstanceByBatchActivityBehaviour.propagateBpmnError(new BpmnError(MultiInstanceByBatchProcessEnginePlugin.ERROR_CODE_INSTANCE_FAILED), execution);
      } catch (Exception ex) {
        throw ProcessEngineLogger.CMD_LOGGER.exceptionBpmnErrorPropagationFailed(MultiInstanceByBatchProcessEnginePlugin.ERROR_CODE_INSTANCE_FAILED, ex);
      }
    } else {
      long finished = execution.getProcessEngineServices().getRuntimeService().createProcessInstanceQuery().processDefinitionId(calledProcessDefinitionId)
          .variableValueEquals(MultiInstanceByBatchProcessEnginePlugin.PARENT_EXECUTION_ID, execution.getId())
          .activityIdIn("IntermediateCatchEvent_AllCompleted").count();

      LOGGER.info("... found " + finished + "/" + configuration.getTotalInstances());

      if (finished == configuration.getTotalInstances()) {
        LOGGER.info("Batch " + configuration.getBatchId() + " finished");

        Batch batch = createSignalBatch(calledProcessDefinitionId, execution, MultiInstanceByBatchProcessEnginePlugin.SIGNAL_COMPLETE);
        LOGGER.info("Created finishing batch " + batch.getId());

        LOGGER.info("Complete external task");

        multiInstanceByBatchActivityBehaviour.finish(execution);
      } else {
        // still waiting
        reschedule(commandContext);
      }
    }
  }

  private Batch createSignalBatch(String calledProcessDefinitionId, ExecutionEntity execution, final String signal) {
    List<Execution> list = execution.getProcessEngineServices().getRuntimeService().createExecutionQuery().processDefinitionId(calledProcessDefinitionId)
        .variableValueEquals(MultiInstanceByBatchProcessEnginePlugin.PARENT_EXECUTION_ID, execution.getId()).signalEventSubscriptionName(signal).list();

    List<SignalProcessInstanceData> data = new ArrayList<SignalProcessInstanceBatchJobHandler.SignalProcessInstanceData>(list.size());
    for (Execution child : list) {
      data.add(new SignalProcessInstanceData(child.getId(), signal));
    }

    Batch batch = CustomBatchBuilder //
        .of(data) //
        .jobHandler(SignalProcessInstanceBatchJobHandler.INSTANCE) //
        .jobsPerSeed(JOBS_PER_SEED) //
        .invocationsPerBatchJob(INVOCATIONS_PER_BATCH_JOB) //
        .create();
    return batch;
  }

  private void reschedule(CommandContext commandContext) {
    TimerEntity currentJob = (TimerEntity) commandContext.getCurrentJob();
    currentJob.createNewTimerJob(currentJob.calculateRepeat());
  }

  @Override
  public CheckStartProcessInstanceBatchJobConfiguration newConfiguration(String canonicalString) {
    String[] split = canonicalString.split(DELIMITER, 2);
    if (split.length != 2) {
      throw new ProcessEngineException("CheckStartProcessInstanceBatchJobConfiguration could not be parsed: " + canonicalString);
    }

    return new CheckStartProcessInstanceBatchJobConfiguration(Integer.parseInt(split[0]), split[1]);
  }

  @Override
  public void onDelete(CheckStartProcessInstanceBatchJobConfiguration configuration, JobEntity jobEntity) {
    // do nothing
  }

  public static class CheckStartProcessInstanceBatchJobConfiguration implements JobHandlerConfiguration {
    String batchId;
    int totalInstances;

    public CheckStartProcessInstanceBatchJobConfiguration(int totalInstances, String batchId) {
      this.totalInstances = totalInstances;
      this.batchId = batchId;
    }

    public int getTotalInstances() {
      return totalInstances;
    }

    public String getBatchId() {
      return batchId;
    }

    @Override
    public String toCanonicalString() {
      return Integer.toString(totalInstances) + DELIMITER + batchId;
    }

  }

}
