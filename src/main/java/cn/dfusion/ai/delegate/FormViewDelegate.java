package cn.dfusion.ai.delegate;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.form.api.FormInfo;
import org.flowable.form.model.FormField;
import org.flowable.form.model.SimpleFormModel;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @author ldf
 * @email ldf@dfusion.cn
 * @date 2020/9/10 下午1:07
 */
public class FormViewDelegate implements JavaDelegate {
    @Autowired
    private RuntimeService runtimeService;

    @Override
    public void execute(DelegateExecution execution) {
        FormInfo info = runtimeService.getStartFormModel(execution.getProcessDefinitionId(), execution.getProcessInstanceId());

        SimpleFormModel sfm = (SimpleFormModel) info.getFormModel();
        List<FormField> fields = sfm.getFields();
        for (FormField ff : fields) {
            System.out.println();
            System.out.println("id: " + ff.getId());
            System.out.println("name: " + ff.getName());
            System.out.println("type: " + ff.getType());
            System.out.println("placeholder: " + ff.getPlaceholder());
            System.out.println("value: " + ff.getValue());
            System.out.println("value from variable: " + execution.getVariable(ff.getId()));
            System.out.println();
        }
    }
}
