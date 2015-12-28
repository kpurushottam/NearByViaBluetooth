package com.krp.social.nearby;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Kumar Purushottam on 26-12-2015.
 */
public class NearByRecyclerAdapter extends RecyclerView.Adapter<NearByRecyclerViewHolder> {

    private List<User> mListUsers;
    private OnNearByUserSelectListener userSelectListener;

    public NearByRecyclerAdapter(OnNearByUserSelectListener listener) {
        userSelectListener = listener;
    }

    @Override
    public NearByRecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return NearByRecyclerViewHolder.getInstance(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recycler_nearby, parent, false), userSelectListener);
    }

    @Override
    public void onBindViewHolder(NearByRecyclerViewHolder holder, int position) {
        User user = mListUsers.get(position);
        holder.setUser(user);
    }

    @Override
    public int getItemCount() {
        return mListUsers == null ? 0 : mListUsers.size();
    }

    public void addData(User user) {
        if(mListUsers == null) {
            mListUsers = new ArrayList<>();
        }
        mListUsers.add(user);
    }

    public void refreshDataSet(User... users) {
        if(mListUsers == null) {
            mListUsers = new ArrayList<>(users.length);
        }
        mListUsers.removeAll(mListUsers);

        int len = users.length;
        for(int i=0; i<len; i++) {
            mListUsers.add(users[i]);
        }
    }

    public void refreshDataSet(List<User> users) {
        if(mListUsers == null) {
            mListUsers = new ArrayList<>(users.size());
        }
        mListUsers.removeAll(mListUsers);
        mListUsers.addAll(users);
    }

    public void refreshDataSet() {
        if(mListUsers != null) {
            mListUsers.remove(mListUsers);
        }
        notifyDataSetChanged();
    }

    public interface OnNearByUserSelectListener {
        void onUserSelected(User user);
    }

    public List<User> getNearByUsers() {
        return mListUsers;
    }
}
