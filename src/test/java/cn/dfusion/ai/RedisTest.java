package cn.dfusion.ai;

import cn.dfusion.ai.util.RedisUtils;
import com.google.gson.internal.LinkedTreeMap;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.form.api.FormDefinition;
import org.flowable.form.api.FormDeployment;
import org.flowable.form.api.FormRepositoryService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class RedisTest {
	@Autowired
	private RedisUtils redisUtils;
	@Autowired
	private RepositoryService repositoryService;
	@Autowired
	private FormRepositoryService formRepositoryService;

	@Test
	public void contextLoads() {
		Deployment deployment = repositoryService.createDeployment()
				.name("慢阻肺AI诊疗流程")
				.addClasspathResource("processes/copd.bpmn20.xml")
				.deploy();
		FormDeployment formDeployment = formRepositoryService.createDeployment()
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
		List<FormDefinition> formDefinitionList = formRepositoryService.createFormDefinitionQuery().deploymentId(formDeployment.getId()).list();

		redisUtils.set("formDefinitionList", formDefinitionList);

		System.out.println(ToStringBuilder.reflectionToString(redisUtils.get("formDefinitionList", List.class)));
		List cache = redisUtils.get("formDefinitionList", List.class);
		for(Object fd : cache){
			System.err.println(((LinkedTreeMap)fd).get("key"));
			System.err.println(fd);
		}
	}

}
