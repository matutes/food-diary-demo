package com.nivgelbermann.fooddiarydemo.adapters;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.nivgelbermann.fooddiarydemo.R;
import com.nivgelbermann.fooddiarydemo.data.CategoriesContract;
import com.nivgelbermann.fooddiarydemo.data.Category;
import com.nivgelbermann.fooddiarydemo.data.FoodsContract;
import com.nivgelbermann.fooddiarydemo.models.DateCard;
import com.nivgelbermann.fooddiarydemo.models.FoodItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Niv on 28-Aug-17.
 */

public class OuterRecyclerViewAdapter extends RecyclerView.Adapter<OuterRecyclerViewAdapter.CardDateViewHolder> {
    private static final String TAG = "OuterRecyclerViewAdapte";

    private Cursor mCursor;
    private Cursor mInnerCursor;

    // Variable to collect all the InnerRecyclerViewAdapters that have been
    // bound to a ViewHolder and displayed in OuterRecyclerView
    private ArrayList<InnerRecyclerViewAdapter> mInnerAdapters;
    //    private InnerRecyclerViewAdapter.FoodItemViewHolder.FoodItemListener mInnerAdapterListener;
    private Context mContext;
    private RecyclerView.RecycledViewPool mViewPool;
    private List<Category> mCategories;
    private List<FoodItem> mFoodItems;

    static class CardDateViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.card_header_date) TextView date;
        @BindView(R.id.card_header_weekday) TextView dayOfWeek;
        @BindView(R.id.card_header_month) TextView month;
        @BindView(R.id.card_header_year) TextView year;
        @BindView(R.id.card_recyclerview) RecyclerView innerRecyclerView;

        CardDateViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

    //    public OuterRecyclerViewAdapter(InnerRecyclerViewAdapter.FoodItemViewHolder.FoodItemListener listener) {
