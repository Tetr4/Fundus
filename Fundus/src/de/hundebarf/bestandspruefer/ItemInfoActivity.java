package de.hundebarf.bestandspruefer;

import java.lang.reflect.Field;
import java.util.Set;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import de.hundebarf.bestandspruefer.collection.Item;
import de.hundebarf.bestandspruefer.database.CacheConnection;
import de.hundebarf.bestandspruefer.database.DatabaseConnection;
import de.hundebarf.bestandspruefer.database.DatabaseException;
import de.hundebarf.bestandspruefer.database.ServiceConnection;
import de.hundebarf.bestandspruefer.database.tasks.DatabaseConnectionTask;

public class ItemInfoActivity extends Activity {
	public static final String ITEM_ID = "ITEM_ID";
	private static final int NUMBERPICKER_MAX_VALUE = 9999;
	private Dialog mQuantityDialog;
	private TextView mStock;
	
	private Item mItem;
	private int mItemID;
	
	private CacheConnection mCacheConnection;
	private ServiceConnection mRemoteConnection;
	private DatabaseConnectionTask<Item> mItemTask;
	private DatabaseConnectionTask<Integer> mUpdateQuantityTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_item_info);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		// get item which info should be displayed
		mItemID = getIntent().getExtras().getInt(ITEM_ID, Integer.MIN_VALUE);
		if (mItemID == Integer.MIN_VALUE) {
			throw new IllegalArgumentException(
					"The item's ID has to be given as an extra with key 'ITEM_ID'");
		}
		
		mCacheConnection = new CacheConnection(this);
		mRemoteConnection = new ServiceConnection(this);
		loadItemAsync(mItemID);
		
		initUpdateStockButton();
	}

	private void initUpdateStockButton() {
		mStock = (TextView) findViewById(R.id.textview_stock);
		
		ImageButton editButton = (ImageButton) findViewById(R.id.edit_quantity_button);
		RelativeLayout editQuantityBar = (RelativeLayout) findViewById(R.id.edit_quantity_bar);
		
		OnClickListener onClickListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				createNumberPickerDialog();
			}
		};
		
		editButton.setOnClickListener(onClickListener);
		editQuantityBar.setOnClickListener(onClickListener);
	}

	private void createNumberPickerDialog() {
		// inflate layout
		// No rootView required for AlertDialog
		@SuppressLint("InflateParams") 
		RelativeLayout dialogLayout = (RelativeLayout) getLayoutInflater()
				.inflate(R.layout.numberpicker_dialog, null);
		final NumberPicker numberPicker = (NumberPicker) dialogLayout
				.findViewById(R.id.numberpicker);
		
		// set numberpicker values
		numberPicker.setMaxValue(NUMBERPICKER_MAX_VALUE);
		numberPicker.setMinValue(0);
		int currentQuantity = Integer.parseInt(mStock.getText().toString());
		if (currentQuantity < 0)
			numberPicker.setValue(0);
		else {
			numberPicker.setValue(currentQuantity);
		}
		numberPicker.setWrapSelectorWheel(false);
		
		// create dialog from layout
		Builder builder = new AlertDialog.Builder(this);
		builder.setView(dialogLayout);
		builder.setTitle(getResources().getString(R.string.edit_quantity));
		// TODO resource string
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				int quantity = numberPicker.getValue();
				updateQuantityAsync(quantity);
				dialog.dismiss();
			}
		});
		mQuantityDialog = builder.create();
		mQuantityDialog.show();

		// colorize
		int dialogColor = getResources().getColor(R.color.green);

		// Title Color
		int titleTextViewId = mQuantityDialog.getContext().getResources()
				.getIdentifier("android:id/alertTitle", null, null);
		TextView titleTextView = (TextView) mQuantityDialog
				.findViewById(titleTextViewId);
		titleTextView.setTextColor(dialogColor);

		// Dialog divider color
		int dividerId = mQuantityDialog.getContext().getResources()
				.getIdentifier("android:id/titleDivider", null, null);
		View divider = mQuantityDialog.findViewById(dividerId);
		divider.setBackgroundColor(dialogColor);

		// NumberPicker divider color
		try {
			// Reflection Magic!
			Field selectionDivider = NumberPicker.class
					.getDeclaredField("mSelectionDivider");
			selectionDivider.setAccessible(true);
			selectionDivider.set(numberPicker, new ColorDrawable(dialogColor));
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}

	private void loadItemAsync(final int itemId) {
		mItemTask = new DatabaseConnectionTask<Item>(this) {

			@Override
			protected Item executeQuery(DatabaseConnection connection)
					throws DatabaseException {
				return connection.queryItem(itemId);
			}

			@Override
			protected void onSuccess(Item item, DatabaseConnection connection) {
				fillFields(item);
			}

			@Override
			protected void onFailure(DatabaseException e,
					DatabaseConnection connection) {
			}

			@Override
			protected void onFinished(
					Set<DatabaseConnection> successfulConnections) {
				if (successfulConnections.contains(mRemoteConnection)) {
					hideOfflineMode();
				} else if (successfulConnections.contains(mCacheConnection)) {
					showOfflineMode();
				} else {
					showFailureLoadingData(null);
				}
			}
		};
		mItemTask.execute(mCacheConnection, mRemoteConnection);
	}

	private void showOfflineMode() {
		// FIXME
		getActionBar().setTitle(mItem.name + " (offline)");
	}
	
	private void hideOfflineMode() {
		// FIXME
		getActionBar().setTitle(mItem.name);
	}

	private void showFailureLoadingData(Exception e) {
		// FIXME
		if (e != null) {
			e.printStackTrace();
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(this, "Could not load item", Toast.LENGTH_SHORT)
					.show();
		}
		finish();
	}
	
	private void showFailureUpdatingQuantity(Exception e) {
		// FIXME
		if (e != null) {
			e.printStackTrace();
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(this, "Could not update quantity", Toast.LENGTH_SHORT)
					.show();
		}
		finish();
	}

	private void updateQuantityAsync(final int quantity) {
		mUpdateQuantityTask = new DatabaseConnectionTask<Integer>(this) {

			@Override
			protected Integer executeQuery(DatabaseConnection connection)
					throws DatabaseException {
				connection.updateQuantity(mItemID, quantity);
				return quantity;
			}

			@Override
			protected void onSuccess(Integer newQuantity,
					DatabaseConnection connection) {
				mStock.setText(Integer.toString(newQuantity));
			}

			@Override
			protected void onFailure(DatabaseException e,
					DatabaseConnection connection) {
				showFailureUpdatingQuantity(e);
			}

			@Override
			protected void onFinished(
					Set<DatabaseConnection> successfulConnections) {
			}
		};
		mUpdateQuantityTask.execute(new ServiceConnection(this));
	}

	@Override
	protected void onResume() {
		super.onResume();
		mItemTask.onResume();
		if (mUpdateQuantityTask != null) {
			mUpdateQuantityTask.onResume();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		mItemTask.onPause();
		if (mUpdateQuantityTask != null) {
			mUpdateQuantityTask.onPause();
		}
		if (mQuantityDialog != null) {
			mQuantityDialog.dismiss();
		}
	}

	private void fillFields(Item item) {
		mItem = item;
		
		getActionBar().setTitle(item.name);

		TextView id = (TextView) findViewById(R.id.textview_id);
		id.setText(Integer.toString(item.id));

		TextView shortName = (TextView) findViewById(R.id.textview_shortname);
		shortName.setText(item.shortName);

		TextView supplier = (TextView) findViewById(R.id.textview_supplier);
		supplier.setText(item.supplier);

		TextView quantity = (TextView) findViewById(R.id.textview_quantity);
		quantity.setText(item.quantityContent + " " + item.unitContent);

		TextView category = (TextView) findViewById(R.id.textview_category);
		category.setText(item.category);

		TextView price = (TextView) findViewById(R.id.textview_price);
		price.setText(String.format("%.2f €", item.price / 100.0));

		TextView description = (TextView) findViewById(R.id.textview_description);
		TextView descriptionTitle = (TextView) findViewById(R.id.textview_description_title);
		if (item.description.equals("null")) {
			description.setVisibility(View.GONE);
			descriptionTitle.setVisibility(View.GONE);
		} else {
			description.setVisibility(View.VISIBLE);
			descriptionTitle.setVisibility(View.VISIBLE);
			description.setText(item.description);
		}

		TextView stock = (TextView) findViewById(R.id.textview_stock);
		stock.setText(Integer.toString(item.stock));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.item_info, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_refresh) {
			loadItemAsync(mItemID);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

}
