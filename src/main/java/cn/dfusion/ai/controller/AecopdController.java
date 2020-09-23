package cn.dfusion.ai.controller;

import org.flowable.bpmn.model.*;
import org.flowable.engine.FormService;
import org.flowable.engine.*;
import org.flowable.engine.form.FormProperty;
import org.flowable.engine.form.StartFormData;
import org.flowable.engine.form.TaskFormData;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.persistence.entity.HistoricProcessInstanceEntity;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.form.api.*;
import org.flowable.form.model.FormField;
import org.flowable.form.model.SimpleFormModel;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author LDF
 * @date 2020/7/8
 */
@RestController
@RequestMapping("copd")
public class AecopdController {

    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private ProcessEngine processEngine;
    @Autowired
    private FormService formService;
    @Autowired
    private FormRepositoryService formRepositoryService;


    @GetMapping
    public Map<String, Object> getForm(@RequestParam(defaultValue = "fd") String processKey) {
        Map<String, Object> result = new HashMap<>();
        ProcessDefinition pd = repositoryService.createProcessDefinitionQuery().processDefinitionKey(processKey)
                .latestVersion().singleResult();
        StartFormData form = formService.getStartFormData(pd.getId());
        FormInfo info = formRepositoryService.getFormModelByKey(form.getFormKey());
        result.put("form", info.getFormModel());
        return result;
    }

    @GetMapping("first")
    public Map<String, Object> start2(String processKey, String outcome, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        Map<String, String> param = new HashMap<>();
        ProcessDefinition pd = repositoryService.createProcessDefinitionQuery().processDefinitionKey(processKey)
                .latestVersion().singleResult();

        param.put("name", "老刘");
        ProcessInstance pi = formService.submitStartFormData(pd.getId(), UUID.randomUUID().toString(), param);
        result.put("result", pi.getId());
        return result;
    }

    @GetMapping("getTaskForm")
    public Map<String, Object> getTaskForm(String processId) {
        Map<String, Object> result = new HashMap<>();
        Task task = taskService.createTaskQuery().processInstanceId(processId).active().singleResult();
        if (null != task && null != task.getFormKey()) {
            TaskFormData form = formService.getTaskFormData(task.getId());
            FormInfo info = formRepositoryService.getFormModelByKey(form.getFormKey());
            result.put("form", info.getFormModel());
        }
        return result;
    }


    @GetMapping("decide")
    public Map<String, Object> decide(String processId, HttpServletRequest request) {
        int i = 0;
        Map<String, Object> map = new LinkedHashMap<>();
        String tips = "";
        while (true) {
            i++;
            if (null != this.getTaskForm(processId) && this.getTaskForm(processId).size() > 0) {
                //带表单任务自动推进
                if (null != request && null != request.getParameter("choice")) {
                    this.push(processId, request);
                    request = null;
                } else {
                    map.putAll(this.getTaskForm(processId));
                    break;
                }
            } else {
                //不带表单任务自动推进
                Map<String, Object> retMap = this.push(processId, request);
                if (null != retMap) {
                    System.out.println("---->" + retMap);
                    tips += retMap.get("tips") + "<br/>";
                }else {
                    tips += "【流程结束】AECOPD AI诊断结束";
                    break;
                }
            }
        }
        map.put("tips", tips);
        map.put("i", i);
        return map;
    }

