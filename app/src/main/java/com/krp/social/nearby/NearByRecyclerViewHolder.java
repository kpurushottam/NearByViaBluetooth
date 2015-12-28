package com.krp.social.nearby;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by Kumar Purushottam on 26-12-2015.
 */
public class NearByRecyclerViewHolder extends RecyclerView.ViewHolder {
    private ImageView mUserProfile;
    private TextView mUserName, mUserAge;

    private User mUser;

    private NearByRecyclerViewHolder(
            View itemView, ImageView ivProfile, TextView tvName, TextView tvAge,
            final NearByRecyclerAdapter.OnNearByUserSelectListener listener
    ) {
        super(itemView);

        mUserProfile = ivProfile;
        mUserName = tvName;
        mUserAge = tvAge;

        if(listener != null) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onUserSelected(mUser);
                }
            });
        }
    }

    public static NearByRecyclerViewHolder getInstance(
            View itemView, NearByRecyclerAdapter.OnNearByUserSelectListener listener
    ) {
        return new NearByRecyclerViewHolder(itemView,
                (ImageView) itemView.findViewById(R.id.iv_profile),
                (TextView) itemView.findViewById(R.id.tv_user_name),
                (TextView) itemView.findViewById(R.id.tv_user_age), listener);
    }

    public void setUser(User user) {
        mUser = user;
        mUserName.setText(user.deviceName);
        mUserAge.setText(String.format(mUserAge.getResources()
                .getString(R.string.profile_age), user.deviceAddress));
    }
}
