package cn.iocoder.yudao.module.iot.service.device;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.iot.controller.admin.device.vo.device.IotDeviceAuthInfoRespVO;
import cn.iocoder.yudao.module.iot.controller.admin.device.vo.device.IotDeviceSaveReqVO;
import cn.iocoder.yudao.module.iot.controller.app.device.vo.IotAppDeviceBindReqVO;
import cn.iocoder.yudao.module.iot.controller.app.device.vo.IotAppDeviceRespVO;
import cn.iocoder.yudao.module.iot.core.biz.dto.IotDeviceAuthReqDTO;
import cn.iocoder.yudao.module.iot.core.util.IotDeviceAuthUtils;
import cn.iocoder.yudao.module.iot.core.util.IotProductAuthUtils;
import cn.iocoder.yudao.module.iot.dal.dataobject.device.IotDeviceDO;
import cn.iocoder.yudao.module.iot.dal.dataobject.device.IotMemberDeviceDO;
import cn.iocoder.yudao.module.iot.dal.dataobject.product.IotProductDO;
import cn.iocoder.yudao.module.iot.dal.mysql.device.IotMemberDeviceMapper;
import cn.iocoder.yudao.module.iot.service.product.IotProductService;
import cn.iocoder.yudao.module.member.api.user.MemberUserApi;
import cn.iocoder.yudao.module.member.api.user.dto.MemberUserRespDTO;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.iot.enums.ErrorCodeConstants.DEVICE_ALREADY_BOUND;
import static cn.iocoder.yudao.module.iot.enums.ErrorCodeConstants.DEVICE_NOT_EXISTS;
import static cn.iocoder.yudao.module.iot.enums.ErrorCodeConstants.MEMBER_DEVICE_MEMBER_NOT_EXISTS;
import static cn.iocoder.yudao.module.iot.enums.ErrorCodeConstants.MEMBER_DEVICE_MOBILE_NOT_MATCH;
import static cn.iocoder.yudao.module.iot.enums.ErrorCodeConstants.MEMBER_DEVICE_NOT_EXISTS;
import static cn.iocoder.yudao.module.iot.enums.ErrorCodeConstants.MEMBER_DEVICE_REGISTER_PRODUCT_INVALID;
import static cn.iocoder.yudao.module.iot.enums.ErrorCodeConstants.MEMBER_DEVICE_REGISTER_SIGN_INVALID;

