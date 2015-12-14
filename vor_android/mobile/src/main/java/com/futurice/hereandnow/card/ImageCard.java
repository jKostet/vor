package com.futurice.hereandnow.card;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.futurice.cascade.util.RCLog;
import com.futurice.hereandnow.Constants;
import com.futurice.hereandnow.HereAndNowApplication;
import com.futurice.hereandnow.R;
import com.futurice.hereandnow.activity.ImageViewActivity;
import com.futurice.hereandnow.singleton.ModelSingleton;
import com.futurice.hereandnow.utils.FileUtils;
import com.futurice.hereandnow.utils.ImageUtils;
import com.futurice.scampiclient.HereAndNowService;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Card that matches the EIT demo card.
 *
 * @author teemuk
 */
public class ImageCard extends BaseCard {
    public static final int CARD_TYPE = 0;    // Needed for ListView recycling
    public static final String TAG = ImageCard.class.getName();
    @NonNull
    private String text = "";

    @NonNull
    private String mCardType = "";

    @NonNull
    private Uri imageUri = Uri.EMPTY;
    private String mImageBase64;

    @NonNull
    private Uri thumbnailUri = Uri.EMPTY;

    public ImageCard(@NonNull final String name, @NonNull final Context context) {
        this(name, HereAndNowService.generateUid(), context);
    }

    public ImageCard(final String name, final long uid, final Context context) {
        super(name, uid, context, R.layout.card_layout);
    }

    /**
     * Helper function for saving a local thumbnail of the image file.
     *
     * @param imageFile
     * @return
     */
    @NonNull
    public static Uri createThumbnail(@NonNull final Uri imageFile) {
        // For resource content we'll just use the main image as thumbnail
        if (imageFile.toString().startsWith("android.resource")) {
            return imageFile;
        }

        try {
            Context context = HereAndNowApplication.getStaticContext();
            final String filePath = FileUtils.getPath(context, imageFile);
            if (filePath == null) {
                throw new IllegalArgumentException("Can not find path to file: " + imageFile);
            }

            final File localImageFile = new File(filePath);
            int thumbnailSize = MediaStore.Video.Thumbnails.MICRO_KIND;
            final Bitmap bmThumbnail = ImageUtils.createImageThumbnail(filePath, thumbnailSize);

            if (bmThumbnail != null) {
                String path = Environment.getExternalStorageDirectory() + "/image_thumbnails/";
                final File directory = new File(path);
                if (!directory.exists() && !directory.mkdirs()) {
                    throw new IllegalStateException("Can not create external storage directory for thumbnails: " + imageFile);
                }

                final File file = new File(directory.getAbsolutePath() + "/" + localImageFile.getName() + ".jpg");
                final OutputStream outStream = new FileOutputStream(file);

                try {
                    bmThumbnail.compress(Bitmap.CompressFormat.JPEG, 90, outStream);
                    outStream.flush();
                } catch (NullPointerException e) {
                    RCLog.e(ImageCard.class.getSimpleName(), "Can not compress thumbnail", e);
                } finally {
                    outStream.close();
                }

                return Uri.fromFile(file);
            } else {
                RCLog.i(TAG, "Can not create bitmap: " + imageFile);
            }
        } catch (IOException e) {
            RCLog.e(BaseCard.class.getSimpleName(), "Problem writing thumbnail", e);
        }

        return Uri.EMPTY;
    }

