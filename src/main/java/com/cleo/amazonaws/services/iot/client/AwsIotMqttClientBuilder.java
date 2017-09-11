package com.cleo.amazonaws.services.iot.client;

import com.amazonaws.services.iot.client.AWSIotMqttClient;
import com.amazonaws.services.iot.client.core.AbstractAwsIotClient;
import com.amazonaws.services.iot.client.core.AwsIotConnectionType;
import com.amazonaws.services.iot.client.core.AwsIotRuntimeException;
import com.amazonaws.services.iot.client.mqtt.AwsIotMqttConnection;

import java.lang.reflect.Field;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AwsIotMqttClientBuilder {

    // No instances allowed.
    private AwsIotMqttClientBuilder() {
    }

    @Builder
    private static AWSIotMqttClient newAWSIotMqttClient(String clientEndpoint,
                                                       String clientId,
                                                       String awsAccessKeyId,
                                                       String awsSecretAccessKey,
                                                       String sessionToken) {

        final boolean awsIotEndpoint = !clientEndpoint.contains("://");
        final String clientEndpointToUse = awsIotEndpoint ? clientEndpoint : "foobar.iot.us-west-2.amazonaws.com";

        final AWSIotMqttClient client = new AWSIotMqttClient(clientEndpointToUse, clientId, awsAccessKeyId, awsSecretAccessKey, sessionToken);
        final AwsIotMqttConnection connection;
        try {
            if (awsIotEndpoint) {
                Field field = AbstractAwsIotClient.class.getDeclaredField("connection");
                field.setAccessible(true);
                connection = (AwsIotMqttConnection) field.get(client);
                field = AwsIotMqttConnection.class.getDeclaredField("socketFactory");
                field.setAccessible(true);
                field.set(connection, new ProxyAwareSSLSocketFactory());
            } else {
                connection = new AwsIotMqttConnection(client, new ProxyAwareSSLSocketFactory(), clientEndpoint);
            }
            Field field = AbstractAwsIotClient.class.getDeclaredField("connection");
            field.setAccessible(true);
            field.set(client, connection);

            final AwsIotConnectionType connectionType = clientEndpoint.startsWith("wss") ? AwsIotConnectionType.MQTT_OVER_TLS : AwsIotConnectionType.MQTT_OVER_WEBSOCKET;
            field = AbstractAwsIotClient.class.getDeclaredField("connectionType");
            field.setAccessible(true);
            field.set(client, connectionType);
        } catch (Exception e) {
            throw new AwsIotRuntimeException(e);
        }

        return client;
    }
}
