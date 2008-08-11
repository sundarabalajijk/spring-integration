/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.DefaultChannelRegistry;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.router.SplitterMessageHandlerAdapter;

/**
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public class CorrelationIdTests {

	@Test
	public void testCorrelationIdPassedIfAvailable() {
		Object correlationId = "123-ABC";
		Message<String> message = MessageBuilder.fromPayload("test")
				.setCorrelationId(correlationId).build();
		DefaultMessageHandlerAdapter adapter = new DefaultMessageHandlerAdapter();
		adapter.setObject(new TestBean());
		adapter.setMethodName("upperCase");
		adapter.afterPropertiesSet();
		Message<?> reply = adapter.handle(message);
		assertEquals(correlationId, reply.getHeaders().getCorrelationId());
	}

	@Test
	public void testCorrelationIdCopiedFromMessageIdByDefault() {
		Message<String> message = MessageBuilder.fromPayload("test").build();
		DefaultMessageHandlerAdapter adapter = new DefaultMessageHandlerAdapter();
		adapter.setObject(new TestBean());
		adapter.setMethodName("upperCase");
		adapter.afterPropertiesSet();
		Message<?> reply = adapter.handle(message);
		assertEquals(message.getHeaders().getId(), reply.getHeaders().getCorrelationId());
	}

	@Test
	public void testCorrelationIdCopiedFromMessageCorrelationIdIfAvailable() {
		Message<String> message = MessageBuilder.fromPayload("test")
				.setCorrelationId("correlationId").build();
		DefaultMessageHandlerAdapter adapter = new DefaultMessageHandlerAdapter();
		adapter.setObject(new TestBean());
		adapter.setMethodName("upperCase");
		adapter.afterPropertiesSet();
		Message<?> reply = adapter.handle(message);
		assertEquals(message.getHeaders().getCorrelationId(), reply.getHeaders().getCorrelationId());
		assertTrue(message.getHeaders().getCorrelationId().equals(reply.getHeaders().getCorrelationId()));
	}

	@Test
	public void testCorrelationNotPassedIfAlreadySetByHandler() throws Exception {
		Object correlationId = "123-ABC";
		Message<String> message = MessageBuilder.fromPayload("test")
				.setCorrelationId(correlationId).build();
		AbstractMessageHandlerAdapter adapter = new AbstractMessageHandlerAdapter() {
			@Override
            protected Message<?> handleReturnValue(Object returnValue, Message<?> originalMessage) {
				Message<?> resultMessage = this.createReplyMessage(returnValue, originalMessage);
				return MessageBuilder.fromMessage(resultMessage)
						.setCorrelationId("456-XYZ").build();
            }
		};
		adapter.setObject(new TestBean());
		adapter.setMethodName("upperCase");
		adapter.afterPropertiesSet();
		Message<?> reply = adapter.handle(message);
		assertEquals("456-XYZ", reply.getHeaders().getCorrelationId());
	}

	@Test
	public void testCorrelationNotCopiedIfAlreadySetByHandler() throws Exception {
		Message<?> message = new StringMessage("test");
		AbstractMessageHandlerAdapter adapter = new AbstractMessageHandlerAdapter() {
			@Override
            protected Message<?> handleReturnValue(Object returnValue, Message<?> originalMessage) {
				Message<?> resultMessage = this.createReplyMessage(returnValue, originalMessage);
				return MessageBuilder.fromMessage(resultMessage).setCorrelationId("456-XYZ").build();
            }
		};
		adapter.setObject(new TestBean());
		adapter.setMethodName("upperCase");
		adapter.afterPropertiesSet();
		Message<?> reply = adapter.handle(message);
		assertEquals("456-XYZ", reply.getHeaders().getCorrelationId());
	}

	@Test
	public void testCorrelationIdWithSplitter() throws Exception {
		Message<?> message = new StringMessage("test1,test2");
		DefaultMessageHandlerAdapter adapter = new DefaultMessageHandlerAdapter();
		adapter.setObject(new TestBean());
		adapter.setMethodName("upperCase");
		adapter.afterPropertiesSet();
		QueueChannel testChannel = new QueueChannel();
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel("testChannel", testChannel);
		SplitterMessageHandlerAdapter splitter = new SplitterMessageHandlerAdapter(
				new TestBean(), TestBean.class.getMethod("split", String.class));
		splitter.setOutputChannelName("testChannel");
		splitter.setChannelRegistry(channelRegistry);
		splitter.afterPropertiesSet();
		splitter.handle(message);
		Message<?> reply1 = testChannel.receive(100);
		Message<?> reply2 = testChannel.receive(100);
		assertEquals(message.getHeaders().getId(), reply1.getHeaders().getCorrelationId());
		assertEquals(message.getHeaders().getId(), reply2.getHeaders().getCorrelationId());		
	}


	private static class TestBean {

		public String upperCase(String input) {
			return input.toUpperCase();
		}

		public String[] split(String input) {
			return input.split(",");
		}
	}

}
