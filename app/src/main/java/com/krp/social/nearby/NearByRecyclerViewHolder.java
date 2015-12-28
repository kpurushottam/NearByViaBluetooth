package com.krp.social.nearby;

import android.graphics.BitmapFactory;
import android.media.Image;
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

    private NearByRecyclerViewHolder(
            View itemView, ImageView ivProfile, TextView tvName, TextView tvAge
    ) {
        super(itemView);

        mUserProfile = ivProfile;
        mUserName = tvName;
        mUserAge = tvAge;
    }

    public static NearByRecyclerViewHolder getInstance(View itemView) {
        return new NearByRecyclerViewHolder(itemView,
                (ImageView) itemView.findViewById(R.id.iv_profile),
                (TextView) itemView.findViewById(R.id.tv_user_name),
                (TextView) itemView.findViewById(R.id.tv_user_age));
    }

    public void setValues(String profileImageUrl, String username, String age) {
        mUserProfile.setImageBitmap(BitmapFactory.decodeFile(profileImageUrl));
        mUserName.setText(username);
        mUserAge.setText(String.format(mUserAge.getResources()
                .getString(R.string.profile_age), age));
    }
}
