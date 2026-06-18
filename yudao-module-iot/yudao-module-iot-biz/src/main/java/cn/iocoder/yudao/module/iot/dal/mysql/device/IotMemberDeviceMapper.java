package cn.iocoder.yudao.module.iot.dal.mysql.device;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.iot.dal.dataobject.device.IotMemberDeviceDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * IoT 会员-设备绑定 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface IotMemberDeviceMapper extends BaseMapperX<IotMemberDeviceDO> {

    /**
     * 按设备编号查询绑定（用于一对一判重）
     */
    default IotMemberDeviceDO selectByDeviceId(Long deviceId) {
        return selectOne(IotMemberDeviceDO::getDeviceId, deviceId);
    }

    /**
     * 按会员编号查询绑定列表
     */
    default List<IotMemberDeviceDO> selectListByMemberId(Long memberId) {
        return selectList(IotMemberDeviceDO::getMemberId, memberId);
    }

    /**
     * 按手机号查询绑定列表
     */
    default List<IotMemberDeviceDO> selectListByMobile(String mobile) {
        return selectList(IotMemberDeviceDO::getMobile, mobile);
    }

    /**
     * 按会员编号 + 设备编号查询绑定（用于解绑校验）
     */
    default IotMemberDeviceDO selectByMemberIdAndDeviceId(Long memberId, Long deviceId) {
        return selectOne(IotMemberDeviceDO::getMemberId, memberId,
                IotMemberDeviceDO::getDeviceId, deviceId);
    }

}
