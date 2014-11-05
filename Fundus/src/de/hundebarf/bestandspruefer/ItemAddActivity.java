package de.hundebarf.bestandspruefer;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

public class ItemAddActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_item_add);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		super.onCreate(savedInstanceState);
		// TODO price in cent
		// TODO set last edit date 
		// TODO Tax group enum?
		
		ImageButton addButton = (ImageButton)findViewById(R.id.add_button);
		addButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(isInputCorrect()) {
					saveData();
				} else {
					showWarning();
				}
				
			}
		});
	}
	
	protected void showWarning() {
		// TODO Auto-generated method stub
		CharSequence warning = getResources().getText(R.string.empty_fields_warning);
		Toast.makeText(this, warning, Toast.LENGTH_SHORT).show();
		EditText b = (EditText) findViewById(R.id.b);
		b.setError("Required");
	}

	protected void saveData() {
		// TODO Auto-generated method stub
		
	}

	protected boolean isInputCorrect() {
		// TODO Auto-generated method stub
		return false;
	}

}
