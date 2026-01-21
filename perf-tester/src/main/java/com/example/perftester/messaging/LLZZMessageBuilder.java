package com.example.perftester.messaging;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.UUID;

/**
 * LLZZ message format builder/parser for legacy mainframe MQ integration.
 *
 * Format:
 * - LL (2 bytes): Total message length (big-endian, includes LLZZ header)
 * - ZZ (2 bytes): Segment indicator (00=complete, 01=first, 02=middle, 03=last)
 * - Application Header (104 bytes): Fixed-position banking header
 * - Payload: Variable length content (SWIFT MT, ISO 20022, etc.)
 */
public class LLZZMessageBuilder {

    private static final int LLZZ_HEADER_SIZE = 4;
    private static final int APP_HEADER_SIZE = 104;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public enum SegmentType {
        COMPLETE((byte) 0x00),
        FIRST((byte) 0x01),
        MIDDLE((byte) 0x02),
        LAST((byte) 0x03);

        private final byte code;

        SegmentType(byte code) {
            this.code = code;
        }

        public byte getCode() {
            return code;
        }

        public static SegmentType fromCode(byte code) {
            for (SegmentType type : values()) {
                if (type.code == code) {
                    return type;
                }
            }
            return COMPLETE;
        }
    }

    public record LLZZMessage(
            int length,
            SegmentType segment,
            ApplicationHeader header,
            byte[] payload
    ) {}

    public record ApplicationHeader(
            String messageType,    // 8 bytes
            String version,        // 4 bytes
            String sourceBic,      // 11 bytes
            String targetBic,      // 11 bytes
            String timestamp,      // 14 bytes
            String sequenceNo,     // 8 bytes
            String transactionId,  // 24 bytes
            String correlationId   // 24 bytes
    ) {
        public static ApplicationHeader create(String messageType, String sourceBic, String targetBic) {
            return new ApplicationHeader(
                    messageType,
                    "V100",
                    sourceBic,
                    targetBic,
                    LocalDateTime.now().format(TIMESTAMP_FORMAT),
                    String.format("%08d", 1),
                    generateId("TXN"),
                    generateId("COR")
            );
        }

        private static String generateId(String prefix) {
            String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 21);
            return prefix + uuid;
        }
    }

    /**
     * Build a complete LLZZ message with application header.
     */
    public byte[] build(ApplicationHeader header, String payload) {
        return build(header, payload.getBytes(StandardCharsets.UTF_8), SegmentType.COMPLETE);
    }

    /**
     * Build a complete LLZZ message with application header and segment type.
     */
    public byte[] build(ApplicationHeader header, byte[] payload, SegmentType segment) {
        int totalSize = LLZZ_HEADER_SIZE + APP_HEADER_SIZE + payload.length;
        ByteBuffer buf = ByteBuffer.allocate(totalSize);
        buf.order(ByteOrder.BIG_ENDIAN);

        // LLZZ header
        buf.putShort((short) totalSize);
        buf.put(segment.getCode());
        buf.put((byte) 0x00);

        // Application header (fixed positions)
        buf.put(fixedBytes(header.messageType(), 8));
        buf.put(fixedBytes(header.version(), 4));
        buf.put(fixedBytes(header.sourceBic(), 11));
        buf.put(fixedBytes(header.targetBic(), 11));
        buf.put(fixedBytes(header.timestamp(), 14));
        buf.put(fixedBytes(header.sequenceNo(), 8));
        buf.put(fixedBytes(header.transactionId(), 24));
        buf.put(fixedBytes(header.correlationId(), 24));

        // Payload
        buf.put(payload);

        return buf.array();
    }

    /**
     * Build a simple LLZZ message (header + payload only, no application header).
     */
    public byte[] buildSimple(byte[] payload) {
        int totalSize = LLZZ_HEADER_SIZE + payload.length;
        ByteBuffer buf = ByteBuffer.allocate(totalSize);
        buf.order(ByteOrder.BIG_ENDIAN);

        buf.putShort((short) totalSize);
        buf.putShort((short) 0);
        buf.put(payload);

        return buf.array();
    }

    /**
     * Parse an LLZZ message.
     */
    public LLZZMessage parse(byte[] raw) {
        ByteBuffer buf = ByteBuffer.wrap(raw);
        buf.order(ByteOrder.BIG_ENDIAN);

        int ll = buf.getShort() & 0xFFFF;
        SegmentType segment = SegmentType.fromCode(buf.get());
        buf.get(); // reserved byte

        ApplicationHeader header = null;
        byte[] payload;

        if (ll > LLZZ_HEADER_SIZE + APP_HEADER_SIZE) {
            // Has application header
            header = new ApplicationHeader(
                    readFixedString(buf, 8),
                    readFixedString(buf, 4),
                    readFixedString(buf, 11),
                    readFixedString(buf, 11),
                    readFixedString(buf, 14),
                    readFixedString(buf, 8),
                    readFixedString(buf, 24),
                    readFixedString(buf, 24)
            );
            payload = new byte[ll - LLZZ_HEADER_SIZE - APP_HEADER_SIZE];
        } else {
            payload = new byte[ll - LLZZ_HEADER_SIZE];
        }

        buf.get(payload);
        return new LLZZMessage(ll, segment, header, payload);
    }

    /**
     * Generate a sample MT103 SWIFT message with LLZZ wrapper.
     */
    public byte[] buildSampleMT103(String sourceBic, String targetBic, String amount, String currency) {
        String mt103 = """
                {1:F01%sXXXX0000000000}{2:I103%sXXXXN}{4:
                :20:%s
                :23B:CRED
                :32A:%s%s%s
                :50K:/BE68539007547034
                JOHN DOE
                123 MAIN STREET
                BRUSSELS
                :59:/DE89370400440532013000
                ACME CORPORATION
                456 BUSINESS AVENUE
                FRANKFURT
                :71A:SHA
                -}""".formatted(
                sourceBic,
                targetBic,
                generateReference(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd")),
                currency,
                amount
        );

        ApplicationHeader header = ApplicationHeader.create("MT103", sourceBic, targetBic);
        return build(header, mt103);
    }

    /**
     * Format message as hex dump for debugging.
     */
    public String toHexDump(byte[] data) {
        StringBuilder sb = new StringBuilder();
        StringBuilder ascii = new StringBuilder();

        for (int i = 0; i < data.length; i++) {
            if (i % 16 == 0) {
                if (i > 0) {
                    sb.append("  |").append(ascii).append("|\n");
                    ascii.setLength(0);
                }
                sb.append(String.format("%08X  ", i));
            }

            sb.append(String.format("%02X ", data[i]));
            ascii.append(isPrintable(data[i]) ? (char) data[i] : '.');
        }

        // Pad last line
        int remaining = 16 - (data.length % 16);
        if (remaining < 16) {
            sb.append("   ".repeat(remaining));
        }
        sb.append("  |").append(ascii).append("|");

        return sb.toString();
    }

    private byte[] fixedBytes(String value, int length) {
        byte[] result = new byte[length];
        Arrays.fill(result, (byte) ' ');
        if (value != null) {
            byte[] src = value.getBytes(StandardCharsets.UTF_8);
            System.arraycopy(src, 0, result, 0, Math.min(src.length, length));
        }
        return result;
    }

    private String readFixedString(ByteBuffer buf, int length) {
        byte[] bytes = new byte[length];
        buf.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8).trim();
    }

    private String generateReference() {
        return "REF" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private boolean isPrintable(byte b) {
        return b >= 32 && b < 127;
    }
}
