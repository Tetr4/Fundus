package de.hundebarf.bestandspruefer.collection;


public class Item implements Comparable<Item>{
	public int id;
	public String name;
	public String shortName;
	public String barcode;
	public String category;
	public int quantityContent;
	public String unitContent;
	public String size;
	public String description;
	public int price;
	public int quanityPackage;
	public String unitPackage;
	public String taxGroup;
	public String buyingPrice;
	public String supplier;
	public int stock;
	
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

	public Item() {
	}

	@Override
	public int compareTo(Item another) {
		return name.compareTo(another.name);
	}
	
//    ID, barcode, name, kurzname, warengruppe, menge(inhalt),
//    einheit(inhalt),groeße,beschreibung,verkaufspreis,
//    menge(verpackung),einheit(verpackung),steuergruppe,
//    einkaufspreis,lieferant,artikelnummer,[idealbestand,warnbestand]
	
}
