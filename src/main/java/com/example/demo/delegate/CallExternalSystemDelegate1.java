package com.example.demo.delegate;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

/**
 * @author LDF
 * @date 2020/6/18
 */
public class CallExternalSystemDelegate1 implements JavaDelegate {
    public void execute(DelegateExecution delegateExecution) {
        System.out.println("TODO 完成其他服务任务。。。"
                + delegateExecution.getVariable("employee"));
        delegateExecution.setVariable("result", "过期产");
        System.out.println("过期产");
    }
}
