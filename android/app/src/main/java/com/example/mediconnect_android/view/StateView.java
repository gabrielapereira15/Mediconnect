package com.example.mediconnect_android.view;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.example.mediconnect_android.R;
import com.google.android.material.button.MaterialButton;

/**
 * The loading, empty and error states for a screen that fetches something.
 *
 * Every data-backed screen previously went straight from blank to populated,
 * with nothing in between: a tap did nothing visible for the length of the
 * request and then content appeared. Sitting this over the content view gives
 * all of them the same three states, so an empty list and a failed request no
 * longer look identical.
 *
 * Usage:
 *
 *   stateView.showLoading();
 *   Background.run(client::getDoctors,
 *       doctors -> { adapter.submit(doctors); stateView.showContentOrEmpty(doctors.isEmpty()); },
 *       error   -> stateView.showError(this::reload));
 */
public class StateView extends FrameLayout {

    private LinearLayout loading;
    private LinearLayout message;
    private ImageView icon;
    private TextView title;
    private TextView body;
    private MaterialButton action;

    /** The view this state sits in front of — hidden unless content is shown. */
    @Nullable
    private View contentView;

    @Nullable
    private ObjectAnimator pulse;

    public StateView(Context context) {
        this(context, null);
    }

    public StateView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StateView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.view_state, this, true);

        loading = findViewById(R.id.state_loading);
        message = findViewById(R.id.state_message);
        icon = findViewById(R.id.state_icon);
        title = findViewById(R.id.state_title);
        body = findViewById(R.id.state_body);
        action = findViewById(R.id.state_action);
    }

    /**
     * The view to show once content is available. StateView takes over
     * managing its visibility.
     */
    public void setContentView(@Nullable View view) {
        this.contentView = view;
    }

    /** Placeholder rows, gently pulsing so it reads as activity not breakage. */
    public void showLoading() {
        setVisibility(VISIBLE);
        loading.setVisibility(VISIBLE);
        message.setVisibility(GONE);
        setContentVisible(false);
        startPulse();
    }

    /** Hides the whole state view and reveals the content. */
    public void showContent() {
        stopPulse();
        setVisibility(GONE);
        setContentVisible(true);
    }

    /** Nothing went wrong, there is simply nothing to show yet. */
    public void showEmpty(@DrawableRes int iconRes, @StringRes int titleRes,
                          @StringRes int bodyRes) {
        showEmpty(iconRes, titleRes, bodyRes, 0, null);
    }

    /** An empty state that offers a way forward, e.g. "Book an appointment". */
    public void showEmpty(@DrawableRes int iconRes, @StringRes int titleRes,
                          @StringRes int bodyRes, @StringRes int actionRes,
                          @Nullable Runnable onAction) {
        stopPulse();
        setVisibility(VISIBLE);
        loading.setVisibility(GONE);
        message.setVisibility(VISIBLE);
        setContentVisible(false);

        icon.setImageResource(iconRes);
        title.setText(titleRes);
        body.setText(bodyRes);
        bindAction(actionRes, onAction);
    }

    /** The request failed. Always offers a retry, since that is the fix. */
    public void showError(@Nullable Runnable onRetry) {
        showError(R.string.state_error_title, R.string.state_error_body, onRetry);
    }

    public void showError(@StringRes int titleRes, @StringRes int bodyRes,
                          @Nullable Runnable onRetry) {
        stopPulse();
        setVisibility(VISIBLE);
        loading.setVisibility(GONE);
        message.setVisibility(VISIBLE);
        setContentVisible(false);

        icon.setImageResource(R.drawable.baseline_cloud_off_24);
        title.setText(titleRes);
        body.setText(bodyRes);
        bindAction(R.string.state_retry, onRetry);
    }

    /** Convenience for the common "loaded, but possibly nothing there" case. */
    public void showContentOrEmpty(boolean isEmpty, @DrawableRes int iconRes,
                                   @StringRes int titleRes, @StringRes int bodyRes) {
        if (isEmpty) {
            showEmpty(iconRes, titleRes, bodyRes);
        } else {
            showContent();
        }
    }

    private void bindAction(@StringRes int actionRes, @Nullable Runnable onAction) {
        if (actionRes == 0 || onAction == null) {
            action.setVisibility(GONE);
            action.setOnClickListener(null);
            return;
        }
        action.setVisibility(VISIBLE);
        action.setText(actionRes);
        action.setOnClickListener(v -> onAction.run());
    }

    private void setContentVisible(boolean visible) {
        if (contentView != null) {
            contentView.setVisibility(visible ? VISIBLE : GONE);
        }
    }

    private void startPulse() {
        if (pulse != null && pulse.isRunning()) {
            return;
        }
        pulse = ObjectAnimator.ofFloat(loading, View.ALPHA, 1f, 0.45f);
        pulse.setDuration(700);
        pulse.setRepeatMode(ValueAnimator.REVERSE);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.start();
    }

    private void stopPulse() {
        if (pulse != null) {
            pulse.cancel();
            pulse = null;
        }
        loading.setAlpha(1f);
    }

    @Override
    protected void onDetachedFromWindow() {
        // Otherwise the animator keeps a reference to a dead view hierarchy.
        stopPulse();
        super.onDetachedFromWindow();
    }
}
