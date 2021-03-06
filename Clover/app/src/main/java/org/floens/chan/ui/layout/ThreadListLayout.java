/*
 * Clover - 4chan browser https://github.com/Floens/Clover/
 * Copyright (C) 2014  Floens
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.floens.chan.ui.layout;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.floens.chan.R;
import org.floens.chan.core.model.ChanThread;
import org.floens.chan.core.model.Loadable;
import org.floens.chan.core.model.Post;
import org.floens.chan.core.model.PostImage;
import org.floens.chan.core.presenter.ReplyPresenter;
import org.floens.chan.ui.adapter.PostAdapter;
import org.floens.chan.ui.cell.PostCell;
import org.floens.chan.ui.cell.ThreadStatusCell;
import org.floens.chan.ui.view.ThumbnailView;
import org.floens.chan.utils.AndroidUtils;
import org.floens.chan.utils.AnimationUtils;

import java.util.List;

import static org.floens.chan.utils.AndroidUtils.ROBOTO_MEDIUM;

/**
 * A layout that wraps around a {@link RecyclerView} to manage showing posts.
 */
public class ThreadListLayout extends LinearLayout implements ReplyLayout.ReplyLayoutCallback {
    private ReplyLayout reply;
    private TextView searchStatus;
    private RecyclerView recyclerView;
    private LinearLayoutManager linearLayoutManager;
    private PostAdapter postAdapter;
    private ChanThread showingThread;
    private ThreadListLayoutCallback callback;
    private boolean replyOpen;

    public ThreadListLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        reply = (ReplyLayout) findViewById(R.id.reply);
        reply.setCallback(this);

        searchStatus = (TextView) findViewById(R.id.search_status);
        searchStatus.setTypeface(ROBOTO_MEDIUM);

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        linearLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(linearLayoutManager);
    }

    public void setCallbacks(PostAdapter.PostAdapterCallback postAdapterCallback, PostCell.PostCellCallback postCellCallback, ThreadStatusCell.Callback statusCellCallback, ThreadListLayoutCallback callback) {
        this.callback = callback;
        postAdapter = new PostAdapter(recyclerView, postAdapterCallback, postCellCallback, statusCellCallback);
        recyclerView.setAdapter(postAdapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                // onScrolled can be called after cleanup()
                if (showingThread != null) {
                    showingThread.loadable.listViewIndex = Math.max(0, linearLayoutManager.findFirstVisibleItemPosition());
                }
            }
        });
    }

    public boolean onBack() {
        if (reply.onBack()) {
            return true;
        } else if (replyOpen) {
            openReply(false);
            return true;
        } else {
            return false;
        }
    }

    public void openReply(boolean open) {
        if (showingThread != null && replyOpen != open) {
            this.replyOpen = open;
            AnimationUtils.animateHeight(reply, replyOpen, getWidth(), 500, reply);
            if (open) {
                reply.focusComment();
            } else {
                AndroidUtils.hideKeyboard(reply);
            }
        }
    }

    public ReplyPresenter getReplyPresenter() {
        return reply.getPresenter();
    }

    public void showPosts(ChanThread thread, boolean initial) {
        showingThread = thread;
        if (initial) {
            reply.bindLoadable(showingThread.loadable);
            linearLayoutManager.scrollToPositionWithOffset(thread.loadable.listViewIndex, 0);
        }
        postAdapter.setThread(thread);
    }

    public void showError(String error) {
        postAdapter.showError(error);
    }

    public void showSearch(boolean show) {
        AnimationUtils.animateHeight(searchStatus, show);

        if (show) {
            searchStatus.setText(R.string.search_empty);
        } else {
            postAdapter.clearFilter();
        }
    }

    public void filterList(String query, List<Post> filter, boolean clearFilter, boolean setEmptyText, boolean hideKeyboard) {
        if (clearFilter) {
            postAdapter.clearFilter();
        }

        if (hideKeyboard) {
            AndroidUtils.hideKeyboard(this);
        }

        if (setEmptyText) {
            searchStatus.setText(R.string.search_empty);
        }

        if (query != null) {
            postAdapter.filterList(filter);
            searchStatus.setText(getContext().getString(R.string.search_results,
                    getContext().getResources().getQuantityString(R.plurals.posts, filter.size(), filter.size()), query));
        }
    }

    public void cleanup() {
        /*if (ChanBuild.DEVELOPER_MODE) {
            Pin pin = ChanApplication.getWatchManager().findPinByLoadable(showingThread.loadable);
            if (pin == null) {
                for (Post post : showingThread.posts) {
                    if (post.comment instanceof SpannedString) {
                        SpannedString commentSpannable = (SpannedString) post.comment;
                        PostLinkable[] linkables = commentSpannable.getSpans(0, commentSpannable.length(), PostLinkable.class);
                        for (PostLinkable linkable : linkables) {
                            ChanApplication.getRefWatcher().watch(linkable, linkable.key + " " + linkable.value);
                        }
                    }
                }
            }
        }*/

        postAdapter.cleanup();
        reply.cleanup();
        openReply(false);
        showSearch(false);
        showingThread = null;
    }

    public ThumbnailView getThumbnail(PostImage postImage) {
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();

        ThumbnailView thumbnail = null;
        for (int i = 0; i < layoutManager.getChildCount(); i++) {
            View view = layoutManager.getChildAt(i);
            if (view instanceof PostCell) {
                PostCell postView = (PostCell) view;
                Post post = postView.getPost();
                if (post.hasImage && post.imageUrl.equals(postImage.imageUrl)) {
                    thumbnail = postView.getThumbnailView();
                    break;
                }
            }
        }
        return thumbnail;
    }

    public void scrollTo(int position, boolean smooth) {
        if (smooth) {
            recyclerView.smoothScrollToPosition(position);
        } else {
            recyclerView.scrollToPosition(position);
        }
    }

    public void highlightPost(Post post) {
        postAdapter.highlightPost(post);
    }

    public void highlightPostId(String id) {
        postAdapter.highlightPostId(id);
    }

    @Override
    public void highlightPostNo(int no) {
        postAdapter.highlightPostNo(no);
    }

    @Override
    public void showThread(Loadable loadable) {
        callback.showThread(loadable);
    }

    @Override
    public void requestNewPostLoad() {
        callback.requestNewPostLoad();
    }

    public interface ThreadListLayoutCallback {
        void showThread(Loadable loadable);

        void requestNewPostLoad();
    }
}
