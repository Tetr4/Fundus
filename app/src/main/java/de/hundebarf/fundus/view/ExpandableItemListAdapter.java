package de.hundebarf.fundus.view;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import de.hundebarf.fundus.R;
import de.hundebarf.fundus.model.Category;
import de.hundebarf.fundus.model.Item;

public class ExpandableItemListAdapter extends BaseExpandableListAdapter {
	private Activity mContext;
	private List<Category> mFullCategories;
	private List<Category> mCategories = new LinkedList<>();

	public ExpandableItemListAdapter(Activity context, List<Category> categories) {
		// TODO Animate
		mContext = context;
		mFullCategories = categories;
		mCategories.addAll(mFullCategories);
	}

	@Override
	public Object getChild(int categoryPosition, int itemPosition) {
		return mCategories.get(categoryPosition).get(itemPosition);
	}

	@Override
	public int getChildrenCount(int categoryPosition) {
		return mCategories.get(categoryPosition).size();
	}

	@Override
	public long getChildId(int categoryPosition, int itemPosition) {
		Item child = mCategories.get(categoryPosition).get(itemPosition);
		return child.id;
	}

	@Override
	public View getChildView(int categoryPosition, int itemPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {
		// child -> item

		if (convertView == null) {
			// inflate new view
			LayoutInflater inflater = mContext.getLayoutInflater();
			convertView = inflater.inflate(R.layout.expandable_list_child_item,
					parent, false);
		}

		// set text
		Item item = (Item) getChild(categoryPosition, itemPosition);
		TextView itemTextView = (TextView) convertView
				.findViewById(R.id.textview_child);
		itemTextView.setText(item.name);
		return convertView;
	}

	@Override
	public Object getGroup(int categoryPosition) {
		return mCategories.get(categoryPosition);
	}

	@Override
	public int getGroupCount() {
		return mCategories.size();
	}

	@Override
	public long getGroupId(int categoryPosition) {
		return categoryPosition;
	}

	@Override
	public View getGroupView(int categoryPosition, boolean isExpanded,
			View convertView, ViewGroup parent) {
		// group -> category

		if (convertView == null) {
			// inflate new view
			LayoutInflater inflater = mContext.getLayoutInflater();
			convertView = inflater.inflate(R.layout.expandable_list_group_item,
					parent, false);
		}

		// set text
		Category category = (Category) getGroup(categoryPosition);
		TextView groupTextView = (TextView) convertView
				.findViewById(R.id.textview_group);
		groupTextView.setText(category.getName());
		return convertView;
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}

	public void filterItems(String query) {
		query = query.toLowerCase(Locale.GERMANY);
		mCategories.clear();

		if (query.isEmpty()) {
			mCategories.addAll(mFullCategories);
		} else {
			for (Category curCategory : mFullCategories) {
				Category filteredCategory = new Category(curCategory.getName());
				for (Item curItem : curCategory) {
					if (curItem.name.toLowerCase(Locale.GERMANY).contains(query)) {
						filteredCategory.add(curItem);
					}
				}
				if (filteredCategory.size() > 0) {
					mCategories.add(filteredCategory);
				}
			}
		}
		
		notifyDataSetChanged();
	}
}
