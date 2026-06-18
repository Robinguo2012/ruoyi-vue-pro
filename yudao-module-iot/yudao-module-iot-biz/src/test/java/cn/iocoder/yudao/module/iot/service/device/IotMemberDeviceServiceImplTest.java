package cn.iocoder.yudao.module.iot.service.device;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.iot.controller.admin.device.vo.device.IotDeviceAuthInfoRespVO;
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
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomLongId;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomPojo;
import static cn.iocoder.yudao.module.iot.enums.ErrorCodeConstants.DEVICE_ALREADY_BOUND;
import static cn.iocoder.yudao.module.iot.enums.ErrorCodeConstants.DEVICE_NOT_EXISTS;
import static cn.iocoder.yudao.module.iot.enums.ErrorCodeConstants.MEMBER_DEVICE_MOBILE_NOT_MATCH;
import static cn.iocoder.yudao.module.iot.enums.ErrorCodeConstants.MEMBER_DEVICE_NOT_EXISTS;
import static cn.iocoder.yudao.module.iot.enums.ErrorCodeConstants.MEMBER_DEVICE_REGISTER_SIGN_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link IotMemberDeviceServiceImpl} 的单元测试类
 *
 * @author 芋道源码
 */
@Import(IotMemberDeviceServiceImpl.class)
class IotMemberDeviceServiceImplTest extends BaseDbUnitTest {

    private static final String PRODUCT_KEY = "ke5EYZurCB3FwFXp";
    private static final String PRODUCT_SECRET = "b9d1872813914761ac49f22dcc1ac0da";

    @Resource
    private IotMemberDeviceServiceImpl memberDeviceService;
    @Resource
    private IotMemberDeviceMapper memberDeviceMapper;

    @MockBean
    private IotDeviceService deviceService;
    @MockBean
    private IotProductService productService;
    @MockBean
    private MemberUserApi memberUserApi;

    // ==================== 手机设备注册（幂等） ====================

    @Test
    public void test_registerMobileDevice_create_whenNotExists() {
        // 准备参数
        Long memberId = randomLongId();
        String mobile = "13800138000";
        String sign = IotProductAuthUtils.buildSign(PRODUCT_KEY, mobile, PRODUCT_SECRET);
        when(memberUserApi.getUser(memberId)).thenReturn(buildMember(memberId, mobile));
        when(productService.getProductByProductKey(PRODUCT_KEY)).thenReturn(buildProduct());
        // 设备不存在
        when(deviceService.getDeviceFromCache(PRODUCT_KEY, mobile)).thenReturn(null);
        Long deviceId = randomLongId();
        when(deviceService.createDevice(any())).thenReturn(deviceId);
        when(deviceService.getDevice(deviceId)).thenReturn(
                randomPojo(IotDeviceDO.class).setId(deviceId).setDeviceSecret("secret_xxx"));

        IotDeviceAuthInfoRespVO resp = memberDeviceService.registerMobileDevice(memberId, mobile, PRODUCT_KEY, sign);

        // 断言：新建设备并返回三元组
        verify(deviceService).createDevice(any());
        assertEquals(mobile, resp.getUsername().split("&")[0]); // username = deviceName & productKey
        assertNotNull(resp.getClientId());
        assertNotNull(resp.getPassword());
    }

    @Test
    public void test_registerMobileDevice_idempotent_whenExists() {
        Long memberId = randomLongId();
        String mobile = "13800138000";
        String sign = IotProductAuthUtils.buildSign(PRODUCT_KEY, mobile, PRODUCT_SECRET);
        when(memberUserApi.getUser(memberId)).thenReturn(buildMember(memberId, mobile));
        when(productService.getProductByProductKey(PRODUCT_KEY)).thenReturn(buildProduct());
        // 设备已存在（携带 deviceSecret）
        IotDeviceDO device = randomPojo(IotDeviceDO.class)
                .setProductKey(PRODUCT_KEY).setDeviceName(mobile).setDeviceSecret("secret_exist");
        when(deviceService.getDeviceFromCache(PRODUCT_KEY, mobile)).thenReturn(device);

        IotDeviceAuthInfoRespVO resp = memberDeviceService.registerMobileDevice(memberId, mobile, PRODUCT_KEY, sign);

        // 断言：幂等——不创建设备，password 与现有 deviceSecret 计算结果一致
        verify(deviceService, never()).createDevice(any());
        IotDeviceAuthReqDTO expected = IotDeviceAuthUtils.getAuthInfo(PRODUCT_KEY, mobile, "secret_exist");
        assertEquals(expected.getPassword(), resp.getPassword());
    }

