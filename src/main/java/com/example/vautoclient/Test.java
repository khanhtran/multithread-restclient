package com.example.vautoclient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.List;
import java.util.ArrayList;

public class Test {

	public static void main(String[] args) {
		ExecutorService es = Executors.newFixedThreadPool(4);
		List<Future<?>> futures = new ArrayList<>();
		List<Runnable> taskList = new ArrayList<>();
		for (Runnable task : taskList) {
			futures.add(es.submit(task));
		}

		for (Future<?> future : futures) {
			try {
				future.get();
			} catch (Exception e) {
				// do logging and nothing else } }
			}
		}
	}
}
