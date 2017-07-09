package de.hundebarf.fundus.model;

import com.google.gson.annotations.SerializedName;

public class Item implements Comparable<Item> {

	@SerializedName("ID")
	public int id;

	@SerializedName("name")
	public String name;

	@SerializedName("kurzname")
	public String shortName;

	@SerializedName("barcode")
	public String barcode;

	@SerializedName("warengruppe")
	public String category;

	@SerializedName("menge(inhalt)")
	public int quantityContent;

	@SerializedName("einheit(inhalt)")
	public String unitContent;

	@SerializedName("groeße")
	public String size;

	@SerializedName("beschreibung")
	public String description;

	@SerializedName("verkaufspreis")
	public int price;

	@SerializedName("menge(verpackung)")
	public int quanityPackage;

	@SerializedName("einheit(verpackung)")
	public String unitPackage;

	@SerializedName("steuergruppe")
	public String taxGroup;

	@SerializedName("einkaufspreis")
	public int buyingPrice;

	@SerializedName("lieferant")
	public String supplier;

	@SerializedName("menge")
	public int stock;

	public Item() {

	}

	public Item(int id, String name) {
		this.id = id;
		this.name = name;
	}

	public Item(int id, String name, String category) {
		this(id, name);
		this.category = category;
	}

	public Item(int id, String name, String category, String barcode) {
		this(id, name);
		this.category = category;
	}

	@Override
	public int compareTo(Item another) {
		// sort by name
		return name.compareTo(another.name);
	}

	public boolean equals(Item another) {
		return id == another.id;
	}

	@Override
	public String toString() {
		if (name != null) {
			return name;
		} else {
			return super.toString();
		}
	}
}
