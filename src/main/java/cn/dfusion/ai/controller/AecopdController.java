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
        result.put("result", info.getFormModel());
        return result;
    }

    @PostMapping
    public Map<String, Object> start1(String processKey, String outcome, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        ProcessDefinition pd = repositoryService.createProcessDefinitionQuery().processDefinitionKey(processKey)
                .latestVersion().singleResult();
        StartFormData form = formService.getStartFormData(pd.getId());
        SimpleFormModel info = (SimpleFormModel) formRepositoryService.getFormModelByKey(form.getFormKey()).getFormModel();

        Map<String, Object> properties = new HashMap<>();

        for(FormField field : info.getFields()) {
            properties.putIfAbsent(field.getId(), request.getParameter(field.getId()));
        }

        ProcessInstance pi = runtimeService.startProcessInstanceWithForm(pd.getId(), outcome, properties, pd.getName());
        result.put("result", pi.getId());
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
        if(null!=task && null!=task.getFormKey()) {
            TaskFormData form = formService.getTaskFormData(task.getId());
            FormInfo info = formRepositoryService.getFormModelByKey(form.getFormKey());
            result.put("result", info.getFormModel());
        }
        return result;
    }
    @GetMapping("push")
    public Map<String, Object> push(String processId) {
        Map<String, Object> result = new HashMap<>();
        result.put("isApprove",true);
//        ProcessDefinition pd = repositoryService.createProcessDefinitionQuery().processDefinitionKey("fd")
//                .latestVersion().singleResult();
        Task task = taskService.createTaskQuery().processInstanceId(processId).active().singleResult();
        System.err.println("=====>" + task.getName());
        System.err.println("=====>" + runtimeService.getVariable(processId, "name" ));
        System.err.println("=====>" + runtimeService.getVariable(processId, "message1" ));
        testNextTasks(processId);
//        if(null!=task||null!=task.getFormKey()) {
//            //带表单的完成审批
//            taskService.completeTaskWithForm(task.getId(), "", "", new HashMap<>());
//        }else {
            //不带表单
            taskService.complete(task.getId(), result);
//        }
        //当前任务信息
        //Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        return result;
    }

    /**
     * 获取下一个节点的信息测试
     */
    private void testNextTasks(String processInstanceId) {

        //流程实例id
        //String processInstanceId = "5b945750-81db-11e9-a576-1a73f8e23adc";

        //当前任务信息
        Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();

        //获取流程发布Id信息
        String definitionId = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult().getProcessDefinitionId();

        //获取bpm对象
        BpmnModel bpmnModel = repositoryService.getBpmnModel(definitionId);

        //传节点定义key 获取当前节点
        FlowNode flowNode = (FlowNode) bpmnModel.getFlowElement(task.getTaskDefinitionKey());

        //输出连线
        List<SequenceFlow> outgoingFlows = flowNode.getOutgoingFlows();

        //遍历返回下一个节点信息
        for (SequenceFlow outgoingFlow : outgoingFlows) {
            //类型自己判断
            FlowElement targetFlowElement = outgoingFlow.getTargetFlowElement();
            //用户任务
            if (targetFlowElement instanceof UserTask) {
                System.out.println("1用户任务节点：" + targetFlowElement.getName());
            }else if (targetFlowElement instanceof ServiceTask) {
                System.out.println("1服务任务节点：" + targetFlowElement.getName());
            }else if (targetFlowElement instanceof ExclusiveGateway) {
                setExclusiveGateway(targetFlowElement);
            }
        }
    }


    private void setExclusiveGateway(FlowElement targetFlow) {
        //排他网关，获取连线信息
        List<SequenceFlow> targetFlows = ((ExclusiveGateway) targetFlow).getOutgoingFlows();
        for (SequenceFlow sequenceFlow : targetFlows) {
            //目标节点信息
            FlowElement targetFlowElement = sequenceFlow.getTargetFlowElement();
            if (targetFlowElement instanceof UserTask) {
                System.out.println("2用户任务节点：" + targetFlowElement.getName());
                // do something
            } else if (targetFlowElement instanceof EndEvent) {
                System.out.println("2终点：" + targetFlowElement.getName());
                // do something
            } else if (targetFlowElement instanceof ServiceTask) {
                // do something
                System.out.println("2服务任务节点：" + targetFlowElement.getName());
            } else if (targetFlowElement instanceof ExclusiveGateway) {
                //递归寻找
                setExclusiveGateway(targetFlowElement);
            } else if (targetFlowElement instanceof SubProcess) {
                // do something
            }
        }
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
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("fd");
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
     * 获取指定用户组流程任务列表
     *
     * @return
     */
    @GetMapping("getNextId")
    public String getNextId(String processId) {
        Task task = taskService.createTaskQuery().processInstanceId(processId).active().singleResult();
        return task.getId();
    }

    @GetMapping("assess1")
    public String assess(String processId,
                         String age,
                         Double growthRate,
                         Integer fatherHeightPercentile,
                         Integer motherHeightPercentile) {
        Task task = taskService.createTaskQuery().processInstanceId(processId).active().singleResult();
        if (task == null) {
            return "流程不存在";
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("parentHeight", age);
        taskService.complete(task.getId(), variables);
        String tips = "生长速率 --> 正常";
        return tips;
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
        // ""这个参数本来可以写删除原因
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

    /**
     * 流程以及表单的部署
     */
    @RequestMapping(value = "nform")
    public Map<String, Object> deployTest() {
        Deployment deployment;
        deployment = repositoryService.createDeployment()
                .name("表单流程")
                .addClasspathResource("processes/fd.bpmn20.xml")
                .deploy();

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().
                deploymentId(deployment.getId())
                .singleResult();
        String processDefinitionId = processDefinition.getId();
        FormDeployment formDeployment = formRepositoryService.createDeployment()
                .name("formd")
                .addClasspathResource("forms/d.form")
                .parentDeploymentId(deployment.getId())
                .deploy();
        FormDeployment formDeploymentg1 = formRepositoryService.createDeployment()
                .name("formg1")
                .addClasspathResource("forms/g1.form")
                .parentDeploymentId(deployment.getId())
                .deploy();
        FormDefinition formDefinition = formRepositoryService.createFormDefinitionQuery().deploymentId(formDeploymentg1.getId()).singleResult();
        FormDefinition formDefinitiong1 = formRepositoryService.createFormDefinitionQuery().deploymentId(formDeploymentg1.getId()).singleResult();
        String formDefinitionId = formDefinition.getId();
        String formDefinitionIdg1 = formDefinitiong1.getId();


        System.out.println("是否有开始表单: " + processDefinition.hasStartFormKey());
        StartFormData form = formService.getStartFormData(processDefinitionId);
        FormInfo info = formRepositoryService.getFormModelByKey(form.getFormKey());
        FormModel model = info.getFormModel();
        SimpleFormModel sim = (SimpleFormModel) model;
        System.out.println("form--->"+form.toString());
        System.out.println("info--->"+info.toString());
        System.out.println("model--->"+model.toString());
        System.out.println("sim--->"+sim.getFields());
        System.out.println("formproperty--->"+form.getFormProperties());
//        Object o = formService.getRenderedStartForm(processDefinitionId);
//        System.err.println("===> " + o);



        List<FormProperty> formProperties1 = form.getFormProperties();
        for (FormProperty formProperty : formProperties1) {
            System.err.println(formProperty.toString());

        }

        //启动实例并且设置表单的值
        String outcome = "laoliu";
        Map<String, Object> formProperties = new HashMap<>();
        formProperties.put("reason", "家里有事");
        formProperties.put("startTime", "2020-8-17");
        formProperties.put("endTime", "2020-8-17");
        String processInstanceName = "formDemo";
        runtimeService.startProcessInstanceWithForm(processDefinitionId, outcome, formProperties, processInstanceName);
        HistoricProcessInstanceEntity historicProcessInstanceEntity = (HistoricProcessInstanceEntity) historyService.createHistoricProcessInstanceQuery()
                .processDefinitionId(processDefinitionId)
                .singleResult();
        String processInstanceId = historicProcessInstanceEntity.getProcessInstanceId();


        //查询表单信息
        SimpleFormModel fm = (SimpleFormModel) runtimeService.getStartFormModel(processDefinitionId, processInstanceId).getFormModel();
        //FormInfo fm = runtimeService.getStartFormModel(processDefinitionId, processInstanceId);


        System.err.println("==============>"+formService.getStartFormKey(processDefinitionId));

//        System.err.println("==============>"+formService.getRenderedStartForm(processDefinitionId));
        System.out.println(fm.getKey());
        System.out.println(fm.getKey());
        System.out.println(fm.getName());
        System.out.println(fm.getOutcomeVariableName());
        System.err.println(fm.getVersion());
        List<FormField> fields = fm.getFields();
        for (FormField ff : fields) {
            System.out.println("######################");
            System.out.println(ff.getId());
            System.out.println(ff.getName());
            System.out.println(ff.getType());
            System.out.println(ff.getPlaceholder());
            System.out.println(ff.getValue());

            System.out.println("######################");

        }
        Map<String, Object> map = new HashMap<>();
        map.put("result", info.getFormModel());
        return map;
//        //查询个人任务并填写表单
//        Map<String, Object> formProperties2 = new HashMap<>();
//        formProperties2.put("reason", "家里有事2222");
//        formProperties2.put("startTime", "2020-8-18");
//        formProperties2.put("endTime", "2020-8-18");
//        formProperties2.put("days", "3");
//        Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
//        System.out.println("是否有任务表单: " + task.getFormKey());
//        String taskId = task.getId();
//        String outcome2 = "老刘";
//        taskService.completeTaskWithForm(taskId, formDefinitionId, outcome2, formProperties2);
//
//        //获取个人任务表单
//        FormInfo taskFm = taskService.getTaskFormModel(taskId);
//
//
//        SimpleFormModel sfm = (SimpleFormModel) taskFm.getFormModel();
//        System.out.println(sfm);
//
//        List<FormField> formFields = sfm.getFields();
//        for (FormField ff : formFields) {
//            System.out.println("######################");
//            System.out.println(ff.getId());
//            System.out.println(ff.getName());
//            System.out.println(ff.getType());
//            System.out.println(ff.getPlaceholder());
//            System.out.println(ff.getValue());
//            System.out.println("######################");
//
//        }
    }

}
