package com.example.vautoclient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.web.client.RestTemplate;

/**
 * 
 * @author Khanh
 *
 */
public class VAutoClient {
	private String baseUrl;
	private RestTemplate restTemplate;

	private static final String API_GET_DATASET = "/api/datasetId";
	private static final String API_ANSWER = "/api/{datasetId}/answer";
	private static final String API_GET_VEHICLE_IDS = "/api/{datasetId}/vehicles";
	private static final String API_GET_VEHICLE = "/api/{datasetId}/vehicles/{vehicleid}";
	private static final String API_GET_DEALER = "/api/{datasetId}/dealers/{dealerid}";

	public VAutoClient(RestTemplate restTemplate, String baseUrl) {
		this.restTemplate = restTemplate;
		this.baseUrl = baseUrl;
	}

	public void process() {
		System.out.println("Building data...");
		long t = System.currentTimeMillis();
		Dataset dataset = getDataset();
		Set<Integer> vehicleIds = getVehicleIDs(dataset.getDatasetId());
		final Map<Integer, List<Vehicle>> dealerVehiclesMap = new ConcurrentHashMap<>();

		ExecutorService threadPool = Executors.newFixedThreadPool(vehicleIds.size());
		final CountDownLatch latch = new CountDownLatch(vehicleIds.size());
		for (int vehicleId : vehicleIds) {
			threadPool.submit(() -> {
				Vehicle vehicle = getVehicle(dataset.getDatasetId(), vehicleId);
				Integer dealerId = vehicle.getDealerId();
				if (dealerVehiclesMap.containsKey(dealerId)) {
					dealerVehiclesMap.get(dealerId).add(vehicle);
				} else {
					List<Vehicle> lst = new ArrayList<>();
					lst.add(vehicle);
					dealerVehiclesMap.put(dealerId, lst);
				}
				latch.countDown();
			});
		}
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		threadPool.shutdown();
		/* all threads had completed */
		ExecutorService threadPool2 = Executors.newFixedThreadPool(dealerVehiclesMap.size());
		final CountDownLatch latch2 = new CountDownLatch(dealerVehiclesMap.size());
		List<Dealer> dealers = new CopyOnWriteArrayList<>();

		for (int dealerId : dealerVehiclesMap.keySet()) {
			threadPool2.submit(() -> {
				Dealer dealer = getDealer(dataset.getDatasetId(), dealerId);
				dealer.setVehicles(dealerVehiclesMap.get(dealerId));
				dealers.add(dealer);
				latch2.countDown();
			});
		}
		
		try {
			latch2.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		threadPool2.shutdown();
		long t1 = System.currentTimeMillis();
		System.out.println("Building data: " + (t1 - t));
		postAnswer(dataset.getDatasetId(), dealers);
		long t2 = System.currentTimeMillis();
		System.out.println("Posting answer: " + (t2 - t1));
	}

	private void postAnswer(String datasetId, List<Dealer> dealers) {
		Map<String, List<Dealer>> answer = new HashMap<>();
		answer.put("dealers", dealers);
		String result = restTemplate.postForObject(baseUrl + API_ANSWER, answer, String.class, datasetId);
		System.out.println("result: " + result);
	}

	private Dealer getDealer(String datasetId, int dealerId) {
		long t1 = System.currentTimeMillis();
		Dealer dealer = restTemplate.getForObject(baseUrl + API_GET_DEALER, Dealer.class, datasetId, dealerId);
		long t2 = System.currentTimeMillis();
		System.out.println("\tgetDealer: " + (t2 - t1));
		return dealer;
	}

	private Vehicle getVehicle(String datasetId, int vehicleId) {
		long t1 = System.currentTimeMillis();
		Vehicle vehicle = restTemplate.getForObject(baseUrl + API_GET_VEHICLE, Vehicle.class, datasetId, vehicleId);
		long t2 = System.currentTimeMillis();
		System.out.println("\tgetVehicle: " + (t2 - t1));
		return vehicle;
	}

	@SuppressWarnings("unchecked")
	private Set<Integer> getVehicleIDs(String datasetId) {
		HashMap<String, Collection<Integer>> map = restTemplate.getForObject(baseUrl + API_GET_VEHICLE_IDS,
				HashMap.class, datasetId);
		Collection<Integer> ids = (Collection<Integer>) map.get("vehicleIds");
		Set<Integer> result = new HashSet<>();
		result.addAll(ids);
		return result;
	}

	private Dataset getDataset() {
		return this.restTemplate.getForObject(baseUrl + API_GET_DATASET, Dataset.class);
	}
}
