package com.cleo.amazonaws.services.iot.client;

import com.amazonaws.services.iot.client.AWSIotMqttClient;
import com.amazonaws.services.iot.client.core.AbstractAwsIotClient;
import com.amazonaws.services.iot.client.core.AwsIotConnectionType;
import com.amazonaws.services.iot.client.core.AwsIotRuntimeException;
import com.amazonaws.services.iot.client.mqtt.AwsIotMqttConnection;

import java.lang.reflect.Field;
import java.util.regex.Pattern;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AwsIotMqttClientBuilder {

    private static final String EndpointPattern = "iot\\.([\\w-]+)\\.amazonaws\\.com";

    @Builder(builderMethodName = "newClient")
    public static AWSIotMqttClient newAWSIotMqttClient(String clientEndpoint,
                                                       String clientId,
                                                       String awsAccessKeyId,
                                                       String awsSecretAccessKey,
                                                       String sessionToken) {

        final boolean awsIotEndpoint = !clientEndpoint.contains("://");
        final String clientEndpointToUse = awsIotEndpoint ? clientEndpoint : "foobar.iot.us-west-2.amazonaws.com";

        AWSIotMqttClient client = new AWSIotMqttClient(clientEndpointToUse, clientId, awsAccessKeyId, awsSecretAccessKey, sessionToken);
        if (!awsIotEndpoint) {
            log.info("Using direct websocket connection override.");
            try {
                AwsIotMqttConnection connection = new AwsIotMqttConnection(client, null, clientEndpoint);
                Field field = AbstractAwsIotClient.class.getDeclaredField("connection");
                field.setAccessible(true);
                field.set(client, connection);

                field = AbstractAwsIotClient.class.getDeclaredField("connectionType");
                field.setAccessible(true);
                field.set(client, AwsIotConnectionType.MQTT_OVER_TLS);
            } catch (Exception e) {
                throw new AwsIotRuntimeException(e);
            }
        }

        return client;
    }
}
