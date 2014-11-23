package de.hundebarf.bestandspruefer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.ActionBar;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.Toast;
import de.hundebarf.bestandspruefer.collection.Category;
import de.hundebarf.bestandspruefer.collection.Item;
import de.hundebarf.bestandspruefer.database.CacheConnection;
import de.hundebarf.bestandspruefer.database.DatabaseConnection;
import de.hundebarf.bestandspruefer.database.DatabaseException;
import de.hundebarf.bestandspruefer.database.RemoteConnection;
import de.hundebarf.bestandspruefer.database.tasks.DatabaseConnectionTask;
import de.hundebarf.bestandspruefer.scanner.Decoder.OnDecodedCallback;
import de.hundebarf.bestandspruefer.scanner.ScannerFragment;

public class ItemSelectActivity extends Activity {
	public static final String TAG = ItemSelectActivity.class.getSimpleName();

	// Barcode Scanner
	private ScannerFragment mScannerFragment;
	private ImageButton mScannerButton;

	// Item list
	private SearchView mSearchView;
	private ExpandableListView mExpandableListView;
	private ExpandableItemListAdapter mListAdapter;
	private List<Category> mCategories = new ArrayList<Category>();
	private Map<String, Integer> mBarcodeToID = new HashMap<String, Integer>();

	// data loader
	private DatabaseConnectionTask<List<Item>> mListTask;
	private DatabaseConnection mCacheConnection;
	private DatabaseConnection mRemoteConnection;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_item_select);
		
		mCacheConnection = new CacheConnection();
		mRemoteConnection= new RemoteConnection(this);

		initExpandableListView();
		loadItemsAsync();
		initSearchView();
		initScanner();
	}

	private void initExpandableListView() {
		mExpandableListView = (ExpandableListView) findViewById(R.id.expandable_list_view);
		mListAdapter = new ExpandableItemListAdapter(this, mCategories);
		mExpandableListView.setAdapter(mListAdapter);
		mExpandableListView.setOnChildClickListener(new OnChildClickListener() {
			@Override
			public boolean onChildClick(ExpandableListView parent, View v,
					int groupPosition, int childPosition, long id) {
				// Item in ExpandableListView is clicked -> start
				// ItemInfoActivity
				Intent intent = new Intent(ItemSelectActivity.this,
						ItemInfoActivity.class);
				Item item = (Item) mListAdapter.getChild(groupPosition,
						childPosition);
				intent.putExtra(ItemInfoActivity.ITEM_ID, item.id);
				startActivity(intent);
				return true;
			}
		});
	}

	private void loadItemsAsync() {
		mListTask = new DatabaseConnectionTask<List<Item>>(this) {
			@Override
			protected List<Item> executeQuery(DatabaseConnection connection)
					throws DatabaseException {
				return connection.queryItemList();
			}
			
			@Override
			protected void onSuccess(List<Item> result, DatabaseConnection connection) {
				fillList(result);
			}
			
			@Override
			protected void onFailure(DatabaseException e, DatabaseConnection connection) {
				
			}
			
			@Override
			protected void onFinished(Set<DatabaseConnection> successfulConnections) {
				if(successfulConnections.contains(mRemoteConnection)) {
					// everything okay!
				} else if (successfulConnections.contains(mCacheConnection)) {
					showOfflineMode();
				} else {
					showFailureLoadingData(null);
				}
			}
		};
		mListTask.execute(mCacheConnection, mRemoteConnection);
	}

	private void initSearchView() {
		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		mSearchView = (SearchView) findViewById(R.id.search_view);

		// green underline
		int searchPlateId = mSearchView.getContext().getResources()
				.getIdentifier("android:id/search_plate", null, null);
		View searchPlate = mSearchView.findViewById(searchPlateId);
		searchPlate.setBackgroundResource(R.drawable.edit_text_underline);

		mSearchView.setSearchableInfo(searchManager
				.getSearchableInfo(getComponentName()));

		mSearchView.setOnQueryTextListener(new OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				mSearchView.clearFocus();
				mListAdapter.filterItems(query);
				return true;
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				if (newText.isEmpty()) {
					collapseAllCategories();
				} else {
					expandAllCategories();
				}
				mListAdapter.filterItems(newText);
				return true;
			}
		});
	}

	private void initScanner() {
		mScannerButton = (ImageButton) findViewById(R.id.scanner_button);
		mScannerFragment = (ScannerFragment) getFragmentManager()
				.findFragmentById(R.id.scanner_fragment);

		mScannerFragment.setOnDecodedCallback(new OnDecodedCallback() {
			@Override
			public void onDecoded(String decodedData) {
				if (mBarcodeToID.containsKey(decodedData)) {
					// barcode recognized -> start ItemInfoActivity
					int id = mBarcodeToID.get(decodedData);
					Intent intent = new Intent(ItemSelectActivity.this, ItemInfoActivity.class);
					intent.putExtra(ItemInfoActivity.ITEM_ID, id);
					startActivity(intent);
				} else {
					// show not recognized toast
					String notRecognizedMessage = getResources().getString(
							R.string.not_recognized_info);
					Toast.makeText(ItemSelectActivity.this, notRecognizedMessage,
							Toast.LENGTH_SHORT).show();
				}
			}
		});

		mScannerButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mScannerFragment.isExpanded()) {
					mScannerFragment.collapse();
				} else {
					mScannerFragment.expand();
				}
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		mListTask.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mListTask.onPause();
	}

	private void fillList(List<Item> items) {
		// associate items with categories and barcodes with ids
		// HashSet/-Map for fast collision checking
		String[] disabledCategoriesArray = getResources().getStringArray(
				R.array.disabled_categories);
		Set<String> disabledCategories = new HashSet<String>();
		Collections.addAll(disabledCategories, disabledCategoriesArray);

		Map<String, List<Item>> categoryToItems = new HashMap<String, List<Item>>();
		for (Item curItem : items) {
			// disable some categories
			if (disabledCategories.contains(curItem.category)) {
				continue;
			}

			// remove special category items
			if (curItem.name.equals(curItem.category)) {
				continue;
			}

			// create new empty list for category if not exists
			if (!categoryToItems.containsKey(curItem.category)) {
				categoryToItems.put(curItem.category, new ArrayList<Item>());
			}

			// add items to list
			List<Item> itemsInCategory = categoryToItems.get(curItem.category);
			itemsInCategory.add(curItem);

			// associate barcodes with ids
			if (curItem.barcode != null) {
				mBarcodeToID.put(curItem.barcode, curItem.id);
			}
		}

		// create category objects (for convenient usage)
		mCategories.clear();
		for (String curCategoryName : categoryToItems.keySet()) {
			Category newCategory = new Category(curCategoryName);
			List<Item> curItems = categoryToItems.get(curCategoryName);
			Collections.sort(curItems);
			newCategory.addAll(curItems);
			mCategories.add(newCategory);
		}

		// sort
		Collections.sort(mCategories);

		// show list
		mListAdapter.filterItems("");
		mListAdapter.notifyDataSetChanged();
	}

	private void showOfflineMode() {
		// FIXME
		ActionBar actionBar = getActionBar();
		CharSequence title = getActionBar().getTitle();
		actionBar.setTitle(title + " (offline)");
	}

	private void showFailureLoadingData(DatabaseException e) {
		// FIXME
		if (e != null) {
			e.printStackTrace();
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(this, "Could not load item list", Toast.LENGTH_SHORT)
					.show();
		}
	}

	private void expandAllCategories() {
		mExpandableListView.smoothScrollToPosition(0);
		int groupCount = mListAdapter.getGroupCount();
		for (int curGroupNr = 0; curGroupNr < groupCount; curGroupNr++) {
			mExpandableListView.expandGroup(curGroupNr);
		}
	}

	private void collapseAllCategories() {
		int groupCount = mListAdapter.getGroupCount();
		for (int curGroupNr = 0; curGroupNr < groupCount; curGroupNr++) {
			mExpandableListView.collapseGroup(curGroupNr);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		 getMenuInflater().inflate(R.menu.item_select, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch (id) {
//		case R.id.action_add_item:
//			startActivity(new Intent(this, ItemAddActivity.class));
//			break;
			
		case R.id.action_refresh:
			loadItemsAsync();
			break;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

}
