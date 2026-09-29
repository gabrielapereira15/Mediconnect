package com.example.mediconnect_android.activity;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.example.mediconnect_android.R;
import com.example.mediconnect_android.databinding.ActivityWelcomeBinding;
import com.example.mediconnect_android.databinding.ViewWelcomeTileBinding;
import com.example.mediconnect_android.util.ActivityUtils;

/**
 * The first screen (board P01).
 *
 * It used to ask for an email, a terms checkbox and a link to a page of
 * placeholder Latin, and before that whether you were a patient or a
 * clinic — a question no patient can answer wrongly, because the clinic's
 * side is a website. It says what the app is for and asks for nothing;
 * signing in is the next screen.
 */
public class WelcomeActivity extends AppCompatActivity {

    private ActivityWelcomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWelcomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        bindTile(binding.tileBook, R.drawable.ic_calendar_check, R.string.welcome_tile_book,
                R.color.md_primary, R.color.md_on_primary);
        bindTile(binding.tileForms, R.drawable.ic_form, R.string.welcome_tile_forms,
                R.color.md_primary_container, R.color.md_on_primary_container);
        bindTile(binding.tileRecord, R.drawable.ic_record, R.string.welcome_tile_record,
                R.color.md_rating_container, R.color.md_on_rating_container);

        binding.btnGetStarted.setOnClickListener(
                v -> ActivityUtils.startActivity(this, SignInActivity.class));
    }

    /**
     * Fills in one tile.
     *
     * The three differ only in colour, so the shape is one drawable tinted
     * here; three near-identical drawables would be three more places to
     * keep in step with the palette.
     */
    private void bindTile(ViewWelcomeTileBinding tile, @DrawableRes int iconRes,
                          @StringRes int labelRes, @ColorRes int fill, @ColorRes int onFill) {
        tile.tileIcon.setImageResource(iconRes);
        tile.tileLabel.setText(labelRes);
        tile.tileIcon.setImageTintList(ContextCompat.getColorStateList(this, onFill));
        tile.tileLabel.setTextColor(ContextCompat.getColor(this, onFill));
        tint(tile.tileRoot, fill);
    }

    private void tint(View view, @ColorRes int colour) {
        Drawable background = ContextCompat.getDrawable(this, R.drawable.tile_welcome);
        if (background == null) {
            return;
        }
        Drawable tinted = DrawableCompat.wrap(background.mutate());
        DrawableCompat.setTint(tinted, ContextCompat.getColor(this, colour));
        view.setBackground(tinted);
    }
}
