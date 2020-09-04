package cn.dfusion.ai.controller;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author LDF
 * @date 2020/7/8
 */
@RestController
@RequestMapping("basicDiagnosis")
public class DiagnosisController {

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
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("growth8");
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
//    @GetMapping("add")
//    public String addExpense(String name,
//                             String heightPercentile,
//                             String gender,
//                             String birthDate,
//                             String measureDate,
//                             String height,
//                             String weight) {
//        Map<String, Object> map = new HashMap<>();
//        //基本信息
//        map.put("name", name);
//        map.put("heightPercentile", heightPercentile);
//        map.put("gender", gender);
//        map.put("birthDate", birthDate);
//        map.put("measureDate", measureDate);
//        map.put("height", height);
//        map.put("weight", weight);
//        //检验检查信息
//
//        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("growth7", map);
//        return "提交成功,流程ID为：" + processInstance.getId();
//    }

    /**
     * 获取指定用户组流程任务列表
     *
     * @param group
     * @return
     */
//    @GetMapping("nextId")
//    public Object list(String group) {
//        List<Task> tasks = taskService.createTaskQuery().taskCandidateGroup(group).list();
//        return tasks.get(tasks.size() - 1).getId();
//    }

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
     * @param
     * @param taskId 任务id
     */
//    public void nextFlowNode(String node, String taskId) {
//        Task task = processEngine.getTaskService().createTaskQuery().taskId(taskId).singleResult();
//        ExecutionEntity ee = (ExecutionEntity) processEngine.getRuntimeService().createExecutionQuery()
//                .executionId(task.getExecutionId()).singleResult();
//        // 当前审批节点
//        String crruentActivityId = ee.getActivityId();
//        BpmnModel bpmnModel = processEngine.getRepositoryService().getBpmnModel(task.getProcessDefinitionId());
//        FlowNode flowNode = (FlowNode) bpmnModel.getFlowElement(crruentActivityId);
//        // 输出连线
//        List<SequenceFlow> outFlows = flowNode.getOutgoingFlows();
//        for (SequenceFlow sequenceFlow : outFlows) {
//            //当前审批节点
//            if ("now".equals(node)) {
//                FlowElement sourceFlowElement = sequenceFlow.getSourceFlowElement();
//                System.out.println("当前节点: id=" + sourceFlowElement.getId() + ",name=" + sourceFlowElement.getName());
//            } else if ("next".equals(node)) {
//                // 下一个审批节点
//                FlowElement targetFlow = sequenceFlow.getTargetFlowElement();
//                if (targetFlow instanceof UserTask) {
//                    System.out.println("下一节点: id=" + targetFlow.getId() + ",name=" + targetFlow.getName());
//                }
//                // 如果下个审批节点为结束节点
//                if (targetFlow instanceof EndEvent) {
//                    System.out.println("下一节点为结束节点：id=" + targetFlow.getId() + ",name=" + targetFlow.getName());
//                }
//            }
//        }
//    }

