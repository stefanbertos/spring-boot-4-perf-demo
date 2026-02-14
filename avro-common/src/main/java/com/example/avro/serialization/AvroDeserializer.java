package com.example.avro.serialization;

import com.example.avro.MqMessage;

import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.kafka.common.serialization.Deserializer;

public class AvroDeserializer implements Deserializer<MqMessage> {

    private final DatumReader<MqMessage> reader = new SpecificDatumReader<>(MqMessage.getClassSchema());

    @Override
    public MqMessage deserialize(String topic, byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        try {
            var decoder = DecoderFactory.get().binaryDecoder(data, null);
            return reader.read(null, decoder);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize Avro message", e);
        }
    }
}