    @Test
    public void test_registerMobileDevice_fail_whenMobileNotMatch() {
        Long memberId = randomLongId();
        when(memberUserApi.getUser(memberId)).thenReturn(buildMember(memberId, "13900000000"));
        assertServiceException(() -> memberDeviceService
                .registerMobileDevice(memberId, "13800138000", PRODUCT_KEY, "any"), MEMBER_DEVICE_MOBILE_NOT_MATCH);
    }

    @Test
    public void test_registerMobileDevice_fail_whenSignInvalid() {
        Long memberId = randomLongId();
        String mobile = "13800138000";
        when(memberUserApi.getUser(memberId)).thenReturn(buildMember(memberId, mobile));
        when(productService.getProductByProductKey(PRODUCT_KEY)).thenReturn(buildProduct());
        assertServiceException(() -> memberDeviceService
                .registerMobileDevice(memberId, mobile, PRODUCT_KEY, "wrong_sign"), MEMBER_DEVICE_REGISTER_SIGN_INVALID);
    }

    // ==================== 扫码绑定 ====================

    @Test
    public void test_bindDevice_success() {
        Long memberId = randomLongId();
        when(memberUserApi.getUser(memberId)).thenReturn(buildMember(memberId, "13800138000"));
        IotDeviceDO device = randomPojo(IotDeviceDO.class)
                .setProductKey("pk_switch").setDeviceName("switch001");
        when(deviceService.getDeviceFromCache("pk_switch", "switch001")).thenReturn(device);

        IotAppDeviceBindReqVO reqVO = new IotAppDeviceBindReqVO()
                .setProductKey("pk_switch").setDeviceName("switch001").setNickname("客厅开关");
        Long bindId = memberDeviceService.bindDevice(memberId, reqVO);

        IotMemberDeviceDO record = memberDeviceMapper.selectById(bindId);
        assertNotNull(record);
        assertEquals(memberId, record.getMemberId());
        assertEquals("13800138000", record.getMobile());
        assertEquals(device.getId(), record.getDeviceId());
        assertEquals("客厅开关", record.getNickname());
    }

    @Test
    public void test_bindDevice_fail_whenDeviceNotExists() {
        Long memberId = randomLongId();
        when(memberUserApi.getUser(memberId)).thenReturn(buildMember(memberId, "13800138000"));
        when(deviceService.getDeviceFromCache("pk_switch", "switch001")).thenReturn(null);

        IotAppDeviceBindReqVO reqVO = new IotAppDeviceBindReqVO()
                .setProductKey("pk_switch").setDeviceName("switch001");
        assertServiceException(() -> memberDeviceService.bindDevice(memberId, reqVO), DEVICE_NOT_EXISTS);
    }

    @Test
    public void test_bindDevice_fail_whenAlreadyBound() {
        Long memberId = randomLongId();
        when(memberUserApi.getUser(memberId)).thenReturn(buildMember(memberId, "13800138000"));
        IotDeviceDO device = randomPojo(IotDeviceDO.class)
                .setProductKey("pk_switch").setDeviceName("switch001");
        when(deviceService.getDeviceFromCache("pk_switch", "switch001")).thenReturn(device);
        // 预置：该设备已被他人绑定
        memberDeviceMapper.insert(IotMemberDeviceDO.builder()
                .memberId(randomLongId()).mobile("13700000000")
                .deviceId(device.getId()).productKey("pk_switch").deviceName("switch001")
                .build());

        IotAppDeviceBindReqVO reqVO = new IotAppDeviceBindReqVO()
                .setProductKey("pk_switch").setDeviceName("switch001");
        assertServiceException(() -> memberDeviceService.bindDevice(memberId, reqVO), DEVICE_ALREADY_BOUND);
    }

