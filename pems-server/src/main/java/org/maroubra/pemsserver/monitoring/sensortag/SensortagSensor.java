package org.maroubra.pemsserver.monitoring.sensortag;

import io.reactivex.Flowable;
import io.reactivex.processors.PublishProcessor;
import org.maroubra.pemsserver.monitoring.AbstractSensor;
import org.maroubra.pemsserver.monitoring.SensorLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tinyb.BluetoothDevice;
import tinyb.BluetoothGattCharacteristic;
import tinyb.BluetoothGattService;

import java.util.UUID;

public class SensortagSensor extends AbstractSensor {

    private static final Logger log = LoggerFactory.getLogger(SensortagSensor.class);

    private final SensortagSensorConfig config;
    private final BluetoothDevice sensortagDevice;
    private final PublishProcessor<SensorLog> sensorLogPublisher = PublishProcessor.create();

    public SensortagSensor(SensortagSensorConfig config, BluetoothDevice sensortagDevice) {
        this.config = config;
        this.sensortagDevice = sensortagDevice;
    }

    @Override
    protected boolean start() {
        if (!sensortagDevice.connect())
            return false;

        boolean allCharacteristicsStarted =
                startTemperatureCharacteristic() &&
                startHumidityCharacteristic() &&
                startBarometerCharacteristic() &&
                startOpticalCharacteristic();

        if (!allCharacteristicsStarted) {
            sensortagDevice.disconnect();
            return false;
        }

        return true;
    }

    @Override
    protected boolean stop() {
        return sensortagDevice.disconnect();
    }

    @Override
    protected Flowable<SensorLog> logs() {
        return sensorLogPublisher.onBackpressureLatest();
    }

    private boolean startTemperatureCharacteristic() {
        BluetoothGattService service = getService(SensortagUUID.UUID_TEMP_SENSOR_ENABLE);

        BluetoothGattCharacteristic tempValue = service.find(SensortagUUID.UUID_TEMP_SENSOR_DATA.toString());
        BluetoothGattCharacteristic tempConfig = service.find(SensortagUUID.UUID_TEMP_SENSOR_CONFIG.toString());
        BluetoothGattCharacteristic tempPeriod = service.find(SensortagUUID.UUID_TEMP_SENSOR_PERIOD.toString());

        if (tempValue == null || tempConfig == null || tempPeriod == null) {
            log.error("Could not find the correct characteristics.");
            return false;
        }

        // 1 second update period
        tempPeriod.writeValue(new byte[] { 0x64 });

        // enable the temperature sensor
        tempConfig.writeValue(new byte[] { 0x01 });

        tempValue.enableValueNotifications(new TemperatureNotification(config, sensorLogPublisher));

        return true;
    }

    private boolean startHumidityCharacteristic() {
        BluetoothGattService service = getService(SensortagUUID.UUID_HUM_SENSOR_ENABLE);

        BluetoothGattCharacteristic humidityValue = service.find(SensortagUUID.UUID_HUM_SENSOR_DATA.toString());
        BluetoothGattCharacteristic humidityConfig = service.find(SensortagUUID.UUID_HUM_SENSOR_CONFIG.toString());
        BluetoothGattCharacteristic humidityPeriod = service.find(SensortagUUID.UUID_HUM_SENSOR_PERIOD.toString());

        if (humidityValue == null || humidityConfig == null || humidityPeriod == null) {
            log.error("Could not find the correct characteristics.");
            return false;
        }

        // 1 second update period
        humidityPeriod.writeValue(new byte[] { 0x64 });

        // enable the temperature sensor
        humidityConfig.writeValue(new byte[] { 0x01 });

        humidityValue.enableValueNotifications(new HumidityNotification(config, sensorLogPublisher));

        return true;
    }

    private boolean startBarometerCharacteristic() {
        BluetoothGattService service = getService(SensortagUUID.UUID_BARO_SENSOR_ENABLE);

        BluetoothGattCharacteristic barometerValue = service.find(SensortagUUID.UUID_BARO_SENSOR_DATA.toString());
        BluetoothGattCharacteristic barometerConfig = service.find(SensortagUUID.UUID_BARO_SENSOR_CONFIG.toString());
        BluetoothGattCharacteristic barometerPeriod = service.find(SensortagUUID.UUID_BARO_SENSOR_PERIOD.toString());

        if (barometerValue == null || barometerConfig == null || barometerPeriod == null) {
            log.error("Could not find the correct characteristics.");
            return false;
        }

        // 1 second update period
        barometerPeriod.writeValue(new byte[] { 0x64 });

        // enable the temperature sensor
        barometerConfig.writeValue(new byte[] { 0x01 });

        barometerValue.enableValueNotifications(new PressureNotification(config, sensorLogPublisher));

        return true;
    }

    private boolean startOpticalCharacteristic() {
        BluetoothGattService service = getService(SensortagUUID.UUID_LUXO_SENSOR_ENABLE);

        BluetoothGattCharacteristic opticalValue = service.find(SensortagUUID.UUID_LUXO_SENSOR_DATA.toString());
        BluetoothGattCharacteristic opticalConfig = service.find(SensortagUUID.UUID_LUXO_SENSOR_CONFIG.toString());
        BluetoothGattCharacteristic opticalPeriod = service.find(SensortagUUID.UUID_LUXO_SENSOR_PERIOD.toString());

        if (opticalValue == null || opticalConfig == null || opticalPeriod == null) {
            log.error("Could not find the correct characteristics.");
            return false;
        }

        // 1 second update period
        opticalPeriod.writeValue(new byte[] { 0x64 });

        // enable the temperature sensor
        opticalConfig.writeValue(new byte[] { 0x01 });

        opticalValue.enableValueNotifications(new OpticalNotification(config, sensorLogPublisher));

        return true;
    }

    private BluetoothGattService getService(UUID uuid) {
        return sensortagDevice.find(uuid.toString());
    }

}