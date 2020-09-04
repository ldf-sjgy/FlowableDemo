package cn.dfusion.ai.delegate;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

/**
 * @author LDF
 * @date 2020/6/18
 */
public class PrintReportDelegate implements JavaDelegate {
    public void execute(DelegateExecution delegateExecution) {
        System.out.println("TODO 打印预诊报告。。。"
                + delegateExecution.getVariable("name"));
    }
}
