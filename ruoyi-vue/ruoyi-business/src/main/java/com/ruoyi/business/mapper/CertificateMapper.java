package com.ruoyi.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.business.domain.Certificate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 电子证书Mapper
 *
 * @author ruoyi
 */
@Mapper
public interface CertificateMapper extends BaseMapper<Certificate> {

    int insertCertificate(Certificate certificate);

    /** 某人当前生效（GENERATED）的证书 */
    Certificate selectActiveByUserId(@Param("userId") Long userId);

    /** 某人全部证书（含已作废），按生成时间倒序 */
    List<Certificate> selectByUserId(@Param("userId") Long userId);

    /** 按转正申请取证书 */
    Certificate selectByPromotionId(@Param("promotionId") Long promotionId);

    /** 证书编号是否已存在（全局唯一校验） */
    int countByCertNo(@Param("certNo") String certNo);

    /** 作废某条转正申请对应的证书（超管撤回转正时用） */
    int revokeByPromotionId(@Param("promotionId") Long promotionId);

    /** 作废某人的全部生效证书 */
    int revokeByUserId(@Param("userId") Long userId);
}
