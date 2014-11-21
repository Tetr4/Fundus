package de.hundebarf.bestandspruefer.database;

import java.util.List;

import de.hundebarf.bestandspruefer.collection.Item;

public interface DatabaseConnection {
	public List<Item> queryItemList() throws DatabaseException;
	public Item queryItem(int itemId) throws DatabaseException;
	public void updateQuantity(int itemId, int quantity) throws DatabaseException;
	public void addItem(Item item) throws DatabaseException;
}
