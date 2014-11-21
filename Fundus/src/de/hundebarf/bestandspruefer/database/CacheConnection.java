package de.hundebarf.bestandspruefer.database;

import java.util.ArrayList;
import java.util.List;

import de.hundebarf.bestandspruefer.collection.Item;

public class CacheConnection implements DatabaseConnection {

	public CacheConnection() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<Item> queryItemList() throws DatabaseException {
		// TODO Auto-generated method stub
		ArrayList<Item> list = new ArrayList<Item>();
		int i = 0;
		list.add(new Item(i++, "test Item", "frostfleisch", "1234"));
		list.add(new Item(i++, "test Item", "frostfleisch", "1234"));
		list.add(new Item(i++, "test Item", "sonstiges", "1234"));
		list.add(new Item(i++, "test Item", "sonstiges", "1234"));
		list.add(new Item(i++, "Pferde Nackensehne test", "sonstiges", "4036503099100"));
		list.add(new Item(i++, "test Item", "sonstiges", "1234"));
		list.add(new Item(i++, "test Item", "sonstiges", "1234"));
		list.add(new Item(i++, "test Item", "sonstiges", "1234"));
		list.add(new Item(i++, "test Item", "blubb", "1234"));
		list.add(new Item(i++, "test Item", "bla", "1234"));
		return list;
	}

	@Override
	public Item queryItem(int itemId) throws DatabaseException {
		// TODO Auto-generated method stub
		Item item = new Item(0, "test Item", "bla", "1234");
		item.description = "null";
		return item;

	}

	@Override
	public void updateQuantity(int itemId, int quantity)
			throws DatabaseException {
		// TODO Auto-generated method stub

	}

	@Override
	public void addItem(Item item) throws DatabaseException {
		// TODO Auto-generated method stub

	}

}
