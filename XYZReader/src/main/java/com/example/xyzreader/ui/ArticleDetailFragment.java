package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import timber.log.Timber;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "item_id";

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private int mDarkMutedColor;
    private int mLightMutedColor;
    private int mLightVibrantColor;

    private ImageView mPhotoView;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);
    private View mMetaBar;
    private TextView mTitleView;
    private TextView mBylineView;
    private TextView mBodyView;
    private Toolbar mToolbar;
    private CollapsingToolbarLayout mCollapsingBar;
    private FloatingActionButton mShareBtn;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        setHasOptionsMenu(true);
    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);

        mDarkMutedColor = ContextCompat.getColor(getActivity(), R.color.theme_primary_dark);
        mLightMutedColor = ContextCompat.getColor(getActivity(), R.color.theme_primary_dark);
        mLightVibrantColor = ContextCompat.getColor(getActivity(), R.color.theme_accent);

        mToolbar = mRootView.findViewById(R.id.toolbar);
        getActivityCast().setSupportActionBar(mToolbar);
        mCollapsingBar = mRootView.findViewById(R.id.detail_collapsing);
        mMetaBar = mRootView.findViewById(R.id.meta_bar);

        mPhotoView = mRootView.findViewById(R.id.photo);

        mTitleView = mRootView.findViewById(R.id.article_title);
        mBylineView = mRootView.findViewById(R.id.article_byline);
        mBylineView.setMovementMethod(new LinkMovementMethod());
        mBodyView = mRootView.findViewById(R.id.article_body);
        //mBodyView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Rosario-Regular.ttf"));


        mShareBtn = mRootView.findViewById(R.id.share_fab);
        mShareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));
            }
        });

        bindViews();
        updateStatusBarColor();

        return mRootView;
    }


    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Timber.e(ex.getMessage());
            Timber.i("passing today's date");
            return new Date();
        }
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        if (mCursor != null) {
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);
            mTitleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            updateByLineView();
            mBodyView.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY).replaceAll("(\r\n|\n)", "<br />")));
            String imageUrl = mCursor.getString(ArticleLoader.Query.PHOTO_URL);
            ImageLoaderHelper.getInstance(getActivity()).load(imageUrl, new ImageLoaderHelper.ImagePaletteLoaderCallback() {

                @Override
                public void onImagePaletteLoaded(Bitmap bitmap, Palette palette) {
                    if (!isAdded()) {
                        return;
                    }

                    mDarkMutedColor = palette.getDarkMutedColor(mDarkMutedColor);
                    mLightMutedColor = palette.getDarkMutedColor(mLightMutedColor);
                    mLightVibrantColor = palette.getLightVibrantColor(mLightVibrantColor);
                    mPhotoView.setImageBitmap(bitmap);
                    mMetaBar.setBackgroundColor(mDarkMutedColor);
                    mCollapsingBar.setContentScrimColor(mLightMutedColor);

                    updateStatusBarColor();
                }

                @Override
                public void onImagePaletteError(VolleyError error) {

                }
            });
        } else {
            mRootView.setVisibility(View.GONE);
            mTitleView.setText("N/A");
            mBylineView.setText("N/A" );
            mBodyView.setText("N/A");
        }
    }

    private void updateByLineView() {
        Date publishedDate = parsePublishedDate();
        if (!publishedDate.before(START_OF_EPOCH.getTime())) {
            mBylineView.setText(Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            publishedDate.getTime(),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by <font color='#ffffff'>"
                            + mCursor.getString(ArticleLoader.Query.AUTHOR)
                            + "</font>"));

        } else {
            // If date is before 1902, just show the string
            mBylineView.setText(Html.fromHtml(
                    outputFormat.format(publishedDate) + " by <font color='#ffffff'>"
                    + mCursor.getString(ArticleLoader.Query.AUTHOR)
                            + "</font>"));
        }
    }

    private void updateStatusBarColor() {
        if (isAdded()) {
            getActivityCast().updateStatusBarColor(mItemId, mDarkMutedColor);
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            updateStatusBarColor();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Timber.e("Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }
}
