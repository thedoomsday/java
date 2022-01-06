package application;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import shared.MessageStructure;

public class ConcurrentMessageStore {
	
	private Queue<MessageStructure> messages = new LinkedList<>();
    private static final int QUEUE_CAPACITY = 10000;
    private ReentrantLock lock = new ReentrantLock();
    private Condition hasMessages = lock.newCondition();
    private Condition hasCapacity = lock.newCondition();
    
    public void push(MessageStructure item) throws InterruptedException {
        try {
        	lock.lockInterruptibly();
            while (messages.size() == QUEUE_CAPACITY) {
            	System.out.println("Manager Thread ID " + Thread.currentThread().getId() + "(main thread) waiting throttled because publish store is full.");
            	hasCapacity.await();
            }
            messages.add(item);
            hasMessages.signal();
        } finally {
            lock.unlock();
        }

    }

    public MessageStructure pop() throws InterruptedException {
        try {
            lock.lockInterruptibly();
            while (messages.size() == 0) {
            	System.out.println("Publisher Thread ID " + Thread.currentThread().getId() + " waiting for messages to publish.");
            	hasMessages.await();
            }
            return messages.remove();
        } finally {
        	hasCapacity.signal();
            lock.unlock();
        }
    }
    
    public int size() {
    	try {
    		lock.lock();
    		return messages.size();
    	} finally {
    		lock.unlock();
    	}
    }
	
}
