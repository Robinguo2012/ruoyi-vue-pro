package cn.iocoder.yudao.module.iot.service.thingmodel;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.iot.dal.dataobject.thingmodel.IotThingModelDO;
import cn.iocoder.yudao.module.iot.dal.dataobject.thingmodel.model.ThingModelProperty;
import cn.iocoder.yudao.module.iot.dal.dataobject.thingmodel.model.dataType.ThingModelBoolOrEnumDataSpecs;
import cn.iocoder.yudao.module.iot.dal.dataobject.thingmodel.model.dataType.ThingModelDateOrTextDataSpecs;
import cn.iocoder.yudao.module.iot.dal.mysql.thingmodel.IotThingModelMapper;
import cn.iocoder.yudao.module.iot.enums.thingmodel.IotDataSpecsDataTypeEnum;
import cn.iocoder.yudao.module.iot.enums.thingmodel.IotThingModelTypeEnum;
import cn.iocoder.yudao.module.iot.service.device.IotDeviceModbusPointService;
import cn.iocoder.yudao.module.iot.service.product.IotProductService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link IotThingModelServiceImpl} 的单元测试
 *
 * @author 芋道源码
 */
@Import(IotThingModelServiceImpl.class)
public class IotThingModelServiceImplTest extends BaseDbUnitTest {

    @Resource
    private IotThingModelServiceImpl thingModelService;

    @MockBean
    private IotProductService productService;
    @MockBean
    private IotDeviceModbusPointService deviceModbusPointService;

    @Resource
    private IotThingModelMapper thingModelMapper;

    @Test
    public void test_getThingModelListByProductIds_filterByProductIds() {
        // 插入 3 个产品的物模型
        thingModelMapper.insert(buildThingModelRow(1L, "temp", IotThingModelTypeEnum.PROPERTY.getType()));
        thingModelMapper.insert(buildThingModelRow(2L, "humi", IotThingModelTypeEnum.PROPERTY.getType()));
        thingModelMapper.insert(buildThingModelRow(3L, "switch", IotThingModelTypeEnum.PROPERTY.getType()));

        // 只查产品 1、2
        List<IotThingModelDO> result = thingModelService.getThingModelListByProductIds(
                Arrays.asList(1L, 2L), null);

        assertEquals(2, result.size());
        assertEquals(2, result.stream().map(IotThingModelDO::getProductId).distinct().count());
    }

    @Test
    public void test_getThingModelListByProductIds_filterByType() {
        // 产品 10 同时有属性和服务
        thingModelMapper.insert(buildThingModelRow(10L, "p1", IotThingModelTypeEnum.PROPERTY.getType()));
        thingModelMapper.insert(buildThingModelRow(10L, "svc1", IotThingModelTypeEnum.SERVICE.getType()));

        // 只取属性
        List<IotThingModelDO> result = thingModelService.getThingModelListByProductIds(
                Collections.singletonList(10L), IotThingModelTypeEnum.PROPERTY.getType());

        assertEquals(1, result.size());
        assertEquals("p1", result.get(0).getIdentifier());
    }

    @Test
    public void test_getThingModelListByProductIds_empty() {
        List<IotThingModelDO> result = thingModelService.getThingModelListByProductIds(
                Collections.emptyList(), null);
        assertTrue(result.isEmpty());
    }

    private IotThingModelDO buildThingModelRow(Long productId, String identifier, Integer type) {
        return IotThingModelDO.builder()
                .productId(productId).productKey("pk_" + productId)
                .identifier(identifier).name(identifier).type(type).build();
    }

    @Test
    public void testConvertThingModelPropertyValue_boolFromBooleanTrue() {
        assertBoolValueConvertedToByte(true, (byte) 1);
    }

    @Test
    public void testConvertThingModelPropertyValue_boolFromBooleanFalse() {
        assertBoolValueConvertedToByte(false, (byte) 0);
    }

    @Test
    public void testConvertThingModelPropertyValue_boolFromStringTrue() {
        assertBoolValueConvertedToByte("true", (byte) 1);
    }

    @Test
    public void testConvertThingModelPropertyValue_boolFromStringFalse() {
        assertBoolValueConvertedToByte("false", (byte) 0);
    }

    @Test
    public void testConvertThingModelPropertyValue_boolFromNumberOne() {
        assertBoolValueConvertedToByte(1, (byte) 1);
    }

    @Test
    public void testConvertThingModelPropertyValue_boolFromNumberZero() {
        assertBoolValueConvertedToByte(0, (byte) 0);
    }

    @Test
    public void testConvertThingModelPropertyValue_boolInvalid() {
        IotThingModelDO thingModel = buildThingModel("PowerSwitch", IotDataSpecsDataTypeEnum.BOOL.getDataType());

        Object result = thingModelService.convertThingModelPropertyValue(thingModel, "yes");

        assertNull(result);
    }

    @Test
    public void testConvertThingModelPropertyValue_enumFromStringNumber() {
        IotThingModelDO thingModel = buildEnumThingModel("WorkMode", enumSpec("Auto", 1), enumSpec("Manual", 2));

        Object result = thingModelService.convertThingModelPropertyValue(thingModel, "1");

        assertEquals((byte) 1, result);
    }