//    public OuterRecyclerViewAdapter(Context context) {
    public OuterRecyclerViewAdapter(Context context, ArrayList<FoodItem> foodItems) {
//        mInnerAdapterListener = listener;
        mContext = context;
        mInnerAdapters = new ArrayList<>();
        mViewPool = new RecyclerView.RecycledViewPool();

        mCategories = new ArrayList<>();
        ContentResolver contentResolver = mContext.getContentResolver();
        Cursor cursor = contentResolver.query(
                CategoriesContract.CONTENT_URI,
                null,
                null,
                null,
                null);
        if (cursor != null && cursor.moveToFirst()) { // TODO Reverse condition and add room for error
            do {
                mCategories.add(new Category(
                        cursor.getString(cursor.getColumnIndex(CategoriesContract.Columns._ID)),
                        cursor.getString(cursor.getColumnIndex(CategoriesContract.Columns.NAME)),
                        cursor.getString(cursor.getColumnIndex(CategoriesContract.Columns.COLOR))));
            } while (cursor.moveToNext());
            cursor.close();
        }

        mFoodItems = foodItems;
    }

    @Override
    public CardDateViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Log.d(TAG, "onCreateViewHolder: new view requested");
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_outer_rv_card, parent, false);
//        return new CardDateViewHolder(view);
        CardDateViewHolder holder = new CardDateViewHolder(view);
        mViewPool.setMaxRecycledViews(0, 30);
        holder.innerRecyclerView.setRecycledViewPool(mViewPool);
        return holder;
    }

    @Override
    public void onBindViewHolder(CardDateViewHolder holder, int position) {
        // Log.d(TAG, "onBindViewHolder: starts with position " + position);

        boolean isHolderNew = (holder.date.getText().toString().trim().isEmpty());
        DateCard date = new DateCard(0, 0, 0);

        if ((mCursor == null) || (mCursor.getCount() == 0)) {
            // Do nothing for now
        } else {
            if (!mCursor.moveToPosition(position)) {
                throw new IllegalStateException("Couldn't move cursor to position " + position);
            }

            date = new DateCard(
                    mCursor.getInt(mCursor.getColumnIndex(FoodsContract.Columns.DAY)),
                    mCursor.getInt(mCursor.getColumnIndex(FoodsContract.Columns.MONTH)),
                    mCursor.getInt(mCursor.getColumnIndex(FoodsContract.Columns.YEAR)));
            // Log.d(TAG, "onBindViewHolder: date: " + date);

            holder.date.setText(String.format(Locale.getDefault(), "%02d", date.getDate()));
            holder.dayOfWeek.setText(date.getDayOfWeek());
            holder.month.setText(date.getMonthName());
            holder.year.setText(String.valueOf(date.getYear()));
            // Log.d(TAG, "onBindViewHolder: date has been set to views");
        }

        if (isHolderNew) {
            // If holder is new, create a new LinearLayoutManager for the holder's RecyclerView.
            // Otherwise, do nothing, holder's RecyclerView will re-use its LinearLayoutManager.
            holder.innerRecyclerView.setLayoutManager(new LinearLayoutManager(holder.innerRecyclerView.getContext()));
            holder.innerRecyclerView.setHasFixedSize(true);
            // TODO Compile to phone with above line commented and below lines un-commented. Check whether scrolling animation is actually smoother.
//            LinearLayoutManager manager = new LinearLayoutManager(holder.innerRecyclerView.getContext());
//            manager.setItemPrefetchEnabled(true);
//            manager.setInitialPrefetchItemCount(6); // Could require modification
//            holder.innerRecyclerView.setLayoutManager(manager);
        }

        // Create ArrayList<FoodItem> containing data for InnerAdapter
        // by approaching DB for every adapter
        /*
        ContentResolver contentResolver = mContext.getContentResolver();
        String[] projection = new String[]{FoodsContract.Columns._ID,
                FoodsContract.Columns.FOOD_ITEM,
                FoodsContract.Columns.DAY,
                FoodsContract.Columns.MONTH,
                FoodsContract.Columns.YEAR,
                FoodsContract.Columns.HOUR,
                FoodsContract.Columns.CATEGORY_ID};
        String selection = FoodsContract.Columns.DAY + "=? AND "
                + FoodsContract.Columns.MONTH + "=? AND "
                + FoodsContract.Columns.YEAR + "=?";
        String[] selectionArgs = {String.valueOf(date.getDate()),
                String.valueOf(date.getMonth()),
                String.valueOf(date.getYear())};
        String sortOrder = FoodsContract.Columns.HOUR + " DESC,"
                + FoodsContract.Columns.FOOD_ITEM + " COLLATE NOCASE DESC";
        Cursor cursor = contentResolver.query(FoodsContract.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder);
        ArrayList<FoodItem> items;
        if (cursor == null || !cursor.moveToFirst()) {
            throw new IllegalStateException("Couldn't move cursor to first. Cursor: " + (cursor == null));
        }

            items = new ArrayList<>();
            do {
                items.add(new FoodItem(
                        cursor.getString(cursor.getColumnIndex(FoodsContract.Columns._ID)),
                        cursor.getString(cursor.getColumnIndex(FoodsContract.Columns.FOOD_ITEM)),
                        cursor.getLong(cursor.getColumnIndex(FoodsContract.Columns.HOUR)),
                        cursor.getInt(cursor.getColumnIndex(FoodsContract.Columns.DAY)),
                        cursor.getInt(cursor.getColumnIndex(FoodsContract.Columns.MONTH)),
                        cursor.getInt(cursor.getColumnIndex(FoodsContract.Columns.YEAR)),
                        cursor.getInt(cursor.getColumnIndex(FoodsContract.Columns.CATEGORY_ID))));
            } while (cursor.moveToNext());
            cursor.close();

        */

        // Create ArrayList<FoodItem> containing data for InnerAdapter
        // by approaching DB once (in PageFragment), then getting sublists of fetched data
        final int adapterDate = date.getDate();
        Collection<FoodItem> filteredCollection = Collections2.filter(mFoodItems,
                new Predicate<FoodItem>() {
                    @Override
                    public boolean apply(FoodItem input) {
                        return input.getDay() == adapterDate;
                    }
                });
        List<FoodItem> items = new ArrayList<>(filteredCollection);

        // No need for listener interface implementation, as it's performed in PageFragment.
        InnerRecyclerViewAdapter adapter = new InnerRecyclerViewAdapter(
//                mInnerAdapterListener, date.getDate());
//                (InnerRecyclerViewAdapter.FoodItemViewHolder.FoodItemListener) mContext, date.getDate(), items, mCategories);
                (InnerRecyclerViewAdapter.FoodItemViewHolder.FoodItemListener) mContext, adapterDate, items, mCategories);
        mInnerAdapters.add(adapter);
        holder.innerRecyclerView.setAdapter(adapter);

        // By default, the inner loader in PageFragment has finished loading
        // before this OuterAdapter has even started initializing the adapters for
        // all the InnerAdapters.
        // That's why we need to force the InnerAdapter to swap cursor
        // (actually swapping with the same cursor it already has)
        // to make it refresh its rows.
        adapter.swapCursor(mInnerCursor);

        // Log.d(TAG, "onBindViewHolder: ends");
    }

    @Override
    public int getItemCount() {
        if (mCursor == null || mCursor.getCount() == 0) {
            return 0;
        }
        return mCursor.getCount();
    }

    /**
     * Swap in a new Cursor, returning the old Cursor. <p>
     * The returned old Cursor in <em>not</em> closed.
     *
     * @param newCursor The new Cursor to be used
     * @return Returns the previously set Cursor, or null if there wasn't one.
     * If the given new Cursor is the same instance as the previously set
     * Cursor, null is also returned.
     */
    public Cursor swapCursor(Cursor newCursor) {
        Log.d(TAG, "swapCursor: starts");
        if (newCursor == mCursor) {
            Log.d(TAG, "swapCursor: ends, returning null because cursor hasn't changed");
            return null;
        }

        final Cursor oldCursor = mCursor;
        mCursor = newCursor;
        if (newCursor != null) {
            // Notify the observers about the new cursor
            notifyDataSetChanged();
        } else {
            // Notify the observers about the lack of a data set
            notifyItemRangeRemoved(0, getItemCount());
        }
        Log.d(TAG, "swapCursor: ends, returning old cursor");
        return oldCursor;
    }

    public Cursor swapInnerCursors(Cursor newCursor) {
        Log.d(TAG, "swapInnerCursors: starts");
        Cursor oldCursor = null;
        for (InnerRecyclerViewAdapter adapter : mInnerAdapters) {
            oldCursor = adapter.swapCursor(newCursor);
        }
        mInnerCursor = newCursor;
        Log.d(TAG, "swapInnerCursors: ends");
        return oldCursor;
    }
}
