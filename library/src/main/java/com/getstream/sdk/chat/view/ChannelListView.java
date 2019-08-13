package com.getstream.sdk.chat.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.getstream.sdk.chat.R;
import com.getstream.sdk.chat.adapter.ChannelListItemAdapter;
import com.getstream.sdk.chat.model.Channel;
import com.getstream.sdk.chat.model.Event;
import com.getstream.sdk.chat.rest.Message;
import com.getstream.sdk.chat.rest.User;
import com.getstream.sdk.chat.rest.core.ChatEventHandler;
import com.getstream.sdk.chat.rest.response.ChannelUserRead;
import com.getstream.sdk.chat.utils.BaseStyle;
import com.getstream.sdk.chat.viewmodel.ChannelListViewModel;

import java.util.List;

public class ChannelListView extends RecyclerView {
    final String TAG = ChannelListView.class.getSimpleName();

    private Style style;

    // our connection to the channel scope
    private ChannelListViewModel viewModel;
    private UserClickListener userClickListener;
    private ChannelClickListener channelClickListener;
    private ChannelClickListener channelLongClickListener;
    private ChannelListItemAdapter adapter;

    public ChannelListView(Context context) {
        super(context);
        this.setHasFixedSize(true);
        this.setLayoutManager(new LinearLayoutManager(context));
    }