    @Test
    public void testConvertThingModelPropertyValue_enumFromSpecName() {
        IotThingModelDO thingModel = buildEnumThingModel("WorkMode", enumSpec("Auto", 1), enumSpec("Manual", 2));

        Object result = thingModelService.convertThingModelPropertyValue(thingModel, "Manual");

        assertEquals((byte) 2, result);
    }

    @Test
    public void testConvertThingModelPropertyValue_enumInvalidSpecValue() {
        IotThingModelDO thingModel = buildEnumThingModel("WorkMode", enumSpec("Auto", 1), enumSpec("Manual", 2));

        Object result = thingModelService.convertThingModelPropertyValue(thingModel, 3);

        assertNull(result);
    }

    @Test
    public void testConvertThingModelPropertyValue_enumOutOfTinyIntRange() {
        IotThingModelDO thingModel = buildThingModel("WorkMode", IotDataSpecsDataTypeEnum.ENUM.getDataType());

        Object result = thingModelService.convertThingModelPropertyValue(thingModel, 128);

        assertNull(result);
    }

    @Test
    public void testConvertThingModelPropertyValue_dateFromLocalDateTime() {
        IotThingModelDO thingModel = buildThingModel("CollectTime", IotDataSpecsDataTypeEnum.DATE.getDataType());
        LocalDateTime collectTime = LocalDateTime.of(2025, 1, 2, 3, 4, 5);

        Object result = thingModelService.convertThingModelPropertyValue(thingModel, collectTime);

        assertEquals(LocalDateTimeUtil.toEpochMilli(collectTime), result);
    }

    @Test
    public void testConvertThingModelPropertyValue_dateFromString() {
        IotThingModelDO thingModel = buildThingModel("CollectTime", IotDataSpecsDataTypeEnum.DATE.getDataType());
        LocalDateTime collectTime = LocalDateTime.of(2025, 1, 2, 3, 4, 5);

        Object result = thingModelService.convertThingModelPropertyValue(thingModel, "2025-01-02 03:04:05");

        assertEquals(LocalDateTimeUtil.toEpochMilli(collectTime), result);
    }

    @Test
    public void testConvertThingModelPropertyValue_intInvalid() {
        IotThingModelDO thingModel = buildThingModel("Temperature", IotDataSpecsDataTypeEnum.INT.getDataType());

        Object result = thingModelService.convertThingModelPropertyValue(thingModel, "abc");

        assertNull(result);
    }

    @Test
    public void testConvertThingModelPropertyValue_textOverLength() {
        IotThingModelDO thingModel = buildTextThingModel("Remark", 3);

        Object result = thingModelService.convertThingModelPropertyValue(thingModel, "abcd");

        assertNull(result);
    }

    @Test
    public void testConvertThingModelPropertyValue_structToJson() {
        IotThingModelDO thingModel = buildThingModel("GeoLocation", IotDataSpecsDataTypeEnum.STRUCT.getDataType());
        Map<String, Object> value = new HashMap<>();
        value.put("Longitude", 120.1D);

        Object result = thingModelService.convertThingModelPropertyValue(thingModel, value);

        assertEquals(JsonUtils.toJsonString(value), result);
    }

    private void assertBoolValueConvertedToByte(Object reportedValue, byte expected) {
        IotThingModelDO thingModel = buildThingModel("PowerSwitch", IotDataSpecsDataTypeEnum.BOOL.getDataType());

        Object result = thingModelService.convertThingModelPropertyValue(thingModel, reportedValue);

        assertEquals(expected, result);
    }

    private IotThingModelDO buildThingModel(String identifier, String dataType) {
        ThingModelProperty property = new ThingModelProperty();
        property.setIdentifier(identifier);
        property.setDataType(dataType);
        return IotThingModelDO.builder().identifier(identifier).property(property).build();
    }

    private IotThingModelDO buildEnumThingModel(String identifier, ThingModelBoolOrEnumDataSpecs... dataSpecsList) {
        IotThingModelDO thingModel = buildThingModel(identifier, IotDataSpecsDataTypeEnum.ENUM.getDataType());
        thingModel.getProperty().setDataSpecsList(Arrays.asList(dataSpecsList));
        return thingModel;
    }

    private ThingModelBoolOrEnumDataSpecs enumSpec(String name, Integer value) {
        ThingModelBoolOrEnumDataSpecs dataSpecs = new ThingModelBoolOrEnumDataSpecs();
        dataSpecs.setName(name);
        dataSpecs.setValue(value);
        return dataSpecs;
    }

    private IotThingModelDO buildTextThingModel(String identifier, Integer length) {
        IotThingModelDO thingModel = buildThingModel(identifier, IotDataSpecsDataTypeEnum.TEXT.getDataType());
        ThingModelDateOrTextDataSpecs dataSpecs = new ThingModelDateOrTextDataSpecs();
        dataSpecs.setLength(length);
        thingModel.getProperty().setDataSpecs(dataSpecs);
        return thingModel;
    }

}
