package com.camunda.consulting.multi_instance_by_batch_plugin;

import java.util.ArrayList;
import java.util.List;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.impl.batch.BatchJobHandler;
import org.camunda.bpm.engine.impl.bpmn.behavior.ParallelMultiInstanceActivityBehavior;
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParseListener;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.camunda.bpm.engine.impl.jobexecutor.JobHandler;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl;
import org.camunda.bpm.engine.impl.util.xml.Element;

import com.camunda.consulting.multi_instance_by_batch_plugin.batch.SignalProcessInstanceBatchJobHandler;
import com.camunda.consulting.multi_instance_by_batch_plugin.batch.StartProcessInstanceBatchJobHandler;
import com.camunda.consulting.multi_instance_by_batch_plugin.job.CheckStartProcessInstanceBatchJobHandler;

public class MultiInstanceByBatchProcessEnginePlugin implements ProcessEnginePlugin {

  public static final String SIGNAL_COMPENSATE = "Signal_Compensate";
  public static final String SIGNAL_COMPLETE = "Signal_Complete";
  public static final String VARIABLE_COMPENSATE = "compensate";
  public static final String ERROR_CODE_INSTANCE_FAILED = "InstanceFailed";

  public static final String PARENT_EXECUTION_ID = "MIBB_parent_exection_id";
  public static final String CALLED_PROCDEF_ID = "MIBB_called_procdef_id";

  @SuppressWarnings("rawtypes")
  @Override
  public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
    if (processEngineConfiguration.getCustomBatchJobHandlers() == null) {
      processEngineConfiguration.setCustomBatchJobHandlers(new ArrayList<BatchJobHandler<?>>());
    }

    processEngineConfiguration.getCustomBatchJobHandlers().add(StartProcessInstanceBatchJobHandler.INSTANCE);
    processEngineConfiguration.getCustomBatchJobHandlers().add(SignalProcessInstanceBatchJobHandler.INSTANCE);

    if (processEngineConfiguration.getCustomJobHandlers() == null) {
      processEngineConfiguration.setCustomJobHandlers(new ArrayList<JobHandler>());
    }
    processEngineConfiguration.getCustomJobHandlers().add(CheckStartProcessInstanceBatchJobHandler.INSTANCE);

    List<BpmnParseListener> parseListeners = processEngineConfiguration.getCustomPreBPMNParseListeners();
    if (parseListeners == null) {
      parseListeners = new ArrayList<BpmnParseListener>();
      processEngineConfiguration.setCustomPreBPMNParseListeners(parseListeners);
    }
    parseListeners.add(new AbstractBpmnParseListener() {

      @Override
      public void parseMultiInstanceLoopCharacteristics(Element activityElement, Element multiInstanceLoopCharacteristicsElement, ActivityImpl activity) {
        // TODO: only for explicitly configures ones
        activity.setActivityBehavior(new MultiInstanceByBatchActivityBehaviour((ParallelMultiInstanceActivityBehavior) activity.getActivityBehavior()));
      }

      @Override
      public void parseCallActivity(Element callActivityElement, ScopeImpl scope, ActivityImpl activity) {
        if (scope != null && scope.getActivityBehavior() instanceof ParallelMultiInstanceActivityBehavior) {
          // TODO: only for explicitly configures ones
          // activity.setActivityBehavior(new
          // MultiInstanceByBatchActivityBehaviour((ParallelMultiInstanceActivityBehavior)
          // scope.getActivityBehavior()));
        }
      }
    });
  }

  @Override
  public void postInit(ProcessEngineConfigurationImpl processEngineConfiguration) {

  }

  @Override
  public void postProcessEngineBuild(ProcessEngine processEngine) {

  }

}
