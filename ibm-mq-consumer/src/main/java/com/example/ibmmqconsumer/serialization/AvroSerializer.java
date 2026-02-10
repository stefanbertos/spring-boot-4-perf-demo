package com.example.ibmmqconsumer.serialization;

import java.io.ByteArrayOutputStream;

import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.serialization.Serializer;

public class AvroSerializer<T extends SpecificRecordBase> implements Serializer<T> {

    @Override
    public byte[] serialize(String topic, T data) {
        if (data == null) {
            return new byte[0];
        }
        try (var outputStream = new ByteArrayOutputStream()) {
            DatumWriter<T> writer = new SpecificDatumWriter<>(data.getSchema());
            var encoder = EncoderFactory.get().binaryEncoder(outputStream, null);
            writer.write(data, encoder);
            encoder.flush();
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize Avro message", e);
        }
    }
}
