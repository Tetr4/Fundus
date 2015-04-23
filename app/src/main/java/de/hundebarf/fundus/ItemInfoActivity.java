package de.hundebarf.fundus;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.lang.reflect.Field;

import de.hundebarf.fundus.collection.Item;
import de.hundebarf.fundus.database.ServiceConnection;
import de.hundebarf.fundus.view.TitledTextView;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class ItemInfoActivity extends BaseActivity {
	public static final String ITEM_ID = "ITEM_ID";
	private static final int NUMBERPICKER_MAX_VALUE = 9999;
	private Dialog mQuantityDialog;
	private TextView mStock;
    private ActionBar mActionBar;

	private int mItemID;
    private boolean mItemDataAvailable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_item_info);
        mActionBar = getActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayHomeAsUpEnabled(true);
        }

		// get item which info should be displayed
		mItemID = getIntent().getExtras().getInt(ITEM_ID, Integer.MIN_VALUE);
		if (mItemID == Integer.MIN_VALUE) {
			throw new IllegalArgumentException("The item's ID has to be given as an extra with key 'ITEM_ID'");
		}

		initUpdateStockButton();
	}

    @Override
    protected void onResume() {
        super.onResume();
        // load item from cache, then from service
        loadItemFromCache(mItemID);
        refresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mQuantityDialog != null) {
            mQuantityDialog.dismiss();
        }
    }

    @Override
    protected void onRefresh() {
        loadItemFromService(mItemID);
    }

    private void loadItemFromCache(int itemId) {
        FundusApplication app = (FundusApplication) getApplication();
        ServiceConnection cacheConnection = app.getCacheConnection();
        cacheConnection.queryItem(itemId, new Callback<Item>() {

            @Override
            public void success(Item item, Response response) {
                fillFields(item);
                // TODO show item age
            }

            @Override
            public void failure(RetrofitError error) {
            }
        });
    }

    private void loadItemFromService(int itemId) {
        FundusApplication app = (FundusApplication) getApplication();
        ServiceConnection serviceConnection = app.getServiceConnection();
        serviceConnection.queryItem(itemId, new Callback<Item>() {

            @Override
            public void success(Item item, Response response) {
                fillFields(item);
                handleServiceSuccess();
            }

            @Override
            public void failure(RetrofitError error) {
                handleServiceError(error);
                if (!mItemDataAvailable) {
                    finish();
                }
            }
        });
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
		int dialogColor = getResources().getColor(R.color.fundus_green);

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
        } catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

	private void updateQuantity(int itemId, final int quantity) {
        // TODO refresh animation
		FundusApplication app = (FundusApplication) getApplication();
		ServiceConnection serviceConnection = app.getServiceConnection();
		serviceConnection.updateQuantity(itemId, quantity, new Callback<Response>() {

			@Override
			public void success(Response response1, Response response2) {
				mStock.setText(Integer.toString(quantity));
                handleServiceSuccess();
			}

			@Override
			public void failure(RetrofitError error) {
                handleServiceError(error);
            }
        });
	}

	private void fillFields(Item item) {
        mItemDataAvailable = true;
        if (mActionBar != null) {
            mActionBar.setTitle(item.name);
        }

        fillField(R.id.titled_textview_id, Integer.toString(item.id));
        fillField(R.id.titled_textview_shortname, item.shortName);
        fillField(R.id.titled_textview_supplier, item.supplier);
        fillField(R.id.titled_textview_quantity, item.quantityContent + " " + item.unitContent);
        fillField(R.id.titled_textview_category, item.category);
        fillField(R.id.titled_textview_price, String.format("%.2f €", item.price / 100.0));
        fillField(R.id.titled_textview_category, item.category);
        fillField(R.id.titled_textview_description, item.description);
        TextView stock = (TextView) findViewById(R.id.textview_stock);
        stock.setText(Integer.toString(item.stock));
	}

    private void fillField(int id, String value) {
        TitledTextView titledTextView = (TitledTextView) findViewById(id);
        if(value == null || value.isEmpty()) {
            // hide
            titledTextView.setVisibility(View.GONE);
        } else {
            titledTextView.setText(value);
            titledTextView.setVisibility(View.VISIBLE);
        }
    }


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.item_info, menu);
		return super.onCreateOptionsMenu(menu);
	}

}

