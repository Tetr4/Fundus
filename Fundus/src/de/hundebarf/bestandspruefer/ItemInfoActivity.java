package de.hundebarf.bestandspruefer;
import java.lang.reflect.Field;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import de.hundebarf.bestandspruefer.collection.Item;
import de.hundebarf.bestandspruefer.database.tasks.DownloadItemInfoTask;
import de.hundebarf.bestandspruefer.database.tasks.DownloadItemInfoTask.OnItemInfoDownloadedCallback;
import de.hundebarf.bestandspruefer.database.tasks.UpdateQuantityTask;
import de.hundebarf.bestandspruefer.database.tasks.UpdateQuantityTask.OnQuantityUpdatedCallback;

public class ItemInfoActivity extends Activity implements OnItemInfoDownloadedCallback, OnQuantityUpdatedCallback {
	public static final String ITEM_ID = "ITEM_ID";
	private static final int NUMBERPICKER_MAX_VALUE = 9999;
	private int mItemID;
	private TextView mStock;
	private Dialog mQuantityDialog;
	private DownloadItemInfoTask mDownloadInfoTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_item_info);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		// get item which info should be displayed
		mItemID = getIntent().getExtras().getInt(ITEM_ID, -1);
		if (mItemID > -1) {
			mDownloadInfoTask = new DownloadItemInfoTask(this, this);
			mDownloadInfoTask.execute(mItemID);
		} else {
			throw new IllegalArgumentException("The item's ID has to be given as an extra with key 'ITEM_ID'");
		}
		mStock = (TextView) findViewById(R.id.textview_stock);
		initUpdateStockButton();
		
		super.onCreate(savedInstanceState);
	}

	private void initUpdateStockButton() {
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
		@SuppressLint("InflateParams") // No rootView required for AlertDialog
		RelativeLayout dialogLayout = (RelativeLayout) getLayoutInflater().inflate(R.layout.numberpicker_dialog, null);
		final NumberPicker numberPicker = (NumberPicker) dialogLayout.findViewById(R.id.numberpicker);
		numberPicker.setDividerDrawable(getResources().getDrawable(R.color.green));
		numberPicker.setMaxValue(NUMBERPICKER_MAX_VALUE);
		numberPicker.setMinValue(0);
		int currentQuantity = Integer.parseInt(mStock.getText().toString());
		if(currentQuantity < 0)
			numberPicker.setValue(0);
		else {
			numberPicker.setValue(currentQuantity);
		}
		numberPicker.setWrapSelectorWheel(false);
		
		Builder builder = new AlertDialog.Builder(this);
		builder.setView(dialogLayout);
		builder.setTitle(getResources().getString(R.string.edit_quantity));
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				int quantity = numberPicker.getValue();
				new UpdateQuantityTask(ItemInfoActivity.this, ItemInfoActivity.this).execute(mItemID, quantity);
				dialog.dismiss();
			}
		});

		mQuantityDialog = builder.create();
		mQuantityDialog.show();
		
		int dialogColor = getResources().getColor(R.color.green);
		
		// Title Color
		int titleTextViewId = mQuantityDialog.getContext().getResources().getIdentifier("android:id/alertTitle", null, null);
		TextView titleTextView = (TextView) mQuantityDialog.findViewById(titleTextViewId);
		titleTextView.setTextColor(dialogColor);
		
		// Divider Color
		int dividerId = mQuantityDialog.getContext().getResources().getIdentifier("android:id/titleDivider", null, null);
		View divider = mQuantityDialog.findViewById(dividerId);
		divider.setBackgroundColor(dialogColor);
		
		// NumberPicker Divider Color
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
	
	@Override
	protected void onPause() {
		if(mQuantityDialog != null) {
			mQuantityDialog.dismiss();
		}
		mDownloadInfoTask.dismissProgressDialog();
		super.onPause();
	}

	@Override
	public void onItemInfoDownloaded(Item item) {
		fillFields(item);
	}

	@Override
	public void onItemInfoDownloadException(Exception exception) {
		exception.printStackTrace();
		Toast.makeText(ItemInfoActivity.this, exception.getMessage(), Toast.LENGTH_LONG).show();
		// TODO Offline mode
		// TODO Load from cache if possible
		finish();
	}

	@Override
	public void onQuantityUpdated(int newQuantity) {
		mStock.setText(Integer.toString(newQuantity));
	}

	@Override
	public void onQuantityUpdateException(Exception exception) {
		exception.printStackTrace();
		Toast.makeText(ItemInfoActivity.this, exception.getMessage(), Toast.LENGTH_LONG).show();
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
		price.setText(String.format("%.2f €", item.price/100.0));
		
		TextView description = (TextView) findViewById(R.id.textview_description);
		TextView descriptionTitle = (TextView) findViewById(R.id.textview_description_title);
		if(item.description.equals("null")) {
			description.setVisibility(View.GONE);
			descriptionTitle.setVisibility(View.GONE);
		} else {
			description.setText(item.description);
		}
		
		TextView stock = (TextView) findViewById(R.id.textview_stock);
		stock.setText(Integer.toString(item.stock));
	}

}
