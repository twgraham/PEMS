package org.maroubra.pemsserver.monitoring;

import com.mongodb.rx.client.MongoCollection;
import org.maroubra.pemsserver.database.MongoCollectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Completable;
import rx.Observable;

import javax.inject.Inject;
import java.util.List;

public class MonitoringServiceImpl implements MonitoringService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringServiceImpl.class);

    private final SensorFactory sensorFactory;
    private final MongoCollection<SensorConfig> sensorConfigCollection;
    private final MongoCollection<SensorLog> sensorLogsCollection;

    private List<AbstractSensor> runningSensors;

    @Inject
    public MonitoringServiceImpl(SensorFactory sensorFactory, MongoCollection<SensorConfig> sensorConfigCollection, MongoCollection<SensorLog> sensorLogCollection) {
        this.sensorFactory = sensorFactory;
        this.sensorConfigCollection = sensorConfigCollection;
        this.sensorLogsCollection = sensorLogCollection;
    }

    @Inject
    public MonitoringServiceImpl(SensorFactory sensorFactory, MongoCollectionFactory collectionFactory) {
        this(sensorFactory, collectionFactory.getCollection(SensorConfig.class), collectionFactory.getCollection(SensorLog.class));
    }

    @Override
    public Completable initializeSensors() {
        return listSensors().flatMap(sensorConfig -> {
            AbstractSensor sensor = sensorFactory.build(sensorConfig);
            if (sensor.start()) {
                runningSensors.add(sensor);
                sensor.logs().subscribe(this::recordSensorLog);
            }

            return null;
        }).toCompletable();
    }

    @Override
    public Observable<SensorConfig> listSensors() {
        return sensorConfigCollection.find().toObservable();
    }

    private void recordSensorLog(SensorLog sensorLog) {
        sensorLogsCollection.insertOne(sensorLog);
    }
}