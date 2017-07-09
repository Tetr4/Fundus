package de.hundebarf.fundus;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.hundebarf.fundus.model.Category;
import de.hundebarf.fundus.model.Item;
import de.hundebarf.fundus.service.ServiceConnection;
import de.hundebarf.fundus.view.ScannerFragment;
import de.hundebarf.fundus.view.ExpandableItemListAdapter;
import de.klimek.scanner.OnDecodedCallback;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class ItemSelectActivity extends BaseActivity {
    // TODO better separation of concerns

    // Barcode Scanner
    private ScannerFragment mScannerFragment;
    private ImageButton mScannerButton;
    private Toast mNotRecognizedToast;

    // Item list
    private SearchView mSearchView;
    private ExpandableListView mExpandableListView;
    private ExpandableItemListAdapter mListAdapter;
    private List<Category> mCategories = new ArrayList<>();
    private Map<String, Integer> mBarcodeToItemID = new HashMap<>();
    private boolean mItemDataAvailable = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_select);

        initExpandableListView();
        initSearchView();
        initScanner();
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (!mItemDataAvailable) {
            loadItemsFromCache();
            refresh();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mScannerFragment.collapseNoAnim();
    }

    @Override
    protected void onRefresh() {
        loadItemsFromService();
    }

    private void loadItemsFromCache() {
        FundusApplication app = (FundusApplication) getApplication();
        ServiceConnection serviceConnection = app.getCacheConnection();
        serviceConnection.queryItemList(new Callback<List<Item>>() {

            @Override
            public void success(List<Item> items, Response response) {
                fillList(items);
            }

            @Override
            public void failure(RetrofitError error) {
                Log.v(TAG, "itemlist not in cache");
            }
        });
    }

    private void loadItemsFromService() {
        FundusApplication app = (FundusApplication) getApplication();
        ServiceConnection serviceConnection = app.getServiceConnection();
        serviceConnection.queryItemList(new Callback<List<Item>>() {

            @Override
            public void success(List<Item> items, Response response) {
                fillList(items);
                handleServiceSuccess();
                // TODO show age of items
            }

            @Override
            public void failure(RetrofitError error) {
                handleServiceError(error);
            }
        });
    }

    private void initExpandableListView() {
        mExpandableListView = (ExpandableListView) findViewById(R.id.expandable_list_view);
        mListAdapter = new ExpandableItemListAdapter(this, mCategories);
        mExpandableListView.setAdapter(mListAdapter);
        mExpandableListView.setOnChildClickListener(new OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                Item item = (Item) mListAdapter.getChild(groupPosition, childPosition);
                startItemInfoActivity(item.id);
                return true;
            }
        });
    }

    private void initSearchView() {
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        mSearchView = (SearchView) findViewById(R.id.search_view);

        // underline color
        int searchPlateId = mSearchView.getContext().getResources()
                .getIdentifier("android:id/search_plate", null, null);
        View searchPlate = mSearchView.findViewById(searchPlateId);
        searchPlate.setBackgroundResource(R.drawable.edit_text_underline);

        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        mSearchView.setOnQueryTextListener(new OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mSearchView.clearFocus();
                mListAdapter.filterItems(query);
                // start ItemInfoActivity if exactly 1 Item is found
                if (mListAdapter.getGroupCount() == 1 && mListAdapter.getChildrenCount(0) == 1) {
                    Item item = (Item) mListAdapter.getChild(0, 0);
                    startItemInfoActivity(item.id);
                }
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
        mScannerFragment = (ScannerFragment) getFragmentManager().findFragmentById(R.id.collapsible_scanner_fragment);
        mNotRecognizedToast = Toast.makeText(this, getString(R.string.not_recognized_info), Toast.LENGTH_SHORT);

        mScannerFragment.setOnDecodedCallback(new OnDecodedCallback() {
            @Override
            public void onDecoded(String decodedData) {
                if (mBarcodeToItemID.containsKey(decodedData)) {
                    // barcode recognized
                    int id = mBarcodeToItemID.get(decodedData);
                    startItemInfoActivity(id);
                } else {
                    // show not recognized toast
                    mNotRecognizedToast.show();
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

    private void fillList(List<Item> items) {
        mItemDataAvailable = true;
        // disabled Categories as HashSet for fast checking
        String[] disabledCategoriesArray = getResources().getStringArray(R.array.disabled_categories);
        String[] disabledItemsArray = getResources().getStringArray(R.array.disabled_items);
        Set<String> disabledItems = new HashSet<>();
        Set<String> disabledCategories = new HashSet<>();
        Collections.addAll(disabledCategories, disabledCategoriesArray);
        Collections.addAll(disabledItems, disabledItemsArray);

        // associate items with categories and barcodes with ids
        Map<String, List<Item>> categoryToItems = new HashMap<>();
        for (Item curItem : items) {

            // items without category
            if (curItem.category == null) {
                curItem.category = getString(R.string.without_category);
            }

            // disable some categories
            if (disabledCategories.contains(curItem.category)) {
                continue;
            }

            // disable some items
            if (disabledItems.contains(curItem.name)) {
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
                mBarcodeToItemID.put(curItem.barcode, curItem.id);
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
        Collections.sort(mCategories);

        // show list
        mListAdapter.filterItems("");
        mListAdapter.notifyDataSetChanged();
    }

    private void startItemInfoActivity(int id) {
        Intent intent = new Intent(ItemSelectActivity.this, ItemInfoActivity.class);
        intent.putExtra(ItemInfoActivity.ITEM_ID, id);
        startActivity(intent);
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
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {

            // case R.id.action_add_item:
            // startActivity(new Intent(this, ItemAddActivity.class));
            // break;

            case R.id.action_switch_user:
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                return true;

            default:
                // refresh handling is implemented in BaseActivity
                return super.onOptionsItemSelected(item);
        }
    }
}
