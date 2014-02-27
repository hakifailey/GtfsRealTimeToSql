package com.transitfeeds.gtfsrealtimetosql;

import java.util.ArrayList;
import java.util.List;

public class DataCopierRow {

	private List<String> mValues = new ArrayList<String>();

	public void add(String value) {
		mValues.add(value);
	}
	
	public void add(int value) {
		mValues.add(Integer.toString(value));
	}

	public void add(long value) {
		mValues.add(Long.toString(value));
	}

    public void add(float value) {
        mValues.add(Float.toString(value));
    }

    public void add(double value) {
        mValues.add(Double.toString(value));
    }

    public void addNull() {
		add("");
	}
	
	public void addNull(int count) {
		for (int i = 1 ; i <= count; i++) {
			addNull();
		}
	}

	public byte[] getBytes(String separator) {
		return getString(separator).getBytes();
	}
	
	public String getString(String separator) {
		return toString(mValues, separator);
	}

	private String toString(List<String> parts, String separator) {
		String ret = "";
		
		int i = 0;
		for (String part : parts) {
			if (i++ > 0) {
				ret += separator;
			}
			
			ret += part;
		}
		return ret + "\n";
	}
}