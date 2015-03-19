package de.hundebarf.fundus.collection;

import java.util.ArrayList;

public class Category extends ArrayList<Item> implements Comparable<Category> {
	private static final long serialVersionUID = -6498036279736276579L;
	private String mName;
	
	public Category(String name) {
		mName = name;
	}

	public String getName() {
		return mName;
	}

	public void setName(String name) {
		mName = name;
	}

	@Override
	public int compareTo(Category another) {
		return mName.compareTo(another.getName());
	}
	
}
