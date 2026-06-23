package cn.iocoder.yudao.module.iot.controller.app.thingmodel.vo;

import cn.iocoder.yudao.module.iot.dal.dataobject.thingmodel.model.ThingModelEvent;
import cn.iocoder.yudao.module.iot.dal.dataobject.thingmodel.model.ThingModelProperty;
import cn.iocoder.yudao.module.iot.dal.dataobject.thingmodel.model.ThingModelService;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户 APP - IoT 产品物模型 Response VO")
@Data
public class IotAppThingModelRespVO {

    @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "5")
    private Long productId;

    @Schema(description = "功能标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "kwhp")
    private String identifier;

    @Schema(description = "功能名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "正向有功电能")
    private String name;

    @Schema(description = "功能描述", example = "电表读数")
    private String description;

    @Schema(description = "功能类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer type;

    @Schema(description = "属性")
    private ThingModelProperty property;

    @Schema(description = "事件")
    private ThingModelEvent event;

    @Schema(description = "服务")
    private ThingModelService service;

}
