package com.ruoyi.business.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 逐题作答与评分明细 answer_sheet_item
 */
@Data
@TableName("answer_sheet_item")
public class AnswerSheetItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long answerSheetId;
    private Long questionId;

    /** 实操题目ID（exam_subject_item.id）；理论题为空 */
    private Long subjectItemId;

    private Integer seq;
    private String qtype;
    private Integer isSubjective;
    private String userAnswer;
    private Integer isCorrect;
    private BigDecimal aiScore;
    private BigDecimal manualScore;
    private String manualComment;
    private Long manualUserId;
    private Date manualTime;

    /** 题干快照（落库）：进入考核时写入。
     *  题目表（question / exam_subject_item）可以被管理员编辑或删除，
     *  若只靠实时 JOIN，题目被改后批阅界面就取不到题干与满分（实测 exam 31 的题目被重建后，
     *  旧答卷的 stem/score 变成 null，批阅提示出现「null 的分数超过了本题满分 null 分」）。 */
    private String stemSnapshot;

    /** 本题满分快照（落库）：进入考核时写入，同上原因 */
    private BigDecimal fullScore;

    /** 题干（展示字段）：优先取快照，回退 question.stem / exam_subject_item.title */
    @TableField(exist = false)
    private String stem;

    /** 正确答案（展示字段） */
    @TableField(exist = false)
    private String answer;

    /** 理论题选项 JSON（展示字段）：批阅/结果页据此渲染选项，标出正确项与实习生所选 */
    @TableField(exist = false)
    private String optionsJson;

    /** 题干附件（展示字段） */
    @TableField(exist = false)
    private String attachmentUrl;

    /** 题目分值（展示字段）：优先取快照，回退 question.score / exam_subject_item.score */
    @TableField(exist = false)
    private BigDecimal score;

    /** 实操题目描述（展示字段） */
    @TableField(exist = false)
    private String subjectDescription;

    /** 实操题目参考图 JSON（展示字段） */
    @TableField(exist = false)
    private String subjectImages;

    /** 实操题目附件 JSON（展示字段） */
    @TableField(exist = false)
    private String subjectAttachments;
}
