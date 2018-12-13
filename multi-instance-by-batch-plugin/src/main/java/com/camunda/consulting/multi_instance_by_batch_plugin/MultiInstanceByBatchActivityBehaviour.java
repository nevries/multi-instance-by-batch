package com.camunda.consulting.multi_instance_by_batch_plugin;

import static org.camunda.bpm.engine.impl.util.CallableElementUtil.getProcessDefinitionToCall;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.batch.Batch;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.impl.bpmn.behavior.CallActivityBehavior;
import org.camunda.bpm.engine.impl.bpmn.behavior.ParallelMultiInstanceActivityBehavior;
import org.camunda.bpm.engine.impl.core.model.CallableElement;
import org.camunda.bpm.engine.impl.el.FixedValue;
import org.camunda.bpm.engine.impl.jobexecutor.TimerDeclarationImpl;
import org.camunda.bpm.engine.impl.jobexecutor.TimerDeclarationType;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.pvm.PvmActivity;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.camunda.bpm.engine.impl.pvm.process.ProcessDefinitionImpl;
import org.camunda.bpm.extension.batch.CustomBatchBuilder;

import com.camunda.consulting.multi_instance_by_batch_plugin.batch.StartProcessInstanceBatchJobHandler;
import com.camunda.consulting.multi_instance_by_batch_plugin.batch.StartProcessInstanceBatchJobHandler.StartProcessInstanceData;
import com.camunda.consulting.multi_instance_by_batch_plugin.job.CheckStartProcessInstanceBatchJobHandler;

public class MultiInstanceByBatchActivityBehaviour extends ParallelMultiInstanceActivityBehavior {

  // TODO: make configurable
  private static final int INVOCATIONS_PER_JOB = 10;
  private static final int JOBS_PER_SEED = 10;
  private static final String REPEAT_INTERVAL = "R/PT10S";

  public MultiInstanceByBatchActivityBehaviour(ParallelMultiInstanceActivityBehavior original) {
    setCollectionElementVariable(original.getCollectionElementVariable());
    setCollectionExpression(original.getCollectionExpression());
    setCollectionVariable(original.getCollectionVariable());
    setCompletionConditionExpression(original.getCompletionConditionExpression());
    setLoopCardinalityExpression(original.getLoopCardinalityExpression());
  }

  @Override
  protected void createInstances(ActivityExecution execution, int nrOfInstances) throws Exception {
    PvmActivity innerActivity = getInnerActivity(execution.getActivity());

    // we want to start new processes by batch, this only works if the child is
    // a call activity
    if (!(innerActivity.getActivityBehavior() instanceof CallActivityBehavior)) {
      throw new ProcessEngineException("Multi instance by batch only works for multi instance call activities.");
    }
    CallableElement callableElement = ((CallActivityBehavior) innerActivity.getActivityBehavior()).getCallableElement();

    List<StartProcessInstanceData> data = prepareInstanceData(execution, nrOfInstances, callableElement);

    Batch batch = createBatch(data);

    createObserverJob((ExecutionEntity) execution, nrOfInstances, batch);
  }

  private void createObserverJob(ExecutionEntity execution, int nrOfInstances, Batch batch) {
    ExecutionEntity executionEntity = execution;

    TimerDeclarationImpl timerDeclaration = new TimerDeclarationImpl(new FixedValue(REPEAT_INTERVAL), TimerDeclarationType.CYCLE,
        CheckStartProcessInstanceBatchJobHandler.TYPE);
    timerDeclaration.setRawJobHandlerConfiguration(
        new CheckStartProcessInstanceBatchJobHandler.CheckStartProcessInstanceBatchJobConfiguration(nrOfInstances, batch.getId()).toCanonicalString());
    timerDeclaration.createTimer(executionEntity);
  }

  private Batch createBatch(List<StartProcessInstanceData> data) {
    return CustomBatchBuilder //
          .of(data) //
          .jobHandler(StartProcessInstanceBatchJobHandler.INSTANCE) //
          .jobsPerSeed(JOBS_PER_SEED).invocationsPerBatchJob(INVOCATIONS_PER_JOB).create();
  }

  private List<StartProcessInstanceData> prepareInstanceData(ActivityExecution execution, int nrOfInstances, CallableElement callableElement) {
    // process definition to call
    ProcessDefinitionImpl definition = getProcessDefinitionToCall(execution, callableElement);

    execution.setVariableLocal(MultiInstanceByBatchProcessEnginePlugin.CALLED_PROCDEF_ID, definition.getId());

    List<StartProcessInstanceData> data = new ArrayList<StartProcessInstanceData>(nrOfInstances);
    for (int i = 0; i < nrOfInstances; i++) {
      // businessKey
      String businessKey = callableElement.getBusinessKey(execution);

      // variables
      Map<String, Object> variables = new HashMap<String, Object>();
      // ... loop counter
      variables.put(LOOP_COUNTER, i);
      // ... current element
      if (usesCollection() && collectionElementVariable != null) {
        Collection<?> collection = null;
        if (collectionExpression != null) {
          collection = (Collection<?>) collectionExpression.getValue(execution);
        } else if (collectionVariable != null) {
          collection = (Collection<?>) execution.getVariable(collectionVariable);
        }

        Object value = getElementAtIndex(i, collection);
        variables.put(collectionElementVariable, value);
      }
      // ... input mapping
      variables.putAll(callableElement.getInputVariables(execution));
      // ... reference to parent execution
      variables.put(MultiInstanceByBatchProcessEnginePlugin.PARENT_EXECUTION_ID, execution.getId());

      data.add(new StartProcessInstanceData(definition.getId(), execution.getId(), businessKey, variables));
    }
    return data;
  }

  @Override
  public void propagateBpmnError(BpmnError error, ActivityExecution execution) throws Exception {
    super.propagateBpmnError(error, execution);
  }

  public void finish(ActivityExecution execution) {
    leave(execution);
  }

}
