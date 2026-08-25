package com.aaax.core.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

@Slf4j
@Component
public class QrCodeUtil {
    public static final int SMALL_SIZE = 100;
    public static final int MEDIUM_SIZE = 200;
    public static final int LARGE_SIZE = 500;
    public static final int HUGE_SIZE = 1000;

    private static final int DEFAULT_FOREGROUND_COLOR = 0xFFFFFFFF;
    private static final int DEFAULT_BACKGROUND_COLOR = 0x00000000;

    public static byte[] generateCustomizeSize(String input, int width, int height) throws WriterException, IOException {
        return generate(input, width, height, DEFAULT_FOREGROUND_COLOR, DEFAULT_BACKGROUND_COLOR);
    }

    /**
     * @param input           text to QR code
     * @param foregroundColor code color (Hex color code)
     * @param backgroundColor background color (Hex color code)
     * @return return QR code png image
     */
    public static byte[] generateCustomizeColor(String input, int foregroundColor, int backgroundColor) throws WriterException, IOException {
        return generate(input, MEDIUM_SIZE, MEDIUM_SIZE, foregroundColor, backgroundColor);
    }

    public static byte[] generate(String input, int size) throws WriterException, IOException {
        return generate(input, size, size, DEFAULT_FOREGROUND_COLOR, MEDIUM_SIZE);
    }

    public static byte[] generate(String input) throws WriterException, IOException {
        return generate(input, MEDIUM_SIZE, MEDIUM_SIZE, DEFAULT_FOREGROUND_COLOR, MEDIUM_SIZE);
    }

    public static byte[] generate(String input, int width, int height, int foregroundColor, int backgroundColor) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(input, BarcodeFormat.QR_CODE, width, height);

        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageConfig con = new MatrixToImageConfig(foregroundColor, backgroundColor);

        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream, con);
        byte[] pngData = pngOutputStream.toByteArray();
        return pngData;
    }

    public static String generateCustomizeSizeToBase64(String input, int width, int height) throws WriterException, IOException {
        return toBase64(generateCustomizeSize(input, width, height));
    }

    public static String generateCustomizeColorToBase64(String input, int foregroundColor, int backgroundColor) throws IOException, WriterException {
        return toBase64(generateCustomizeColor(input, foregroundColor, backgroundColor));
    }

    public static String generateToBase64(String input, int size) throws WriterException, IOException {
        return toBase64(generate(input, size));
    }

    public static String generateToBase64(String input) throws WriterException, IOException {
        return toBase64(generate(input));
    }

    public static String generateToBase64(String input, int width, int height, int foregroundColor, int backgroundColor) throws WriterException, IOException {
        return toBase64(generate(input, width, height, foregroundColor, backgroundColor));
    }

    private static String toBase64(byte[] bytes) {
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
    }
}
