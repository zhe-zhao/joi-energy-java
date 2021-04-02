package uk.tw.energy.builders;

import uk.tw.energy.domain.ElectricityReading;
import uk.tw.energy.domain.MeterReadings;
import uk.tw.energy.generator.ElectricityReadingsGenerator;

import java.util.ArrayList;
import java.util.List;

public class MeterReadingsBuilder {

    private static final String DEFAULT_METER_ID = "id";

    private String smartMeterId = DEFAULT_METER_ID;
    private List<ElectricityReading> electricityReadings = new ArrayList<>();

    public MeterReadingsBuilder setSmartMeterId(String smartMeterId) {
        this.smartMeterId = smartMeterId;
        return this;
    }

    public MeterReadingsBuilder generateElectricityReadings() {
        return generateElectricityReadings(5, -1);
    }

    public MeterReadingsBuilder generateElectricityReadings(int number) {
        return generateElectricityReadings(number, -1);
    }

    public MeterReadingsBuilder generateElectricityReadings(double readingVal) {
        return generateElectricityReadings(5, readingVal);
    }

    public MeterReadingsBuilder generateElectricityReadings(int number, double readingVal) {
        ElectricityReadingsGenerator readingsBuilder = new ElectricityReadingsGenerator();
        this.electricityReadings = readingsBuilder.generate(number, readingVal);
        return this;
    }

    public MeterReadings build() {
        return new MeterReadings(smartMeterId, electricityReadings);
    }
}
