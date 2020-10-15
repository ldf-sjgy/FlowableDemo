package cn.dfusion.ai.service.impl;

import cn.dfusion.ai.entity.ExamEntity;
import cn.dfusion.ai.service.ExamService;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import org.springframework.stereotype.Service;

/**
 * @author ldf
 * @email ldf@dfusion.cn
 * @date 2020/9/27 下午4:30
 */
@Service
public class ExamServiceImpl implements ExamService {
    public ExamEntity queryByAbbr(String itemAbbr) {
        ExamEntity examEntity = new ExamEntity();
        ExamEntity examEntity1 = examEntity.selectOne(new EntityWrapper<ExamEntity>().eq("item_abbr", itemAbbr));
        return examEntity1;
    }
}