    // ==================== 查询绑定列表 ====================

    @Test
    public void test_getBoundDeviceList_byMemberId() {
        Long memberId = randomLongId();
        Long deviceId1 = randomLongId();
        Long deviceId2 = randomLongId();
        memberDeviceMapper.insert(buildBind(memberId, "13800138000", deviceId1, "pk1", "d1"));
        memberDeviceMapper.insert(buildBind(memberId, "13800138000", deviceId2, "pk2", "d2"));
        Map<Long, IotDeviceDO> deviceMap = new HashMap<>();
        deviceMap.put(deviceId1, randomPojo(IotDeviceDO.class).setId(deviceId1).setState(1));
        deviceMap.put(deviceId2, randomPojo(IotDeviceDO.class).setId(deviceId2).setState(2));
        when(deviceService.getDeviceMap(any())).thenReturn(deviceMap);

        List<IotAppDeviceRespVO> list = memberDeviceService.getBoundDeviceList(memberId, null);

        assertEquals(2, list.size());
        assertTrue(list.stream().anyMatch(v -> v.getState() != null));
    }

    @Test
    public void test_getBoundDeviceList_byMobile() {
        Long memberId = randomLongId();
        String mobile = "13800138000";
        Long deviceId = randomLongId();
        memberDeviceMapper.insert(buildBind(memberId, mobile, deviceId, "pk1", "d1"));
        when(deviceService.getDeviceMap(any())).thenReturn(new HashMap<>());

        List<IotAppDeviceRespVO> list = memberDeviceService.getBoundDeviceList(memberId, mobile);

        assertEquals(1, list.size());
        assertEquals("d1", list.get(0).getDeviceName());
    }

    @Test
    public void test_getBoundDeviceList_empty() {
        List<IotAppDeviceRespVO> list = memberDeviceService.getBoundDeviceList(randomLongId(), null);
        assertTrue(list.isEmpty());
    }

    // ==================== 解绑 ====================

    @Test
    public void test_unbindDevice_success() {
        Long memberId = randomLongId();
        IotDeviceDO device = randomPojo(IotDeviceDO.class).setProductKey("pk1").setDeviceName("d1");
        when(deviceService.getDeviceFromCache("pk1", "d1")).thenReturn(device);
        memberDeviceMapper.insert(buildBind(memberId, "13800138000", device.getId(), "pk1", "d1"));

        memberDeviceService.unbindDevice(memberId, "pk1", "d1");

        assertNull(memberDeviceMapper.selectByMemberIdAndDeviceId(memberId, device.getId()));
    }

    @Test
    public void test_unbindDevice_fail_whenNotBound() {
        Long memberId = randomLongId();
        IotDeviceDO device = randomPojo(IotDeviceDO.class).setProductKey("pk1").setDeviceName("d1");
        when(deviceService.getDeviceFromCache("pk1", "d1")).thenReturn(device);

        assertServiceException(() -> memberDeviceService.unbindDevice(memberId, "pk1", "d1"), MEMBER_DEVICE_NOT_EXISTS);
    }

    // ==================== 辅助方法 ====================

    private MemberUserRespDTO buildMember(Long id, String mobile) {
        return new MemberUserRespDTO().setId(id).setMobile(mobile);
    }

    private IotProductDO buildProduct() {
        return randomPojo(IotProductDO.class)
                .setProductKey(PRODUCT_KEY).setProductSecret(PRODUCT_SECRET).setRegisterEnabled(true);
    }

    private IotMemberDeviceDO buildBind(Long memberId, String mobile, Long deviceId,
                                        String productKey, String deviceName) {
        return IotMemberDeviceDO.builder()
                .memberId(memberId).mobile(mobile).deviceId(deviceId)
                .productKey(productKey).deviceName(deviceName).build();
    }

}
