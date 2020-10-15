package cn.dfusion.ai.entity;

import com.baomidou.mybatisplus.activerecord.Model;
import com.baomidou.mybatisplus.annotations.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ldf
 * @email ldf@dfusion.cn
 * @date 2020/8/26 下午8:52
 */
@Data
@TableName("api_exam")
public class ExamEntity extends Model<ExamEntity>{
    private Integer id;
    private String gender;
    private String type_name;
    private String item_name;
    private String item_abbr;
    private String item_unit;
    private Double item_lower;
    private Double item_upper;
    private String item_description;
    private String item_inc_meaning;
    private String item_dec_meaning;

    @Override
    protected Serializable pkVal() {
        return this.id;
    }
}
