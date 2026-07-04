package cn.iocoder.yudao.module.iot.controller.app.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Schema(description = "用户 APP - IoT 更新绑定设备 Request VO")
@Data
public class IotAppDeviceUpdateReqVO {

    @Schema(description = "产品标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "ke5EYZurCB3FwFXp")
    @NotBlank(message = "产品标识不能为空")
    private String productKey;

    @Schema(description = "设备名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "switch001")
    @NotBlank(message = "设备名称不能为空")
    private String deviceName;

    @Schema(description = "设备备注名（用户自定义，如「客厅开关」；为空则清除别名）", example = "客厅开关")
    private String nickname;

}
