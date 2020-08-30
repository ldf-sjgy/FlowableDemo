package com.example.demo;

import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ErrorMsg;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.*;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.HistoricProcessInstanceEntity;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.form.api.FormDefinition;
import org.flowable.form.api.FormDeployment;
import org.flowable.form.api.FormInfo;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.model.FormField;
import org.flowable.form.model.SimpleFormModel;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.Normalizer;
import java.util.*;

/**
 * @author LDF
 * @date 2020/7/8
 */
@RestController
@RequestMapping("basicDiagnosis")
public class TestController {

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

    /**
     * 创建启动流程
     *
     * @return
     */
    @GetMapping("start")
    public String start() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("growth7");
        return processInstance.getId();
    }

    /**
     * 创建流程
     *
     * @param gender
     * @param birthDate
     * @param measureDate
     * @param height
     * @param weight
     * @return
     */
    @GetMapping("add")
    public String addExpense(String name,
                             String heightPercentile,
                             String gender,
                             String birthDate,
                             String measureDate,
                             String height,
                             String weight) {
        Map<String, Object> map = new HashMap<>();
        //基本信息
        map.put("name", name);
        map.put("heightPercentile", heightPercentile);
        map.put("gender", gender);
        map.put("birthDate", birthDate);
        map.put("measureDate", measureDate);
        map.put("height", height);
        map.put("weight", weight);
        //检验检查信息

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("growth7", map);
        return "提交成功,流程ID为：" + processInstance.getId();
    }

    /**
     * 获取指定用户组流程任务列表
     *
     * @param group
     * @return
     */
    @GetMapping("nextId")
    public Object list(String group) {
        List<Task> tasks = taskService.createTaskQuery().taskCandidateGroup(group).list();
        return tasks.get(tasks.size() - 1).getId();
    }

    /**
     * 获取指定用户组流程任务列表
     *
     * @return
     */
    @GetMapping("getNextId")
    public Object getNextId(String processId) {
        Task task = this.processEngine.getTaskService().createTaskQuery().processInstanceId(processId).active().singleResult();
        return task.getId();
    }

    /**
     * 获取任务节点
     *
     * @param node   查询节点选择
     * @param taskId 任务id
     */
    public void nextFlowNode(String node, String taskId) {
        Task task = processEngine.getTaskService().createTaskQuery().taskId(taskId).singleResult();
        ExecutionEntity ee = (ExecutionEntity) processEngine.getRuntimeService().createExecutionQuery()
                .executionId(task.getExecutionId()).singleResult();
        // 当前审批节点
        String crruentActivityId = ee.getActivityId();
        BpmnModel bpmnModel = processEngine.getRepositoryService().getBpmnModel(task.getProcessDefinitionId());
        FlowNode flowNode = (FlowNode) bpmnModel.getFlowElement(crruentActivityId);
        // 输出连线
        List<SequenceFlow> outFlows = flowNode.getOutgoingFlows();
        for (SequenceFlow sequenceFlow : outFlows) {
            //当前审批节点
            if ("now".equals(node)) {
                FlowElement sourceFlowElement = sequenceFlow.getSourceFlowElement();
                System.out.println("当前节点: id=" + sourceFlowElement.getId() + ",name=" + sourceFlowElement.getName());
            } else if ("next".equals(node)) {
                // 下一个审批节点
                FlowElement targetFlow = sequenceFlow.getTargetFlowElement();
                if (targetFlow instanceof UserTask) {
                    System.out.println("下一节点: id=" + targetFlow.getId() + ",name=" + targetFlow.getName());
                }
                // 如果下个审批节点为结束节点
                if (targetFlow instanceof EndEvent) {
                    System.out.println("下一节点为结束节点：id=" + targetFlow.getId() + ",name=" + targetFlow.getName());
                }
            }


        }
    }

    @GetMapping("assess")
    public String assess(String taskId, String a1, String a2, String a3) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return "流程不存在";
        }
        a1 = "是";
        a2 = "是";
        Map<String, Object> variables = new HashMap<>();
        if (StringUtils.isNotBlank(a1)) {
            variables.put("growthRate", Boolean.valueOf("true"));
        } else {
            variables.put("growthRate", Boolean.valueOf("false"));
        }
        if (a2.equals("是")) {
            variables.put("parentHeight", Boolean.valueOf("true"));
        } else {
            variables.put("parentHeight", Boolean.valueOf("false"));
        }
        taskService.complete(taskId, variables);
        String tips = "正常";
        if ((variables.get("growthRate") == null || (Boolean) variables.get("growthRate")) && !((Boolean) variables.get("parentHeight"))) {
            tips = "家族矮小史";
        } else if (!(Boolean) variables.get("growthRate") && ((Boolean) variables.get("parentHeight"))) {
            tips = "年生长速率低";
        } else if (!(Boolean) variables.get("growthRate") && !((Boolean) variables.get("parentHeight"))) {
            tips = "年生长速率低且家族矮小史";
        }
        return tips;
    }

    @GetMapping("assess1")
    public String assess1(String taskId, String a1, String a2, String a3) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return "流程不存在";
        }
        Map<String, Object> variables = new HashMap<>();
        if (a1 == null) {
            variables.put("growthRate", null);
        } else if (a1.equals("是")) {
            variables.put("growthRate", true);
        } else if (a1.equals("否")) {
            variables.put("growthRate", false);
        }
        if (a2.equals("是")) {
            variables.put("parentHeight", Boolean.valueOf("true"));
        } else {
            variables.put("parentHeight", Boolean.valueOf("false"));
        }
        taskService.complete(taskId, variables);
        String tips = "";
        if (a1 == null) {
            tips = "growthRate is null";
        } else if (a1.equals("是")) {
            tips = "ture";
        } else if (a1.equals("否")) {
            tips = "false";
        }
        return tips;
    }

    @GetMapping("assess2")
    public String assessPregnantWeek(String taskId, String a1) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return "流程不存在";
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("pregnantWeek", a1);
        taskService.complete(taskId, variables);
        int pregnantWeek = Integer.parseInt(a1);
        String tips = "正常";
        if (pregnantWeek < 37) {
            tips = "早产";
        } else if (pregnantWeek > 42) {
            tips = "过期产";
        }
        return tips;
    }

    @GetMapping("assess3")
    public String assessSGA(String taskId, String a1, String a2, String a3, String a4) {
        String tips = "正常";
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return "流程不存在";
        }
        Map<String, Object> variables = new HashMap<>();
        int pregnantWeek = Integer.parseInt(a1);
        double height = Double.parseDouble(a2);
        double weight = Double.parseDouble(a3);
        String gender = a4;
        boolean isSGA = false;
        if (pregnantWeek == 21 && weight <= 0.32) {
            isSGA = true;
        } else if (pregnantWeek == 22 && weight <= 0.32) {
            isSGA = true;
        } else if (pregnantWeek == 23 && weight <= 0.365) {
            isSGA = true;
        } else if (pregnantWeek == 24 && weight <= 0.417) {
            isSGA = true;
        } else if (pregnantWeek == 25 && weight <= 0.477) {
            isSGA = true;
        } else if (pregnantWeek == 26 && weight <= 0.546) {
            isSGA = true;
        } else if (pregnantWeek == 27 && weight <= 0.627) {
            isSGA = true;
        } else if (pregnantWeek == 28 && weight <= 0.72) {
            isSGA = true;
        } else if (pregnantWeek == 29 && weight <= 0.829) {
            isSGA = true;
        } else if (pregnantWeek == 30 && weight <= 0.955) {
            isSGA = true;
        } else if (pregnantWeek == 31 && weight <= 1.1) {
            isSGA = true;
        } else if (pregnantWeek == 32 && weight <= 1.284) {
            isSGA = true;
        } else if (pregnantWeek == 33 && weight <= 1.499) {
            isSGA = true;
        } else if (pregnantWeek == 34 && weight <= 1.728) {
            isSGA = true;
        } else if (pregnantWeek == 35 && weight <= 1.974) {
            isSGA = true;
        } else if (pregnantWeek == 36 && weight <= 2.224) {
            isSGA = true;
        } else if (pregnantWeek >= 37 && height <= 46.6 && weight <= 0.257 && gender.equals('女')) {
            isSGA = true;
        } else if (pregnantWeek >= 37 && height <= 47.1 && weight <= 0.262 && gender.equals('男')) {
            isSGA = true;
        }
        variables.put("isSGA", isSGA);
        taskService.complete(taskId, variables);
        if (isSGA) {
            tips = "符合SGA，进一步确认是否追赶生长";
        }
        return tips;
    }

    @GetMapping("assess4")
    public String assessFamilyShort(String taskId, String a1, String a2) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return "流程不存在";
        }
        boolean isFamilyShort = false;
        Map<String, Object> variables = new HashMap<>();
        Double grandFatherHeight = Double.parseDouble(a1);
        Double grandMotherHeight = Double.parseDouble(a2);
        if (grandFatherHeight < 160 || grandMotherHeight < 148) {
            isFamilyShort = true;
        }
        variables.put("isFamilyShort", isFamilyShort);

        taskService.complete(taskId, variables);
        String tips = "正常";
        if (isFamilyShort) {
            tips = "家族矮小史";
        }
        return tips;
    }

    @GetMapping("assess5")
    public String assessChronicDisease(String taskId, String a1) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return "流程不存在";
        }
        boolean isChronicDisease = false;
        Map<String, Object> variables = new HashMap<>();
        isChronicDisease = a1.equals("是") ? true : false;
        variables.put("isChronicDisease", isChronicDisease);

        taskService.complete(taskId, variables);
        String tips = "正常";
        if (isChronicDisease) {
            tips = "慢性病史";
        }
        return tips;
    }

    @GetMapping("assess6")
    public String assessDrugAllerg(String taskId, String a1) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return "流程不存在";
        }
        boolean isDrugAllerg = false;
        Map<String, Object> variables = new HashMap<>();
        isDrugAllerg = a1.equals("是") ? true : false;
        variables.put("isDrugAllerg", isDrugAllerg);

        taskService.complete(taskId, variables);
        String tips = "正常";
        if (isDrugAllerg) {
            tips = "药物过敏史";
        }
        return tips;
    }

    @GetMapping("assess7")
    public String assessHeight(String taskId, String a1, String a2) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return "流程不存在";
        }
        Map<String, Object> variables = new HashMap<>();
        int heightPercentile = Integer.parseInt(a1);
        int hereditaryHeightDiff = Integer.parseInt(a2);
        variables.put("heightPercentile", heightPercentile);
        variables.put("hereditaryHeightDiff", hereditaryHeightDiff);

        taskService.complete(taskId, variables);
        String tips = "正常";
        if (heightPercentile < 3) {
            tips = "身材矮小";
        } else if (heightPercentile > 10 && hereditaryHeightDiff < 30){
            tips = "直接跳到assess10：上下部量";
        }
        return tips;
    }

    @GetMapping("assess8")
    public String assessPerinatalInjury(String taskId, String a1) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return "流程不存在";
        }
        boolean isPerinatalInjury = false;
        Map<String, Object> variables = new HashMap<>();
        isPerinatalInjury = a1.equals("是") ? true : false;
        variables.put("isPerinatalInjury", isPerinatalInjury);
        taskService.complete(taskId, variables);
        String tips = "正常";
        if (isPerinatalInjury) {
            tips = "有围产期损伤";
        }
        return tips;
    }

    @GetMapping("assess9")
    public String assessParentDevelopmentDelay(String taskId, String a1) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return "流程不存在";
        }
        boolean isParentDevelopmentDelay = false;
        Map<String, Object> variables = new HashMap<>();
        isParentDevelopmentDelay = a1.equals("是") ? true : false;
        variables.put("isParentDevelopmentDelay", isParentDevelopmentDelay);

        taskService.complete(taskId, variables);
        String tips = "正常";
        if (isParentDevelopmentDelay) {
            tips = "疑似体质性青春期延迟,进一步辅助检查判断";
        }
        return tips;
    }

    @GetMapping("assess10")
    public String assessUpDownMeasure(String taskId, String a1, String a2) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return "流程不存在";
        }
        Double up = Double.parseDouble(a1);
        Double down = Double.parseDouble(a2);
        Map<String, Object> variables = new HashMap<>();
        variables.put("up", up);
        variables.put("down", down);

        taskService.complete(taskId, variables);
        String tips = "正常";
        if (up > down) {
            tips = "疑似软骨发育不良/不全,成骨发育不全、骺软发育不良等";
        } else if (up < down) {
            tips = "疑似粘多糖病、脊椎骨骺发育不良等";
        }
        return tips;
    }

    @GetMapping("assess11")
    public String assessNeedSSS(String taskId, String a1) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return "流程不存在";
        }
        boolean isNeedSSS = false;
        Map<String, Object> variables = new HashMap<>();
        isNeedSSS = a1.equals("是") ? true : false;
        variables.put("isNeedSSS", isNeedSSS);

        taskService.complete(taskId, variables);
        String tips = "不需要进行第二性征查体";
        if (isNeedSSS) {
            tips = "需要进行第二性征查体";
        }
        return tips;
    }

    @GetMapping("assess12")
    public String assessSSS(String taskId, String a1) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return "流程不存在";
        }
        String SSS = a1;
        Map<String, Object> variables = new HashMap<>();
        variables.put("SSS", SSS);
        taskService.complete(taskId, variables);
        String tips = "正常";
        if (SSS.equals("落后")) {
            tips = "青春发育延迟";
        } else if (SSS.equals("提前")) {
            tips = "性早熟";
        }
        return tips;
    }

    @GetMapping("assess13")
    public String assessBoneAge(String taskId, String a1, String a2, String a3, String a4, String a5, String a6, String a7) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return "流程不存在";
        }
        int heightPercentile = Integer.parseInt(a1);
        String heightStatus = "";
        if (heightPercentile >= 25 && heightPercentile <= 75) {
            heightStatus = "正常";
        } else if (heightPercentile > 75) {
            heightStatus = "偏高";
        } else if (heightPercentile < 25) {
            heightStatus = "偏低";
        }
        boolean isDevelopmentDelay = a2.equals("是") ? true : false;
        double growthRate = Double.parseDouble(a3);
        boolean isGrowthRate = false;
        if (growthRate > 6.0) {
            isGrowthRate = true;
        }
        boolean isHereditaryHeightPercentile = isPercentileNormal(a4);
        boolean isBoneAgePercentile = isPercentileNormal(a5);
        double boneAgeDiff = Double.parseDouble(a6);
        String boneAgeStatus = "";
        if (boneAgeDiff < 1 && boneAgeDiff > -1) {
            boneAgeStatus = "骨龄正常";
        } else if (boneAgeDiff <= -1 && boneAgeDiff >= -2) {
            boneAgeStatus = "骨龄落后1-2岁";
        } else if (boneAgeDiff <= -2) {
            boneAgeStatus = "骨龄落后2岁及以上";
        } else if (boneAgeDiff >= 1) {
            boneAgeStatus = "骨龄提前1岁及以上";
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("heightStatus", heightStatus);
        variables.put("isDevelopmentDelay", isDevelopmentDelay);
        variables.put("isGrowthRate", isGrowthRate);
        variables.put("isHereditaryHeightPercentile", isHereditaryHeightPercentile);
        variables.put("isBoneAgePercentile", isBoneAgePercentile);
        variables.put("boneAgeStatus", boneAgeStatus);
        //骨龄提示信息
        String tips = "";
        if (heightStatus.equals("正常") && !isDevelopmentDelay && isGrowthRate && isHereditaryHeightPercentile && isBoneAgePercentile && boneAgeStatus.equals("骨龄正常")) {
            tips = "1.正常人";
            variables.put("result", "1");
        } else if (heightStatus.equals("正常") && !isDevelopmentDelay && isGrowthRate && !isHereditaryHeightPercentile && isBoneAgePercentile && boneAgeStatus.equals("骨龄正常")) {
            tips = "2.孩子生长正常，超过遗传身高，继续观察";
            variables.put("result", "2");
        } else if (heightStatus.equals("偏低") && !isDevelopmentDelay && isGrowthRate && isHereditaryHeightPercentile && isBoneAgePercentile && boneAgeStatus.equals("骨龄落后1-2岁")) {
            tips = "9.可能是晚长，可以考虑观察";
            variables.put("result", "9");
        } else if (heightStatus.equals("偏低") && !isDevelopmentDelay && !isGrowthRate && isHereditaryHeightPercentile && isBoneAgePercentile && boneAgeStatus.equals("骨龄落后1-2岁")) {
            tips = "10.若生长速度慢，那他的身高可能会越来越偏低生长曲线，身高会逐渐矮小，可能需要提前更多关注";
            variables.put("result", "10");
        } else if (heightStatus.equals("偏低") && isDevelopmentDelay && isGrowthRate && isHereditaryHeightPercentile && isBoneAgePercentile && boneAgeStatus.equals("骨龄落后1-2岁")) {
            tips = "14.还要关注他的发育情况，尤其是青春期年龄段的，有部分是青春期发育延迟的，身高可能暂时偏矮";
            variables.put("result", "14");
        } else if (heightStatus.equals("偏低") && !isDevelopmentDelay && isGrowthRate && isHereditaryHeightPercentile && isBoneAgePercentile && boneAgeStatus.equals("骨龄落后2岁及以上")) {
            tips = "11.考虑生长激素缺乏症等的可能，需进一步检查";
            variables.put("result", "11");
        } else if (heightStatus.equals("正常") && !isDevelopmentDelay && !isGrowthRate && isHereditaryHeightPercentile && isBoneAgePercentile && boneAgeStatus.equals("骨龄正常")) {
            tips = "3.生长速度慢，可能是短暂的，需注意排除近期有无疾病、心理压力大等影响长个生物因素，若有尽量去除，下一步生长趋势可能不好，可观察一段时间，若生长速度持续慢，可能影响终身高";
            variables.put("result", "3");
        } else if (heightStatus.equals("正常") && !isDevelopmentDelay && isGrowthRate && isHereditaryHeightPercentile && !isBoneAgePercentile && boneAgeStatus.equals("骨龄正常")) {
            tips = "4.孩子可能属于早长类型，注意青春发育情况，骨龄暂时不提前，下一步有可能出现骨龄加速，影响生长，导致终身高偏矮或矮小";
            variables.put("result", "4");
        } else if (heightStatus.equals("正常") && !isDevelopmentDelay && isGrowthRate && isHereditaryHeightPercentile && isBoneAgePercentile && boneAgeStatus.equals("骨龄落后1-2岁")) {
            tips = "5.孩子的身高生长超过遗传身高，骨龄落后可能是临时的，或者存在骨骼系统疾病，建议观察，注意骨龄变化情况";
            variables.put("result", "5");
        } else if (heightStatus.equals("偏高") && !isDevelopmentDelay && isGrowthRate && isHereditaryHeightPercentile && isBoneAgePercentile && boneAgeStatus.equals("骨龄正常")) {
            tips = "6.遗传身高本身就高可能，考虑正常高个可能，观察";
            variables.put("result", "6");
        } else if (heightStatus.equals("偏高") && !isDevelopmentDelay && isGrowthRate && !isHereditaryHeightPercentile && isBoneAgePercentile && boneAgeStatus.equals("骨龄提前1岁及以上")) {
            tips = "7.早长个可能，需注意评估性发育情况，观察骨龄是否快速进展";
            variables.put("result", "7");
        } else if (heightStatus.equals("偏高") && !isDevelopmentDelay && !isGrowthRate && isBoneAgePercentile && boneAgeStatus.equals("骨龄正常")) {
            tips = "8.这种情况常见于青春发育较早的孩子，青春期后期可能，需注意性腺发育情况";
            variables.put("result", "8");
        } else if (heightStatus.equals("偏低") && !isDevelopmentDelay && isGrowthRate && !isHereditaryHeightPercentile && isBoneAgePercentile && boneAgeStatus.equals("骨龄正常")) {
            tips = "12.遗传性矮小可能，可观察，或者进一步检查";
            variables.put("result", "12");
        } else if (heightStatus.equals("偏低") && !isDevelopmentDelay && !isGrowthRate && isHereditaryHeightPercentile && !isBoneAgePercentile && boneAgeStatus.equals("骨龄正常")) {
            tips = "13.明确矮小原因";
            variables.put("result", "13");
        } else if (heightStatus.equals("偏低") && isDevelopmentDelay && !isGrowthRate && isHereditaryHeightPercentile && !isBoneAgePercentile && boneAgeStatus.equals("骨龄落后1-2岁")) {
            tips = "15.性腺发育异常疾病可能";
            variables.put("result", "15");
        } else {
            tips = "骨龄检查的其他情况，建议继续观察";
            variables.put("result", "0");
        }
        taskService.complete(taskId, variables);
        return tips;
    }

    //判断身高百分位数是否正常[3, 97]--正常
    private boolean isPercentileNormal(String strPercentile) {
        int percentile = Integer.parseInt(strPercentile);
        boolean isNormal = false;
        if (percentile >= 3 && percentile <= 97) {
            isNormal = true;
        }
        return isNormal;
    }

    @GetMapping("assess13_2")
    public String assessBoneAge(String taskId, String a1, String a2, String a3) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return "流程不存在";
        }
        //骨龄初步诊断
        String SSS = a1;//第二性征："落后"/"提前"
        int boneAgePercentile = Integer.parseInt(a2);
        double boneAgeDiff = Double.parseDouble(a3);
        Map<String, Object> variables = new HashMap<>();
        variables.put("SSS", SSS);
        variables.put("boneAgePercentile", boneAgePercentile);
        variables.put("boneAgeDiff", boneAgeDiff);
        String firstDiagnosis;
        if(boneAgePercentile < 6 || boneAgeDiff <= -2){ //骨龄落后2岁及以上
            firstDiagnosis = "考虑矮小，建议进一步检查。";
        } else if(SSS.equals("提前") && boneAgeDiff >= 2){//骨龄提前2岁及以上
            firstDiagnosis = "考虑性早熟，建议进一步检查。";
        } else{
            firstDiagnosis = "考虑非病理性问题，建议改善生活方式，定期复查。";
        }
        taskService.complete(taskId, variables);
        return firstDiagnosis;
    }
    @GetMapping("assess14")
    public Map assessCheck(String taskId, //
                              String tp, String alb, String glb, String ag, String alt, String ast, String op, String tbil, String dbil, String ibil, String alp, String y, String z,//肝功
                              String crea, String urea, String ua, String cys,//肾功能
                              String tsh, String t3, String t4, String ft3, String ft4,//甲功
                              String e2, String p, String t, String prl, String lh, String fsh,//激素六项
                              String ca, String mg, String pa, String k, String na, String cl, String co2,//电解质
                              String bglu, String bc, String igf_1, String igfbp_3,//
                              String brbc, String hgb, String wbc, String neut, //血常规
                              String rbc, String pro, String wbcc//尿常规
    ) {
        Map<String, Object> variables = new HashMap<>();
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            variables.put("msg", "流程不存在");
            return variables;
        }
        //缺省值
        variables.put("checkResult", 0);
        String tips = "检验检查的其他情况，建议持续观察";
        //肝功
        tp      = getStr(tp);
        alb     = getStr(alb);
        glb     = getStr(glb);
        ag      = getStr(ag);
        alt     = getStr(alt);
        ast     = getStr(ast);
        op      = getStr(op);
        tbil    = getStr(tbil);
        ibil    = getStr(ibil);
        alp     = getStr(alp);
        y       = getStr(y);
        z       = getStr(z);
        //肾功能
        crea    = getStr(crea);
        urea    = getStr(urea);
        ua      = getStr(ua);
        cys     = getStr(cys);
        //甲功
        tsh     = getStr(tsh);
        t3      = getStr(t3);
        t4      = getStr(t4);
        ft3     = getStr(ft3);
        ft4     = getStr(ft4);
        //激素六项
        e2      = getStr(e2);
        p       = getStr(p);
        t       = getStr(t);
        prl     = getStr(prl);
        lh      = getStr(lh);
        fsh     = getStr(fsh);
        //电解质
        ca      = getStr(ca);
        mg      = getStr(mg);
        pa      = getStr(pa);
        k       = getStr(k);
        na      = getStr(na);
        cl      = getStr(cl);
        co2     = getStr(co2);
        //String bglu, String bc, String igf_1, String igfbp_3,//
        bglu    = getStr(bglu);
        bc      = getStr(bc);
        igf_1   = getStr(igf_1);
        igfbp_3 = getStr(igfbp_3);
        //血常规
        brbc    = getStr(brbc);
        hgb     = getStr(hgb);
        wbc     = getStr(wbc);
        neut    = getStr(neut);
        //尿常规
        rbc     = getStr(rbc);
        pro     = getStr(pro);
        wbcc    = getStr(wbcc);

        if(tp  .equals("降低") &&
           alb .equals("正常") &&
           glb .equals("正常") &&
           ag  .equals("正常") &&
           alt .equals("正常") &&
           ast .equals("正常") &&
           op  .equals("正常") &&
           tbil.equals("正常") &&
           ibil.equals("正常") &&
           alp .equals("正常") &&
           y   .equals("正常") &&
           z   .equals("正常") &&
           crea.equals("正常") &&
           urea.equals("正常") &&
           ua  .equals("正常") &&
           cys .equals("正常") &&
           tsh .equals("正常") &&
           t3  .equals("正常") &&
           t4  .equals("正常") &&
           ft3 .equals("正常") &&
           ft4 .equals("正常") &&
           e2  .equals("正常") &&
           p   .equals("正常") &&
           t   .equals("正常") &&
           prl .equals("正常") &&
           lh  .equals("正常") &&
           fsh .equals("正常") &&
           ca  .equals("正常") &&
           mg  .equals("正常") &&
           pa  .equals("正常") &&
           k   .equals("正常") &&
           na  .equals("正常") &&
           cl  .equals("正常") &&
           co2 .equals("正常") &&
           bglu.equals("正常") &&
           bc  .equals("正常") &&
           igf_1  .equals("正常") &&
           igfbp_3.equals("正常") &&
           brbc.equals("正常") &&
           hgb .equals("正常") &&
           wbc .equals("正常") &&
           neut.equals("正常") &&
           rbc .equals("正常") &&
           pro .equals("正常") &&
           wbcc.equals("正常")
        ){
            tips = "1.营养不良";
            variables.put("checkResult", 1);
        }else if(tp  .equals("正常") &&
                alb .equals("正常") &&
                glb .equals("正常") &&
                ag  .equals("正常") &&
                alt .equals("升高") &&
                ast .equals("正常") &&
                op  .equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp .equals("正常") &&
                y   .equals("正常") &&
                z   .equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua  .equals("正常") &&
                cys .equals("正常") &&
                tsh .equals("正常") &&
                t3  .equals("正常") &&
                t4  .equals("正常") &&
                ft3 .equals("正常") &&
                ft4 .equals("正常") &&
                e2  .equals("正常") &&
                p   .equals("正常") &&
                t   .equals("正常") &&
                prl .equals("正常") &&
                lh  .equals("正常") &&
                fsh .equals("正常") &&
                ca  .equals("正常") &&
                mg  .equals("正常") &&
                pa  .equals("正常") &&
                k   .equals("正常") &&
                na  .equals("正常") &&
                cl  .equals("正常") &&
                co2 .equals("正常") &&
                bglu.equals("正常") &&
                bc  .equals("正常") &&
                igf_1  .equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb .equals("正常") &&
                wbc .equals("正常") &&
                neut.equals("正常") &&
                rbc .equals("正常") &&
                pro .equals("正常") &&
                wbcc.equals("正常")
        ){
            tips = "3.肝损害";
            variables.put("checkResult", 3);
        }else if(tp  .equals("正常") &&
                alb .equals("正常") &&
                glb .equals("正常") &&
                ag  .equals("正常") &&
                alt .equals("正常") &&
                ast .equals("正常") &&
                op  .equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp .equals("正常") &&
                y   .equals("正常") &&
                z   .equals("正常") &&
                crea.equals("升高") &&
                urea.equals("正常") &&
                ua  .equals("正常") &&
                cys .equals("正常") &&
                tsh .equals("正常") &&
                t3  .equals("正常") &&
                t4  .equals("正常") &&
                ft3 .equals("正常") &&
                ft4 .equals("正常") &&
                e2  .equals("正常") &&
                p   .equals("正常") &&
                t   .equals("正常") &&
                prl .equals("正常") &&
                lh  .equals("正常") &&
                fsh .equals("正常") &&
                ca  .equals("正常") &&
                mg  .equals("正常") &&
                pa  .equals("正常") &&
                k   .equals("正常") &&
                na  .equals("正常") &&
                cl  .equals("正常") &&
                co2 .equals("正常") &&
                bglu.equals("正常") &&
                bc  .equals("正常") &&
                igf_1  .equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb .equals("正常") &&
                wbc .equals("正常") &&
                neut.equals("正常") &&
                rbc .equals("正常") &&
                pro .equals("正常") &&
                wbcc.equals("正常")
        ){
            tips = "4.肾功能不全";
            variables.put("checkResult", 4);
        }else if(tp  .equals("正常") &&
                alb .equals("正常") &&
                glb .equals("正常") &&
                ag  .equals("正常") &&
                alt .equals("正常") &&
                ast .equals("正常") &&
                op  .equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp .equals("正常") &&
                y   .equals("正常") &&
                z   .equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua  .equals("升高") &&
                cys .equals("正常") &&
                tsh .equals("正常") &&
                t3  .equals("正常") &&
                t4  .equals("正常") &&
                ft3 .equals("正常") &&
                ft4 .equals("正常") &&
                e2  .equals("正常") &&
                p   .equals("正常") &&
                t   .equals("正常") &&
                prl .equals("正常") &&
                lh  .equals("正常") &&
                fsh .equals("正常") &&
                ca  .equals("正常") &&
                mg  .equals("正常") &&
                pa  .equals("正常") &&
                k   .equals("正常") &&
                na  .equals("正常") &&
                cl  .equals("正常") &&
                co2 .equals("正常") &&
                bglu.equals("正常") &&
                bc  .equals("正常") &&
                igf_1  .equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb .equals("正常") &&
                wbc .equals("正常") &&
                neut.equals("正常") &&
                rbc .equals("正常") &&
                pro .equals("正常") &&
                wbcc.equals("正常")
        ){
            tips = "5.高尿酸血症";
            variables.put("checkResult", 5);
        }else if(tp  .equals("正常") &&
                alb .equals("正常") &&
                glb .equals("正常") &&
                ag  .equals("正常") &&
                alt .equals("正常") &&
                ast .equals("正常") &&
                op  .equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp .equals("正常") &&
                y   .equals("正常") &&
                z   .equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua  .equals("正常") &&
                cys .equals("正常") &&
                tsh .equals("升高") &&
                t3  .equals("正常") &&
                t4  .equals("正常") &&
                ft3 .equals("正常") &&
                ft4 .equals("正常") &&
                e2  .equals("正常") &&
                p   .equals("正常") &&
                t   .equals("正常") &&
                prl .equals("正常") &&
                lh  .equals("正常") &&
                fsh .equals("正常") &&
                ca  .equals("正常") &&
                mg  .equals("正常") &&
                pa  .equals("正常") &&
                k   .equals("正常") &&
                na  .equals("正常") &&
                cl  .equals("正常") &&
                co2 .equals("正常") &&
                bglu.equals("正常") &&
                bc  .equals("正常") &&
                igf_1  .equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb .equals("正常") &&
                wbc .equals("正常") &&
                neut.equals("正常") &&
                rbc .equals("正常") &&
                pro .equals("正常") &&
                wbcc.equals("正常")
        ){
            tips = "6.常见于甲减";
            variables.put("checkResult", 6);
        } else if(tp  .equals("正常") &&
                alb .equals("正常") &&
                glb .equals("正常") &&
                ag  .equals("正常") &&
                alt .equals("正常") &&
                ast .equals("正常") &&
                op  .equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp .equals("正常") &&
                y   .equals("正常") &&
                z   .equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua  .equals("正常") &&
                cys .equals("正常") &&
                tsh .equals("降低") &&
                t3  .equals("正常") &&
                t4  .equals("正常") &&
                ft3 .equals("正常") &&
                ft4 .equals("正常") &&
                e2  .equals("正常") &&
                p   .equals("正常") &&
                t   .equals("正常") &&
                prl .equals("正常") &&
                lh  .equals("正常") &&
                fsh .equals("正常") &&
                ca  .equals("正常") &&
                mg  .equals("正常") &&
                pa  .equals("正常") &&
                k   .equals("正常") &&
                na  .equals("正常") &&
                cl  .equals("正常") &&
                co2 .equals("正常") &&
                bglu.equals("正常") &&
                bc  .equals("正常") &&
                igf_1  .equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb .equals("正常") &&
                wbc .equals("正常") &&
                neut.equals("正常") &&
                rbc .equals("正常") &&
                pro .equals("正常") &&
                wbcc.equals("正常")
        ){
            tips = "7.常见于甲亢";
            variables.put("checkResult", 7);
        }else if(tp  .equals("正常") &&
                alb .equals("正常") &&
                glb .equals("正常") &&
                ag  .equals("正常") &&
                alt .equals("正常") &&
                ast .equals("正常") &&
                op  .equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp .equals("正常") &&
                y   .equals("正常") &&
                z   .equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua  .equals("正常") &&
                cys .equals("正常") &&
                tsh .equals("正常") &&
                t3  .equals("正常") &&
                t4  .equals("正常") &&
                ft3 .equals("降低") &&
                ft4 .equals("正常") &&
                e2  .equals("正常") &&
                p   .equals("正常") &&
                t   .equals("正常") &&
                prl .equals("正常") &&
                lh  .equals("正常") &&
                fsh .equals("正常") &&
                ca  .equals("正常") &&
                mg  .equals("正常") &&
                pa  .equals("正常") &&
                k   .equals("正常") &&
                na  .equals("正常") &&
                cl  .equals("正常") &&
                co2 .equals("正常") &&
                bglu.equals("正常") &&
                bc  .equals("正常") &&
                igf_1  .equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb .equals("正常") &&
                wbc .equals("正常") &&
                neut.equals("正常") &&
                rbc .equals("正常") &&
                pro .equals("正常") &&
                wbcc.equals("正常")
        ){
            tips = "8.常见于甲减";
            variables.put("checkResult", 8);
        }else if(tp  .equals("正常") &&
                alb .equals("正常") &&
                glb .equals("正常") &&
                ag  .equals("正常") &&
                alt .equals("正常") &&
                ast .equals("正常") &&
                op  .equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp .equals("正常") &&
                y   .equals("正常") &&
                z   .equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua  .equals("正常") &&
                cys .equals("正常") &&
                tsh .equals("正常") &&
                t3  .equals("正常") &&
                t4  .equals("正常") &&
                ft3 .equals("升高") &&
                ft4 .equals("正常") &&
                e2  .equals("正常") &&
                p   .equals("正常") &&
                t   .equals("正常") &&
                prl .equals("正常") &&
                lh  .equals("正常") &&
                fsh .equals("正常") &&
                ca  .equals("正常") &&
                mg  .equals("正常") &&
                pa  .equals("正常") &&
                k   .equals("正常") &&
                na  .equals("正常") &&
                cl  .equals("正常") &&
                co2 .equals("正常") &&
                bglu.equals("正常") &&
                bc  .equals("正常") &&
                igf_1  .equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb .equals("正常") &&
                wbc .equals("正常") &&
                neut.equals("正常") &&
                rbc .equals("正常") &&
                pro .equals("正常") &&
                wbcc.equals("正常")
        ){
            tips = "9.常见于甲亢";
            variables.put("checkResult", 9);
        }else if(tp  .equals("正常") &&
                alb .equals("正常") &&
                glb .equals("正常") &&
                ag  .equals("正常") &&
                alt .equals("正常") &&
                ast .equals("正常") &&
                op  .equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp .equals("正常") &&
                y   .equals("正常") &&
                z   .equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua  .equals("正常") &&
                cys .equals("正常") &&
                tsh .equals("正常") &&
                t3  .equals("正常") &&
                t4  .equals("正常") &&
                ft3 .equals("正常") &&
                ft4 .equals("降低") &&
                e2  .equals("正常") &&
                p   .equals("正常") &&
                t   .equals("正常") &&
                prl .equals("正常") &&
                lh  .equals("正常") &&
                fsh .equals("正常") &&
                ca  .equals("正常") &&
                mg  .equals("正常") &&
                pa  .equals("正常") &&
                k   .equals("正常") &&
                na  .equals("正常") &&
                cl  .equals("正常") &&
                co2 .equals("正常") &&
                bglu.equals("正常") &&
                bc  .equals("正常") &&
                igf_1  .equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb .equals("正常") &&
                wbc .equals("正常") &&
                neut.equals("正常") &&
                rbc .equals("正常") &&
                pro .equals("正常") &&
                wbcc.equals("正常")
        ){
            tips = "10.常见于甲减";
            variables.put("checkResult", 10);
        }else if(tp  .equals("正常") &&
                alb .equals("正常") &&
                glb .equals("正常") &&
                ag  .equals("正常") &&
                alt .equals("正常") &&
                ast .equals("正常") &&
                op  .equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp .equals("正常") &&
                y   .equals("正常") &&
                z   .equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua  .equals("正常") &&
                cys .equals("正常") &&
                tsh .equals("正常") &&
                t3  .equals("正常") &&
                t4  .equals("正常") &&
                ft3 .equals("正常") &&
                ft4 .equals("升高") &&
                e2  .equals("正常") &&
                p   .equals("正常") &&
                t   .equals("正常") &&
                prl .equals("正常") &&
                lh  .equals("正常") &&
                fsh .equals("正常") &&
                ca  .equals("正常") &&
                mg  .equals("正常") &&
                pa  .equals("正常") &&
                k   .equals("正常") &&
                na  .equals("正常") &&
                cl  .equals("正常") &&
                co2 .equals("正常") &&
                bglu.equals("正常") &&
                bc  .equals("正常") &&
                igf_1  .equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb .equals("正常") &&
                wbc .equals("正常") &&
                neut.equals("正常") &&
                rbc .equals("正常") &&
                pro .equals("正常") &&
                wbcc.equals("正常")
        ){
            tips = "11.常见于甲亢";
            variables.put("checkResult", 11);
        } else if(tp  .equals("正常") &&
                alb .equals("正常") &&
                glb .equals("正常") &&
                ag  .equals("正常") &&
                alt .equals("正常") &&
                ast .equals("正常") &&
                op  .equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp .equals("正常") &&
                y   .equals("正常") &&
                z   .equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua  .equals("正常") &&
                cys .equals("正常") &&
                tsh .equals("正常") &&
                t3  .equals("正常") &&
                t4  .equals("正常") &&
                ft3 .equals("正常") &&
                ft4 .equals("正常") &&
                e2  .equals("升高") &&
                p   .equals("正常") &&
                t   .equals("正常") &&
                prl .equals("正常") &&
                lh  .equals("正常") &&
                fsh .equals("正常") &&
                ca  .equals("正常") &&
                mg  .equals("正常") &&
                pa  .equals("正常") &&
                k   .equals("正常") &&
                na  .equals("正常") &&
                cl  .equals("正常") &&
                co2 .equals("正常") &&
                bglu.equals("正常") &&
                bc  .equals("正常") &&
                igf_1  .equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb .equals("正常") &&
                wbc .equals("正常") &&
                neut.equals("正常") &&
                rbc .equals("正常") &&
                pro .equals("正常") &&
                wbcc.equals("正常")
        ){
            tips = "12.女孩要发育或已经发育";
            variables.put("checkResult", 12);
        } else if(tp  .equals("正常") &&
                alb .equals("正常") &&
                glb .equals("正常") &&
                ag  .equals("正常") &&
                alt .equals("正常") &&
                ast .equals("正常") &&
                op  .equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp .equals("正常") &&
                y   .equals("正常") &&
                z   .equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua  .equals("正常") &&
                cys .equals("正常") &&
                tsh .equals("正常") &&
                t3  .equals("正常") &&
                t4  .equals("正常") &&
                ft3 .equals("正常") &&
                ft4 .equals("正常") &&
                e2  .equals("正常") &&
                p   .equals("正常") &&
                t   .equals("升高") &&
                prl .equals("正常") &&
                lh  .equals("正常") &&
                fsh .equals("正常") &&
                ca  .equals("正常") &&
                mg  .equals("正常") &&
                pa  .equals("正常") &&
                k   .equals("正常") &&
                na  .equals("正常") &&
                cl  .equals("正常") &&
                co2 .equals("正常") &&
                bglu.equals("正常") &&
                bc  .equals("正常") &&
                igf_1  .equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb .equals("正常") &&
                wbc .equals("正常") &&
                neut.equals("正常") &&
                rbc .equals("正常") &&
                pro .equals("正常") &&
                wbcc.equals("正常")
        ){
            tips = "13.男孩要发育或已经发育";
            variables.put("checkResult", 13);
        } else if(tp  .equals("正常") &&
                alb .equals("正常") &&
                glb .equals("正常") &&
                ag  .equals("正常") &&
                alt .equals("正常") &&
                ast .equals("正常") &&
                op  .equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp .equals("正常") &&
                y   .equals("正常") &&
                z   .equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua  .equals("正常") &&
                cys .equals("正常") &&
                tsh .equals("正常") &&
                t3  .equals("正常") &&
                t4  .equals("正常") &&
                ft3 .equals("正常") &&
                ft4 .equals("正常") &&
                e2  .equals("正常") &&
                p   .equals("正常") &&
                t   .equals("正常") &&
                prl .equals("正常") &&
                lh  .equals("升高") &&
                fsh .equals("正常") &&
                ca  .equals("正常") &&
                mg  .equals("正常") &&
                pa  .equals("正常") &&
                k   .equals("正常") &&
                na  .equals("正常") &&
                cl  .equals("正常") &&
                co2 .equals("正常") &&
                bglu.equals("正常") &&
                bc  .equals("正常") &&
                igf_1  .equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb .equals("正常") &&
                wbc .equals("正常") &&
                neut.equals("正常") &&
                rbc .equals("正常") &&
                pro .equals("正常") &&
                wbcc.equals("正常")
        ){
            tips = "14.性腺轴启动";
            variables.put("checkResult", 14);
        } else if(tp  .equals("正常") &&
                alb .equals("正常") &&
                glb .equals("正常") &&
                ag  .equals("正常") &&
                alt .equals("正常") &&
                ast .equals("正常") &&
                op  .equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp .equals("正常") &&
                y   .equals("正常") &&
                z   .equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua  .equals("正常") &&
                cys .equals("正常") &&
                tsh .equals("正常") &&
                t3  .equals("正常") &&
                t4  .equals("正常") &&
                ft3 .equals("正常") &&
                ft4 .equals("正常") &&
                e2  .equals("正常") &&
                p   .equals("正常") &&
                t   .equals("正常") &&
                prl .equals("正常") &&
                lh  .equals("降低") &&
                fsh .equals("正常") &&
                ca  .equals("正常") &&
                mg  .equals("正常") &&
                pa  .equals("正常") &&
                k   .equals("正常") &&
                na  .equals("正常") &&
                cl  .equals("正常") &&
                co2 .equals("正常") &&
                bglu.equals("正常") &&
                bc  .equals("正常") &&
                igf_1  .equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb .equals("正常") &&
                wbc .equals("正常") &&
                neut.equals("正常") &&
                rbc .equals("正常") &&
                pro .equals("正常") &&
                wbcc.equals("正常")
        ){
            tips = "15.青春期后不升高，提示垂体功能减退";
            variables.put("checkResult", 15);
        } else if(tp  .equals("正常") &&
                alb .equals("正常") &&
                glb .equals("正常") &&
                ag  .equals("正常") &&
                alt .equals("正常") &&
                ast .equals("正常") &&
                op  .equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp .equals("正常") &&
                y   .equals("正常") &&
                z   .equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua  .equals("正常") &&
                cys .equals("正常") &&
                tsh .equals("正常") &&
                t3  .equals("正常") &&
                t4  .equals("正常") &&
                ft3 .equals("正常") &&
                ft4 .equals("正常") &&
                e2  .equals("正常") &&
                p   .equals("正常") &&
                t   .equals("正常") &&
                prl .equals("正常") &&
                lh  .equals("正常") &&
                fsh .equals("正常") &&
                ca  .equals("正常") &&
                mg  .equals("正常") &&
                pa  .equals("正常") &&
                k   .equals("正常") &&
                na  .equals("升高") &&
                cl  .equals("正常") &&
                co2 .equals("正常") &&
                bglu.equals("正常") &&
                bc  .equals("正常") &&
                igf_1  .equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb .equals("正常") &&
                wbc .equals("正常") &&
                neut.equals("正常") &&
                rbc .equals("正常") &&
                pro .equals("正常") &&
                wbcc.equals("正常")
        ){
            tips = "16.垂体疾病";
            variables.put("checkResult", 16);
        } else if(tp  .equals("正常") &&
                alb .equals("正常") &&
                glb .equals("正常") &&
                ag  .equals("正常") &&
                alt .equals("正常") &&
                ast .equals("正常") &&
                op  .equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp .equals("正常") &&
                y   .equals("正常") &&
                z   .equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua  .equals("正常") &&
                cys .equals("正常") &&
                tsh .equals("正常") &&
                t3  .equals("正常") &&
                t4  .equals("正常") &&
                ft3 .equals("正常") &&
                ft4 .equals("正常") &&
                e2  .equals("正常") &&
                p   .equals("正常") &&
                t   .equals("正常") &&
                prl .equals("正常") &&
                lh  .equals("正常") &&
                fsh .equals("正常") &&
                ca  .equals("正常") &&
                mg  .equals("正常") &&
                pa  .equals("正常") &&
                k   .equals("正常") &&
                na  .equals("降低") &&
                cl  .equals("正常") &&
                co2 .equals("正常") &&
                bglu.equals("正常") &&
                bc  .equals("正常") &&
                igf_1  .equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb .equals("正常") &&
                wbc .equals("正常") &&
                neut.equals("正常") &&
                rbc .equals("正常") &&
                pro .equals("正常") &&
                wbcc.equals("正常")
        ){
            tips = "17.垂体疾病";
            variables.put("checkResult", 17);
        } else if(tp  .equals("正常") &&
                alb .equals("正常") &&
                glb .equals("正常") &&
                ag  .equals("正常") &&
                alt .equals("正常") &&
                ast .equals("正常") &&
                op  .equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp .equals("正常") &&
                y   .equals("正常") &&
                z   .equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua  .equals("正常") &&
                cys .equals("正常") &&
                tsh .equals("正常") &&
                t3  .equals("正常") &&
                t4  .equals("正常") &&
                ft3 .equals("正常") &&
                ft4 .equals("正常") &&
                e2  .equals("正常") &&
                p   .equals("正常") &&
                t   .equals("正常") &&
                prl .equals("正常") &&
                lh  .equals("正常") &&
                fsh .equals("正常") &&
                ca  .equals("正常") &&
                mg  .equals("正常") &&
                pa  .equals("正常") &&
                k   .equals("正常") &&
                na  .equals("正常") &&
                cl  .equals("正常") &&
                co2 .equals("降低") &&
                bglu.equals("正常") &&
                bc  .equals("正常") &&
                igf_1  .equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb .equals("正常") &&
                wbc .equals("正常") &&
                neut.equals("正常") &&
                rbc .equals("正常") &&
                pro .equals("正常") &&
                wbcc.equals("正常")
        ){
            tips = "18.酸中毒";
            variables.put("checkResult", 18);
        } else if(tp  .equals("正常") &&
                alb .equals("正常") &&
                glb .equals("正常") &&
                ag  .equals("正常") &&
                alt .equals("正常") &&
                ast .equals("正常") &&
                op  .equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp .equals("正常") &&
                y   .equals("正常") &&
                z   .equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua  .equals("正常") &&
                cys .equals("正常") &&
                tsh .equals("正常") &&
                t3  .equals("正常") &&
                t4  .equals("正常") &&
                ft3 .equals("正常") &&
                ft4 .equals("正常") &&
                e2  .equals("正常") &&
                p   .equals("正常") &&
                t   .equals("正常") &&
                prl .equals("正常") &&
                lh  .equals("正常") &&
                fsh .equals("正常") &&
                ca  .equals("正常") &&
                mg  .equals("正常") &&
                pa  .equals("正常") &&
                k   .equals("正常") &&
                na  .equals("正常") &&
                cl  .equals("正常") &&
                co2 .equals("正常") &&
                bglu.equals("升高") &&
                bc  .equals("正常") &&
                igf_1  .equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb .equals("正常") &&
                wbc .equals("正常") &&
                neut.equals("正常") &&
                rbc .equals("正常") &&
                pro .equals("正常") &&
                wbcc.equals("正常")
        ){
            tips = "19.糖尿病或糖尿病前期";
            variables.put("checkResult", 19);
        } else if(tp  .equals("正常") &&
                alb .equals("正常") &&
                glb .equals("正常") &&
                ag  .equals("正常") &&
                alt .equals("正常") &&
                ast .equals("正常") &&
                op  .equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp .equals("正常") &&
                y   .equals("正常") &&
                z   .equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua  .equals("正常") &&
                cys .equals("正常") &&
                tsh .equals("正常") &&
                t3  .equals("正常") &&
                t4  .equals("正常") &&
                ft3 .equals("正常") &&
                ft4 .equals("正常") &&
                e2  .equals("正常") &&
                p   .equals("正常") &&
                t   .equals("正常") &&
                prl .equals("正常") &&
                lh  .equals("正常") &&
                fsh .equals("正常") &&
                ca  .equals("正常") &&
                mg  .equals("正常") &&
                pa  .equals("正常") &&
                k   .equals("正常") &&
                na  .equals("正常") &&
                cl  .equals("正常") &&
                co2 .equals("正常") &&
                bglu.equals("正常") &&
                bc  .equals("正常") &&
                igf_1  .equals("正常") &&
                igfbp_3.equals("正常") &&
                (brbc.equals("升高") || brbc.equals("降低")) &&
                hgb .equals("正常") &&
                wbc .equals("正常") &&
                neut.equals("正常") &&
                rbc .equals("正常") &&
                pro .equals("正常") &&
                wbcc.equals("正常")
        ){
            tips = "20.贫血";
            variables.put("checkResult", 20);
        } else if(tp  .equals("正常") &&
                alb .equals("正常") &&
                glb .equals("正常") &&
                ag  .equals("正常") &&
                alt .equals("正常") &&
                ast .equals("正常") &&
                op  .equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp .equals("正常") &&
                y   .equals("正常") &&
                z   .equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua  .equals("正常") &&
                cys .equals("正常") &&
                tsh .equals("正常") &&
                t3  .equals("正常") &&
                t4  .equals("正常") &&
                ft3 .equals("正常") &&
                ft4 .equals("正常") &&
                e2  .equals("正常") &&
                p   .equals("正常") &&
                t   .equals("正常") &&
                prl .equals("正常") &&
                lh  .equals("正常") &&
                fsh .equals("正常") &&
                ca  .equals("正常") &&
                mg  .equals("正常") &&
                pa  .equals("正常") &&
                k   .equals("正常") &&
                na  .equals("正常") &&
                cl  .equals("正常") &&
                co2 .equals("正常") &&
                bglu.equals("正常") &&
                bc  .equals("正常") &&
                igf_1  .equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                (hgb .equals("升高") || hgb .equals("降低")) &&
                wbc .equals("正常") &&
                neut.equals("正常") &&
                rbc .equals("正常") &&
                pro .equals("正常") &&
                wbcc.equals("正常")
        ){
            tips = "21.贫血";
            variables.put("checkResult", 21);
        } else if(tp  .equals("正常") &&
                alb .equals("正常") &&
                glb .equals("正常") &&
                ag  .equals("正常") &&
                alt .equals("正常") &&
                ast .equals("正常") &&
                op  .equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp .equals("正常") &&
                y   .equals("正常") &&
                z   .equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua  .equals("正常") &&
                cys .equals("正常") &&
                tsh .equals("正常") &&
                t3  .equals("正常") &&
                t4  .equals("正常") &&
                ft3 .equals("正常") &&
                ft4 .equals("正常") &&
                e2  .equals("正常") &&
                p   .equals("正常") &&
                t   .equals("正常") &&
                prl .equals("正常") &&
                lh  .equals("正常") &&
                fsh .equals("正常") &&
                ca  .equals("正常") &&
                mg  .equals("正常") &&
                pa  .equals("正常") &&
                k   .equals("正常") &&
                na  .equals("正常") &&
                cl  .equals("正常") &&
                co2 .equals("正常") &&
                bglu.equals("正常") &&
                bc  .equals("正常") &&
                igf_1  .equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb .equals("正常") &&
                (wbc .equals("升高") || wbc .equals("降低")) &&
                neut.equals("正常") &&
                rbc .equals("正常") &&
                pro .equals("正常") &&
                wbcc.equals("正常")
        ){
            tips = "22.有感染";
            variables.put("checkResult", 22);
        } else if(tp  .equals("正常") &&
                alb .equals("正常") &&
                glb .equals("正常") &&
                ag  .equals("正常") &&
                alt .equals("正常") &&
                ast .equals("正常") &&
                op  .equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp .equals("正常") &&
                y   .equals("正常") &&
                z   .equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua  .equals("正常") &&
                cys .equals("正常") &&
                tsh .equals("正常") &&
                t3  .equals("正常") &&
                t4  .equals("正常") &&
                ft3 .equals("正常") &&
                ft4 .equals("正常") &&
                e2  .equals("正常") &&
                p   .equals("正常") &&
                t   .equals("正常") &&
                prl .equals("正常") &&
                lh  .equals("正常") &&
                fsh .equals("正常") &&
                ca  .equals("正常") &&
                mg  .equals("正常") &&
                pa  .equals("正常") &&
                k   .equals("正常") &&
                na  .equals("正常") &&
                cl  .equals("正常") &&
                co2 .equals("正常") &&
                bglu.equals("正常") &&
                bc  .equals("正常") &&
                igf_1  .equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb .equals("正常") &&
                wbc .equals("正常") &&
                (neut.equals("升高") || neut.equals("降低")) &&
                rbc .equals("正常") &&
                pro .equals("正常") &&
                wbcc.equals("正常")
        ){
            tips = "23.有感染";
            variables.put("checkResult", 23);
        } else if(tp  .equals("正常") &&
                alb .equals("正常") &&
                glb .equals("正常") &&
                ag  .equals("正常") &&
                alt .equals("正常") &&
                ast .equals("正常") &&
                op  .equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp .equals("正常") &&
                y   .equals("正常") &&
                z   .equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua  .equals("正常") &&
                cys .equals("正常") &&
                tsh .equals("正常") &&
                t3  .equals("正常") &&
                t4  .equals("正常") &&
                ft3 .equals("正常") &&
                ft4 .equals("正常") &&
                e2  .equals("正常") &&
                p   .equals("正常") &&
                t   .equals("正常") &&
                prl .equals("正常") &&
                lh  .equals("正常") &&
                fsh .equals("正常") &&
                ca  .equals("正常") &&
                mg  .equals("正常") &&
                pa  .equals("正常") &&
                k   .equals("正常") &&
                na  .equals("正常") &&
                cl  .equals("正常") &&
                co2 .equals("正常") &&
                bglu.equals("正常") &&
                bc  .equals("正常") &&
                igf_1  .equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb .equals("正常") &&
                wbc .equals("正常") &&
                neut.equals("正常") &&
                (rbc .equals("升高") || rbc .equals("降低")) &&
                pro .equals("正常") &&
                wbcc.equals("正常")
        ){
            tips = "24.有肾病";
            variables.put("checkResult", 24);
        } else if(tp  .equals("正常") &&
                alb .equals("正常") &&
                glb .equals("正常") &&
                ag  .equals("正常") &&
                alt .equals("正常") &&
                ast .equals("正常") &&
                op  .equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp .equals("正常") &&
                y   .equals("正常") &&
                z   .equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua  .equals("正常") &&
                cys .equals("正常") &&
                tsh .equals("正常") &&
                t3  .equals("正常") &&
                t4  .equals("正常") &&
                ft3 .equals("正常") &&
                ft4 .equals("正常") &&
                e2  .equals("正常") &&
                p   .equals("正常") &&
                t   .equals("正常") &&
                prl .equals("正常") &&
                lh  .equals("正常") &&
                fsh .equals("正常") &&
                ca  .equals("正常") &&
                mg  .equals("正常") &&
                pa  .equals("正常") &&
                k   .equals("正常") &&
                na  .equals("正常") &&
                cl  .equals("正常") &&
                co2 .equals("正常") &&
                bglu.equals("正常") &&
                bc  .equals("正常") &&
                igf_1  .equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb .equals("正常") &&
                wbc .equals("正常") &&
                neut.equals("正常") &&
                rbc .equals("正常") &&
                (pro .equals("升高") || pro .equals("降低")) &&
                wbcc.equals("正常")
        ){
            tips = "25.有肾病";
            variables.put("checkResult", 25);
        } else if(tp  .equals("正常") &&
                alb .equals("正常") &&
                glb .equals("正常") &&
                ag  .equals("正常") &&
                alt .equals("正常") &&
                ast .equals("正常") &&
                op  .equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp .equals("正常") &&
                y   .equals("正常") &&
                z   .equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua  .equals("正常") &&
                cys .equals("正常") &&
                tsh .equals("正常") &&
                t3  .equals("正常") &&
                t4  .equals("正常") &&
                ft3 .equals("正常") &&
                ft4 .equals("正常") &&
                e2  .equals("正常") &&
                p   .equals("正常") &&
                t   .equals("正常") &&
                prl .equals("正常") &&
                lh  .equals("正常") &&
                fsh .equals("正常") &&
                ca  .equals("正常") &&
                mg  .equals("正常") &&
                pa  .equals("正常") &&
                k   .equals("正常") &&
                na  .equals("正常") &&
                cl  .equals("正常") &&
                co2 .equals("正常") &&
                bglu.equals("正常") &&
                bc  .equals("正常") &&
                igf_1  .equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb .equals("正常") &&
                wbc .equals("正常") &&
                neut.equals("正常") &&
                rbc .equals("正常") &&
                pro .equals("正常") &&
                (wbcc.equals("升高") || wbcc.equals("降低"))
                ){
            tips = "26.尿路感染";
            variables.put("checkResult", 26);
        } else if(tp  .equals("正常") &&
                alb .equals("正常") &&
                glb .equals("正常") &&
                ag  .equals("正常") &&
                alt .equals("正常") &&
                ast .equals("正常") &&
                op  .equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp .equals("正常") &&
                y   .equals("正常") &&
                z   .equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua  .equals("正常") &&
                cys .equals("正常") &&
                tsh .equals("正常") &&
                t3  .equals("正常") &&
                t4  .equals("正常") &&
                ft3 .equals("正常") &&
                ft4 .equals("正常") &&
                e2  .equals("正常") &&
                p   .equals("正常") &&
                t   .equals("正常") &&
                prl .equals("正常") &&
                lh  .equals("正常") &&
                fsh .equals("正常") &&
                ca  .equals("正常") &&
                mg  .equals("正常") &&
                pa  .equals("正常") &&
                k   .equals("正常") &&
                na  .equals("正常") &&
                cl  .equals("正常") &&
                co2 .equals("正常") &&
                bglu.equals("正常") &&
                bc  .equals("正常") &&
                igf_1  .equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb .equals("正常") &&
                wbc .equals("正常") &&
                neut.equals("正常") &&
                rbc .equals("正常") &&
                pro .equals("正常") &&
                wbcc.equals("正常")
                ){
            tips = "30.所有指标全部正常";
            variables.put("checkResult", 30);
        } else if(tp  .equals("正常") &&
                alb .equals("正常") &&
                glb .equals("正常") &&
                ag  .equals("正常") &&
                alt .equals("正常") &&
                ast .equals("正常") &&
                op  .equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp .equals("正常") &&
                y   .equals("正常") &&
                z   .equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua  .equals("正常") &&
                cys .equals("正常") &&
                tsh .equals("正常") &&
                t3  .equals("正常") &&
                t4  .equals("正常") &&
                ft3 .equals("正常") &&
                ft4 .equals("正常") &&
                e2  .equals("正常") &&
                p   .equals("正常") &&
                t   .equals("正常") &&
                prl .equals("正常") &&
                lh  .equals("正常") &&
                fsh .equals("正常") &&
                ca  .equals("正常") &&
                mg  .equals("正常") &&
                pa  .equals("正常") &&
                k   .equals("正常") &&
                na  .equals("正常") &&
                cl  .equals("正常") &&
                co2 .equals("正常") &&
                bglu.equals("正常") &&
                bc  .equals("正常") &&
                igf_1  .equals("降低") &&
                igfbp_3.equals("降低") &&
                brbc.equals("正常") &&
                hgb .equals("正常") &&
                wbc .equals("正常") &&
                neut.equals("正常") &&
                rbc .equals("正常") &&
                pro .equals("正常") &&
                wbcc.equals("正常")
                ){
            tips = "31.IGF-1、IGFBP3降低";
            variables.put("checkResult", 31);
        } else if(tp  .equals("正常") &&
                alb .equals("正常") &&
                glb .equals("正常") &&
                ag  .equals("正常") &&
                alt .equals("正常") &&
                ast .equals("正常") &&
                op  .equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp .equals("正常") &&
                y   .equals("正常") &&
                z   .equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua  .equals("正常") &&
                cys .equals("正常") &&
                tsh .equals("正常") &&
                t3  .equals("正常") &&
                t4  .equals("正常") &&
                ft3 .equals("正常") &&
                ft4 .equals("正常") &&
                e2  .equals("正常") &&
                p   .equals("正常") &&
                t   .equals("正常") &&
                prl .equals("正常") &&
                lh  .equals("降低") &&
                fsh .equals("降低") &&
                ca  .equals("正常") &&
                mg  .equals("正常") &&
                pa  .equals("正常") &&
                k   .equals("正常") &&
                na  .equals("正常") &&
                cl  .equals("正常") &&
                co2 .equals("正常") &&
                bglu.equals("正常") &&
                bc  .equals("正常") &&
                igf_1  .equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb .equals("正常") &&
                wbc .equals("正常") &&
                neut.equals("正常") &&
                rbc .equals("正常") &&
                pro .equals("正常") &&
                wbcc.equals("正常")
                ){
            tips = "32.LH、FSH降低";
            variables.put("checkResult", 32);
        } else if(tp  .equals("正常") &&
                alb .equals("正常") &&
                glb .equals("正常") &&
                ag  .equals("正常") &&
                alt .equals("正常") &&
                ast .equals("正常") &&
                op  .equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp .equals("正常") &&
                y   .equals("正常") &&
                z   .equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua  .equals("正常") &&
                cys .equals("正常") &&
                tsh .equals("正常") &&
                t3  .equals("正常") &&
                t4  .equals("正常") &&
                ft3 .equals("正常") &&
                ft4 .equals("正常") &&
                (e2 .equals("升高") || e2 .equals("降低")) &&
                p   .equals("正常") &&
                (t  .equals("升高") || t  .equals("降低")) &&
                prl .equals("正常") &&
                (lh .equals("升高") || lh .equals("降低")) &&
                (fsh.equals("升高") || fsh.equals("降低")) &&
                ca  .equals("正常") &&
                mg  .equals("正常") &&
                pa  .equals("正常") &&
                k   .equals("正常") &&
                na  .equals("正常") &&
                cl  .equals("正常") &&
                co2 .equals("正常") &&
                bglu.equals("正常") &&
                bc  .equals("正常") &&
                igf_1  .equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb .equals("正常") &&
                wbc .equals("正常") &&
                neut.equals("正常") &&
                rbc .equals("正常") &&
                pro .equals("正常") &&
                wbcc.equals("正常")
                ){
            tips = "33.激素水平异常";
            variables.put("checkResult", 33);
        }
        String check1Case = tips.substring(0, tips.indexOf("."));
        variables.put("check1Case", check1Case);
        variables.put("tips", tips);
        taskService.complete(taskId, variables);
        return variables;
    }

    private String getStr(String tp) {
        tp = (tp == null ? "正常" : tp);
        return tp;
    }
    @GetMapping("assess15")
    public Map assessCheck2(String taskId, String a1, String a2, String a3, String a4, String a5) {
        Map<String, Object> variables = new HashMap<>();
        String treatmentPlan = "";
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            variables.put("msg", "流程不存在");
            return variables;
        }
        //骨龄初步诊断
        String check1Case = a1;//30 31 32 33
        String chromosomeExam = a2;
        String ghTest = a3;
        int boneAgePercentile = Integer.parseInt(a4);
        boolean isFamilyShortHistory = a5.equals("是") ? true : false;//家族矮小史
        variables.put("illnessResult", 0);
        String illnessDiagnosis = "其他疾病，建议持续跟踪";
        if(check1Case.equals("30") && chromosomeExam.equals("正常") && ghTest.equals("正常") &&boneAgePercentile < 3) {
            illnessDiagnosis = "1. 特发性矮小ISS";
            treatmentPlan = "治疗方案：1.特发性矮小ISS";
            variables.put("illnessResult", 1);
        } else if(check1Case.equals("30") && chromosomeExam.equals("正常") && ghTest.equals("正常") &&boneAgePercentile < 3 && isFamilyShortHistory) {
            illnessDiagnosis = "2. 特发性矮小ISS";
            treatmentPlan = "治疗方案：2.特发性矮小ISS";
            variables.put("illnessResult", 2);
        } else if(check1Case.equals("30") && chromosomeExam.equals("异常") && ghTest.equals("正常")) {
            illnessDiagnosis = "3. 特纳综合症或21染色体";
            treatmentPlan = "治疗方案：3. 特纳综合症或21染色体";
            variables.put("illnessResult", 3);
        } else if(check1Case.equals("31") && chromosomeExam.equals("正常") && ghTest.equals("异常")) {
            illnessDiagnosis = "4. 生长激素缺乏症GHD";
            treatmentPlan = "治疗方案：4. 生长激素缺乏症GHD";
            variables.put("illnessResult", 4);
        } else if(check1Case.equals("31") && chromosomeExam.equals("正常") && ghTest.equals("正常")) {
            illnessDiagnosis = "5. 小于胎龄儿SGA";
            treatmentPlan = "治疗方案：5. 小于胎龄儿SGA";
            variables.put("illnessResult", 5);
        } else if(check1Case.equals("32") && chromosomeExam.equals("正常") && ghTest.equals("正常")) {
            illnessDiagnosis = "6. 体质性青春发育延迟CDGP";
            treatmentPlan = "治疗方案：6. 体质性青春发育延迟CDGP";
            variables.put("illnessResult", 6);
        } else if(check1Case.equals("33") && chromosomeExam.equals("正常") && ghTest.equals("正常")) {
            illnessDiagnosis = "7. 中枢性性早熟CPP";
            treatmentPlan = "治疗方案：7. 中枢性性早熟CPP";
            variables.put("illnessResult", 7);
        }
        variables.put("illnessDiagnosis", illnessDiagnosis);
        variables.put("treatmentPlan", treatmentPlan);
        taskService.complete(taskId, variables);
        return variables;
    }
    @GetMapping("assess0")
    public String assess0(String taskId, String a1, String tp) {
        tp = (tp == null ? "正常" : tp);
        System.out.println(tp);
        String tips = "";
        if (a1 == null) {
            tips = "null";
        } else if (a1.equals("是")) {
            tips = "true";
        } else if (a1.equals("否")) {
            tips = "否";
        }

//        variables.put("a1", a1);
        return tips;
    }


    /**
     * 计算身高百分位数
     *
     * @param taskId
     * @param heightPercentile
     * @return
     */
    @GetMapping("check")
    public String computePercentile(String taskId, String heightPercentile, String boneAge, String isPatient, String isUnderTreatment, String isFillIn) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return "流程不存在";
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("heightPercentile", heightPercentile);
        variables.put("boneAge", boneAge);
        variables.put("isPatient", Boolean.valueOf(isPatient));
        variables.put("isUnderTreatment", Boolean.valueOf(isUnderTreatment));
        variables.put("isFillIn", Boolean.valueOf(isFillIn));
        taskService.complete(taskId, variables);
        return "您的任务已处理，流程已推进。 患者上一个处理任务为：【 " + task.getName() + " 】";

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
     * @param taskId
     * @param targetTaskKey
     * @return
     */
    @GetMapping("rollbask")
    public String rollbaskTask(String taskId, String targetTaskKey) {
        Task currentTask = taskService.createTaskQuery().taskId(taskId).singleResult();
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
    @RequestMapping(value = "processDiagram")
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
        InputStream in = diagramGenerator.generateDiagram(bpmnModel, "png", activityIds, flows, engconf.getActivityFontName(), engconf.getLabelFontName(), engconf.getAnnotationFontName(), engconf.getClassLoader(), 1.0);
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
     * 新版获取流程图
     */
    /**
     * 获取流程图
     * @param processDefinedId
     */
//    @GetMapping(value = "/getFlowDiagram/{processDefinedId}")
//    public ErrorMsg getFlowDiagram(@PathVariable(value = "processDefinedId") String processDefinedId) throws IOException {
//        List<String> flows = new ArrayList<>();
//        //获取流程图
//        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinedId);
//        ProcessEngineConfiguration processEngineConfig = processEngine.getProcessEngineConfiguration();
//
//        ProcessDiagramGenerator diagramGenerator = processEngineConfig.getProcessDiagramGenerator();
//        InputStream in = diagramGenerator.generateDiagram(bpmnModel, "bmp", new ArrayList<>(), flows, processEngineConfig.getActivityFontName(),
//                processEngineConfig.getLabelFontName(), processEngineConfig.getAnnotationFontName(), processEngineConfig.getClassLoader(), 1.0);
//
//        // in.available()返回文件的字节长度
//        byte[] buf = new byte[in.available()];
//        // 将文件中的内容读入到数组中
//        in.read(buf);
//        // 进行Base64编码处理
//        String base64Img =  new String(Base64.encodeBase64(buf));
//        in.close();
//        return ErrorMsg.PREVIEW_SUCCESS.setNewData(base64Img);
//    }


    /**
     * 流程以及表单的部署
     */
    @RequestMapping(value = "nform")
    public void deployTest() {
        Deployment deployment = repositoryService.createDeployment()
                .name("表单流程")
                .addClasspathResource("processes/test-form.bpmn20.xml")
                .deploy();

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().
                deploymentId(deployment.getId())
                .singleResult();
        String processDefinitionId = processDefinition.getId();
        FormDeployment formDeployment = formRepositoryService.createDeployment()
                .name("definition-one")
                .addClasspathResource("forms/form1.form")
                .parentDeploymentId(deployment.getId())
                .deploy();
        FormDefinition formDefinition = formRepositoryService.createFormDefinitionQuery().deploymentId(formDeployment.getId()).singleResult();
        String formDefinitionId = formDefinition.getId();


        //启动实例并且设置表单的值
        String outcome = "shareniu";
        Map<String, Object> formProperties = new HashMap<>();
        formProperties.put("reason", "家里有事");
        formProperties.put("startTime", "2020-8-17");
        formProperties.put("endTime", "2020-8-17");
        String processInstanceName = "shareniu";
        runtimeService.startProcessInstanceWithForm(processDefinitionId, outcome, formProperties, processInstanceName);
        HistoricProcessInstanceEntity historicProcessInstanceEntity = (HistoricProcessInstanceEntity) historyService.createHistoricProcessInstanceQuery()
                .processDefinitionId(processDefinitionId)
                .singleResult();
        String processInstanceId = historicProcessInstanceEntity.getProcessInstanceId();


        //查询表单信息
        SimpleFormModel fm = (SimpleFormModel) runtimeService.getStartFormModel(processDefinitionId, processInstanceId).getFormModel();
        //FormInfo fm = runtimeService.getStartFormModel(processDefinitionId, processInstanceId);

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


        //查询个人任务并填写表单
        Map<String, Object> formProperties2 = new HashMap<>();
        formProperties2.put("reason", "家里有事2222");
        formProperties2.put("startTime", "2020-8-18");
        formProperties2.put("endTime", "2020-8-18");
        formProperties2.put("days", "3");
        Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
        String taskId = task.getId();
        String outcome2 = "牛哥";
        taskService.completeTaskWithForm(taskId, formDefinitionId, outcome2, formProperties2);

        //获取个人任务表单
        FormInfo taskFm = taskService.getTaskFormModel(taskId);


        SimpleFormModel sfm = (SimpleFormModel) taskFm.getFormModel();
        System.out.println(sfm);

        List<FormField> formFields = sfm.getFields();
        for (FormField ff : formFields) {
            System.out.println("######################");
            System.out.println(ff.getId());
            System.out.println(ff.getName());
            System.out.println(ff.getType());
            System.out.println(ff.getPlaceholder());
            System.out.println(ff.getValue());
            System.out.println("######################");

        }
    }

}