    public ChannelListView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.setHasFixedSize(true);
        this.setLayoutManager(new LinearLayoutManager(context));
        this.parseAttr(context, attrs);
    }

    public ChannelListView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.setHasFixedSize(true);
        this.setLayoutManager(new LinearLayoutManager(context));
        this.parseAttr(context, attrs);
    }

    private void parseAttr(Context context, @Nullable AttributeSet attrs) {
        // parse the attributes
        style = new Style(context, attrs);
    }

    public void setViewModel(ChannelListViewModel viewModel, LifecycleOwner lifecycleOwner, ChannelListItemAdapter adapter) {
        this.viewModel = viewModel;

        // connect the adapter
        this.adapter = adapter;
        this.setAdapterWithStyle(adapter);

        // connect the viewHolder on click listener...
        adapter.setChannelClickListener(this.channelClickListener);
        adapter.setChannelLongClickListener(this.channelLongClickListener);
        adapter.setUserClickListener(this.userClickListener);

        // listen to events
        viewModel.client().addEventHandler(new ChatEventHandler() {
            @Override
            public void onMessageNew(Event event) {
                Message lastMessage = event.getChannel().getChannelState().getLastMessage();
                Log.i(TAG, "onMessageNew Event: Received a new message with text: " + event.getMessage().getText());
                Log.i(TAG, "onMessageNew State: Last message is: " + lastMessage.getText());
                Log.i(TAG, "onMessageNew Unread Count " + event.getChannel().getChannelState().getCurrentUserUnreadMessageCount());

                new Handler(Looper.getMainLooper()).post(new Runnable() {

                    @Override
                    public void run() {
                        adapter.upsertChannel(event.getChannel());
                    }
                });

            }

            @Override
            public void onChannelDeleted(Event event) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {

                    @Override
                    public void run() {
                        adapter.deleteChannel(event.getChannel());
                    }
                });
            }

            @Override
            public void onChannelUpdated(Event event) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {

                    @Override
                    public void run() {
                        adapter.upsertChannel(event.getChannel());
                    }
                });
            }

            @Override
            public void onMessageRead(Event event) {
                Log.i(TAG, "Event: Message read by user " + event.getUser().getName());
                List<ChannelUserRead> reads = event.getChannel().getChannelState().getLastMessageReads();
                if (reads.size() > 0) {
                    Log.i(TAG, "State: Message read by user " + reads.get(0).getUser().getName());
                }

                new Handler(Looper.getMainLooper()).post(new Runnable() {

                    @Override
                    public void run() {
                        adapter.upsertChannel(event.getChannel());
                    }
                });
            }
        });

        // TODO: this approach is not great for performance
        viewModel.getChannels().observe(lifecycleOwner, channels -> {
            Log.i(TAG, "Oberseve found this many channels: " + channels.size());
            adapter.replaceChannels(channels);
        });
    }

    public void setViewModel(ChannelListViewModel viewModel, LifecycleOwner lifecycleOwner) {
        // default adapter...
        adapter = new ChannelListItemAdapter(getContext());

        this.setViewModel(viewModel, lifecycleOwner, adapter);
    }

    public void setOnUserClickListener(UserClickListener l) {
        this.userClickListener = l;
        if (adapter != null) {
            adapter.setUserClickListener(l);
        }
    }

    public void setOnChannelClickListener(ChannelClickListener l) {
        this.channelClickListener = l;
        if (adapter != null) {
            adapter.setChannelClickListener(l);
        }
    }

    public void setOnLongClickListener(ChannelClickListener l) {
        this.channelLongClickListener = l;
        if (adapter != null) {
            adapter.setChannelLongClickListener(l);
        }
    }

    @Override
    public void setAdapter(Adapter adapter) {
        throw new IllegalArgumentException("Use setAdapterWithStyle instead please");
    }

    // set the adapter and apply the style.
    public void setAdapterWithStyle(ChannelListItemAdapter adapter) {
        super.setAdapter(adapter);
        adapter.setStyle(style);

        this.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();

                if (linearLayoutManager != null) {

                    int lastVisible = linearLayoutManager.findLastCompletelyVisibleItemPosition();
                    Boolean reachedTheEnd = lastVisible == adapter.getItemCount() - 1;
                    // Log.i(TAG, String.format("Last visible is %d out of %d", lastVisible, adapter.getItemCount()));
                    // the viewmodel ensures that we only load once..
                    if (reachedTheEnd) {
                        viewModel.loadMore();
                    }


                }

            }
        });

    }

    public class Style extends BaseStyle {
        final String TAG = Style.class.getSimpleName();
        // dimensions
        private float avatarWidth;
        private float avatarHeight;
        private float initialsTextSize;
        private int dateTextSize;
        private int titleTextSize;
        private int messageTextSize;
        // colors
        private int initialsTextColor;
        private int titleTextColor;
        private int unreadTitleTextColor;
        private int messageTextColor;
        private int unreadMessageTextColor;
        private int dateTextColor;
        // styles
        private int initialsTextStyle;
        private int titleTextStyle;
        private int unreadTitleTextStyle;
        private int messageTextStyle;
        private int unreadMessageTextStyle;

        // layouts
        public @LayoutRes int channelPreviewLayout;


        public Style(Context c, AttributeSet attrs) {
            this.setContext(c);
            TypedArray a = c.obtainStyledAttributes(attrs,
                    R.styleable.ChannelListView, 0, 0);

            avatarWidth = a.getDimension(R.styleable.ChannelListView_avatarWidth, c.getResources().getDimensionPixelSize(R.dimen.stream_channel_avatar_height));
            avatarHeight = a.getDimension(R.styleable.ChannelListView_avatarHeight, c.getResources().getDimensionPixelSize(R.dimen.stream_channel_avatar_width));
            initialsTextSize = a.getDimension(R.styleable.ChannelListView_initialsTextSize, c.getResources().getDimensionPixelSize(R.dimen.stream_channel_initials));
            initialsTextColor = a.getColor(R.styleable.ChannelListView_initialsTextColor, c.getResources().getColor(R.color.stream_channel_initials));
            initialsTextStyle = a.getInt(R.styleable.ChannelListView_initialsTextStyle, Typeface.NORMAL);

            dateTextSize = a.getDimensionPixelSize(R.styleable.ChannelListView_dateTextSize, -1);
            titleTextSize = a.getDimensionPixelSize(R.styleable.ChannelListView_titleTextSize, -1);
            messageTextSize = a.getDimensionPixelSize(R.styleable.ChannelListView_messageTextSize, -1);

            titleTextColor = a.getColor(R.styleable.ChannelListView_titleTextColor, this.getColor(R.color.black));
            unreadTitleTextColor = a.getColor(R.styleable.ChannelListView_unreadTitleTextColor, this.getColor(R.color.black));
            messageTextColor = a.getColor(R.styleable.ChannelListView_titleTextColor, this.getColor(R.color.gray_dark));
            unreadMessageTextColor = a.getColor(R.styleable.ChannelListView_titleTextColor, this.getColor(R.color.black));
            dateTextColor = a.getColor(R.styleable.ChannelListView_dateTextColor, -1);

            titleTextStyle = a.getInt(R.styleable.ChannelListView_titleTextStyleChannel, Typeface.BOLD);
            unreadTitleTextStyle = a.getInt(R.styleable.ChannelListView_unreadTitleTextStyle, Typeface.BOLD);
            messageTextStyle = a.getInt(R.styleable.ChannelListView_messageTextStyle, Typeface.NORMAL);
            unreadMessageTextStyle = a.getInt(R.styleable.ChannelListView_unreadMessageTextStyle, Typeface.BOLD);

            channelPreviewLayout = a.getResourceId(R.styleable.ChannelListView_channelPreviewLayout, R.layout.list_item_channel);

            a.recycle();
        }

        public float getAvatarWidth() {
            return avatarWidth;
        }

        public float getAvatarHeight() {
            return avatarHeight;
        }

        public float getInitialsTextSize() {
            return initialsTextSize;
        }

        public int getDateTextSize() {
            return dateTextSize;
        }

        public int getTitleTextSize() {
            return titleTextSize;
        }

        public int getMessageTextSize() {
            return messageTextSize;
        }

        public int getInitialsTextColor() {
            return initialsTextColor;
        }

        public int getTitleTextColor() {
            return titleTextColor;
        }

        public int getUnreadTitleTextColor() {
            return unreadTitleTextColor;
        }

        public int getMessageTextColor() {
            return messageTextColor;
        }

        public int getUnreadMessageTextColor() {
            return unreadMessageTextColor;
        }

        public int getDateTextColor() {
            return dateTextColor;
        }

        public int getInitialsTextStyle() {
            return initialsTextStyle;
        }

        public int getTitleTextStyle() {
            return titleTextStyle;
        }

        public int getUnreadTitleTextStyle() {
            return unreadTitleTextStyle;
        }

        public int getMessageTextStyle() {
            return messageTextStyle;
        }

        public int getUnreadMessageTextStyle() {
            return unreadMessageTextStyle;
        }
    }


    public interface UserClickListener {
        void onClick(User user);
    }

    public interface ChannelClickListener {
        void onClick(Channel channel);
    }





}