    @GetMapping("push")
    public Map<String, Object> push(String processId, HttpServletRequest request) {

//        ProcessDefinition pd = repositoryService.createProcessDefinitionQuery().processDefinitionKey("fd")
//                .latestVersion().singleResult();
        List<FormDefinition> formDefinitionList = getFormDefinitions();

        //StartFormData form = formService.getStartFormData(pd.getId());
        //result.put("isApprove",true);
        Map<String, Object> variables = new HashMap<>();
        Task task = taskService.createTaskQuery().processInstanceId(processId).active().singleResult();
        if (task == null) {
            return null;
        }
        if (null != task.getFormKey() && null != request.getParameter("choice")) {
            TaskFormData form = formService.getTaskFormData(task.getId());
            SimpleFormModel info = (SimpleFormModel) formRepositoryService.getFormModelByKey(form.getFormKey()).getFormModel();

            for (FormField field : info.getFields()) {
                variables.putIfAbsent(field.getId(), request.getParameter(field.getId()));
            }
            System.out.println("param：" + variables);

            System.err.println("=====>" + runtimeService.getVariable(processId, "name"));
            System.err.println("=====>" + runtimeService.getVariable(processId, "message1"));
            ///////
            for (FormDefinition formDefinition : formDefinitionList) {
                if (task.getFormKey().equals(formDefinition.getKey())) {
                    //带表单的完成审批
                    taskService.completeTaskWithForm(task.getId(), formDefinition.getId(), "outcome", variables);
                    System.out.println("带表单的完成审批");
                }
            }
        } else {
            //不带表单
            taskService.complete(task.getId(), variables);
            System.out.println("不带表单的完成审批");
        }

        System.out.println("=====>" + task.getName());
        String tips = task.getDescription();
        if (null != tips) {
            System.err.println("描述信息=====>" + tips);
            variables.put("tips", tips);
        }

        return variables;
    }

    private List<FormDefinition> getFormDefinitions() {
        Deployment deployment = repositoryService.createDeployment()
                .name("慢阻肺AI诊疗流程")
                .addClasspathResource("processes/copd.bpmn20.xml")
                .deploy();
        FormDeployment formDeploymentg = formRepositoryService.createDeployment()
                .name("all")
                .addClasspathResource("forms/g1.form")
                .addClasspathResource("forms/g2.form")
                .addClasspathResource("forms/g3.form")
                .addClasspathResource("forms/g4.form")
                .addClasspathResource("forms/g5.form")
                .addClasspathResource("forms/g6.form")
                .addClasspathResource("forms/g7.form")
                .addClasspathResource("forms/g8.form")
                .addClasspathResource("forms/g9.form")
                .addClasspathResource("forms/g10.form")
                .addClasspathResource("forms/g11.form")
                .addClasspathResource("forms/g12.form")
                .addClasspathResource("forms/g13.form")
                .addClasspathResource("forms/g14.form")
                .addClasspathResource("forms/g15.form")
                .addClasspathResource("forms/g16.form")
                .addClasspathResource("forms/g17.form")
                .addClasspathResource("forms/g18.form")
                .addClasspathResource("forms/g19.form")
                .addClasspathResource("forms/g20.form")
                .parentDeploymentId(deployment.getId())
                .deploy();
        return formRepositoryService.createFormDefinitionQuery().deploymentId(formDeploymentg.getId()).list();
    }

    /**
     * 创建启动流程
     *
     * @return
     */
    @GetMapping("start")
    public String start() {
        Map<String, Object> map = new HashMap<>();
        map.put("patientId", 1);
        map.put("doctorId", 2);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("copd1");
        return processInstance.getId();
    }

    /**
     * 获取指定用户组流程任务列表
     *
     * @param group
     * @return
     */
    @GetMapping("list")
    public Object list(String group) {
        List<Task> tasks = taskService.createTaskQuery().orderByTaskCreateTime().desc().list();
        for (Task task : tasks) {
            System.out.println(task.toString());
        }
        return tasks.toString();
    }

    /**
     * 查看历史流程记录
     *
     * @param processInstanceId
     * @return
     */
    @GetMapping("historyList")
    public Object getHistoryList(String processInstanceId) {
        List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId).finished().orderByHistoricActivityInstanceEndTime().asc().list();

