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
		Item item = new Item(0, "test Item", "bla", "1234");
		list.add(item);
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
