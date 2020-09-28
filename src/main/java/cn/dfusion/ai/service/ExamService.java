package cn.dfusion.ai.service;

import cn.dfusion.ai.entity.ExamEntity;

/**
 * @author ldf
 * @email ldf@dfusion.cn
 * @date 2020/9/27 下午4:29
 */
public interface ExamService {
    ExamEntity queryByAbbr(String abbr);
}
