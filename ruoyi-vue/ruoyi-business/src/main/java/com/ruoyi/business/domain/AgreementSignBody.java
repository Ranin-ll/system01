package com.ruoyi.business.domain;

/**
 * 保密协议签署参数。
 */
public class AgreementSignBody {

    private String signature;

    private Boolean confirmed;

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public Boolean getConfirmed() {
        return confirmed;
    }

    public void setConfirmed(Boolean confirmed) {
        this.confirmed = confirmed;
    }
}
