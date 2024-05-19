package org.example;

import com.rabbitmq.client.*;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

public class RabbitMQConnector {
    private final Channel _channel;
    private String _queueName;

    private static class CustomConsumer extends DefaultConsumer {
        private String _storedMessage;
        private Channel _channel;

        public CustomConsumer(Channel channelRecv) {
            super(channelRecv);
            _channel = channelRecv;
        }

        public String getStoredMessage() {
            return _storedMessage;
        }

        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
            long deliveryTag = envelope.getDeliveryTag();
            String message = new String(body, StandardCharsets.UTF_8);
            this._storedMessage = message;
            System.out.println(" [x] Received '" + _storedMessage + "'");
            _channel.basicAck(deliveryTag, false);
        }
    }

    public RabbitMQConnector(Connection connection, String queueName) {
        try {
            _channel = connection.createChannel();
            _queueName = queueName;
//            boolean durable = true;
            boolean durable = false;
            _channel.queueDeclare(queueName, durable, false, false, null);
        }
        catch (IOException e) {
            System.out.println("Oops! Error occurred while creating new Channel for Connection to RabbitMQ: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void publishMessageToQueue(String message) {
        try {
            _channel.basicPublish("", _queueName, null, message.getBytes());
//            _channel.basicPublish("", queueName, MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes());
        }
        catch (IOException e) {
            System.out.println("Oops! Error occurred while trying to publish to " + _queueName + ": " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public String consumeMessageFromQueue() {
        try {
            CustomConsumer consumer = new CustomConsumer(_channel);
            boolean autoAck = false;
            String consumerTag = _channel.basicConsume(_queueName, autoAck, consumer);
            _channel.basicCancel(consumerTag);
            return consumer.getStoredMessage();
        }
        catch (IOException e) {
            System.out.println("Oops! Error occurred while trying to consume message from " + _queueName + ": " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public String getMessageFromQueue() {
        try {
            boolean autoAck = false;
            GetResponse response = _channel.basicGet(_queueName, autoAck);
            if (response == null) {
                return "";
            }
            else {
                AMQP.BasicProperties props = response.getProps();
                byte[] body = response.getBody();
                long deliveryTag = response.getEnvelope().getDeliveryTag();
                _channel.basicAck(deliveryTag, false);
                return new String(body, StandardCharsets.UTF_8);
            }
        }
        catch (IOException e) {
            System.out.println("Oops! Error occurred while trying to get message from " + _queueName + ": " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void closeChannel() {
        try {
            _channel.close();
        }
        catch (IOException | TimeoutException e) {
            System.out.println("Oops! Error occurred while trying to close channel: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