    @Override
    public void updateView(@NonNull View view) {
        super.updateView(view);

        final Resources resources = context.getResources();
        final TextView cardTextView = (TextView) view.findViewById(R.id.card_text);
        final TextView cardAuthorTextView = (TextView) view.findViewById(R.id.card_author);
        final LinearLayout likesBarLinearLayout = (LinearLayout) view.findViewById(R.id.likes_bar);
        final TextView likesTextView = (TextView) view.findViewById(R.id.likes_text);
        final ImageView cardLikeButton = (ImageView) view.findViewById(R.id.card_like_button);
        final LinearLayout commentsBar = (LinearLayout) view.findViewById(R.id.comments_bar);
        final ImageView cardImageView = (ImageView) view.findViewById(R.id.card_image);

        final String date = DateUtils.getRelativeDateTimeString(context,
                this.getDate().getTime(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.WEEK_IN_MILLIS,
                0).toString();

        if (getImageUri() != Uri.EMPTY) {
            Picasso.with(context)
                    .load(getImageUri())
                    .resize(500, 500) // Downscale huge images first
                    .onlyScaleDown()
                    .centerInside() // To keep the aspect ratio on resize
                    .into(cardImageView);
            cardImageView.setOnClickListener(v -> {
                Intent viewImageIntent = new Intent(context, ImageViewActivity.class);
                viewImageIntent.putExtra(ImageViewActivity.IMAGE_URI, getImageUri().toString());
                viewImageIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // FIXME Correct?
                context.startActivity(viewImageIntent);
            });
        } else if (mImageBase64 != null){
            cardImageView.setImageBitmap(getImageBitmap());
            cardImageView.setOnClickListener(v -> {
                Intent viewImageIntent = new Intent(context, ImageViewActivity.class);
                viewImageIntent.putExtra(Constants.TYPE_KEY, getCardType());
                viewImageIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // FIXME Correct?
                context.startActivity(viewImageIntent);
            });
        }

        cardTextView.setText(getText());
//        cardAuthorTextView.setText(resources.getString(R.string.card_author, getAuthor(), date));
        cardAuthorTextView.setText(date);
        setLikeBar(resources, likesBarLinearLayout, likesTextView);
        setLikeButton(cardLikeButton, commentsBar);
    }

    private void setLikeBar(
            Resources resources,
            LinearLayout likesBarLinearLayout,
            TextView likesTextView) {
        int likes = ModelSingleton.instance().getLikes(this);
        if (likes > 0) {
            likesBarLinearLayout.setVisibility(View.VISIBLE);
            String likesText = resources.getQuantityString(R.plurals.number_of_likes, likes, likes);
            String likesList = ModelSingleton.instance().getLikesList(this);
            likesTextView.setText(resources.getString(R.string.liked, likesText, likesList));
        } else {
            likesBarLinearLayout.setVisibility(View.GONE);
        }
    }

    private void setLikeButton(ImageView cardLikeButton, LinearLayout commentsBar) {
        int like = userAlreadyLikes() ? R.drawable.card_navbar_liked : R.drawable.card_navbar_like;
        Drawable drawable = ContextCompat.getDrawable(context, like);
        cardLikeButton.setImageDrawable(drawable);

        List<Comment> comments = ModelSingleton.instance().getCommentsList(this);
        if (comments != null && !comments.isEmpty()) {
            commentsBar.setVisibility(View.VISIBLE);
            populateCommentsBar(comments, commentsBar);
        } else {
            commentsBar.setVisibility(View.GONE);
        }
    }

    @Override // BaseCard
    public int getType() {
        return CARD_TYPE;
    }

    @Override
    public boolean matchesSearch(@NonNull final String search) {
        return text.toLowerCase().contains(search.toLowerCase());
    }

    @NonNull
    public String getText() {
        return text;
    }

    public void setText(@NonNull final String text) {
        this.text = text;
    }

    @NonNull
    public String getCardType() {
        return mCardType;
    }

    public void setCardType(@NonNull final String cardType) {
        mCardType = cardType;
    }

    @NonNull
    public Uri getImageUri() {
        return imageUri;
    }

    public void setImageUri(@NonNull final Uri imageUri) {
        this.imageUri = imageUri;
        this.setThumbnailUri(createThumbnail(this.imageUri));
    }

    @NonNull
    public Bitmap getImageBitmap() {
        return FileUtils.base64ToBitmap(mImageBase64);
    }

    public void setImageBase64(@NonNull final String base64) {
        mImageBase64 = base64;
    }

    @NonNull
    public Uri getThumbnailUri() {
        return thumbnailUri;
    }

    public void setThumbnailUri(@NonNull final Uri thumbnailUri) {
        this.thumbnailUri = thumbnailUri;
    }

    public void populateCommentsBar(@NonNull final List<Comment> comments,
                                    @NonNull LinearLayout commentsBar) {
        Log.d(TAG, "Populating the comment bar: " + comments.size());
        commentsBar.removeAllViews();
        for (final Comment comment : comments) {
            final LinearLayout commentItem =
                    (LinearLayout) this.inflateView(R.layout.comment_item, null);

            commentItem.setTag(comment);
            TextView name = (TextView) commentItem.findViewById(R.id.comment_name);
            TextView date = (TextView) commentItem.findViewById(R.id.comment_date);
            TextView text = (TextView) commentItem.findViewById(R.id.comment_text);
            ImageView deleteButton = (ImageView) commentItem.findViewById(R.id.delete_button);
            if (ModelSingleton.instance().myIdTag.get().equalsIgnoreCase(comment.userIdTag)) {
                deleteButton.setVisibility(View.VISIBLE);
                deleteButton.setOnClickListener(v -> {
                    commentItem.setVisibility(View.GONE);
                    deleteLocalUserComment(comment);
                });
            } else {
                deleteButton.setVisibility(View.GONE);
            }
            name.setText(comment.userTag);
            Date timestamp = new Date();
            timestamp.setTime(comment.getTimestamp());
            date.setText(new SimpleDateFormat("dd.MM H.mm").format(timestamp));
            text.setText(comment.getText());
                /*
                commentItem.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    }
                });*/

            commentsBar.addView(commentItem);
        }
    }
}
