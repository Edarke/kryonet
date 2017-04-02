/* Copyright (c) 2008, Nathan Sweet
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * - Neither the name of Esoteric Software nor the names of its contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package com.esotericsoftware.kryonet.network;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.adapters.ConnectionAdapter;
import com.esotericsoftware.kryonet.network.impl.Client;
import com.esotericsoftware.kryonet.network.impl.Server;
import com.esotericsoftware.minlog.Log;
import com.esotericsoftware.minlog.Log.Logger;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ThreadLocalRandom;
import junit.framework.TestCase;
import net.jodah.concurrentunit.Waiter;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.Before;

abstract public class KryoNetTestCase extends TestCase {
	public static final String host = "localhost";


	public int tcpPort = ThreadLocalRandom.current().nextInt(10_000, 30_000);
	public int udpPort = ThreadLocalRandom.current().nextInt(30_000, 50_000);


	protected Server server = new Server(Short.MAX_VALUE,Short.MAX_VALUE);

	protected Client client = new Client(Short.MAX_VALUE, Short.MAX_VALUE);

	protected volatile ClientConnection clientRef;




	private ArrayList<Thread> threads = new ArrayList<>();
	ArrayList<EndPoint> endPoints = new ArrayList<>();
	private Timer timer = new Timer();
	boolean fail;

	protected Waiter test = new Waiter();

	public KryoNetTestCase () {
		// Log.TRACE();
		// Log.DEBUG();
		Log.setLogger(new Logger() {
			public void log (int level, String category, String message, Throwable ex) {
				// if (category == null || category.equals("kryonet")) //
				super.log(level, category, message, ex);
			}
		});

		Log.INFO();
	}


	protected void reg(Kryo k, Class<?>... types){
		for(Class<?> c : types){
			k.register(c);
		}
	}
	protected void reg(Kryo k, Kryo k2, Class<?>... types){
		for(Class<?> c : types){
			k.register(c);
			k2.register(c);
		}
	}


	@Before
	@Override
	protected void setUp () throws Exception {
		System.err.println("---- " + getClass().getSimpleName());
		timer = new Timer();

		server.addListener(new ConnectionAdapter<ClientConnection>() {
			@Override
			public void onConnected(ClientConnection connection) {
				System.err.println("initializing clientref");
				clientRef = connection;
			}
		});
	}

	@After
	@Override
	protected void tearDown () throws Exception {
		stopEndPoints();
		Log.INFO();
		timer.cancel();
	}

	public void startEndPoint (EndPoint endPoint) {
		endPoints.add(endPoint);
		Thread thread = new Thread(endPoint, endPoint.getClass().getSimpleName());
		thread.setUncaughtExceptionHandler((t,e) -> {
			e.printStackTrace();
			test.fail(e);
		});
		threads.add(thread);
		thread.start();
	}

	public void stopEndPoints () {
		for (EndPoint endPoint : endPoints)
			endPoint.stop();
		endPoints.clear();
		this.server.stop();
		this.client.stop();
	}

	public void stopEndPoints (int stopAfterMillis) {
		timer.schedule(new TimerTask() {
			public void run () {
				stopEndPoints();
			}
		}, stopAfterMillis);
	}

	public void waitForThreads (int stopAfterMillis) {
		if (stopAfterMillis > 10000) throw new IllegalArgumentException("stopAfterMillis must be < 10000");
		stopEndPoints(stopAfterMillis);
		waitForThreads();
	}

	public void waitForThreads () {
		fail = false;
		TimerTask failTask = new TimerTask() {
			public void run () {
				stopEndPoints();
				fail = true;
			}
		};
		timer.schedule(failTask, 11000);
		while (true) {
			for (Iterator iter = threads.iterator(); iter.hasNext();) {
				Thread thread = (Thread)iter.next();
				if (!thread.isAlive()) iter.remove();
			}
			if (threads.isEmpty()) break;
			try {
				Thread.sleep(100);
			} catch (InterruptedException ignored) {
			}
		}
		failTask.cancel();
		if (fail) fail("Test did not complete in a timely manner.");
		// Give sockets a chance to close before starting the next test.
		try {
			Thread.sleep(1000);
		} catch (InterruptedException ignored) {
		}
	}


	public void start(AbstractClient... clients){
		for(AbstractClient client : clients) {
			startEndPoint(client);
			try {
				client.connect(1000, host, tcpPort, udpPort);
			} catch (IOException e) {
				test.fail(e);
				e.printStackTrace();
			}
		}
	}


	public void start(Server server, AbstractClient... clients) {
		startEndPoint(server);
		try {
			server.bind(tcpPort, udpPort);
		} catch (IOException e) {
			e.printStackTrace();
		}

		start(clients);
	}


	public static void sleep(int time){
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}
	}


	public static class Instance extends BaseMatcher<Object> {

		private final Class<?> expectedType;
		public Instance(Class<?> expectedType){
			this.expectedType = expectedType;
		}


		public static Instance Of(Class<?> type){
			return new Instance(type);
		}

		@Override
		public boolean matches(Object item) {
			return expectedType.isInstance(item);
		}

		@Override
		public void describeTo(Description description) {

		}
	}
}