/**
 * IoT 会员-设备绑定 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class IotMemberDeviceServiceImpl implements IotMemberDeviceService {

    @Resource
    private IotMemberDeviceMapper memberDeviceMapper;
    @Resource
    private IotDeviceService deviceService;
    @Resource
    private IotProductService productService;
    @Resource
    private MemberUserApi memberUserApi;

    @Override
    public IotDeviceAuthInfoRespVO registerMobileDevice(Long memberId, String mobile, String productKey, String sign) {
        // 1. 校验当前会员的手机号与请求一致（防止为他人手机号注册设备）
        MemberUserRespDTO member = requireMember(memberId);
        if (!mobile.equals(member.getMobile())) {
            throw exception(MEMBER_DEVICE_MOBILE_NOT_MATCH);
        }
        // 2. 校验产品存在且开启动态注册
        IotProductDO product = productService.getProductByProductKey(productKey);
        if (product == null || BooleanUtil.isFalse(product.getRegisterEnabled())) {
            throw exception(MEMBER_DEVICE_REGISTER_PRODUCT_INVALID);
        }
        // 3. 校验签名（一型一密）
        if (!IotProductAuthUtils.verifySign(productKey, mobile, product.getProductSecret(), sign)) {
            throw exception(MEMBER_DEVICE_REGISTER_SIGN_INVALID);
        }
        // 4. 幂等：获取或创建手机设备（deviceName = 手机号）
        IotDeviceDO device = deviceService.getDeviceFromCache(productKey, mobile);
        if (device == null) {
            Long deviceId = deviceService.createDevice(new IotDeviceSaveReqVO()
                    .setDeviceName(mobile)
                    .setProductId(product.getId()));
            device = deviceService.getDevice(deviceId);
        }
        // 5. 计算 MQTT 认证三元组
        IotDeviceAuthReqDTO authInfo = IotDeviceAuthUtils.getAuthInfo(productKey, mobile, device.getDeviceSecret());
        return BeanUtils.toBean(authInfo, IotDeviceAuthInfoRespVO.class);
    }

    @Override
    public Long bindDevice(Long memberId, IotAppDeviceBindReqVO reqVO) {
        // 1. 获取会员信息（手机号冗余存储）
        MemberUserRespDTO member = requireMember(memberId);
        // 2. 校验设备存在（外部设备须已自注册上线）
        IotDeviceDO device = deviceService.getDeviceFromCache(reqVO.getProductKey(), reqVO.getDeviceName());
        if (device == null) {
            throw exception(DEVICE_NOT_EXISTS);
        }
        // 3. 一对一判重：一个设备仅归属一个会员
        if (memberDeviceMapper.selectByDeviceId(device.getId()) != null) {
            throw exception(DEVICE_ALREADY_BOUND);
        }
        // 4. 插入绑定关系
        IotMemberDeviceDO memberDevice = IotMemberDeviceDO.builder()
                .memberId(memberId)
                .mobile(member.getMobile())
                .deviceId(device.getId())
                .productKey(device.getProductKey())
                .deviceName(device.getDeviceName())
                .nickname(reqVO.getNickname())
                .build();
        memberDeviceMapper.insert(memberDevice);
        return memberDevice.getId();
    }

    @Override
    public List<IotAppDeviceRespVO> getBoundDeviceList(Long memberId, String mobile) {
        // 1. 查询绑定关系（按手机号或会员编号）
        List<IotMemberDeviceDO> list = StrUtil.isNotBlank(mobile)
                ? memberDeviceMapper.selectListByMobile(mobile)
                : memberDeviceMapper.selectListByMemberId(memberId);
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        // 2. 关联设备实时信息
        List<Long> deviceIds = list.stream().map(IotMemberDeviceDO::getDeviceId).collect(Collectors.toList());
        Map<Long, IotDeviceDO> deviceMap = deviceService.getDeviceMap(deviceIds);
        // 3. 组装返回
        return list.stream().map(memberDevice -> {
            IotAppDeviceRespVO respVO = BeanUtils.toBean(memberDevice, IotAppDeviceRespVO.class);
            IotDeviceDO device = deviceMap.get(memberDevice.getDeviceId());
            if (device != null) {
                respVO.setProductId(device.getProductId());
                respVO.setState(device.getState());
                respVO.setOnlineTime(device.getOnlineTime());
                respVO.setPicUrl(device.getPicUrl());
            }
            return respVO;
        }).collect(Collectors.toList());
    }

    @Override
    public void unbindDevice(Long memberId, String productKey, String deviceName) {
        // 1. 校验设备与绑定关系属于当前会员
        IotDeviceDO device = deviceService.getDeviceFromCache(productKey, deviceName);
        if (device == null) {
            throw exception(DEVICE_NOT_EXISTS);
        }
        IotMemberDeviceDO memberDevice = memberDeviceMapper
                .selectByMemberIdAndDeviceId(memberId, device.getId());
        if (memberDevice == null) {
            throw exception(MEMBER_DEVICE_NOT_EXISTS);
        }
        // 2. 删除绑定
        memberDeviceMapper.deleteById(memberDevice.getId());
    }

    /**
     * 获取会员信息，不存在则抛异常
     */
    private MemberUserRespDTO requireMember(Long memberId) {
        MemberUserRespDTO member = memberUserApi.getUser(memberId);
        if (member == null) {
            throw exception(MEMBER_DEVICE_MEMBER_NOT_EXISTS);
        }
        return member;
    }

}
