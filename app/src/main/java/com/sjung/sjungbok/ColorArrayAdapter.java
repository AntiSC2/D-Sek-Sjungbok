package com.sjung.sjungbok;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

public class ColorArrayAdapter extends ArrayAdapter<String> {
	private int positionofSelf;
	private int blueOne = -1;

	public ColorArrayAdapter(Context context, String[] values, int positionOfSelf) {
		super(context, R.layout.drawer_list_item, values);
		this.positionofSelf = positionOfSelf;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		View view = super.getView(position, convertView, parent);
		if (position == positionofSelf) {
			view.setBackgroundColor(Color.parseColor("#E16990"));
		} else {
			view.setBackgroundColor(Color.parseColor("#F280A1"));
		}

		return view;
	}
}