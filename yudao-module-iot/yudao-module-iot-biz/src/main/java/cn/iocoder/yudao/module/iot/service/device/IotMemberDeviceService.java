package cn.iocoder.yudao.module.iot.service.device;

import cn.iocoder.yudao.module.iot.controller.admin.device.vo.device.IotDeviceAuthInfoRespVO;
import cn.iocoder.yudao.module.iot.controller.app.device.vo.IotAppDeviceBindReqVO;
import cn.iocoder.yudao.module.iot.controller.app.device.vo.IotAppDeviceRespVO;

import java.util.List;

/**
 * IoT 会员-设备绑定 Service 接口
 * <p>
 * 描述「会员（手机 App 用户）」与「外部 IoT 设备」之间的绑定关系管理，
 * 同时提供手机设备（手机号作为 DeviceName）的幂等注册能力。
 * <p>
 * 注意：会员编号由 Controller 层通过 {@code getLoginUserId()} 获取后传入，
 * 保证 Service 层不依赖安全上下文、便于单元测试。
 *
 * @author 芋道源码
 */
public interface IotMemberDeviceService {

    /**
     * 手机设备注册（幂等）：手机号作为 DeviceName，已注册则返回现有 deviceSecret，
     * 未注册则自动创建。返回用于连接 MQTT Broker 的三元组。
     *
     * @param memberId   会员编号
     * @param mobile     手机号（即设备名称）
     * @param productKey 产品标识
     * @param sign       注册签名（App 端用 productSecret 计算）
     * @return MQTT 认证三元组
     */
    IotDeviceAuthInfoRespVO registerMobileDevice(Long memberId, String mobile, String productKey, String sign);

    /**
     * 扫码绑定外部设备（一对一：一个设备仅归属一个会员）
     *
     * @param memberId 会员编号
     * @param reqVO    绑定请求（产品标识 + 设备名称，来自设备二维码）
     * @return 绑定记录编号
     */
    Long bindDevice(Long memberId, IotAppDeviceBindReqVO reqVO);

    /**
     * 查询会员绑定的设备列表，支持按手机号过滤
     *
     * @param memberId 会员编号
     * @param mobile   手机号（为空则按 memberId 查询）
     * @return 绑定设备列表
     */
    List<IotAppDeviceRespVO> getBoundDeviceList(Long memberId, String mobile);

    /**
     * 解绑设备
     *
     * @param memberId   会员编号
     * @param productKey 产品标识
     * @param deviceName 设备名称
     */
    void unbindDevice(Long memberId, String productKey, String deviceName);

}
