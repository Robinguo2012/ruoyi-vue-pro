package cn.iocoder.yudao.module.iot.controller.app.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Schema(description = "用户 APP - IoT 手机设备注册 Request VO")
@Data
public class IotAppDeviceRegisterReqVO {

    @Schema(description = "手机号（即设备名称 DeviceName）", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    @NotBlank(message = "手机号不能为空")
    private String mobile;

    @Schema(description = "产品标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "ke5EYZurCB3FwFXp")
    @NotBlank(message = "产品标识不能为空")
    private String productKey;

    @Schema(description = "注册签名（App 端用 productSecret 计算）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "签名不能为空")
    private String sign;

}
