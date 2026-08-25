package com.aaax.core.utils;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class CsvUtil {
    public static byte[] generateCsvAndDownload(List<String[]> data, String filename) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(0xEF);
        outputStream.write(0xBB);
        outputStream.write(0xBF);
        try (CSVPrinter csvPrinter = new CSVPrinter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8), CSVFormat.DEFAULT)) {
            for (String[] row : data) {
                csvPrinter.printRecord((Object[]) row);
            }
            csvPrinter.flush();
        }
        return outputStream.toByteArray();
    }
}
