package cn.iocoder.yudao.module.iot.controller.app.thingmodel;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.iot.controller.app.thingmodel.vo.IotAppThingModelRespVO;
import cn.iocoder.yudao.module.iot.dal.dataobject.thingmodel.IotThingModelDO;
import cn.iocoder.yudao.module.iot.service.thingmodel.IotThingModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 用户 APP - IoT 物模型 Controller
 * <p>
 * 提供按产品编号集合批量获取物模型的能力，便于 App 端在渲染设备列表时一次拉取各产品的物模型。
 * 所有接口默认需要 App 端登录态（会员 token）。
 *
 * @author 芋道源码
 */
@Tag(name = "用户 APP - IoT 物模型")
@RestController
@RequestMapping("/iot/app/thing-model")
@Validated
public class AppIotThingModelController {

    @Resource
    private IotThingModelService thingModelService;

    @GetMapping("/list")
    @Operation(summary = "批量获取产品物模型")
    @Parameter(name = "productIds", description = "产品编号集合", required = true, example = "5,11,16")
    @Parameter(name = "type", description = "物模型类型（1=属性 2=服务 3=事件，为空表示全部）", example = "1")
    public CommonResult<Map<Long, List<IotAppThingModelRespVO>>> getThingModelList(
            @RequestParam("productIds") Collection<Long> productIds,
            @RequestParam(value = "type", required = false) Integer type) {
        List<IotThingModelDO> list = thingModelService.getThingModelListByProductIds(productIds, type);
        // 按 productId 分组返回，方便 App 按产品取用
        Map<Long, List<IotAppThingModelRespVO>> result = list.stream()
                .collect(Collectors.groupingBy(IotThingModelDO::getProductId,
                        Collectors.mapping(
                                item -> BeanUtils.toBean(item, IotAppThingModelRespVO.class),
                                Collectors.toList())));
        return success(result);
    }

}
