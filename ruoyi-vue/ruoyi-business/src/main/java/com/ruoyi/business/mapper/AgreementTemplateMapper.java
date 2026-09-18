package com.ruoyi.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.business.domain.AgreementTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 协议模板 Mapper
 *
 * <p>注意：注册/签署流程（InternAuthMapper）也在读本表 —— 那里读的是
 * <code>status = 'EFFECTIVE' order by effective_time desc limit 1</code>，
 * 这里的归档动作必须保证**任意时刻最多只有一行 EFFECTIVE**。</p>
 *
 * @author ruoyi
 */
@Mapper
public interface AgreementTemplateMapper extends BaseMapper<AgreementTemplate> {

    /**
     * 全部模板，按生效时间倒序（生效中的排最前）
     */
    @Select("SELECT id, agreement_name AS agreementName, version_no AS versionNo, content, "
            + "file_url AS fileUrl, effective_time AS effectiveTime, status, sign_count AS signCount, "
            + "create_by AS createBy, create_time AS createTime, update_time AS updateTime "
            + "FROM agreement_template "
            + "ORDER BY effective_time DESC, id DESC")
    List<AgreementTemplate> selectAllOrderByEffective();

    /**
     * 把当前所有生效中的模板归档（发布新版本前调用）
     */
    @Update("UPDATE agreement_template SET status = 'ARCHIVED', update_time = NOW() WHERE status = 'EFFECTIVE'")
    int archiveEffective();

    /**
     * 校验版本号是否已存在
     */
    @Select("SELECT COUNT(*) FROM agreement_template WHERE version_no = #{versionNo}")
    int countByVersionNo(@Param("versionNo") String versionNo);
}
