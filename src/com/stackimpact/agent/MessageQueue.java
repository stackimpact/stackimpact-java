package com.stackimpact.agent;


import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;


public class MessageQueue {

    public static class Message {
        public String topic;
        public Object content;
        public long addedAt;
    }

    public final static long FLUSH_INTERVAL = 5 * 1000;
    public final static long MESSAGE_TTL = 10 * 60 * 1000;

    private Agent agent;

    private List<Message> queue = (List<Message>)Collections.synchronizedList(new ArrayList<Message>());
    private Timer flushTimer;
    private long backoffSeconds;
    private long lastFlushTS;


    public MessageQueue(Agent agent) {
        this.agent = agent;
    }


    public List<Message> getQueue() {
        return queue;
    }


    public long getBackoffSeconds() {
        return backoffSeconds;
    }


    public void reset() {
        backoffSeconds = 0;
        lastFlushTS = AgentUtils.millis();
        queue.clear();
    }


    public void start() {
        reset();

        flushTimer = new Timer();
        flushTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    flush();
                }
                catch(Exception ex) {
                    agent.logException(ex);
                }
            }
        }, FLUSH_INTERVAL, FLUSH_INTERVAL);
    }


    public void stop() {
        if (flushTimer != null) {
            flushTimer.cancel();
        }
    }


    public void add(String topic, Object content) {
        Message message = new Message();
        message.topic = topic;
        message.content = content;
        message.addedAt = AgentUtils.millis();
        queue.add(message);
    }
    

    public void flush() {
        long now = AgentUtils.millis();

        if (!agent.isAutoProfilingMode() && lastFlushTS > now - FLUSH_INTERVAL) {
          return;
        }

        if (queue.size() == 0) {
            return;
        }

        // flush only if backoff time is elapsed
        if (lastFlushTS + backoffSeconds * 1000 > now) {
          return;
        }

        // expire old messages
        synchronized(queue) {
            Iterator<Message> iter = queue.iterator();
            while (iter.hasNext()) {
                Message message = iter.next();
                if (message.addedAt < now - MESSAGE_TTL) {
                    iter.remove();
                }
            }
        }

        if (queue.size() == 0) {
            return;
        }

        // read queue
        List<Message> outgoing = (List<Message>)Collections.synchronizedList(new ArrayList<Message>(queue));
        queue.clear();

        Map payloadObj = new HashMap();
        List messagesArr = new ArrayList();
        payloadObj.put("messages", messagesArr);

        for (Message message : outgoing) {
            HashMap messageObj = new HashMap();
            messageObj.put("topic", message.topic);
            messageObj.put("content", message.content);
            messagesArr.add(messageObj);
        }

        lastFlushTS = now;

        try {
            agent.getAPIRequest().post("upload", payloadObj);
            backoffSeconds = 0;
        }
        catch(Exception ex) {
            agent.logError("Error uploading messages to the dashboard, backing off next upload");
            agent.logException(ex);

            queue.addAll(0, outgoing);

            // increase backoff up to 1 minute
            if (backoffSeconds == 0) {
              backoffSeconds = 10;
            }
            else if (backoffSeconds * 2 < 60) {
              backoffSeconds *= 2;
            }
        }
    }
}