    @GetMapping("assess1")
    public String assess(String taskId,
                         String age,
                         Double growthRate,
                         Integer fatherHeightPercentile,
                         Integer motherHeightPercentile) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return "流程不存在";
        }

        Map<String, Object> variables = new HashMap<>();
        if (null != growthRate) {
            variables.put("growthRate", this.getGrowthRateStatus(age, growthRate));
        } else {
            variables.put("growthRate", true);
        }
        boolean isFatherHeightNormal = this.isPercentileNormal(fatherHeightPercentile);
        boolean isMotherHeightNormal = this.isPercentileNormal(motherHeightPercentile);
        boolean isParentHeightNormal = (isFatherHeightNormal && isMotherHeightNormal);
        variables.put("parentHeight", isParentHeightNormal);
        taskService.complete(taskId, variables);
        String tips = "生长速率 --> 正常";
        if ((growthRate == null || this.getGrowthRateStatus(age, growthRate)) && !isParentHeightNormal) {
            tips = "有家族矮小史";
        } else if ((growthRate != null && !this.getGrowthRateStatus(age, growthRate)) && isParentHeightNormal) {
            tips = "年生长速率低";
        } else if ((growthRate != null && !this.getGrowthRateStatus(age, growthRate)) && !isParentHeightNormal) {
            tips = "年生长速率低且有家族矮小史";
        }
        return tips;
    }

    /**
     * 判断生长速率是否正常
     *
     * @param age
     * @param growthRate
     * @return
     */
    private boolean getGrowthRateStatus(String age, double growthRate) {
        boolean isNormal = false;
        String year;

        int index = age.indexOf("岁");
        if (index == -1) {
            //0-1岁
            if (growthRate >= 25) {
                isNormal = true;
            }
        } else {
            //1岁以上
            year = age.substring(0, index);
            switch (year) {
                case "1":
                    if (growthRate >= 12) {
                        isNormal = true;
                    }
                    break;
                case "2":
                    if (10 >= growthRate && growthRate >= 8) {
                        isNormal = true;
                    }
                    break;
                case "3":
                case "4":
                case "5":
                case "6":
                case "7":
                case "8":
                case "9":
                    if (growthRate >= 6) {
                        isNormal = true;
                    }
                    break;
                default:
                    if (growthRate >= 5) {
                        isNormal = true;
                    }
                    break;
            }
        }
        return isNormal;
    }


    /**
     * 判断出生孕周是否正常
     *
     * @param taskId
     * @param pregnantWeek
     * @return
     */
    @GetMapping("assess2")
    public String assessPregnantWeek(String taskId,
                                     Integer pregnantWeek) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return "流程不存在";
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("pregnantWeek", pregnantWeek);
        taskService.complete(taskId, variables);
        String tips = "出生孕周 --> 正常";
        if (pregnantWeek < 37) {
            tips = "早产";
        } else if (pregnantWeek > 42) {
            tips = "过期产";
        }
        return tips;
    }

    /**
     * 判断是否符合SGA
     *
     * @param taskId
     * @param pregnantWeek
     * @param height (cm)
     * @param weight (kg)
     * @param gender
     * @return
     */
    @GetMapping("assess3")
    public String assessSGA(String taskId,
                            Integer pregnantWeek,
                            Double height,
                            Double weight,
                            String gender) {
        String tips = "不符合SGA --> 正常";
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return "流程不存在";
        }
        Map<String, Object> variables = new HashMap<>();
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

    /**
     * 是否有家族矮小史
     *
     * @param taskId
     * @param grandfatherHeight
     * @param grandmotherHeight
     * @return
     */
    @GetMapping("assess4")
    public String assessFamilyShort(String taskId,
                                    Double grandfatherHeight,
                                    Double grandmotherHeight) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return "流程不存在";
        }
        boolean isFamilyShort = false;
        Map<String, Object> variables = new HashMap<>();
        if (grandfatherHeight < 160 || grandmotherHeight < 148) {
            isFamilyShort = true;
        }
        variables.put("isFamilyShort", isFamilyShort);

        taskService.complete(taskId, variables);
        String tips = "无家族矮小史 --> 正常";
        if (isFamilyShort) {
            tips = "有家族矮小史";
        }
        return tips;
    }

    @GetMapping("assess5")
    public String assessChronicDisease(String taskId,
                                       String normal) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return "流程不存在";
        }
        boolean isChronicDisease = false;
        Map<String, Object> variables = new HashMap<>();
        isChronicDisease = normal.equals("是") ? true : false;
        variables.put("isChronicDisease", isChronicDisease);

        taskService.complete(taskId, variables);
        String tips = "无慢性病史 --> 正常";
        if (isChronicDisease) {
            tips = "有慢性病史";
        }
        return tips;
    }

    @GetMapping("assess6")
    public String assessDrugAllerg(String taskId,
                                   String normal) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return "流程不存在";
        }
        boolean isDrugAllerg = false;
        Map<String, Object> variables = new HashMap<>();
        isDrugAllerg = normal.equals("是") ? true : false;
        variables.put("isDrugAllerg", isDrugAllerg);

        taskService.complete(taskId, variables);
        String tips = "无药物过敏史 --> 正常";
        if (isDrugAllerg) {
            tips = "有药物过敏史";
        }
        return tips;
    }

    @GetMapping("assess7")
    public String assessHeight(String taskId,
                               Integer heightPercentile,
                               Integer hereditaryHeightDiff) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return "流程不存在";
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("heightPercentile", heightPercentile);
        variables.put("hereditaryHeightDiff", hereditaryHeightDiff);

        taskService.complete(taskId, variables);
        String tips = "测量身高体重 --> 正常";
        if (heightPercentile < 3) {
            tips = "身材矮小，需要进一步详细问诊";
        } else if (heightPercentile > 10 && hereditaryHeightDiff < 30) {
            tips = "测量身高体重 --> 正常；不需详细问诊，直接进行上下部量";
        } else if (heightPercentile <= 10 && hereditaryHeightDiff >= 30) {
            tips = "需要进一步详细问诊";
        }
        return tips;
    }

    @GetMapping("assess8")
    public String assessPerinatalInjury
            (String taskId,
             String perinatalInjury) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return "流程不存在";
        }
        boolean isPerinatalInjury = false;
        Map<String, Object> variables = new HashMap<>();
        isPerinatalInjury = perinatalInjury.equals("是") ? true : false;
        variables.put("isPerinatalInjury", isPerinatalInjury);
        taskService.complete(taskId, variables);
        String tips = "无围产期损伤 --> 正常";
        if (isPerinatalInjury) {
            tips = "有围产期损伤";
        }
        return tips;
    }

    @GetMapping("assess9")
    public String assessParentDevelopmentDelay(String taskId,
                                               String parentDevelopmentDelay) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return "流程不存在";
        }
        boolean isParentDevelopmentDelay = false;
        Map<String, Object> variables = new HashMap<>();
        isParentDevelopmentDelay = parentDevelopmentDelay.equals("是") ? true : false;
        variables.put("isParentDevelopmentDelay", isParentDevelopmentDelay);

        taskService.complete(taskId, variables);
        String tips = "无体质性青春期发育延迟 --> 正常";
        if (isParentDevelopmentDelay) {
            tips = "疑似体质性青春期延迟,进一步辅助检查判断";
        }
        return tips;
    }

    @GetMapping("assess10")
    public String assessUpDownMeasure(String taskId,
                                      Double up,
                                      Double down) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return "流程不存在";
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("up", up);
        variables.put("down", down);

        taskService.complete(taskId, variables);
        String tips = "上下部量 --> 正常";
        if (up > down) {
            tips = "疑似软骨发育不良/不全,成骨发育不全、骺软发育不良等";
        } else if (up < down) {
            tips = "疑似粘多糖病、脊椎骨骺发育不良等";
        }
        return tips;
    }

    @GetMapping("assess11")
    public String assessNeedSSS(String taskId,
                                String needSSS) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return "流程不存在";
        }
        boolean isNeedSSS = false;
        Map<String, Object> variables = new HashMap<>();
        isNeedSSS = needSSS.equals("是") ? true : false;
        variables.put("isNeedSSS", isNeedSSS);

        taskService.complete(taskId, variables);
        String tips = "不需要进行第二性征查体";
        if (isNeedSSS) {
            tips = "需要进行第二性征查体";
        }
        return tips;
    }

    @GetMapping("assess12")
    public String assessSSS(String taskId,
                            String SSS) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return "流程不存在";
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("SSS", SSS);
        taskService.complete(taskId, variables);
        String tips = "第二性征查体 --> 正常";
        if (SSS.equals("落后")) {
            tips = "青春发育延迟";
        } else if (SSS.equals("提前")) {
            tips = "性早熟";
        }
        return tips;
    }

    @GetMapping("assess13")
    public String assessBoneAge(String taskId,
                                Integer heightPercentile,
                                String developmentDelay,
                                Double growthRate,
                                Integer hereditaryHeightPercentile,
                                Integer boneAgePercentile,
                                Double boneAgeDiff) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return "流程不存在";
        }

        String heightStatus = "";
        if (heightPercentile >= 25 && heightPercentile <= 75) {
            heightStatus = "正常";
        } else if (heightPercentile > 75) {
            heightStatus = "偏高";
        } else if (heightPercentile < 25) {
            heightStatus = "偏低";
        }
        boolean isDevelopmentDelay = developmentDelay.equals("是") ? true : false;
        boolean isGrowthRate = false;
        if (growthRate > 6.0) {
            isGrowthRate = true;
        }
        boolean isHereditaryHeightPercentile = isPercentileNormal(hereditaryHeightPercentile);
        boolean isBoneAgePercentile = isPercentileNormal(boneAgePercentile);

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
            tips = "1.正常";
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
            tips = "其他情况，建议继续观察";
            variables.put("result", "0");
        }
        taskService.complete(taskId, variables);
        return "骨龄检查 --> " + tips;
    }

    //判断身高百分位数是否正常[3, 97]--正常
    private boolean isPercentileNormal(Integer percentile) {
        boolean isNormal = false;
        if (percentile >= 3 && percentile <= 97) {
            isNormal = true;
        }
        return isNormal;
    }

    @GetMapping("assess13_2")
    public String assessBoneAge(String taskId,
                                String SSS, //第二性征："落后"/"提前"
                                Integer boneAgePercentile,
                                Double boneAgeDiff) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return "流程不存在";
        }
        //骨龄初步诊断
        Map<String, Object> variables = new HashMap<>();
        variables.put("SSS", SSS);
        variables.put("boneAgePercentile", boneAgePercentile);
        variables.put("boneAgeDiff", boneAgeDiff);
        String firstDiagnosis;
        if (boneAgePercentile < 6 || boneAgeDiff <= -2) { //骨龄落后2岁及以上
            firstDiagnosis = "【 考虑矮小，建议进一步检查 】";
        } else if (SSS.equals("提前") && boneAgeDiff >= 2) {//骨龄提前2岁及以上
            firstDiagnosis = "【 考虑性早熟，建议进一步检查 】";
        } else {
            firstDiagnosis = "【 考虑非病理性问题，建议改善生活方式，定期复查 】";
        }
        taskService.complete(taskId, variables);
        return firstDiagnosis;
    }

    @GetMapping("assess14")
    public Map assessCheck(String taskId,
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
        String tips = "其他情况，建议持续观察";
        //肝功
        tp = getStr(tp);
        alb = getStr(alb);
        glb = getStr(glb);
        ag = getStr(ag);
        alt = getStr(alt);
        ast = getStr(ast);
        op = getStr(op);
        tbil = getStr(tbil);
        ibil = getStr(ibil);
        alp = getStr(alp);
        y = getStr(y);
        z = getStr(z);
        //肾功能
        crea = getStr(crea);
        urea = getStr(urea);
        ua = getStr(ua);
        cys = getStr(cys);
        //甲功
        tsh = getStr(tsh);
        t3 = getStr(t3);
        t4 = getStr(t4);
        ft3 = getStr(ft3);
        ft4 = getStr(ft4);
        //激素六项
        e2 = getStr(e2);
        p = getStr(p);
        t = getStr(t);
        prl = getStr(prl);
        lh = getStr(lh);
        fsh = getStr(fsh);
        //电解质
        ca = getStr(ca);
        mg = getStr(mg);
        pa = getStr(pa);
        k = getStr(k);
        na = getStr(na);
        cl = getStr(cl);
        co2 = getStr(co2);
        //String bglu, String bc, String igf_1, String igfbp_3,//
        bglu = getStr(bglu);
        bc = getStr(bc);
        igf_1 = getStr(igf_1);
        igfbp_3 = getStr(igfbp_3);
        //血常规
        brbc = getStr(brbc);
        hgb = getStr(hgb);
        wbc = getStr(wbc);
        neut = getStr(neut);
        //尿常规
        rbc = getStr(rbc);
        pro = getStr(pro);
        wbcc = getStr(wbcc);

        if (tp.equals("降低") &&
                alb.equals("正常") &&
                glb.equals("正常") &&
                ag.equals("正常") &&
                alt.equals("正常") &&
                ast.equals("正常") &&
                op.equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp.equals("正常") &&
                y.equals("正常") &&
                z.equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua.equals("正常") &&
                cys.equals("正常") &&
                tsh.equals("正常") &&
                t3.equals("正常") &&
                t4.equals("正常") &&
                ft3.equals("正常") &&
                ft4.equals("正常") &&
                e2.equals("正常") &&
                p.equals("正常") &&
                t.equals("正常") &&
                prl.equals("正常") &&
                lh.equals("正常") &&
                fsh.equals("正常") &&
                ca.equals("正常") &&
                mg.equals("正常") &&
                pa.equals("正常") &&
                k.equals("正常") &&
                na.equals("正常") &&
                cl.equals("正常") &&
                co2.equals("正常") &&
                bglu.equals("正常") &&
                bc.equals("正常") &&
                igf_1.equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb.equals("正常") &&
                wbc.equals("正常") &&
                neut.equals("正常") &&
                rbc.equals("正常") &&
                pro.equals("正常") &&
                wbcc.equals("正常")
                ) {
            tips = "1.营养不良";
            variables.put("checkResult", 1);
        } else if (tp.equals("正常") &&
                alb.equals("正常") &&
                glb.equals("正常") &&
                ag.equals("正常") &&
                alt.equals("升高") &&
                ast.equals("正常") &&
                op.equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp.equals("正常") &&
                y.equals("正常") &&
                z.equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua.equals("正常") &&
                cys.equals("正常") &&
                tsh.equals("正常") &&
                t3.equals("正常") &&
                t4.equals("正常") &&
                ft3.equals("正常") &&
                ft4.equals("正常") &&
                e2.equals("正常") &&
                p.equals("正常") &&
                t.equals("正常") &&
                prl.equals("正常") &&
                lh.equals("正常") &&
                fsh.equals("正常") &&
                ca.equals("正常") &&
                mg.equals("正常") &&
                pa.equals("正常") &&
                k.equals("正常") &&
                na.equals("正常") &&
                cl.equals("正常") &&
                co2.equals("正常") &&
                bglu.equals("正常") &&
                bc.equals("正常") &&
                igf_1.equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb.equals("正常") &&
                wbc.equals("正常") &&
                neut.equals("正常") &&
                rbc.equals("正常") &&
                pro.equals("正常") &&
                wbcc.equals("正常")
                ) {
            tips = "3.肝损害";
            variables.put("checkResult", 3);
        } else if (tp.equals("正常") &&
                alb.equals("正常") &&
                glb.equals("正常") &&
                ag.equals("正常") &&
                alt.equals("正常") &&
                ast.equals("正常") &&
                op.equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp.equals("正常") &&
                y.equals("正常") &&
                z.equals("正常") &&
                crea.equals("升高") &&
                urea.equals("正常") &&
                ua.equals("正常") &&
                cys.equals("正常") &&
                tsh.equals("正常") &&
                t3.equals("正常") &&
                t4.equals("正常") &&
                ft3.equals("正常") &&
                ft4.equals("正常") &&
                e2.equals("正常") &&
                p.equals("正常") &&
                t.equals("正常") &&
                prl.equals("正常") &&
                lh.equals("正常") &&
                fsh.equals("正常") &&
                ca.equals("正常") &&
                mg.equals("正常") &&
                pa.equals("正常") &&
                k.equals("正常") &&
                na.equals("正常") &&
                cl.equals("正常") &&
                co2.equals("正常") &&
                bglu.equals("正常") &&
                bc.equals("正常") &&
                igf_1.equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb.equals("正常") &&
                wbc.equals("正常") &&
                neut.equals("正常") &&
                rbc.equals("正常") &&
                pro.equals("正常") &&
                wbcc.equals("正常")
                ) {
            tips = "4.肾功能不全";
            variables.put("checkResult", 4);
        } else if (tp.equals("正常") &&
                alb.equals("正常") &&
                glb.equals("正常") &&
                ag.equals("正常") &&
                alt.equals("正常") &&
                ast.equals("正常") &&
                op.equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp.equals("正常") &&
                y.equals("正常") &&
                z.equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua.equals("升高") &&
                cys.equals("正常") &&
                tsh.equals("正常") &&
                t3.equals("正常") &&
                t4.equals("正常") &&
                ft3.equals("正常") &&
                ft4.equals("正常") &&
                e2.equals("正常") &&
                p.equals("正常") &&
                t.equals("正常") &&
                prl.equals("正常") &&
                lh.equals("正常") &&
                fsh.equals("正常") &&
                ca.equals("正常") &&
                mg.equals("正常") &&
                pa.equals("正常") &&
                k.equals("正常") &&
                na.equals("正常") &&
                cl.equals("正常") &&
                co2.equals("正常") &&
                bglu.equals("正常") &&
                bc.equals("正常") &&
                igf_1.equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb.equals("正常") &&
                wbc.equals("正常") &&
                neut.equals("正常") &&
                rbc.equals("正常") &&
                pro.equals("正常") &&
                wbcc.equals("正常")
                ) {
            tips = "5.高尿酸血症";
            variables.put("checkResult", 5);
        } else if (tp.equals("正常") &&
                alb.equals("正常") &&
                glb.equals("正常") &&
                ag.equals("正常") &&
                alt.equals("正常") &&
                ast.equals("正常") &&
                op.equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp.equals("正常") &&
                y.equals("正常") &&
                z.equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua.equals("正常") &&
                cys.equals("正常") &&
                tsh.equals("升高") &&
                t3.equals("正常") &&
                t4.equals("正常") &&
                ft3.equals("正常") &&
                ft4.equals("正常") &&
                e2.equals("正常") &&
                p.equals("正常") &&
                t.equals("正常") &&
                prl.equals("正常") &&
                lh.equals("正常") &&
                fsh.equals("正常") &&
                ca.equals("正常") &&
                mg.equals("正常") &&
                pa.equals("正常") &&
                k.equals("正常") &&
                na.equals("正常") &&
                cl.equals("正常") &&
                co2.equals("正常") &&
                bglu.equals("正常") &&
                bc.equals("正常") &&
                igf_1.equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb.equals("正常") &&
                wbc.equals("正常") &&
                neut.equals("正常") &&
                rbc.equals("正常") &&
                pro.equals("正常") &&
                wbcc.equals("正常")
                ) {
            tips = "6.常见于甲减";
            variables.put("checkResult", 6);
        } else if (tp.equals("正常") &&
                alb.equals("正常") &&
                glb.equals("正常") &&
                ag.equals("正常") &&
                alt.equals("正常") &&
                ast.equals("正常") &&
                op.equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp.equals("正常") &&
                y.equals("正常") &&
                z.equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua.equals("正常") &&
                cys.equals("正常") &&
                tsh.equals("降低") &&
                t3.equals("正常") &&
                t4.equals("正常") &&
                ft3.equals("正常") &&
                ft4.equals("正常") &&
                e2.equals("正常") &&
                p.equals("正常") &&
                t.equals("正常") &&
                prl.equals("正常") &&
                lh.equals("正常") &&
                fsh.equals("正常") &&
                ca.equals("正常") &&
                mg.equals("正常") &&
                pa.equals("正常") &&
                k.equals("正常") &&
                na.equals("正常") &&
                cl.equals("正常") &&
                co2.equals("正常") &&
                bglu.equals("正常") &&
                bc.equals("正常") &&
                igf_1.equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb.equals("正常") &&
                wbc.equals("正常") &&
                neut.equals("正常") &&
                rbc.equals("正常") &&
                pro.equals("正常") &&
                wbcc.equals("正常")
                ) {
            tips = "7.常见于甲亢";
            variables.put("checkResult", 7);
        } else if (tp.equals("正常") &&
                alb.equals("正常") &&
                glb.equals("正常") &&
                ag.equals("正常") &&
                alt.equals("正常") &&
                ast.equals("正常") &&
                op.equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp.equals("正常") &&
                y.equals("正常") &&
                z.equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua.equals("正常") &&
                cys.equals("正常") &&
                tsh.equals("正常") &&
                t3.equals("正常") &&
                t4.equals("正常") &&
                ft3.equals("降低") &&
                ft4.equals("正常") &&
                e2.equals("正常") &&
                p.equals("正常") &&
                t.equals("正常") &&
                prl.equals("正常") &&
                lh.equals("正常") &&
                fsh.equals("正常") &&
                ca.equals("正常") &&
                mg.equals("正常") &&
                pa.equals("正常") &&
                k.equals("正常") &&
                na.equals("正常") &&
                cl.equals("正常") &&
                co2.equals("正常") &&
                bglu.equals("正常") &&
                bc.equals("正常") &&
                igf_1.equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb.equals("正常") &&
                wbc.equals("正常") &&
                neut.equals("正常") &&
                rbc.equals("正常") &&
                pro.equals("正常") &&
                wbcc.equals("正常")
                ) {
            tips = "8.常见于甲减";
            variables.put("checkResult", 8);
        } else if (tp.equals("正常") &&
                alb.equals("正常") &&
                glb.equals("正常") &&
                ag.equals("正常") &&
                alt.equals("正常") &&
                ast.equals("正常") &&
                op.equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp.equals("正常") &&
                y.equals("正常") &&
                z.equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua.equals("正常") &&
                cys.equals("正常") &&
                tsh.equals("正常") &&
                t3.equals("正常") &&
                t4.equals("正常") &&
                ft3.equals("升高") &&
                ft4.equals("正常") &&
                e2.equals("正常") &&
                p.equals("正常") &&
                t.equals("正常") &&
                prl.equals("正常") &&
                lh.equals("正常") &&
                fsh.equals("正常") &&
                ca.equals("正常") &&
                mg.equals("正常") &&
                pa.equals("正常") &&
                k.equals("正常") &&
                na.equals("正常") &&
                cl.equals("正常") &&
                co2.equals("正常") &&
                bglu.equals("正常") &&
                bc.equals("正常") &&
                igf_1.equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb.equals("正常") &&
                wbc.equals("正常") &&
                neut.equals("正常") &&
                rbc.equals("正常") &&
                pro.equals("正常") &&
                wbcc.equals("正常")
                ) {
            tips = "9.常见于甲亢";
            variables.put("checkResult", 9);
        } else if (tp.equals("正常") &&
                alb.equals("正常") &&
                glb.equals("正常") &&
                ag.equals("正常") &&
                alt.equals("正常") &&
                ast.equals("正常") &&
                op.equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp.equals("正常") &&
                y.equals("正常") &&
                z.equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua.equals("正常") &&
                cys.equals("正常") &&
                tsh.equals("正常") &&
                t3.equals("正常") &&
                t4.equals("正常") &&
                ft3.equals("正常") &&
                ft4.equals("降低") &&
                e2.equals("正常") &&
                p.equals("正常") &&
                t.equals("正常") &&
                prl.equals("正常") &&
                lh.equals("正常") &&
                fsh.equals("正常") &&
                ca.equals("正常") &&
                mg.equals("正常") &&
                pa.equals("正常") &&
                k.equals("正常") &&
                na.equals("正常") &&
                cl.equals("正常") &&
                co2.equals("正常") &&
                bglu.equals("正常") &&
                bc.equals("正常") &&
                igf_1.equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb.equals("正常") &&
                wbc.equals("正常") &&
                neut.equals("正常") &&
                rbc.equals("正常") &&
                pro.equals("正常") &&
                wbcc.equals("正常")
                ) {
            tips = "10.常见于甲减";
            variables.put("checkResult", 10);
        } else if (tp.equals("正常") &&
                alb.equals("正常") &&
                glb.equals("正常") &&
                ag.equals("正常") &&
                alt.equals("正常") &&
                ast.equals("正常") &&
                op.equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp.equals("正常") &&
                y.equals("正常") &&
                z.equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua.equals("正常") &&
                cys.equals("正常") &&
                tsh.equals("正常") &&
                t3.equals("正常") &&
                t4.equals("正常") &&
                ft3.equals("正常") &&
                ft4.equals("升高") &&
                e2.equals("正常") &&
                p.equals("正常") &&
                t.equals("正常") &&
                prl.equals("正常") &&
                lh.equals("正常") &&
                fsh.equals("正常") &&
                ca.equals("正常") &&
                mg.equals("正常") &&
                pa.equals("正常") &&
                k.equals("正常") &&
                na.equals("正常") &&
                cl.equals("正常") &&
                co2.equals("正常") &&
                bglu.equals("正常") &&
                bc.equals("正常") &&
                igf_1.equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb.equals("正常") &&
                wbc.equals("正常") &&
                neut.equals("正常") &&
                rbc.equals("正常") &&
                pro.equals("正常") &&
                wbcc.equals("正常")
                ) {
            tips = "11.常见于甲亢";
            variables.put("checkResult", 11);
        } else if (tp.equals("正常") &&
                alb.equals("正常") &&
                glb.equals("正常") &&
                ag.equals("正常") &&
                alt.equals("正常") &&
                ast.equals("正常") &&
                op.equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp.equals("正常") &&
                y.equals("正常") &&
                z.equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua.equals("正常") &&
                cys.equals("正常") &&
                tsh.equals("正常") &&
                t3.equals("正常") &&
                t4.equals("正常") &&
                ft3.equals("正常") &&
                ft4.equals("正常") &&
                e2.equals("升高") &&
                p.equals("正常") &&
                t.equals("正常") &&
                prl.equals("正常") &&
                lh.equals("正常") &&
                fsh.equals("正常") &&
                ca.equals("正常") &&
                mg.equals("正常") &&
                pa.equals("正常") &&
                k.equals("正常") &&
                na.equals("正常") &&
                cl.equals("正常") &&
                co2.equals("正常") &&
                bglu.equals("正常") &&
                bc.equals("正常") &&
                igf_1.equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb.equals("正常") &&
                wbc.equals("正常") &&
                neut.equals("正常") &&
                rbc.equals("正常") &&
                pro.equals("正常") &&
                wbcc.equals("正常")
                ) {
            tips = "12.女孩要发育或已经发育";
            variables.put("checkResult", 12);
        } else if (tp.equals("正常") &&
                alb.equals("正常") &&
                glb.equals("正常") &&
                ag.equals("正常") &&
                alt.equals("正常") &&
                ast.equals("正常") &&
                op.equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp.equals("正常") &&
                y.equals("正常") &&
                z.equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua.equals("正常") &&
                cys.equals("正常") &&
                tsh.equals("正常") &&
                t3.equals("正常") &&
                t4.equals("正常") &&
                ft3.equals("正常") &&
                ft4.equals("正常") &&
                e2.equals("正常") &&
                p.equals("正常") &&
                t.equals("升高") &&
                prl.equals("正常") &&
                lh.equals("正常") &&
                fsh.equals("正常") &&
                ca.equals("正常") &&
                mg.equals("正常") &&
                pa.equals("正常") &&
                k.equals("正常") &&
                na.equals("正常") &&
                cl.equals("正常") &&
                co2.equals("正常") &&
                bglu.equals("正常") &&
                bc.equals("正常") &&
                igf_1.equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb.equals("正常") &&
                wbc.equals("正常") &&
                neut.equals("正常") &&
                rbc.equals("正常") &&
                pro.equals("正常") &&
                wbcc.equals("正常")
                ) {
            tips = "13.男孩要发育或已经发育";
            variables.put("checkResult", 13);
        } else if (tp.equals("正常") &&
                alb.equals("正常") &&
                glb.equals("正常") &&
                ag.equals("正常") &&
                alt.equals("正常") &&
                ast.equals("正常") &&
                op.equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp.equals("正常") &&
                y.equals("正常") &&
                z.equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua.equals("正常") &&
                cys.equals("正常") &&
                tsh.equals("正常") &&
                t3.equals("正常") &&
                t4.equals("正常") &&
                ft3.equals("正常") &&
                ft4.equals("正常") &&
                e2.equals("正常") &&
                p.equals("正常") &&
                t.equals("正常") &&
                prl.equals("正常") &&
                lh.equals("升高") &&
                fsh.equals("正常") &&
                ca.equals("正常") &&
                mg.equals("正常") &&
                pa.equals("正常") &&
                k.equals("正常") &&
                na.equals("正常") &&
                cl.equals("正常") &&
                co2.equals("正常") &&
                bglu.equals("正常") &&
                bc.equals("正常") &&
                igf_1.equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb.equals("正常") &&
                wbc.equals("正常") &&
                neut.equals("正常") &&
                rbc.equals("正常") &&
                pro.equals("正常") &&
                wbcc.equals("正常")
                ) {
            tips = "14.性腺轴启动";
            variables.put("checkResult", 14);
        } else if (tp.equals("正常") &&
                alb.equals("正常") &&
                glb.equals("正常") &&
                ag.equals("正常") &&
                alt.equals("正常") &&
                ast.equals("正常") &&
                op.equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp.equals("正常") &&
                y.equals("正常") &&
                z.equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua.equals("正常") &&
                cys.equals("正常") &&
                tsh.equals("正常") &&
                t3.equals("正常") &&
                t4.equals("正常") &&
                ft3.equals("正常") &&
                ft4.equals("正常") &&
                e2.equals("正常") &&
                p.equals("正常") &&
                t.equals("正常") &&
                prl.equals("正常") &&
                lh.equals("降低") &&
                fsh.equals("正常") &&
                ca.equals("正常") &&
                mg.equals("正常") &&
                pa.equals("正常") &&
                k.equals("正常") &&
                na.equals("正常") &&
                cl.equals("正常") &&
                co2.equals("正常") &&
                bglu.equals("正常") &&
                bc.equals("正常") &&
                igf_1.equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb.equals("正常") &&
                wbc.equals("正常") &&
                neut.equals("正常") &&
                rbc.equals("正常") &&
                pro.equals("正常") &&
                wbcc.equals("正常")
                ) {
            tips = "15.青春期后不升高，提示垂体功能减退";
            variables.put("checkResult", 15);
        } else if (tp.equals("正常") &&
                alb.equals("正常") &&
                glb.equals("正常") &&
                ag.equals("正常") &&
                alt.equals("正常") &&
                ast.equals("正常") &&
                op.equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp.equals("正常") &&
                y.equals("正常") &&
                z.equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua.equals("正常") &&
                cys.equals("正常") &&
                tsh.equals("正常") &&
                t3.equals("正常") &&
                t4.equals("正常") &&
                ft3.equals("正常") &&
                ft4.equals("正常") &&
                e2.equals("正常") &&
                p.equals("正常") &&
                t.equals("正常") &&
                prl.equals("正常") &&
                lh.equals("正常") &&
                fsh.equals("正常") &&
                ca.equals("正常") &&
                mg.equals("正常") &&
                pa.equals("正常") &&
                k.equals("正常") &&
                na.equals("升高") &&
                cl.equals("正常") &&
                co2.equals("正常") &&
                bglu.equals("正常") &&
                bc.equals("正常") &&
                igf_1.equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb.equals("正常") &&
                wbc.equals("正常") &&
                neut.equals("正常") &&
                rbc.equals("正常") &&
                pro.equals("正常") &&
                wbcc.equals("正常")
                ) {
            tips = "16.垂体疾病";
            variables.put("checkResult", 16);
        } else if (tp.equals("正常") &&
                alb.equals("正常") &&
                glb.equals("正常") &&
                ag.equals("正常") &&
                alt.equals("正常") &&
                ast.equals("正常") &&
                op.equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp.equals("正常") &&
                y.equals("正常") &&
                z.equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua.equals("正常") &&
                cys.equals("正常") &&
                tsh.equals("正常") &&
                t3.equals("正常") &&
                t4.equals("正常") &&
                ft3.equals("正常") &&
                ft4.equals("正常") &&
                e2.equals("正常") &&
                p.equals("正常") &&
                t.equals("正常") &&
                prl.equals("正常") &&
                lh.equals("正常") &&
                fsh.equals("正常") &&
                ca.equals("正常") &&
                mg.equals("正常") &&
                pa.equals("正常") &&
                k.equals("正常") &&
                na.equals("降低") &&
                cl.equals("正常") &&
                co2.equals("正常") &&
                bglu.equals("正常") &&
                bc.equals("正常") &&
                igf_1.equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb.equals("正常") &&
                wbc.equals("正常") &&
                neut.equals("正常") &&
                rbc.equals("正常") &&
                pro.equals("正常") &&
                wbcc.equals("正常")
                ) {
            tips = "17.垂体疾病";
            variables.put("checkResult", 17);
        } else if (tp.equals("正常") &&
                alb.equals("正常") &&
                glb.equals("正常") &&
                ag.equals("正常") &&
                alt.equals("正常") &&
                ast.equals("正常") &&
                op.equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp.equals("正常") &&
                y.equals("正常") &&
                z.equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua.equals("正常") &&
                cys.equals("正常") &&
                tsh.equals("正常") &&
                t3.equals("正常") &&
                t4.equals("正常") &&
                ft3.equals("正常") &&
                ft4.equals("正常") &&
                e2.equals("正常") &&
                p.equals("正常") &&
                t.equals("正常") &&
                prl.equals("正常") &&
                lh.equals("正常") &&
                fsh.equals("正常") &&
                ca.equals("正常") &&
                mg.equals("正常") &&
                pa.equals("正常") &&
                k.equals("正常") &&
                na.equals("正常") &&
                cl.equals("正常") &&
                co2.equals("降低") &&
                bglu.equals("正常") &&
                bc.equals("正常") &&
                igf_1.equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb.equals("正常") &&
                wbc.equals("正常") &&
                neut.equals("正常") &&
                rbc.equals("正常") &&
                pro.equals("正常") &&
                wbcc.equals("正常")
                ) {
            tips = "18.酸中毒";
            variables.put("checkResult", 18);
        } else if (tp.equals("正常") &&
                alb.equals("正常") &&
                glb.equals("正常") &&
                ag.equals("正常") &&
                alt.equals("正常") &&
                ast.equals("正常") &&
                op.equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp.equals("正常") &&
                y.equals("正常") &&
                z.equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua.equals("正常") &&
                cys.equals("正常") &&
                tsh.equals("正常") &&
                t3.equals("正常") &&
                t4.equals("正常") &&
                ft3.equals("正常") &&
                ft4.equals("正常") &&
                e2.equals("正常") &&
                p.equals("正常") &&
                t.equals("正常") &&
                prl.equals("正常") &&
                lh.equals("正常") &&
                fsh.equals("正常") &&
                ca.equals("正常") &&
                mg.equals("正常") &&
                pa.equals("正常") &&
                k.equals("正常") &&
                na.equals("正常") &&
                cl.equals("正常") &&
                co2.equals("正常") &&
                bglu.equals("升高") &&
                bc.equals("正常") &&
                igf_1.equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb.equals("正常") &&
                wbc.equals("正常") &&
                neut.equals("正常") &&
                rbc.equals("正常") &&
                pro.equals("正常") &&
                wbcc.equals("正常")
                ) {
            tips = "19.糖尿病或糖尿病前期";
            variables.put("checkResult", 19);
        } else if (tp.equals("正常") &&
                alb.equals("正常") &&
                glb.equals("正常") &&
                ag.equals("正常") &&
                alt.equals("正常") &&
                ast.equals("正常") &&
                op.equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp.equals("正常") &&
                y.equals("正常") &&
                z.equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua.equals("正常") &&
                cys.equals("正常") &&
                tsh.equals("正常") &&
                t3.equals("正常") &&
                t4.equals("正常") &&
                ft3.equals("正常") &&
                ft4.equals("正常") &&
                e2.equals("正常") &&
                p.equals("正常") &&
                t.equals("正常") &&
                prl.equals("正常") &&
                lh.equals("正常") &&
                fsh.equals("正常") &&
                ca.equals("正常") &&
                mg.equals("正常") &&
                pa.equals("正常") &&
                k.equals("正常") &&
                na.equals("正常") &&
                cl.equals("正常") &&
                co2.equals("正常") &&
                bglu.equals("正常") &&
                bc.equals("正常") &&
                igf_1.equals("正常") &&
                igfbp_3.equals("正常") &&
                (brbc.equals("升高") || brbc.equals("降低")) &&
                hgb.equals("正常") &&
                wbc.equals("正常") &&
                neut.equals("正常") &&
                rbc.equals("正常") &&
                pro.equals("正常") &&
                wbcc.equals("正常")
                ) {
            tips = "20.贫血";
            variables.put("checkResult", 20);
        } else if (tp.equals("正常") &&
                alb.equals("正常") &&
                glb.equals("正常") &&
                ag.equals("正常") &&
                alt.equals("正常") &&
                ast.equals("正常") &&
                op.equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp.equals("正常") &&
                y.equals("正常") &&
                z.equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua.equals("正常") &&
                cys.equals("正常") &&
                tsh.equals("正常") &&
                t3.equals("正常") &&
                t4.equals("正常") &&
                ft3.equals("正常") &&
                ft4.equals("正常") &&
                e2.equals("正常") &&
                p.equals("正常") &&
                t.equals("正常") &&
                prl.equals("正常") &&
                lh.equals("正常") &&
                fsh.equals("正常") &&
                ca.equals("正常") &&
                mg.equals("正常") &&
                pa.equals("正常") &&
                k.equals("正常") &&
                na.equals("正常") &&
                cl.equals("正常") &&
                co2.equals("正常") &&
                bglu.equals("正常") &&
                bc.equals("正常") &&
                igf_1.equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                (hgb.equals("升高") || hgb.equals("降低")) &&
                wbc.equals("正常") &&
                neut.equals("正常") &&
                rbc.equals("正常") &&
                pro.equals("正常") &&
                wbcc.equals("正常")
                ) {
            tips = "21.贫血";
            variables.put("checkResult", 21);
        } else if (tp.equals("正常") &&
                alb.equals("正常") &&
                glb.equals("正常") &&
                ag.equals("正常") &&
                alt.equals("正常") &&
                ast.equals("正常") &&
                op.equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp.equals("正常") &&
                y.equals("正常") &&
                z.equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua.equals("正常") &&
                cys.equals("正常") &&
                tsh.equals("正常") &&
                t3.equals("正常") &&
                t4.equals("正常") &&
                ft3.equals("正常") &&
                ft4.equals("正常") &&
                e2.equals("正常") &&
                p.equals("正常") &&
                t.equals("正常") &&
                prl.equals("正常") &&
                lh.equals("正常") &&
                fsh.equals("正常") &&
                ca.equals("正常") &&
                mg.equals("正常") &&
                pa.equals("正常") &&
                k.equals("正常") &&
                na.equals("正常") &&
                cl.equals("正常") &&
                co2.equals("正常") &&
                bglu.equals("正常") &&
                bc.equals("正常") &&
                igf_1.equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb.equals("正常") &&
                (wbc.equals("升高") || wbc.equals("降低")) &&
                neut.equals("正常") &&
                rbc.equals("正常") &&
                pro.equals("正常") &&
                wbcc.equals("正常")
                ) {
            tips = "22.有感染";
            variables.put("checkResult", 22);
        } else if (tp.equals("正常") &&
                alb.equals("正常") &&
                glb.equals("正常") &&
                ag.equals("正常") &&
                alt.equals("正常") &&
                ast.equals("正常") &&
                op.equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp.equals("正常") &&
                y.equals("正常") &&
                z.equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua.equals("正常") &&
                cys.equals("正常") &&
                tsh.equals("正常") &&
                t3.equals("正常") &&
                t4.equals("正常") &&
                ft3.equals("正常") &&
                ft4.equals("正常") &&
                e2.equals("正常") &&
                p.equals("正常") &&
                t.equals("正常") &&
                prl.equals("正常") &&
                lh.equals("正常") &&
                fsh.equals("正常") &&
                ca.equals("正常") &&
                mg.equals("正常") &&
                pa.equals("正常") &&
                k.equals("正常") &&
                na.equals("正常") &&
                cl.equals("正常") &&
                co2.equals("正常") &&
                bglu.equals("正常") &&
                bc.equals("正常") &&
                igf_1.equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb.equals("正常") &&
                wbc.equals("正常") &&
                (neut.equals("升高") || neut.equals("降低")) &&
                rbc.equals("正常") &&
                pro.equals("正常") &&
                wbcc.equals("正常")
                ) {
            tips = "23.有感染";
            variables.put("checkResult", 23);
        } else if (tp.equals("正常") &&
                alb.equals("正常") &&
                glb.equals("正常") &&
                ag.equals("正常") &&
                alt.equals("正常") &&
                ast.equals("正常") &&
                op.equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp.equals("正常") &&
                y.equals("正常") &&
                z.equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua.equals("正常") &&
                cys.equals("正常") &&
                tsh.equals("正常") &&
                t3.equals("正常") &&
                t4.equals("正常") &&
                ft3.equals("正常") &&
                ft4.equals("正常") &&
                e2.equals("正常") &&
                p.equals("正常") &&
                t.equals("正常") &&
                prl.equals("正常") &&
                lh.equals("正常") &&
                fsh.equals("正常") &&
                ca.equals("正常") &&
                mg.equals("正常") &&
                pa.equals("正常") &&
                k.equals("正常") &&
                na.equals("正常") &&
                cl.equals("正常") &&
                co2.equals("正常") &&
                bglu.equals("正常") &&
                bc.equals("正常") &&
                igf_1.equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb.equals("正常") &&
                wbc.equals("正常") &&
                neut.equals("正常") &&
                (rbc.equals("升高") || rbc.equals("降低")) &&
                pro.equals("正常") &&
                wbcc.equals("正常")
                ) {
            tips = "24.有肾病";
            variables.put("checkResult", 24);
        } else if (tp.equals("正常") &&
                alb.equals("正常") &&
                glb.equals("正常") &&
                ag.equals("正常") &&
                alt.equals("正常") &&
                ast.equals("正常") &&
                op.equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp.equals("正常") &&
                y.equals("正常") &&
                z.equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua.equals("正常") &&
                cys.equals("正常") &&
                tsh.equals("正常") &&
                t3.equals("正常") &&
                t4.equals("正常") &&
                ft3.equals("正常") &&
                ft4.equals("正常") &&
                e2.equals("正常") &&
                p.equals("正常") &&
                t.equals("正常") &&
                prl.equals("正常") &&
                lh.equals("正常") &&
                fsh.equals("正常") &&
                ca.equals("正常") &&
                mg.equals("正常") &&
                pa.equals("正常") &&
                k.equals("正常") &&
                na.equals("正常") &&
                cl.equals("正常") &&
                co2.equals("正常") &&
                bglu.equals("正常") &&
                bc.equals("正常") &&
                igf_1.equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb.equals("正常") &&
                wbc.equals("正常") &&
                neut.equals("正常") &&
                rbc.equals("正常") &&
                (pro.equals("升高") || pro.equals("降低")) &&
                wbcc.equals("正常")
                ) {
            tips = "25.有肾病";
            variables.put("checkResult", 25);
        } else if (tp.equals("正常") &&
                alb.equals("正常") &&
                glb.equals("正常") &&
                ag.equals("正常") &&
                alt.equals("正常") &&
                ast.equals("正常") &&
                op.equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp.equals("正常") &&
                y.equals("正常") &&
                z.equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua.equals("正常") &&
                cys.equals("正常") &&
                tsh.equals("正常") &&
                t3.equals("正常") &&
                t4.equals("正常") &&
                ft3.equals("正常") &&
                ft4.equals("正常") &&
                e2.equals("正常") &&
                p.equals("正常") &&
                t.equals("正常") &&
                prl.equals("正常") &&
                lh.equals("正常") &&
                fsh.equals("正常") &&
                ca.equals("正常") &&
                mg.equals("正常") &&
                pa.equals("正常") &&
                k.equals("正常") &&
                na.equals("正常") &&
                cl.equals("正常") &&
                co2.equals("正常") &&
                bglu.equals("正常") &&
                bc.equals("正常") &&
                igf_1.equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb.equals("正常") &&
                wbc.equals("正常") &&
                neut.equals("正常") &&
                rbc.equals("正常") &&
                pro.equals("正常") &&
                (wbcc.equals("升高") || wbcc.equals("降低"))
                ) {
            tips = "26.尿路感染";
            variables.put("checkResult", 26);
        } else if (tp.equals("正常") &&
                alb.equals("正常") &&
                glb.equals("正常") &&
                ag.equals("正常") &&
                alt.equals("正常") &&
                ast.equals("正常") &&
                op.equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp.equals("正常") &&
                y.equals("正常") &&
                z.equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua.equals("正常") &&
                cys.equals("正常") &&
                tsh.equals("正常") &&
                t3.equals("正常") &&
                t4.equals("正常") &&
                ft3.equals("正常") &&
                ft4.equals("正常") &&
                e2.equals("正常") &&
                p.equals("正常") &&
                t.equals("正常") &&
                prl.equals("正常") &&
                lh.equals("正常") &&
                fsh.equals("正常") &&
                ca.equals("正常") &&
                mg.equals("正常") &&
                pa.equals("正常") &&
                k.equals("正常") &&
                na.equals("正常") &&
                cl.equals("正常") &&
                co2.equals("正常") &&
                bglu.equals("正常") &&
                bc.equals("正常") &&
                igf_1.equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb.equals("正常") &&
                wbc.equals("正常") &&
                neut.equals("正常") &&
                rbc.equals("正常") &&
                pro.equals("正常") &&
                wbcc.equals("正常")
                ) {
            tips = "30.所有指标全部正常";
            variables.put("checkResult", 30);
        } else if (tp.equals("正常") &&
                alb.equals("正常") &&
                glb.equals("正常") &&
                ag.equals("正常") &&
                alt.equals("正常") &&
                ast.equals("正常") &&
                op.equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp.equals("正常") &&
                y.equals("正常") &&
                z.equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua.equals("正常") &&
                cys.equals("正常") &&
                tsh.equals("正常") &&
                t3.equals("正常") &&
                t4.equals("正常") &&
                ft3.equals("正常") &&
                ft4.equals("正常") &&
                e2.equals("正常") &&
                p.equals("正常") &&
                t.equals("正常") &&
                prl.equals("正常") &&
                lh.equals("正常") &&
                fsh.equals("正常") &&
                ca.equals("正常") &&
                mg.equals("正常") &&
                pa.equals("正常") &&
                k.equals("正常") &&
                na.equals("正常") &&
                cl.equals("正常") &&
                co2.equals("正常") &&
                bglu.equals("正常") &&
                bc.equals("正常") &&
                igf_1.equals("降低") &&
                igfbp_3.equals("降低") &&
                brbc.equals("正常") &&
                hgb.equals("正常") &&
                wbc.equals("正常") &&
                neut.equals("正常") &&
                rbc.equals("正常") &&
                pro.equals("正常") &&
                wbcc.equals("正常")
                ) {
            tips = "31.IGF-1、IGFBP3降低";
            variables.put("checkResult", 31);
        } else if (tp.equals("正常") &&
                alb.equals("正常") &&
                glb.equals("正常") &&
                ag.equals("正常") &&
                alt.equals("正常") &&
                ast.equals("正常") &&
                op.equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp.equals("正常") &&
                y.equals("正常") &&
                z.equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua.equals("正常") &&
                cys.equals("正常") &&
                tsh.equals("正常") &&
                t3.equals("正常") &&
                t4.equals("正常") &&
                ft3.equals("正常") &&
                ft4.equals("正常") &&
                e2.equals("正常") &&
                p.equals("正常") &&
                t.equals("正常") &&
                prl.equals("正常") &&
                lh.equals("降低") &&
                fsh.equals("降低") &&
                ca.equals("正常") &&
                mg.equals("正常") &&
                pa.equals("正常") &&
                k.equals("正常") &&
                na.equals("正常") &&
                cl.equals("正常") &&
                co2.equals("正常") &&
                bglu.equals("正常") &&
                bc.equals("正常") &&
                igf_1.equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb.equals("正常") &&
                wbc.equals("正常") &&
                neut.equals("正常") &&
                rbc.equals("正常") &&
                pro.equals("正常") &&
                wbcc.equals("正常")
                ) {
            tips = "32.LH、FSH降低";
            variables.put("checkResult", 32);
        } else if (tp.equals("正常") &&
                alb.equals("正常") &&
                glb.equals("正常") &&
                ag.equals("正常") &&
                alt.equals("正常") &&
                ast.equals("正常") &&
                op.equals("正常") &&
                tbil.equals("正常") &&
                ibil.equals("正常") &&
                alp.equals("正常") &&
                y.equals("正常") &&
                z.equals("正常") &&
                crea.equals("正常") &&
                urea.equals("正常") &&
                ua.equals("正常") &&
                cys.equals("正常") &&
                tsh.equals("正常") &&
                t3.equals("正常") &&
                t4.equals("正常") &&
                ft3.equals("正常") &&
                ft4.equals("正常") &&
                (e2.equals("升高") || e2.equals("降低")) &&
                p.equals("正常") &&
                (t.equals("升高") || t.equals("降低")) &&
                prl.equals("正常") &&
                (lh.equals("升高") || lh.equals("降低")) &&
                (fsh.equals("升高") || fsh.equals("降低")) &&
                ca.equals("正常") &&
                mg.equals("正常") &&
                pa.equals("正常") &&
                k.equals("正常") &&
                na.equals("正常") &&
                cl.equals("正常") &&
                co2.equals("正常") &&
                bglu.equals("正常") &&
                bc.equals("正常") &&
                igf_1.equals("正常") &&
                igfbp_3.equals("正常") &&
                brbc.equals("正常") &&
                hgb.equals("正常") &&
                wbc.equals("正常") &&
                neut.equals("正常") &&
                rbc.equals("正常") &&
                pro.equals("正常") &&
                wbcc.equals("正常")
                ) {
            tips = "33.激素水平异常";
            variables.put("checkResult", 33);
        }
        String check1Case = tips.substring(0, tips.indexOf("."));
        variables.put("check1Case", check1Case);
        variables.put("tips", "辅助检查1.0 --> " + tips);
        taskService.complete(taskId, variables);
        return variables;
    }

    private String getStr(String tp) {
        tp = (tp == null ? "正常" : tp);
        return tp;
    }

    @GetMapping("assess15")
    public Map assessCheck2(String taskId,
                            String check1Case,
                            String chromosomeExam,
                            String ghTest,
                            Integer boneAgePercentile,
                            String familyShortHistory) {
        Map<String, Object> variables = new HashMap<>();
        String treatmentPlan = "";
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            variables.put("msg", "流程不存在");
            return variables;
        }

        boolean isFamilyShortHistory = familyShortHistory.equals("是") ? true : false;//家族矮小史
        variables.put("illnessResult", 0);
        String illnessDiagnosis = "其他疾病，建议持续跟踪";
        if (check1Case.equals("30") && chromosomeExam.equals("正常") && ghTest.equals("正常") && boneAgePercentile < 3) {
            illnessDiagnosis = "1. 特发性矮小ISS";
            treatmentPlan = getTreatmentPlan(1);
            variables.put("illnessResult", 1);
        } else if (check1Case.equals("30") && chromosomeExam.equals("正常") && ghTest.equals("正常") && boneAgePercentile < 3 && isFamilyShortHistory) {
            illnessDiagnosis = "2. 特发性矮小ISS";
            treatmentPlan = getTreatmentPlan(2);
            variables.put("illnessResult", 2);
        } else if (check1Case.equals("30") && chromosomeExam.equals("异常") && ghTest.equals("正常")) {
            illnessDiagnosis = "3. 特纳综合症或21染色体";
            treatmentPlan = getTreatmentPlan(3);
            variables.put("illnessResult", 3);
        } else if (check1Case.equals("31") && chromosomeExam.equals("正常") && ghTest.equals("异常")) {
            illnessDiagnosis = "4. 生长激素缺乏症GHD";
            treatmentPlan = getTreatmentPlan(4);
            variables.put("illnessResult", 4);
        } else if (check1Case.equals("31") && chromosomeExam.equals("正常") && ghTest.equals("正常")) {
            illnessDiagnosis = "5. 小于胎龄儿SGA";
            treatmentPlan = getTreatmentPlan(5);
            variables.put("illnessResult", 5);
        } else if (check1Case.equals("32") && chromosomeExam.equals("正常") && ghTest.equals("正常")) {
            illnessDiagnosis = "6. 体质性青春发育延迟CDGP";
            treatmentPlan = getTreatmentPlan(6);
            variables.put("illnessResult", 6);
        } else if (check1Case.equals("33") && chromosomeExam.equals("正常") && ghTest.equals("正常")) {
            illnessDiagnosis = "7. 中枢性性早熟CPP";
            treatmentPlan = getTreatmentPlan(7);
            variables.put("illnessResult", 7);
        }
        variables.put("illnessDiagnosis", "疾病诊断 --> " + illnessDiagnosis);
        variables.put("treatmentPlan", treatmentPlan);
        taskService.complete(taskId, variables);
        return variables;
    }

    private String getTreatmentPlan(int id) {
        String treatmentPlan = "";
        switch (id) {
            case 1:
                treatmentPlan = "(1)一般治疗<br/>" +
                        "GH分泌为脉冲式分泌，呈昼夜节律性，一般在深睡眠时出现分泌高峰，因此，ISS患儿应保证充足睡眠。平时加强运动锻炼尤其是伸展性运动、跳跃性运动，增加户外阳光照射，注意均衡饮食，避免偏食、挑食，保证营养摄入。 <br/>" +
                        "(2)重组人生长激素（rhGH）<br/>" +
                        "2003年美国FDA批准了rhGH用于治疗ISS的儿童。国内推荐用rhGH治疗的ISS患儿应满足以下条件：1.身高落后于同年龄、同性别、同种族正常儿童身高均值的-2SD；2.出生时身长、体重处于同胎龄儿的正常范围；3.排除了系统性疾病、其他内分泌疾病、营养性疾病、染色体异常、基因异常、骨骼发育不良、心理情感障碍等其他导致的身材矮小原因；4.生长激素激发试验峰值≥10ng/ml；5.起始治疗的年龄为5岁。美国FDA批准用于治疗ISS的rhGH剂量为0.30～0.37 mg/kg/w (相当于0.14-0.18IU/kg/d)。2008年中国《矮身材儿童诊治指南》推荐剂量为0.15~0.20U/（Kg.d）。2013年中国《基因重组人生长激素儿科临床规范应用的建议》，推荐剂量为0.125~0.2U/（Kg.d），每日睡前30 min皮下注射。但rhGH治疗应采用个体化，宜从小剂量开始，最大量不宜超过0.2U/（Kg.d）。一般建议开始治疗的最佳年龄为5岁至青春早期，治疗的疗程一般不少于1年。<br/>" +
                        "(3)芳香化酶抑制剂（aromatase inhibitors，AI）<br/>" +
                        "AI主要通过抑制芳香化酶将雄烯二酮、睾酮转化为雌酮、雌二醇从而延缓骨骺最终融合，达到促线性生长的目的。理论上AI单用或与生长激素联用均可治疗青春期男童ISS，但缺乏大样本、长期的临床研究，AI治疗青春期ISS男性患儿的有效性和安全性还有待证实。<br/>" +
                        "(4)促性腺激素释放激素类似物（Gonadotropin releasing hormone analogue，GnRHa）<br/>" +
                        "GnRHa可抑制第二性征的发育和延缓骨龄进展，从而维持身高生长潜力，达到改善终身高的目的，目前主要用于青春发育期的ISS患儿。无论男性还是女性患儿，单用GnRHa对成年期身高的改善作用有限且差异很大，并与用药时间呈正相关。因此，通常不推荐单独应用GnRHa用于治疗青春期ISS的患儿。有研究发现生长激素和GnRHa的联合治疗可改善ISS患儿的终身高，但费用较高，且缺乏强有力的临床研究。<br/>" +
                        "(5)性激素<br/>" +
                        "CDPG患者男性年龄达14~15岁和女性年龄达12~13岁时，仍无明显第二性征出现，或由于性发育延迟造成精神负担者，可用小剂量性激素诱导发育，多数治疗2~6月后可引起第二性征发育和轻度身高增长，不会增加骨龄进展。男性患者可选择睾酮，氧甲氢龙也已被批准用于男性青春发育延迟。女性患者可选择炔雌醇2~5ug/d，并注意观察患者第二性征发育情况，定期监测骨龄。 ";
                break;
            case 2:
                treatmentPlan = "家族性身材矮小患者大多数不需要治疗，但身高在第3百分位以下或患者、家长对身材矮小有较大精神负担和心理压力时，可应用rhGH治疗。";
                break;
            case 3:
                treatmentPlan = "特纳综合症或21染色体 --> 建议会诊，转诊";
                break;
            case 4:
                treatmentPlan = "(1)一般治疗<br/>" +
                        "GH分泌为脉冲式分泌，呈昼夜节律性，一般在夜间深睡眠时出现分泌高峰，因此，GHD患儿应保证充足睡眠。平时加强运动锻炼尤其是伸展性运动、跳跃性运动，增加户外阳光照射，注意均衡饮食，避免偏食、挑食，保证营养摄入。<br/>" +
                        "(2)基因重组人生长激素(recombinant human growth hormone，rhGH)<br/>" +
                        "1985年美国FDA批准rhGH可用于本症的治疗。<br/>" +
                        "1)剂型<br/>" +
                        "国内可供选择的有rhGH粉剂和水剂两种。<br/>" +
                        "2)剂量<br/>" +
                        "儿童期0.075-0.15u/（Kg.d），青春期0.075-0.2u/（Kg.d），每日1次, 睡前皮下注射。<br/>" +
                        "3)用法<br/>" +
                        "常用的注射部位为脐周围或大腿中部1/2的外、前两侧，每次注射更换注射点，1个月内不要在同一部位注射2次，两针间距1.0cm左右，以防短期重复注射导致皮下组织变性，影响疗效。<br/>" +
                        "4)治疗监测<br/>" +
                        "rhGH治疗过程中应定期监治疗的有效性和安全性，主要监测内容为：生长发育指标、实验室检查指标、不良反应等，具体见表10-3。<br/>" +
                        "5)疗程<br/>" +
                        "治疗疗程视需要而定，总疗程通常不宜短于1-2年，时间太短，患儿获益对其终身高的作用不大。<br/>" +
                        "(3)其他药物<br/>" +
                        "1)注意补充钙、微量元素等。<br/>" +
                        "2)如同时伴有甲状腺功能和肾上腺皮质功能减退者，则应加用左甲状腺素钠片和小剂量氢化可的松0.5-1.0mg/kg.d。";
                break;
            case 5:
                treatmentPlan = "SGA患儿的治疗目标是加速儿童早期的线性生长过程完成追赶性生长，在儿童晚期维持正常生长速度，最终目标是成年期身高达到正常水平。<br/>" +
                        "(1)rhGH治疗 <br/>" +
                        "1)适应证<br/>" +
                        "美国FDA于2001年批准rhGH可用于SGA患儿的治疗，但并非所有出生时诊断为小于胎龄儿的患儿均需要rhGH治疗。2003年国际小于胎龄儿发展建议会议共识声明指出SGA患儿使用rhGH治疗的适应证为：（1）出生时诊断为SGA并且持续矮小（身高低于-2SD）；（2）开始治疗的最早年龄在2-3岁；（3）生长速度等于或者低于同年龄的儿童；（4）排除可能引起矮小的其他原因，如使用生长抑制药物、慢性疾病、内分泌异常和情感剥夺或者其他综合征。<br/>" +
                        "2)起始治疗时间<br/>" +
                        "关于SGA起始治疗的时间，国内外专家未取得一致意见。由于大部分SGA患儿在出生后2-3年内都会呈现追赶生长，身高可以达到与其靶身高相称的生长曲线范畴，故对SGA患儿都应定期随访观察。美国FDA推荐SGA患儿2岁时未实现追赶生长，身高低于同年龄、同性别儿童正常均值-2SD，即可开始rhGH治疗。欧洲专利药品委员会（EMEA）推荐4岁以上身高＜-2.5SD，生长速度低于同年龄均值，身高SDS低于遗传靶身高SDS的1SD可用rhGH治疗。2008年中国《矮身材儿童诊治指南》推荐，一般在3岁时，如其生长仍滞后，应考虑rhGH治疗。2013年中国《基因重组人生长激素儿科临床规范应用的建议》推荐：小于胎龄儿rhGH治疗指征为≥4岁身高仍低于同年龄、同性别正常儿童平均身高-2SD。<br/>" +
                        "3)治疗剂量<br/>" +
                        "治疗早期生长速度与rhGH初始剂量成正比，剂量越大，生长速度越快。FDA推荐的最佳剂量是0.48mg/(Kg.周)，相当于0.2 IU/(Kg.d)，治疗时间为2~6年。若已达到追赶生长或青春发育期，剂量可调整至0.24~0.48mg/(Kg.周)，相当于0.1~0.2 IU/(Kg.d)。2008年中国《矮身材儿童诊治指南》对rhGH的推荐剂量为0.15~0.2 IU/(Kg.d)。临床也应依据治疗的反应和IGF-1水平调整用量，尽可能维持IGF-1在对应年龄范围的1SD~2SD之间，这样既能保证疗效，又能避免潜在风险。<br/>" +
                        "4)治疗监测<br/>" +
                        "因长期大量使用rhGH会使血IGF-1和IGFBP-3浓度、胰岛素水平明显增加，胰岛素敏感性下降，因此rhGH治疗后发生糖代谢紊乱、高血压和高脂血症的风险增加。建议开始治疗前应监测血压，检查IGF-1、IGFBP-3、血脂、胰岛素、空腹血糖等，治疗期间应随时观察生长发育状况，定期复查上述指标。<br/>" +
                        "5)疗程<br/>" +
                        "应采用长疗程治疗，直至达到终身高。欧洲EMEA推荐的治疗终身高标准为：青少年期身高生长速度<2cm/年，且女孩骨龄>14岁，男孩骨龄>16岁，应终止治疗。<br/>" +
                        "6)安全性<br/>" +
                        "在有适应证的前提下按照推荐剂量应用rhGH治疗SGA是安全有效的，不会加快骨成熟，也不会改变正常的身体比例，对青春期开始时间及进程无明显影响。但长期使用会增加成年期糖代谢异常和高血压风险，还会导致胰岛素抵抗和甲状腺功能减低。对胰岛素抵抗的影响，在停用rhGH后，胰岛素水平会降至正常水平。目前尚未发现rhGH治疗会增加原发肿瘤的发生率。<br/>" +
                        "(2)芳香化酶抑制剂（AI）<br/>" +
                        "通过抑制芳香化酶将雄烯二酮、睾酮转化为雌酮、雌二醇，从而延缓骨骺融合，延长生长时间，促进身高生长。GH与芳香化酶抑制剂联合应用2年以上可增加成年期预测终身高，但其有效性和安全性还有待进一步证实。<br/>" +
                        "(3)促性腺激素释放激素类似物<br/>" +
                        "GnRHa通过抑制垂体促性腺激素（LH、FSH）的分泌，使性激素水平降低至青春期前水平，从而延缓骨龄进展，减慢骨骺融合，改善成年终身高，但治疗效果欠理想。";
                break;
            case 6:
                treatmentPlan = "家族性身材矮小患者大多数不需要治疗，但身高在第3百分位以下或患者、家长对身材矮小有较大精神负担和心理压力时，可应用rhGH治疗。";
                break;
            case 7:
                treatmentPlan = "治疗目标为抑制过早或过快的性发育，防止或缓释患儿或家长因性早熟所致的相关的社会或心理问题（如早初潮）；改善因骨龄提前而减损的成年身高也是重要的目标。但并非所有的ICPP都需要治疗。GnRH类似物（GnRHa）是当前主要的治疗选择，目前常用制剂有曲普瑞林和亮丙瑞林的缓释剂。<br/>" +
                        "(1)以改善成年身高为目的的应用指征<br/>" +
                        "1)骨龄大于年龄2岁或以上，但需女孩骨龄≤11.5岁，男孩骨龄≤12.5岁者。<br/>" +
                        "2)预测成年身高：女孩＜150cm，男孩＜160cm。<br/>" +
                        "3)或以骨龄判断的身高SDS＜-2SD（按正常人群参照值或遗传靶身高判断）。<br/>" +
                        "4)发育进程迅速，骨龄增长/年龄增长＞1。<br/>" +
                        "(2)不需治疗的指征<br/>" +
                        "1)性成熟进程缓慢（骨龄进展不超越年龄进展）而对成年身高影响不显者。<br/>" +
                        "2)骨龄虽提前，但身高生长速度亦快，预测成年身高不受损者，需进行定期复查和评估，调整治疗方案。<br/>" +
                        "(3)GnRHa剂量<br/>" +
                        "首剂80-100μg/kg，最大量3.75mg；其后每4周注射1次，体重≥30kg者，曲普瑞林每4周肌注3-3.75mg。已有初潮者首剂后2周宜强化1次。维持剂量应当个体化，根据性腺轴功能抑制情况而定（包括性征、性激素水平和骨龄进展），男孩剂量可偏大。<br/>" +
                        "(4)治疗监测和停药决定<br/>" +
                        "治疗过程中每3-6个月测量身高以及性征发育状况（阴毛进展不代表性腺受抑状况）；首剂3-6个月末复查GnRH激发试验，LH峰值在青春前期水平提示剂量合适。其后对女孩需定期复查基础血清雌二醇（E2）和子宫、卵巢B超；男孩需复查基础血清睾酮浓度以判断性腺轴功能抑制状况。每半年复查骨龄1次，结合身高增长，预测成年身高改善情况。为改善成年身高的目的疗程至少2年，具体疗程需个体化。<br/>" +
                        "(5)GnRHa治疗中部分患者生长减速明显，不推荐常规联合应用重组人生长激素（rhGH），尤其女孩骨龄＞12岁，男孩骨龄＞14岁者。<br/>" +
                        "(6)有中枢器质性病变的CPP患者应当按照病变性质行相应病因治疗。对非进行性损害的颅内肿瘤或先天异常，如下丘脑错构瘤等，无颅压增高或其他中枢神经系统表现者，不需手术，仍按ICPP药物治疗方案治疗。蛛网膜下腔囊肿亦然。";
                break;
        }
        return treatmentPlan;
    }

