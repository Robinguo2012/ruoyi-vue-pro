package cn.iocoder.yudao.module.iot.controller.app.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "用户 APP - IoT 绑定设备 Response VO")
@Data
public class IotAppDeviceRespVO {

    @Schema(description = "绑定记录编号", example = "1024")
    private Long id;

    @Schema(description = "设备编号", example = "2048")
    private Long deviceId;

    @Schema(description = "产品标识", example = "ke5EYZurCB3FwFXp")
    private String productKey;

    @Schema(description = "设备名称", example = "switch001")
    private String deviceName;

    @Schema(description = "设备备注名", example = "客厅开关")
    private String nickname;

    @Schema(description = "设备状态（0 未激活 1 在线 2 离线）", example = "1")
    private Integer state;

    @Schema(description = "最后上线时间")
    private LocalDateTime onlineTime;

    @Schema(description = "设备图片", example = "https://www.iocoder.cn/1.png")
    private String picUrl;

}
