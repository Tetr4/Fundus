package de.hundebarf.bestandspruefer;

import java.lang.reflect.Field;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import android.annotation.SuppressLint;
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
import de.hundebarf.bestandspruefer.database.ServiceConnection;

public class ItemInfoActivity extends BaseActivity {
	public static final String ITEM_ID = "ITEM_ID";
	private static final int NUMBERPICKER_MAX_VALUE = 9999;
	private Dialog mQuantityDialog;
	private TextView mStock;

	private int mItemID;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_item_info);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		// get item which info should be displayed
		mItemID = getIntent().getExtras().getInt(ITEM_ID, Integer.MIN_VALUE);
		if (mItemID == Integer.MIN_VALUE) {
			throw new IllegalArgumentException("The item's ID has to be given as an extra with key 'ITEM_ID'");
		}

		loadItem(mItemID);

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
		RelativeLayout dialogLayout = (RelativeLayout) getLayoutInflater().inflate(R.layout.numberpicker_dialog, null);
		final NumberPicker numberPicker = (NumberPicker) dialogLayout.findViewById(R.id.numberpicker);

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
		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				int quantity = numberPicker.getValue();
				updateQuantity(mItemID, quantity);
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
		TextView titleTextView = (TextView) mQuantityDialog.findViewById(titleTextViewId);
		titleTextView.setTextColor(dialogColor);

		// Dialog divider color
		int dividerId = mQuantityDialog.getContext().getResources()
				.getIdentifier("android:id/titleDivider", null, null);
		View divider = mQuantityDialog.findViewById(dividerId);
		divider.setBackgroundColor(dialogColor);

		// NumberPicker divider color
		try {
			// Reflection Magic!
			Field selectionDivider = NumberPicker.class.getDeclaredField("mSelectionDivider");
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

	private void loadItem(final int itemId) {
		FundusApplication app = (FundusApplication) getApplication();
		ServiceConnection serviceConnection = app.getServiceConnection();
		serviceConnection.queryItem(itemId, new Callback<Item>() {

			@Override
			public void success(Item item, Response response) {
				fillFields(item);
				// TODO show item age
			}

			@Override
			public void failure(RetrofitError error) {
				showFailureLoadingData(error);
			}
		});
	}

	private void showFailureLoadingData(Exception e) {
		// FIXME
		if (e != null) {
			e.printStackTrace();
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(this, "Could not load item", Toast.LENGTH_SHORT).show();
		}
		finish();
	}

	private void showFailureUpdatingQuantity(Exception e) {
		// FIXME
		if (e != null) {
			e.printStackTrace();
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(this, "Could not update quantity", Toast.LENGTH_SHORT).show();
		}
		finish();
	}

	private void updateQuantity(int itemId, final int quantity) {
		FundusApplication app = (FundusApplication) getApplication();
		ServiceConnection serviceConnection = app.getServiceConnection();
		serviceConnection.updateQuantity(itemId, quantity, new Callback<Response>() {

			@Override
			public void success(Response response1, Response response2) {
				mStock.setText(Integer.toString(quantity));
			}

			@Override
			public void failure(RetrofitError error) {
				showFailureUpdatingQuantity(error);

			}
		});
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mQuantityDialog != null) {
			mQuantityDialog.dismiss();
		}
	}

	private void fillFields(Item item) {
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
		if (item.description == null || item.description.isEmpty()){
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
			loadItem(mItemID);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

}
