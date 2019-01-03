package com.example.vautoclient;

/**
 * 
 * @author Khanh
 *
 */
public class Dataset {
	private String datasetId;

	public Dataset() {
		
	}
	
	public String getDatasetId() {
		return datasetId;
	}

	public void setDatasetId(String datasetId) {
		this.datasetId = datasetId;
	}

	@Override
	public String toString() {
		return "Dataset [datasetId=" + datasetId + "]";
	}
}