        return historicActivityInstances;
    }

    /**
     * 驳回流程实例
     *
     * @param processId
     * @param targetTaskKey
     * @return
     */
    @GetMapping("rollbask")
    public String rollbaskTask(String processId, String targetTaskKey) {
        Task currentTask = taskService.createTaskQuery().processInstanceId(processId).active().singleResult();
        if (currentTask == null) {
            return "节点不存在";
        }
        List<String> key = new ArrayList<>();
        key.add(currentTask.getTaskDefinitionKey());


        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(currentTask.getProcessInstanceId())
                .moveActivityIdsToSingleActivityId(key, targetTaskKey)
                .changeState();
        return "驳回成功...";
    }

    /**
     * 终止流程实例
     *
     * @param processInstanceId
     */
    public String deleteProcessInstanceById(String processInstanceId) {
        // ""删除原因
        runtimeService.deleteProcessInstance(processInstanceId, "");
        return "终止流程实例成功";
    }

    /**
     * 挂起流程实例
     *
     * @param processInstanceId 当前流程实例id
     */
    @GetMapping("hangUp")
    public String handUpProcessInstance(String processInstanceId) {
        runtimeService.suspendProcessInstanceById(processInstanceId);
        return "挂起流程成功...";
    }

    /**
     * 恢复（唤醒）被挂起的流程实例
     *
     * @param processInstanceId 流程实例id
     */
    @GetMapping("recovery")
    public String activateProcessInstance(String processInstanceId) {
        runtimeService.activateProcessInstanceById(processInstanceId);
        return "恢复流程成功...";
    }

    /**
     * 判断传入流程实例在运行中是否存在
     *
     * @param processInstanceId
     * @return
     */
    @GetMapping("isExist/running")
    public Boolean isExistProcIntRunning(String processInstanceId) {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        if (processInstance == null) {
            return false;
        }
        return true;
    }

    /**
     * 判断流程实例在历史记录中是否存在
     *
     * @param processInstanceId
     * @return
     */
    @GetMapping("isExist/history")
    public Boolean isExistProcInHistory(String processInstanceId) {
        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        if (historicProcessInstance == null) {
            return false;
        }
        return true;
    }


    /**
     * 我发起的流程实例列表
     *
     * @param userId
     * @return 流程实例列表
     */
    @GetMapping("myTasks")
    public List<HistoricProcessInstance> getMyStartProcint(String userId) {
        List<HistoricProcessInstance> list = historyService
                .createHistoricProcessInstanceQuery()
                .startedBy(userId)
                .orderByProcessInstanceStartTime()
                .asc()
                .list();
        return list;
    }

    /**
     * 查询流程图
     *
     * @param httpServletResponse
     * @param processId
     * @throws Exception
     */
    @GetMapping("processDiagram")
    public void genProcessDiagram(HttpServletResponse httpServletResponse, String processId) throws Exception {
        ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(processId).singleResult();

        //流程走完的不显示图
        if (pi == null) {
            return;
        }
        Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        //使用流程实例ID，查询正在执行的执行对象表，返回流程实例对象
        String InstanceId = task.getProcessInstanceId();
        List<Execution> executions = runtimeService
                .createExecutionQuery()
                .processInstanceId(InstanceId)
                .list();

        //得到正在执行的Activity的Id
        List<String> activityIds = new ArrayList<>();
        List<String> flows = new ArrayList<>();
        for (Execution exe : executions) {
            List<String> ids = runtimeService.getActiveActivityIds(exe.getId());
            activityIds.addAll(ids);
        }

        //获取流程图
        BpmnModel bpmnModel = repositoryService.getBpmnModel(pi.getProcessDefinitionId());
        ProcessEngineConfiguration engconf = processEngine.getProcessEngineConfiguration();
        ProcessDiagramGenerator diagramGenerator = engconf.getProcessDiagramGenerator();
        InputStream in = diagramGenerator.generateDiagram(bpmnModel, "png", activityIds, flows, engconf.getActivityFontName(), engconf.getLabelFontName(), engconf.getAnnotationFontName(), engconf.getClassLoader(), 1.0, true);
        OutputStream out = null;
        byte[] buf = new byte[1024];
        int legth = 0;
        try {
            out = httpServletResponse.getOutputStream();
            while ((legth = in.read(buf)) != -1) {
                out.write(buf, 0, legth);
            }
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }

}

