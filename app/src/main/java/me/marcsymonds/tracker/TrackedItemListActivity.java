package me.marcsymonds.tracker;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class TrackedItemListActivity extends AppCompatActivity {
    private final String TAG = "TrackedItemListActivity";

    private final int TRACKED_ITEM_ADD_UPDATE_REQUEST = 1;

    private boolean mSelectMode = false;
    private Menu mOptionsMenu = null;
    private CharSequence mOptionsMenuTitle;
    private ArrayList<Integer> mSelectedList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracked_item_list);

        setUpActionBar();

        MyAdapter itemsAdapter = new MyAdapter(this, R.layout.content_tracked_item_list_item, TrackedItems.getTrackedItemsList());
        sortList(itemsAdapter);

        ListView lv = (ListView) findViewById(R.id.tracked_items_list);
        lv.setAdapter(itemsAdapter);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState ) {
        super.onSaveInstanceState(outState);
    }
    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void setUpActionBar() {
        ActionBar actionBar = getSupportActionBar();
        Log.d(TAG, String.format("setUpActionBar: %s", actionBar == null ? "NULL" : "NOT NULL"));
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            mOptionsMenuTitle = actionBar.getTitle();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        mOptionsMenu = menu;
        getMenuInflater().inflate(R.menu.menu_tracked_item_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        Menu menu = (Menu)item.getMenuInfo();

        Log.d(TAG, String.format("onOptionsItemSelected %d %s", id, (menu==null?"NULL":"SOMETHING")));
        switch(item.getItemId()) {
            case android.R.id.home:
                setResult(1, new Intent());
                finish();  //return to caller
                return true;

            case R.id.menu_tracked_item_list_select:
                switchSelectMode(true);
                break;

            case R.id.menu_tracked_item_list_add:
                addNewTrackedItem();
                break;

            case R.id.menu_tracked_item_list_select_cancel:
                switchSelectMode(false);
                break;

            case R.id.menu_tracked_item_list_select_all:
                selectAll();
                break;

            case R.id.menu_tracked_item_list_select_delete:
                deleteSelectedItems();
                break;
        }
        return false;
    }

    private void sortList(MyAdapter adapter) {
        adapter.sort(new Comparator<TrackedItem>() {
            @Override
            public int compare(TrackedItem trackedItem, TrackedItem t1) {
                return trackedItem.getName().compareToIgnoreCase(t1.getName());
            }
        });
    }

    private void addNewTrackedItem() {
        Intent intent = new Intent(this, TrackedItemEntryActivity.class);
        intent.putExtra("ID", 0); // 0 = new item.
        startActivityForResult(intent, TRACKED_ITEM_ADD_UPDATE_REQUEST);
    }

    private void updateTrackedItem(int id) {
        Intent intent = new Intent(this, TrackedItemEntryActivity.class);
        intent.putExtra("ID", id);
        startActivityForResult(intent, TRACKED_ITEM_ADD_UPDATE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == TRACKED_ITEM_ADD_UPDATE_REQUEST) {
            Log.d(TAG, String.format("onActivityResult %d %d", requestCode, resultCode));
            if (resultCode > 0) {
                ListView lv = (ListView) findViewById(R.id.tracked_items_list);
                MyAdapter ma = (MyAdapter)lv.getAdapter();

                if (resultCode > 0) {
                    TrackedItem trackedItem = TrackedItems.getItemByID(resultCode);
                    trackedItem.updateMapMarkerInfo();
                }

                sortList(ma);
                ma.notifyDataSetChanged();
            }
        }
    }

    private void selectAll() {
        ListView lv = (ListView) findViewById(R.id.tracked_items_list);
        MyAdapter adap = (MyAdapter)lv.getAdapter();

        boolean unselectAll = (adap.getCount() == mSelectedList.size());

        mSelectedList.clear();
        if (!unselectAll) {
            int idx = 0, count = adap.getCount();

            while (idx < count) {
                mSelectedList.add(idx);
                ++idx;
            }
        }

        ((MyAdapter)lv.getAdapter()).notifyDataSetChanged();

        setSelectMenuOptionsState();
    }

    private void switchSelectMode(boolean selectMode) {
        if (selectMode != mSelectMode) {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(!selectMode);
                actionBar.setDisplayShowHomeEnabled(!selectMode);

                if (selectMode) {
                    actionBar.setTitle(R.string.select);
                }
                else {
                    actionBar.setTitle(mOptionsMenuTitle);
                }
            }

            if (mOptionsMenu != null) {
                mOptionsMenu.setGroupVisible(R.id.menu_tracked_item_list_group_main, !selectMode);
                mOptionsMenu.setGroupVisible(R.id.menu_tracked_item_list_group_select, selectMode);
            }

            ListView lv = (ListView) findViewById(R.id.tracked_items_list);
            ((MyAdapter)lv.getAdapter()).notifyDataSetChanged();

            if (!selectMode) {
                mSelectedList.clear();
            }
            else {
                setSelectMenuOptionsState();
            }

            mSelectMode = selectMode;
        }
    }

    /**
     * Set the state of menu options based on selected items.
     */
    private void setSelectMenuOptionsState() {
        MenuItem mi = mOptionsMenu.findItem(R.id.menu_tracked_item_list_select_delete);

        mi.setEnabled((mSelectedList.size() > 0));
    }

    private void deleteSelectedItems() {
        if (mSelectedList.size() > 0) {
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        ListView lv = (ListView) findViewById(R.id.tracked_items_list);
                        MyAdapter adap = (MyAdapter)lv.getAdapter();

                        doDeleteSelectedItems(adap);
                    }
                }
            };

            Resources res = getResources();

            AlertDialog.Builder msg = new AlertDialog.Builder(this);

            msg
                    .setTitle(res.getString(R.string.delete_selected_items_title))
                    .setMessage(res.getString(R.string.delete_selected_items_message))
                    .setNegativeButton(res.getString(android.R.string.no), dialogClickListener)
                    .setPositiveButton(res.getString(android.R.string.yes), dialogClickListener)
                    .setCancelable(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                msg.setIcon(res.getDrawable(android.R.drawable.ic_menu_delete, null));
            }
            else {
                msg.setIcon(res.getDrawable(android.R.drawable.ic_menu_delete));
            }

            msg.show();
        }
    }

    private void doDeleteSelectedItems(MyAdapter adap) {
        TrackedItem ti;

        // As items are deleted from the list, this alters the positions of all the following items
        // in the array. This means that the list of selected positions (mSelectedList) need to be
        // sorted so that we can removing items starting with the highest index and goind down to
        // the item with the lowest index.

        Collections.sort(mSelectedList, new Comparator<Integer>() {
            @Override
            public int compare(Integer i1, Integer i2) {
                return i2.compareTo(i1); // Compare i2 to i1 so items are sorted in reverse.
            }
        });

        for (int i : mSelectedList) {
            ti = adap.getItem(i);

            Log.d(TAG, String.format("DELETING ITEM - Position:%d, ID:%d", i, ti.getID()));

            Log.d(TAG, String.format("Before del: adap:%d, items:%d", adap.getCount(), TrackedItems.getTrackedItemsList().size()));
            adap.remove(ti);
            Log.d(TAG, String.format("After del: adap:%d, items:%d", adap.getCount(), TrackedItems.getTrackedItemsList().size()));
            TrackedItems.deleteTrackedItem(ti);
        }

        mSelectedList.clear();

        adap.notifyDataSetChanged();
    }

    /**
     * Class that extends ArrayAdapter and lets us draw our own view for each item in the list,
     * rather than the standard single text view supported by the ArrayAdapter.
     *
     * My view will have an optional checkbox, which is used in "select" mode, the name of the
     * tracked item, and a switch to enable/disable the tracked item.
     */
    private class MyAdapter extends ArrayAdapter<TrackedItem> {
        MyAdapter(Context context, int layoutId, ArrayList<TrackedItem> items) {
            super(context, layoutId, items);
        }

        /**
         * Render our own View for an item in the list.
         *
         * @param position index within the array.
         * @param view the previous view for this item in the list.
         * @param parent parent ViewGroup.
         * @return the View to be drawn for this item.
         */
        @Override @NonNull
        public View getView(int position, View view, @NonNull ViewGroup parent) {
            boolean newView = false;

            if (view == null) {
                view = getLayoutInflater().inflate(R.layout.content_tracked_item_list_item, null);
                newView = true;
            }

            TrackedItem item = getItem(position);
            int itemColour = item.getColour();

            view.setBackgroundColor(Color.HSVToColor(75, new float[] {(float)itemColour, 1, 1}));// ColourPickerUtils.hsv2rgb(itemColour, 1, 1));

            CheckBox cb = (CheckBox)view.findViewById(R.id.tracked_item_selected);
            if (mSelectMode) {
                cb.setVisibility(View.VISIBLE);
                cb.setTag(position);

                cb.setChecked((mSelectedList.contains((Integer)position)));
            }
            else {
                cb.setVisibility(View.GONE);
            }

            TextView tv = (TextView)view.findViewById(R.id.tracked_item_name);
            tv.setText(item.getName());// + ":" + item.getID() + " " + position);
            tv.setTag(position);

            Switch sw = (Switch)view.findViewById(R.id.tracked_item_enabled);
            sw.setChecked(item.isEnabled());
            sw.setTag(position);

            sw.setEnabled(!mSelectMode);

            if (newView) {
                // If created a new View for this item, then add event listeners for when things
                // are changed.

                tv.setOnClickListener(new TextView.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        onNameClicked((int)view.getTag());
                    }
                });

                tv.setOnLongClickListener(new TextView.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        switchSelectMode(true);
                        return true;
                    }
                }
                );

                cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        //Log.d(TAG, "Selected Clicked " + String.valueOf(compoundButton.getTag()));
                        onItemSelectedChanged((int)compoundButton.getTag(), b);
                    }
                });

                sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        //Log.d(TAG, "Switch Clicked " + String.valueOf(compoundButton.getTag()));
                        onItemEnabledChanged((int)compoundButton.getTag(), b);
                    }
                });
            }

            return view;
        }

        private void onNameClicked(int position) {
            TrackedItem ti = this.getItem(position);
            updateTrackedItem(ti.getID());
        }

        private void onItemEnabledChanged(int position, boolean enabled) {
            Log.d(TAG, String.format("Enabled changed : %s %s", String.valueOf(position), (enabled ? "ON" : "OFF")));

            TrackedItem ti = this.getItem(position);
            ti.setEnabled(enabled);
            TrackedItems.saveTrackedItem(ti);
        }

        /**
         * The "select" checkbox has been ticked or unticked. Add or remove the position index of
         * the item to/from the list of selected items.
         *
         * @param position position of selected item in array.
         * @param state indicates if state is now ticked or unticked.
         */
        private void onItemSelectedChanged(int position, boolean state) {
            if (state) {
                if (mSelectedList.indexOf(position)< 0) {
                    mSelectedList.add(position);
                }
            }
            else {
                if (mSelectedList.indexOf(position) >= 0) {
                    mSelectedList.remove((Object) position); // (Object) so that it uses the correct method, and not the "int index" one.
                }
            }

            setSelectMenuOptionsState();
        }
    }
}
