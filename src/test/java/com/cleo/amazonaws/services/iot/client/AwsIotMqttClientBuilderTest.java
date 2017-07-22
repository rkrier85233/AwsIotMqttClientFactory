package com.cleo.amazonaws.services.iot.client;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotMqttClient;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.amazonaws.services.iot.client.AWSIotTopic;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.amazonaws.services.securitytoken.model.GetSessionTokenResult;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AwsIotMqttClientBuilderTest {

    @Test
    public void awsClientTest() throws Exception {
        AWSSecurityTokenServiceClientBuilder builder = AWSSecurityTokenServiceClientBuilder.standard();
        builder.setCredentials(DefaultAWSCredentialsProviderChain.getInstance());
        builder.setRegion("us-west-2");
        GetSessionTokenResult sessionTokenResult = builder.build().getSessionToken();
        Credentials credentials = sessionTokenResult.getCredentials();

        final String endpoint = "a2p50suclsf82w.iot.us-west-2.amazonaws.com";
        final String clientId = UUID.randomUUID().toString();
        AWSIotMqttClient client = AwsIotMqttClientBuilder.newClient()
                .clientEndpoint(endpoint)
                .clientId(clientId)
                .awsAccessKeyId(credentials.getAccessKeyId())
                .awsSecretAccessKey(credentials.getSecretAccessKey())
                .sessionToken(credentials.getSessionToken())
                .build();

        client.connect();
        final int numMessages = 5;
        final CountDownLatch latch = new CountDownLatch(numMessages);
        final String subscribeTopic = "some/topic/#";
        final List<AWSIotMessage> messages = new ArrayList<>();
        final AtomicBoolean failed = new AtomicBoolean(false);
        client.subscribe(new AWSIotTopic(subscribeTopic, AWSIotQos.QOS0) {
            @Override
            public void onFailure() {
                super.onFailure();
                failed.set(true);
                while (latch.getCount() > 0) {
                    latch.countDown();
                }
            }

            @Override
            public void onMessage(AWSIotMessage message) {
                super.onMessage(message);
                messages.add(message);
                latch.countDown();
            }
        });


        for (int i = 1; i <= numMessages; i++) {
            String publishTopic = "some/topic/message-" + i;
            client.publish(new AWSIotMessage(publishTopic, AWSIotQos.QOS0, "This is message number: " + i));
            Thread.sleep(100);
        }
        latch.await(10, TimeUnit.SECONDS);


        assertFalse(failed.get());
        assertTrue(messages.size() == numMessages);
        for (int i = 1; i <= numMessages; i++) {
            AWSIotMessage message = messages.get(i-1);
            assertEquals("some/topic/message-" + i, message.getTopic());
            assertEquals("This is message number: " + i, message.getStringPayload());
        }
    }

    @Test
    public void activeMqClientTest() throws Exception {
        final String endpoint = "ws://localhost:61614";
        final String clientId = UUID.randomUUID().toString();
        AWSIotMqttClient client = AwsIotMqttClientBuilder.newClient()
                .clientEndpoint(endpoint)
                .clientId(clientId)
                .awsAccessKeyId("BogusAccessKeyId")
                .awsSecretAccessKey("BogusSecretAccessKey")
                .sessionToken("BogusSessionToken")
                .build();

        client.connect();
        final int numMessages = 5;
        final CountDownLatch latch = new CountDownLatch(numMessages);
        final String subscribeTopic = "some/topic/#";
        final List<AWSIotMessage> messages = new ArrayList<>();
        final AtomicBoolean failed = new AtomicBoolean(false);
        client.subscribe(new AWSIotTopic(subscribeTopic, AWSIotQos.QOS0) {
            @Override
            public void onFailure() {
                super.onFailure();
                failed.set(true);
                while (latch.getCount() > 0) {
                    latch.countDown();
                }
            }

            @Override
            public void onMessage(AWSIotMessage message) {
                super.onMessage(message);
                messages.add(message);
                latch.countDown();
            }
        });


        for (int i = 1; i <= numMessages; i++) {
            String publishTopic = "some/topic/message-" + i;
            client.publish(new AWSIotMessage(publishTopic, AWSIotQos.QOS0, "This is message number: " + i));
            Thread.sleep(100);
        }
        latch.await(10, TimeUnit.SECONDS);


        assertFalse(failed.get());
        assertTrue(messages.size() == numMessages);
        for (int i = 1; i <= numMessages; i++) {
            AWSIotMessage message = messages.get(i-1);
            assertEquals("some/topic/message-" + i, message.getTopic());
            assertEquals("This is message number: " + i, message.getStringPayload());
        }
    }
}