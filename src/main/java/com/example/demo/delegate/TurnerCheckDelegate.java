package com.example.demo.delegate;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

/**
 * @author LDF
 * @date 2020/6/18
 */
public class TurnerCheckDelegate implements JavaDelegate {
    public void execute(DelegateExecution delegateExecution) {
        System.out.println("TODO 完成特纳综合症检查。。。"
                + delegateExecution.getVariable("name"));
    }
}
