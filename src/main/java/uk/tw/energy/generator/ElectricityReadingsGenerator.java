package uk.tw.energy.generator;

import uk.tw.energy.domain.ElectricityReading;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class ElectricityReadingsGenerator {

    public List<ElectricityReading> generate(int number, double readingVal) {
        return generate(number, readingVal, 10);
    }

    public List<ElectricityReading> generate(int number, double readingVal, int interval) {
        List<ElectricityReading> readings = new ArrayList<>();
        Instant now = Instant.now();

        Random readingRandomiser = new Random();
        for (int i = 0; i < number; i++) {
            double positiveReadingVal = readingVal >=0 ? readingVal: readingRandomiser.nextGaussian();
            BigDecimal reading = BigDecimal.valueOf(positiveReadingVal).setScale(4, RoundingMode.CEILING);
            ElectricityReading electricityReading = new ElectricityReading(now.minusSeconds(i * interval), reading);
            readings.add(electricityReading);
        }

        readings.sort(Comparator.comparing(ElectricityReading::getTime));
        return readings;
    }
}
