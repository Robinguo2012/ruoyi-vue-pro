package cn.iocoder.yudao.module.iot.controller.app.device;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.iot.controller.admin.device.vo.device.IotDeviceAuthInfoRespVO;
import cn.iocoder.yudao.module.iot.controller.app.device.vo.IotAppDeviceBindReqVO;
import cn.iocoder.yudao.module.iot.controller.app.device.vo.IotAppDeviceRegisterReqVO;
import cn.iocoder.yudao.module.iot.controller.app.device.vo.IotAppDeviceRespVO;
import cn.iocoder.yudao.module.iot.controller.app.device.vo.IotAppDeviceUpdateReqVO;
import cn.iocoder.yudao.module.iot.service.device.IotMemberDeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * 用户 APP - IoT 设备 Controller
 * <p>
 * 提供手机设备注册（获取 MQTT 三元组）、扫码绑定外部设备、查询绑定设备列表等能力。
 * 所有接口默认需要 App 端登录态（会员 token），通过 {@code getLoginUserId()} 识别当前会员。
 *
 * @author 芋道源码
 */
@Tag(name = "用户 APP - IoT 设备")
@RestController
@RequestMapping("/iot/app/device")
@Validated
public class AppIotDeviceController {

    @Resource
    private IotMemberDeviceService memberDeviceService;

    @PostMapping("/register")
    @Operation(summary = "手机设备注册，获取 MQTT 认证三元组")
    public CommonResult<IotDeviceAuthInfoRespVO> registerDevice(@Valid @RequestBody IotAppDeviceRegisterReqVO reqVO) {
        return success(memberDeviceService.registerMobileDevice(getLoginUserId(),
                reqVO.getMobile(), reqVO.getProductKey(), reqVO.getSign()));
    }

    @PostMapping("/bind")
    @Operation(summary = "扫码绑定外部设备")
    public CommonResult<Long> bindDevice(@Valid @RequestBody IotAppDeviceBindReqVO reqVO) {
        return success(memberDeviceService.bindDevice(getLoginUserId(), reqVO));
    }

    @GetMapping("/list")
    @Operation(summary = "查询当前会员绑定的设备列表")
    @Parameter(name = "mobile", description = "手机号（为空则查询当前登录会员）", example = "13800138000")
    public CommonResult<List<IotAppDeviceRespVO>> getBoundDeviceList(@RequestParam(required = false) String mobile) {
        return success(memberDeviceService.getBoundDeviceList(getLoginUserId(), mobile));
    }

    @DeleteMapping("/unbind")
    @Operation(summary = "解绑设备")
    public CommonResult<Boolean> unbindDevice(@RequestParam("productKey") String productKey,
                                              @RequestParam("deviceName") String deviceName) {
        memberDeviceService.unbindDevice(getLoginUserId(), productKey, deviceName);
        return success(true);
    }

    @PutMapping("/update")
    @Operation(summary = "更新绑定设备的备注信息")
    public CommonResult<Boolean> updateDevice(@Valid @RequestBody IotAppDeviceUpdateReqVO reqVO) {
        memberDeviceService.updateDevice(getLoginUserId(), reqVO);
        return success(true);
    }

}
