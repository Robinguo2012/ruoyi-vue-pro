package cn.iocoder.yudao.module.iot.dal.dataobject.device;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * IoT 会员-设备绑定 DO
 * <p>
 * 描述「会员（手机 App 用户）」与「外部 IoT 设备」之间的一对一绑定关系。
 * 会员通过 App 扫描设备二维码建立绑定，后续可查询/管理其绑定的设备列表。
 *
 * @author 芋道源码
 */
@TableName("iot_member_device")
@KeySequence("iot_member_device_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IotMemberDeviceDO extends TenantBaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 会员编号
     * <p>
     * 关联 member_user.id
     */
    private Long memberId;
    /**
     * 会员手机号
     * <p>
     * 冗余 {@code member_user.mobile}，便于按手机号查询绑定设备
     */
    private String mobile;
    /**
     * IoT 设备编号
     * <p>
     * 关联 {@link IotDeviceDO#getId()}，一对一唯一
     */
    private Long deviceId;
    /**
     * 产品标识
     * <p>
     * 冗余 {@link IotDeviceDO#getProductKey()}
     */
    private String productKey;
    /**
     * 设备名称
     * <p>
     * 冗余 {@link IotDeviceDO#getDeviceName()}
     */
    private String deviceName;
    /**
     * 设备备注名（用户自定义，如「客厅开关」）
     */
    private String nickname;

}
