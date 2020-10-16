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
    private String typeName;
    private String itemName;
    private String itemAbbr;
    private String itemUnit;
    private Double itemLower;
    private Double itemUpper;
    private String itemDescription;
    private String itemIncMeaning;
    private String itemDecMeaning;

    @Override
    protected Serializable pkVal() {
        return this.id;
    }
}