//    @GetMapping("assess0")
//    public String assess0(String taskId, String a1, String tp) {
//        tp = (tp == null ? "正常" : tp);
//        System.out.println(tp);
//        String tips = "";
//        if (a1 == null) {
//            tips = "null";
//        } else if (a1.equals("是")) {
//            tips = "true";
//        } else if (a1.equals("否")) {
//            tips = "否";
//        }
//
////        variables.put("a1", a1);
//        return tips;
//    }


    /**
     * 计算身高百分位数
     *
     * @param taskId
     * @param heightPercentile
     * @return
     */
//    @GetMapping("check")
//    public String computePercentile(String taskId, String heightPercentile, String boneAge, String isPatient, String isUnderTreatment, String isFillIn) {
//        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
//        if (task == null) {
//            return "流程不存在";
//        }
//        Map<String, Object> variables = new HashMap<>();
//        variables.put("heightPercentile", heightPercentile);
//        variables.put("boneAge", boneAge);
//        variables.put("isPatient", Boolean.valueOf(isPatient));
//        variables.put("isUnderTreatment", Boolean.valueOf(isUnderTreatment));
//        variables.put("isFillIn", Boolean.valueOf(isFillIn));
//        taskService.complete(taskId, variables);
//        return "您的任务已处理，流程已推进。 患者上一个处理任务为：【 " + task.getName() + " 】";
//
//    }

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
//    @RequestMapping(value = "nform")
//    public void deployTest() {
//        Deployment deployment = repositoryService.createDeployment()
//                .name("表单流程")
//                .addClasspathResource("processes/test-form.bpmn20.xml")
//                .deploy();
//
//        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().
//                deploymentId(deployment.getId())
//                .singleResult();
//        String processDefinitionId = processDefinition.getId();
//        FormDeployment formDeployment = formRepositoryService.createDeployment()
//                .name("definition-one")
//                .addClasspathResource("forms/form1.form")
//                .parentDeploymentId(deployment.getId())
//                .deploy();
//        FormDefinition formDefinition = formRepositoryService.createFormDefinitionQuery().deploymentId(formDeployment.getId()).singleResult();
//        String formDefinitionId = formDefinition.getId();
//
//
//        //启动实例并且设置表单的值
//        String outcome = "laoliu";
//        Map<String, Object> formProperties = new HashMap<>();
//        formProperties.put("reason", "家里有事");
//        formProperties.put("startTime", "2020-8-17");
//        formProperties.put("endTime", "2020-8-17");
//        String processInstanceName = "shareniu";
//        runtimeService.startProcessInstanceWithForm(processDefinitionId, outcome, formProperties, processInstanceName);
//        HistoricProcessInstanceEntity historicProcessInstanceEntity = (HistoricProcessInstanceEntity) historyService.createHistoricProcessInstanceQuery()
//                .processDefinitionId(processDefinitionId)
//                .singleResult();
//        String processInstanceId = historicProcessInstanceEntity.getProcessInstanceId();
//
//
//        //查询表单信息
//        SimpleFormModel fm = (SimpleFormModel) runtimeService.getStartFormModel(processDefinitionId, processInstanceId).getFormModel();
//        //FormInfo fm = runtimeService.getStartFormModel(processDefinitionId, processInstanceId);
//
//        System.out.println(fm.getKey());
//        System.out.println(fm.getName());
//        System.out.println(fm.getOutcomeVariableName());
//        System.err.println(fm.getVersion());
//        List<FormField> fields = fm.getFields();
//        for (FormField ff : fields) {
//            System.out.println("######################");
//            System.out.println(ff.getId());
//            System.out.println(ff.getName());
//            System.out.println(ff.getType());
//            System.out.println(ff.getPlaceholder());
//            System.out.println(ff.getValue());
//            System.out.println("######################");
//
//        }
//
//
//        //查询个人任务并填写表单
//        Map<String, Object> formProperties2 = new HashMap<>();
//        formProperties2.put("reason", "家里有事2222");
//        formProperties2.put("startTime", "2020-8-18");
//        formProperties2.put("endTime", "2020-8-18");
//        formProperties2.put("days", "3");
//        Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
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
//    }
    @GetMapping("assess16")
    public String assessHealthPlan(String taskId,
                                   String milk,
                                   String egg,
                                   String meat,
                                   String Ca,
                                   String Zn,
                                   String vitaminA,
                                   String vitaminD,
                                   String meanSleepTime,
                                   String meanGetupTime,
                                   String exerciseType,
                                   String exerciseDuration,
                                   String schoolRecord
                                   ) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return "流程不存在";
        }

        Map<String, Object> variables = new HashMap<>();

        String tips = "";

        taskService.complete(taskId, variables);
        return "保健方案 --> " + tips;
    }
}
