package com.haif.oj.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.google.gson.Gson;
import com.haif.oj.model.dto.question.JudgeCase;
import com.haif.oj.model.dto.question.JudgeConfig;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 题目
 * @TableName question
 */
@TableName(value ="question")
@Data
public class Question implements Serializable {

    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 标签列表（json 数组）
     */
    private String tags;

    /**
     * 题目答案
     */
    private String answer;

    /**
     * 题目提交数
     */
    private Integer submitNum;

    /**
     * 题目通过数
     */
    private Integer acceptedNum;

    /**
     * 判题用例（json 对象）
     */
    private String judgeCase;

    /**
     * 判题配置（json 对象）
     */
    private String judgeConfig;

    /**
     * 点赞数
     */
    private Integer thumbNum;

    /**
     * 收藏数
     */
    private Integer favourNum;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    private Integer isDelete;

    private final static Gson GSON = new Gson();

    /**
     * 参数转换
     */
    public Map<String, String> get(List<String> tags, JudgeConfig judgeConfigs, List<JudgeCase> judgeCases){
        Map<String, String> map = new HashMap();
        if (tags != null) {
            map.put("tags", GSON.toJson(tags));
        }
        if (judgeConfigs != null) {
            map.put("judgeConfigs", GSON.toJson(judgeConfigs));
        }
        if (judgeCases != null) {
            map.put("judgeCases", GSON.toJson(judgeCases));
        }
        return map;
    }

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

}